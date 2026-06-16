package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import co.ke.tezza.loanapp.enums.AmendmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "AD_Loan_Amendment_Detail")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MLoanAmendmentDetail extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Loan_Amendment_Detail_ID")
	private Long amendmentDetailId;

	@Column(name = "AD_Loan_Amendment_Detail_UU",  nullable = false)
	private String AD_Loan_Amendment_Detail_UU = UUID.randomUUID().toString();

	// Main amendment data fields
	@Column(name = "new_principal_amount", precision = 19, scale = 4)
	private BigDecimal newPrincipalAmount;

	@Column(name = "new_interest_rate", precision = 8, scale = 4)
	private BigDecimal newInterestRate;
	private BigDecimal newFlatRateAmount;

	@ManyToOne
	@JoinColumn(name = "new_loan_product_id")
	private MLoanProductConfiguration newLoanProduct;
	
	@ManyToOne
	@JoinColumn(name = "AD_Amendment_Configuration_ID")
	private MAmendmentConfiguration amendmentConfiguration;

	@Enumerated(EnumType.STRING)
	private AmendmentType amendmentType;
	@Column(name = "new_term_in_days")
	private Integer newTermInDays;

	@Column(name = "effective_date")
	private LocalDateTime effectiveDate;
	
	private long loanToAmendId;
	private long amendmentRequestId;
	
	
	

}