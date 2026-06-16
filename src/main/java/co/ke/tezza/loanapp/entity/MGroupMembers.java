package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "AD_Group_Member")
@Entity
public class MGroupMembers extends AuditModel{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Group_Member_ID")
	private long groupMemberId;
	private String  firstName;
	private String lastName;
	private String phoneNumber;
	private String residence;
	private boolean isGroupRepresentative;	

}
