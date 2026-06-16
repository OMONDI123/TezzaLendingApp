package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowerAttachments {
	private long id;
	private long attachmentId;
	private String fileUpload;
	private String colleteralOwner;
	private BigDecimal colleteralValue;
	private Date expiryDate;
	private Integer storageDurationDaysOnLoanCompletion;
	private String colleteralNo;

}
