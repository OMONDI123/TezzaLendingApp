package co.ke.tezza.loanapp.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileOutPutResponse {
    private String fileOutputUrl;
    private String fileName;
    private String filePath;
}
