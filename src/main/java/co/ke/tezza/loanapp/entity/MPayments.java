package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import co.ke.tezza.loanapp.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Payment")
public class MPayments extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Payment_ID")
	private long paymentId;

	@Column(name = "paymentid", nullable = true)
	private Long id;
	private BigDecimal amount;
	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "AD_LoanApplication_ID", nullable = true)
	private MLoanApplication loan;

	

	@ManyToMany
	@JoinTable(name = "AD_Installment_Payments", joinColumns = @JoinColumn(name = "AD_Payment_ID"), inverseJoinColumns = @JoinColumn(name = "AD_Installment_ID"))
	private Set<MInstallments> installments = new HashSet<>();

	@Column(name = "payment_method")
	@Enumerated(EnumType.STRING)
	private PaymentType paymentMethod;

	@Column(name = "isPaid", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean ispaid;

	@Size(max = 255)
	@Column(name = "checkoutrequest")
	private String checkoutrequest;

	@Size(max = 255)
	@Column(name = "responsedescription")
	private String responsedescription;

	@Size(max = 255)
	@Column(name = "customermessage")
	private String customermessage;

	@Size(max = 255)
	@Column(name = "responsecode")
	private String responsecode;

	@Size(max = 100)
	@Column(name = "merchantrequest")
	private String merchantrequest;

	@Size(max = 255)
	@Column(name = "description")
	private String description;

	@Size(max = 100)
	@Column(name = "phone_number")
	private String PhoneNumber;

	@Size(max = 100)
	@Column(name = "mpesa_receipt_number")
	private String mpesareceitNumber;

	@Size(max = 100)
	@Column(name = "transaction_date")
	private String transactionDate;

	@Size(max = 100)
	@Column(name = "intent")
	private String intent;

	@Size(max = 100)
	@Column(name = "currency")
	private String currency;
	private String paymentDate;
	private String reference;
	@Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
	private LocalDateTime paymentDateTime;
	@Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
	private LocalDateTime expectedAllocationDate;
	@Column(columnDefinition = "BOOLEAN DEFAULT false")
	private boolean securityPayment;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Payment_Method_ID", nullable = true)
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	private MPaymentMethod paymentMode;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "rejectedby", nullable = true)
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	private MUser rejectedBy;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "receiptedBy", nullable = true)
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	private MUser receiptedBy;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "approvedby", nullable = true)
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	private MUser approvedBy;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String reasonForRejection;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean accountingPosted;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean waiverOrWriteOff;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean waiverWriteOff;
	@Column(name = "interest_only", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean interestOnly;

	@Column(name = "penalties_only", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean penaltiesOnly;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String writeOffWaiverReason;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean walletDeposit;
	
	@Column(name = "customer_id", nullable = true)
	private Long customerId;

	@Column(name = "individual_borrower_id", nullable = true)
	private Long individualBorrowerId;

	@Column(name = "group_borrower_id", nullable = true)
	private Long groupBorrowerId;

	@Column(name = "institution_borrower_id", nullable = true)
	private Long institutionBorrowerId;

	@Column(name = "membership_account_id", nullable = true)
	private Long membershipAccountId;

}
