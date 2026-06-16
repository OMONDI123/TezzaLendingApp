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
import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Menu_Role")
@Audited
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MMenuRoleMapping extends AuditModel{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Menu_Role_ID")
	private long id;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Role_ID",columnDefinition = "BIGINT DEFAULT NULL")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MRoles role;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Menu_ID",columnDefinition = "BIGINT DEFAULT NULL")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MADMenu menu;
	
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable( name = "AD_Sub_Menu_Role_Mapping",joinColumns = @JoinColumn(referencedColumnName = "AD_Menu_Role_ID"),inverseJoinColumns = @JoinColumn(referencedColumnName = "AD_Sub_Menu_ID")   )
	private Set<MADSubMenu> associatedSubMenus=new HashSet<>();
	
	@Column(name = "AD_Sub_Menu_Role_UU")
	private String AD_Sub_Menu_Role_UU;

}
