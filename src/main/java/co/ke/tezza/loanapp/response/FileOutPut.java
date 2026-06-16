package co.ke.tezza.loanapp.response;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileOutPut {
	private String fileOutputUrl;
	private String fileName;
	private String filePath;
	private FileOutputStream fileOutputStream;
	private FileInputStream fileInputStream;

}
