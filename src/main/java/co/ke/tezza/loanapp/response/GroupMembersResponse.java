package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembersResponse {
	private long groupMemberId;
	private String  firstName;
	private String lastName;
	private String phoneNumber;
	private String residence;
	private boolean isGroupRepresentative;

}
