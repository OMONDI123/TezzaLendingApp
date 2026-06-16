package co.ke.tezza.loanapp.model;

import co.ke.tezza.loanapp.enums.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetupModel {
	private long id;
	private String name;
	private String description;
	 private AttachmentType attachmentType;

	String code;

}
