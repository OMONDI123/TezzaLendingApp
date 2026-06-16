package co.ke.tezza.loanapp.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Entity_Role")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MEntityRoleMapping extends AuditModel {

	@Column(name = "AD_Entity_Role_ID")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Table_ID", columnDefinition = "BIGINT DEFAULT NULL")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MTable table;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "AD_Entity_Role_Mapping", joinColumns = @JoinColumn(name = "AD_Entity_Role_ID", referencedColumnName = "AD_Entity_Role_ID"), inverseJoinColumns = @JoinColumn(name = "AD_Role_ID", referencedColumnName = "AD_Role_ID"))
	private Set<MRoles> roles = new HashSet<>();

	private String AD_Entity_Role_UU;

}
