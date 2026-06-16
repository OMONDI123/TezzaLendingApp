package co.ke.tezza.loanapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DatabaseBackupUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupUtil.class);

    private final DataSource dataSource;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String backupDirectory;
    private final GoogleDriveService googleDriveService;

    public DatabaseBackupUtil(DataSource dataSource,
                              @Value("${spring.datasource.url}") String dbUrl,
                              @Value("${spring.datasource.username}") String dbUsername,
                              @Value("${spring.datasource.password}") String dbPassword,
                              @Value("${backup.directory:#{systemProperties['user.home']}}") String backupDirectory,
                              GoogleDriveService googleDriveService) {
        this.dataSource = dataSource;
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.backupDirectory = new File(backupDirectory, "DB_BACKUPS").getAbsolutePath();
        this.googleDriveService = googleDriveService;
        logger.info("Backup directory set to: {}", this.backupDirectory);
    }

    //@Scheduled(fixedDelay = 3000) 
    public ResponseEntity<?> backup() throws Exception {
        System.out.println("\n=== Starting database backup ===");
        logger.info("Starting database backup");
        String message=null;
        int code=500;

        ConnectionParams params = parseJdbcUrl(dbUrl);
        params.user = dbUsername;
        params.password = dbPassword;

        String dbType = extractDbTypeFromUrl(dbUrl);
        String extension;
        if ("postgresql".equals(dbType)) {
            extension = "dmp";
        } else if ("mysql".equals(dbType)) {
            extension = "sql";
        } else {
            throw new UnsupportedOperationException("Unsupported database type from URL: " + dbType);
        }

        String dateStr = LocalDate.now().toString();
        String filename = params.database + "-" + dateStr + "." + extension;

        File backupDir = new File(backupDirectory);
        if (backupDir.exists() && !backupDir.isDirectory()) {
            throw new IOException("Backup path exists but is not a directory: " + backupDir);
        }
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new IOException("Cannot create backup directory: " + backupDir);
        }
        File outputFile = new File(backupDir, filename);

        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase();
            logger.info("Database product: {}", dbProduct);

            if ((dbType.equals("postgresql") && !dbProduct.contains("postgresql")) ||
                    (dbType.equals("mysql") && !dbProduct.contains("mysql"))) {
                logger.warn("Database product from metadata ({}) does not match URL type ({})", dbProduct, dbType);
                message="Database product from metadata ("+dbProduct+") does not match URL type ("+dbType+").";
                
            }

            List<String> command;
            if (dbProduct.contains("postgresql")) {
                System.out.println("Taking PostgreSQL backup...");
                command = buildPgDumpCommand(params, outputFile);
                executeCommandWithEnv(command, "PGPASSWORD", dbPassword);
            } else if (dbProduct.contains("mysql")) {
                System.out.println("Taking MySQL backup...");
                command = buildMysqlDumpCommandWithTempCnf(params, outputFile);
                executeCommand(command);
            } else {
                throw new UnsupportedOperationException("Unsupported database type: " + dbProduct);
            }

            System.out.println("Backup completed: " + outputFile.getAbsolutePath());
            logger.info("Backup completed: {}", outputFile.getAbsolutePath());

            uploadToDrive(outputFile);
            System.out.println("=== Backup and upload finished ===\n");
            message="Database has been backed up and uploaded to google drive successfully.";
            code=200;
        } catch (SQLException e) {
            logger.error("Database connection or metadata error", e);
            throw e;
        } catch (IOException | InterruptedException e) {
            logger.error("Backup process failed", e);
            throw e;
        }
		return new ResponseEntity<>(message,code,null);
    }

   
    private List<String> buildPgDumpCommand(ConnectionParams params, File outputFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add("pg_dump");
        cmd.add("--host=" + params.host);
        cmd.add("--port=" + params.port);
        cmd.add("--username=" + params.user);
        cmd.add("--dbname=" + params.database);
        cmd.add("--format=custom");
        cmd.add("--file=" + outputFile.getAbsolutePath());
        return cmd;
    }

    private List<String> buildMysqlDumpCommandWithTempCnf(ConnectionParams params, File outputFile) throws IOException {
        File tempCnf = File.createTempFile("mysql-", ".cnf");
        tempCnf.deleteOnExit();

        if (!tempCnf.setReadable(false, false) || !tempCnf.setWritable(false, false)) {
            logger.warn("Could not set restrictive permissions on temporary .cnf file");
        }
        if (!tempCnf.setReadable(true, true) || !tempCnf.setWritable(true, true)) {
            logger.warn("Could not set owner permissions on temporary .cnf file");
        }

        try (PrintWriter writer = new PrintWriter(tempCnf)) {
            writer.println("[client]");
            writer.println("user=" + params.user);
            writer.println("password='" + escapeForCnf(params.password) + "'");
            writer.println("host=" + params.host);
            writer.println("port=" + params.port);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("--defaults-extra-file=" + tempCnf.getAbsolutePath());
        cmd.add("--databases");
        cmd.add(params.database);
        cmd.add("--result-file=" + outputFile.getAbsolutePath());
        return cmd;
    }

    private String escapeForCnf(String password) {
        if (password == null) return "";
        return password.replace("'", "''");
    }

    private void executeCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        logger.debug("Executing command: {}", String.join(" ", command));

        Process process = pb.start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + output);
        }
    }

    private void executeCommandWithEnv(List<String> command, String envVar, String envValue)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put(envVar, envValue);
        pb.redirectErrorStream(true);
        logger.debug("Executing command with environment variable {} set", envVar);

        Process process = pb.start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + output);
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private ConnectionParams parseJdbcUrl(String url) throws SQLException {
        ConnectionParams params = new ConnectionParams();
        Pattern pattern = Pattern.compile("jdbc:(postgresql|mysql)://([^:/]+):?(\\d+)?/([^?]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            params.host = matcher.group(2);
            String portStr = matcher.group(3);
            String dbType = matcher.group(1);
            params.port = (portStr != null) ? portStr : (dbType.equals("postgresql") ? "5432" : "3306");
            params.database = matcher.group(4);
        } else {
            throw new SQLException("Unable to parse JDBC URL: " + url);
        }
        return params;
    }

    private String extractDbTypeFromUrl(String url) throws SQLException {
        Pattern pattern = Pattern.compile("jdbc:(postgresql|mysql)://");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new SQLException("Unable to extract database type from URL: " + url);
    }

    private void uploadToDrive(File outputFile) {
        if (googleDriveService != null) {
            try {
                String fileId = googleDriveService.uploadFile(outputFile);
                if (fileId != null) {
                    System.out.println("Upload successful.");
                } else {
                    System.err.println("Upload failed (null response).");
                }
            } catch (Exception e) {
                System.err.println("Upload failed: " + e.getMessage());
                logger.error("Upload error", e);
            }
        } else {
            logger.warn("Google Drive service not configured; skipping upload.");
        }
    }

    private static class ConnectionParams {
        String host;
        String port;
        String database;
        String user;
        String password;
    }
}