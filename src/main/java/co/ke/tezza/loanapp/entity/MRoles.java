package co.ke.tezza.loanapp.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author austine
 *
 */
@Entity
@Table(name = "ad_role")
@Audited

@AllArgsConstructor
@NoArgsConstructor
public class MRoles extends AuditModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ad_role_id")
	private Long id;

	@Column(name = "AD_Role_UU")
	private String aD_Role_UU=UUID.randomUUID().toString();
	@Column(name = "formatted_name", columnDefinition = "TEXT DEFAULT NULL")
	private String formattedName;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean allowMultiOrgAccess;
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "AD_Role_Org_Access", joinColumns = @JoinColumn(referencedColumnName = "AD_Role_ID"), inverseJoinColumns = @JoinColumn(referencedColumnName = "AD_Org_ID"))
	private Set<MOrg> allowedOrganisations = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MRoles(String name) {
		super.setName(name);
	}

	public String getaD_Role_UU() {
		return aD_Role_UU;
	}

	public void setaD_Role_UU(String aD_Role_UU) {
		this.aD_Role_UU = aD_Role_UU;
	}

	public String getFormattedName() {
		return formattedName.replace("Role", "");
	}

	public void setFormattedName(String formattedName) {
		this.formattedName = formattedName;
	}

	public boolean isAllowMultiOrgAccess() {
		return allowMultiOrgAccess;
	}

	public void setAllowMultiOrgAccess(boolean allowMultiOrgAccess) {
		this.allowMultiOrgAccess = allowMultiOrgAccess;
	}

	public Set<MOrg> getAllowedOrganisations() {
		return allowedOrganisations;
	}

	public void setAllowedOrganisations(Set<MOrg> allowedOrganisations) {
		this.allowedOrganisations = allowedOrganisations;
	}

	
	
	

}
