package co.ke.tezza.loanapp.response;

import java.util.Date;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.AttachmentType;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentResponse {
	private long attachmentId;
	private String name;
	private String description;
	private String documentNo;
	private Date created;
	private Date updated;
	private DocStatus docStatus;
	private ApprovalStage approvalStage;
	private AttachmentType attachmentType;
	private boolean isActive;
	private String AD_Attachment_UU;

}
