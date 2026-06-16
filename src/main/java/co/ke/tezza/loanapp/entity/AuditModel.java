
package co.ke.tezza.loanapp.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.util.Utils;
import co.ke.tezza.loanapp.web.security.SpringContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author austine
 *
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "created", "updated", "createdBy", "updatedBy", "AD_User_ID" }, allowGetters = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditModel implements Serializable {

	@Transient
	@JsonIgnore
	private transient Utils utils;

	public Utils getUtils() {
		if (utils == null) {
			utils = SpringContext.getBean(Utils.class);
		}
		return utils;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "isactive", columnDefinition = "BOOLEAN DEFAULT true")
	private boolean isActive = true;

	@Column(name = "processed", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processed = false;

	@Column(name = "processing", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processing = false;

	@Column(name = "isapproved", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean isApproved = false;
	@Column(columnDefinition = "NUMERIC DEFAULT 1")
    private Integer currentApprovalLevel=1;

	@Column(name = "ammend", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean ammend = false;
	@Column(columnDefinition = "TEXT")
	private String amendmentReason;

	@Column(name = "reject", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean reject = false;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String rejectReason;

	@Column(name = "documentno")
	private String documentNo;

	@Column(name = "docstatus")
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus = DocStatus.DRAFT;

	@Column(name = "approvalstage")
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage = ApprovalStage.DRAFT;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "approval_date", nullable = true, updatable = true, columnDefinition = "TIMESTAMP DEFAULT NULL")
	private Date approvalDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "rejected_date", nullable = true, updatable = true, columnDefinition = "TIMESTAMP DEFAULT NULL")
	private Date rejectedDate;

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
	@Column(updatable = false, name = "createdby", columnDefinition = "NUMERIC DEFAULT 0")
	private Long createdBy = 0L;

	@LastModifiedBy
	@Column(name = "updatedby", columnDefinition = "NUMERIC DEFAULT 0")
	private Long updatedBy = 0L;

	@Column(name = "AD_Org_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long adOrgID = 0L;

	@Column(name = "AD_Client_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long adClientId = 0L;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long c_BPartner_ID = 0L;

	@Column(name = "description", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String description;

	@Column(name = "name", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String name;

	@Transient
	private String clientName;

	@Transient

	private String organizationName;

	private static final ThreadLocal<Boolean> settingCommonFields = ThreadLocal.withInitial(() -> false);

	@PrePersist

	public void setCommonFields() {
		if (settingCommonFields.get()) {
			// Prevent recursion
			return;
		}

		try {
			settingCommonFields.set(true);

			Utils utils = getUtils();
			if (utils != null) {
				this.adOrgID = utils.getAD_Org_ID();
				this.adClientId = utils.getAD_Client_ID();
				this.c_BPartner_ID = utils.getC_BPartner_ID();
			} else {
				System.err.println("⚠️ Utils bean is not available in ApplicationContext during persist/update.");
			}

		} catch (Exception ex) {
			System.err.println("❌ Failed to set common fields in AuditModel: " + ex.getMessage());
		} finally {
			settingCommonFields.set(false); // Clean up
		}
	}

}