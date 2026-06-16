package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembers {
	private long id;
	private long groupMemberId;
	private String  firstName;
	private String lastName;
	private String phoneNumber;
	private String residence;
	private boolean isGroupRepresentative;

}
