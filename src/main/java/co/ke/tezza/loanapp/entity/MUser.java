package co.ke.tezza.loanapp.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * @author austine
 *
 */
@Entity
@Table(name = "ad_user")
@Audited
@Data
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "created", "updated", "createdBy", "updatedBy" }, allowGetters = true)
public class MUser implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ad_user_id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long userId;

	@Column(name = "fullname")
	private String fullName;

	@Column(name = "password")
	private String password;

	@Column(name = "email")
	private String email;

	@Column(name = "dateOfBirth")
	private String dateOfBirth;

	@Column(name = "phone_number")
	private String phoneNumber;

	private String firstName;

	private String lastName;
	private String rand;
	private String externalRefrenceNo;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ad_user_role", joinColumns = @JoinColumn(referencedColumnName = "ad_user_id"), inverseJoinColumns = @JoinColumn(referencedColumnName = "ad_role_id"))
	private Set<MRoles> roles = new HashSet<>();

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

	@LastModifiedBy
	@Column(name = "updatedby")
	private long updatedBy;

	@Column(name = "AD_Org_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long adOrgId;

	@Column(name = "AD_Client_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long adClientId;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long C_BPartner_ID;

	@Column(name = "otp_code", columnDefinition = "NUMERIC DEFAULT 0")
	private int otpCode;

	@Column(name = "AD_User_UU")
	private String aD_User_UU;

	@Transient
	private String clientName;

	@Transient
	private String organizationName;
	@Column(name = "device_fingerprint", length = 64)
	private String deviceFingerprint;
	@Column(unique = true, nullable = true)
	private String referralCode;
	@Column(unique = true, nullable = true)
	private String referralLink;

	@ManyToOne
	@JoinColumn(name = "AD_User_Referral_ID", nullable = true)
	private MUser referredBy;
	private String gender;

	// 🌍 Location fields (from IP API)
	private String ip;
	private String network;
	private String version;
	private String city;
	private String region;
	private String regionCode;
	private String country;
	private String countryName;
	private String countryCode;
	private String countryCodeIso3;
	private String countryCapital;
	private String countryTld;
	private String continentCode;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean inEu;
	private String postal;
	private Double latitude;
	private Double longitude;
	private String timezone;
	private String utcOffset;
	private String countryCallingCode;
	private String currency;
	private String currencyName;
	private String languages;
	private Double countryArea;
	private Long countryPopulation;
	private String asn;
	private String org;

	private String currentCountry;
	private String currentCounty;
	private String currentSubCounty;
	private String currentLocality;
	private String currentNearestCity;
	private Double currentLat;
	private Double currentLng;
	private String currentLocationId;
	@Column(columnDefinition = "INT DEFAULT 0")
	private int noOftimesLoggedIn;

}
