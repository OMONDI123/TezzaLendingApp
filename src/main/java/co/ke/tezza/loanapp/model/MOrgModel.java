package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MOrgModel {
	private long id;
	
	private String name;
	
	private String value;
	
	private String description;
	private long adClientId;
	private String kraPin;
	private String location;
	private String city;
	private String county;
	private String physicalAddress;
	private String boxNo;
	private String street;
	private String landMark;

}
