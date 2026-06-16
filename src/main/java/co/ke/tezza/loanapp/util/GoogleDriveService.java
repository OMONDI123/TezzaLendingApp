package co.ke.tezza.loanapp.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${google.drive.oauth.client.secret.file:}")
    private String clientSecretFile;

    @Value("${google.drive.oauth.refresh.token:}")
    private String refreshToken;

    @Value("${google.drive.folder.name:DB_BACKUPS}")
    private String folderName;

    private Drive driveService;

    @PostConstruct
    public void init() {
        System.out.println("Initializing GoogleDriveService...");
        logger.info("Client secret file: {}", clientSecretFile);
        logger.info("Refresh token provided: {}", refreshToken != null && !refreshToken.isEmpty());

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            System.err.println("No refresh token provided. Uploads disabled.");
            return;
        }
        if (clientSecretFile == null || clientSecretFile.trim().isEmpty()) {
            System.err.println("No OAuth client secret file provided. Uploads disabled.");
            return;
        }

        try {
            // Load client secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                    new FileReader(clientSecretFile));
            String clientId = clientSecrets.getDetails().getClientId();
            String clientSecret = clientSecrets.getDetails().getClientSecret();
            System.out.println("Loaded client ID: " + clientId);
            logger.info("Loaded client ID: {}", clientId);

            // Build credential with refresh token
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientId, clientSecret)
                    .build()
                    .setRefreshToken(refreshToken);

            // Refresh token
            boolean refreshed = credential.refreshToken();
            if (!refreshed) {
                throw new RuntimeException("Failed to refresh token. Check credentials and token.");
            }
            System.out.println("Access token refreshed successfully.");

            // Build Drive service
            driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    credential)
                    .setApplicationName("Database Backup Utility")
                    .build();

            System.out.println("Google Drive OAuth service initialized successfully.");
            logger.info("Google Drive OAuth service initialized.");
        } catch (Exception e) {
            System.err.println("Failed to initialize Google Drive OAuth service: " + e.getMessage());
            e.printStackTrace();
            logger.error("Initialization failed", e);
        }
    }
    public String uploadFile(java.io.File file) {
        System.out.println("uploadFile called for: " + file.getAbsolutePath());
        if (driveService == null) {
            System.err.println("Drive service not available.");
            return null;
        }

        try {
            System.out.println("Finding or creating folder: " + folderName);
            String folderId = findOrCreateFolder(folderName);
            if (folderId == null) {
                System.err.println("Could not find or create folder: " + folderName);
                return null;
            }
            System.out.println("Folder ID: " + folderId);

            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));

            FileContent mediaContent = new FileContent("application/octet-stream", file);
            Drive.Files.Create request = driveService.files().create(fileMetadata, mediaContent);
            request.setFields("id");

            // Get the uploader and set a progress listener
            MediaHttpUploader uploader = request.getMediaHttpUploader();
            uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
                private long lastLogged = 0;
                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    long bytesUploaded = uploader.getNumBytesUploaded();
                    double progress = uploader.getProgress();
                    // Log every 5% or when complete
                    if (progress == 1.0 || (int)(progress * 20) > lastLogged) {
                        long total = file.length();
                        System.out.printf("Upload progress: %.2f%% (%d/%d bytes)%n", 
                                          progress * 100, bytesUploaded, total);
                        lastLogged = (int)(progress * 20);
                    }
                }
            });

            System.out.println("Attempting to upload file...");
            File uploadedFile = request.execute();

            System.out.println("Upload successful. File ID: " + uploadedFile.getId());
            return uploadedFile.getId();
        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String findOrCreateFolder(String folderName) throws IOException {
        String query = "name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        List<File> folders = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (folders != null && !folders.isEmpty()) {
            return folders.get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }
}