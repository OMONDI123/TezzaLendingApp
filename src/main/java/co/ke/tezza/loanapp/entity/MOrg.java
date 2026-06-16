package co.ke.tezza.loanapp.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import co.ke.tezza.loanapp.util.Utils;
import co.ke.tezza.loanapp.web.security.SpringContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Org")
@Audited
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "created", "updated", "createdBy", "updatedBy" }, allowGetters = true)
public class MOrg implements Serializable {
	/**
	 * 
	 */

	@Transient
	@JsonIgnore
	private transient Utils utils;

	public Utils getUtils() {
		if (utils == null) {
			utils = SpringContext.getBean(Utils.class);
		}
		return utils;
	}

	private static final long serialVersionUID = 1L;
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	@Column(name = "AD_Org_ID", nullable = false)
	private long id;
	@Column(name = "value", nullable = true, updatable = true)
	private String value;

	@Column(name = "name", nullable = false, updatable = true)
	private String name;

	@Column(name = "description", nullable = true, updatable = true)
	private String description;
	@Transient
	private String clientName;

	@Column(name = "AD_Org_UU", nullable = false, updatable = false)
	private String AD_Org_UU;

	@Column(name = "isactive", columnDefinition = "BOOLEAN DEFAULT true")
	private boolean isActive = true;

	@Column(name = "processed", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processed = false;

	@Column(name = "processing", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processing = false;

	@Column(name = "isapproved", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean isApproved = false;

	@Column(name = "ammend", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean ammend = false;

	@Column(name = "reject", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean reject = false;

	@Column(name = "documentno")
	private String documentNo;

	@Column(name = "docstatus")
	private String docStatus;

	@Column(name = "approvalstage")
	private String approvalStage;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	@CreatedDate
	@CreationTimestamp
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated", nullable = false, updatable = true, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	@LastModifiedDate
	@UpdateTimestamp
	private Date updated;

	@CreatedBy
	@Column(updatable = false, name = "createdby")
	private long createdBy;
	
	private String kraPin;
	private String location;
	private String city;
	private String county;
	private String physicalAddress;
	private String boxNo;
	private String street;
	private String landMark;
	

	@LastModifiedBy
	@Column(name = "updatedby")
	private long updatedBy;

	@Column(name = "AD_Client_ID", nullable = true, updatable = true, columnDefinition = "NUMERIC DEFAULT NULL")
	private long adClientId;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long C_BPartner_ID;

	@CreatedBy
	@Column(updatable = false, name = "AD_User_ID")
	private long AD_User_ID;

	@PrePersist
	
	public void SetAD_Client_ID() {
		Utils utils = getUtils();
		if (utils != null) {
			if (utils.getAD_Client_ID() > 0) {

				adClientId = utils.getAD_Client_ID();

			}
		}

	}

}
