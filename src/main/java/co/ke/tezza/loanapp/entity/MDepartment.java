package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Table(name = "AD_Department")
@Audited
public class MDepartment extends AuditModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5174575497942605556L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "AD_Department_ID")
	private Long id;
	@Column(name = "department")
	private String name;
	
	@Column(name = "AD_Department_UU", nullable = false, updatable = false)
	private String AD_Org_UU;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
	

}
