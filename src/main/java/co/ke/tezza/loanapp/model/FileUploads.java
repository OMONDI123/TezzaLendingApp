package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploads {
	private String fileName;
	private String fullFilePath;
	private String mimeType;
	private long fileSize;
	private String base64File;

}
