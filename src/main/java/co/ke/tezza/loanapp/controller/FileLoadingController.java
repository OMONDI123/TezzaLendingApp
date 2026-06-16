package co.ke.tezza.loanapp.controller;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.util.Utils;

@RestController
@RequestMapping("/file")
@CrossOrigin(origins = "*")
public class FileLoadingController {
	@Autowired
	private Utils utils;

	@GetMapping("/{orgId}/{filename:.+}")
	public ResponseEntity<Resource> serveFile(@PathVariable Long orgId,
	                                          @PathVariable String filename) throws MalformedURLException {
		// Check multiple possible locations in order
		Path filePath = null;
		MADSysConfig orgConf= utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, orgId);
		String orgFileUploadPath =orgConf != null
						? orgConf.getDocumentUploadDir()
						: "/var/www/html/documents/uploads";
		String orgFileDownloadPath=orgConf!=null?orgConf.getDownloadPath():null;
		// 1. First check letterhead uploads directory
		filePath = Paths.get(orgFileUploadPath, filename);
		if (!Files.exists(filePath)) {
			// 2. Check invoices directory in organisation file download paths.
			filePath=Paths.get(orgFileDownloadPath,filename);
			if(!Files.exists(filePath)) {
				// 3. Check invoices directory in user home
				filePath = Paths.get(System.getProperty("user.home"), "invoices", filename);
				if (!Files.exists(filePath)) {
					// 4. Check /tmp directory (fallback for Render)
					filePath = Paths.get("/tmp", filename);
					if (!Files.exists(filePath)) {
						return ResponseEntity.notFound().build();
					}
				}
			}
			
		}

		UrlResource resource = new UrlResource(filePath.toUri());

		// Try to determine content type automatically
		String contentType = null;
		try {
			contentType = Files.probeContentType(filePath);
		} catch (Exception e) {
			// If auto-detection fails, fall back to extension-based detection
			contentType = getContentTypeFromExtension(filename);
		}

		// Default to octet-stream if still unknown
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType(contentType)).body(resource);
	}

	/**
	 * Fallback method to determine content type from file extension
	 */
	private String getContentTypeFromExtension(String filename) {
		String extension = getFileExtension(filename).toLowerCase();

		switch (extension) {
		case "pdf":
			return "application/pdf";
		case "xlsx":
		case "xls":
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		case "csv":
			return "text/csv";
		case "txt":
			return "text/plain";
		case "jpg":
		case "jpeg":
			return "image/jpeg";
		case "png":
			return "image/png";
		case "gif":
			return "image/gif";
		case "bmp":
			return "image/bmp";
		case "webp":
			return "image/webp";
		case "zip":
			return "application/zip";
		default:
			return "application/octet-stream";
		}
	}

	private String getFileExtension(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		return (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) ? filename.substring(lastDotIndex + 1) : "";
	}
}