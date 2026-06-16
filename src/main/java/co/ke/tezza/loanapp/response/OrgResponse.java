package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgResponse {
	private long id;
	private String name;
	private String description;
	private String kraPin;
	private String location;
	private String city;
	private String county;
	private String physicalAddress;
	private String boxNo;
	private String street;
	private String landMark;
	

}
