package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowerAttachments {
	private long attachmentId;
	private String filepath;
	private String actualFilePath;
	private String mimeType;
	private Long fileSize;
	private String fileName;
	private boolean isActive;
	private String documentNo;
	private String docStatus;
	private String approvalStage;
	private String attachmentType;
	private String attachmentName;
	private String colleteralOwner;
	private BigDecimal colleteralValue;
	private Date expiryDate;
	private Integer storageDurationDaysOnLoanCompletion;
	private String colleteralNo;
	private Date created;
	private Date updated;

}
