package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Guarantor_Loan")
public class MGuarantorLoan extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Guarantor_Loan_ID")
	private long guarantorLoanId;
	
	@ManyToOne
	@JoinColumn(name = "AD_guarantor_ID")
	private MNextOfKin guarantor;
	
	@ManyToOne
	@JoinColumn(name = "AD_Loan_Application_ID")
	private MLoanApplication loan;
	private BigDecimal guaranteeAmount=BigDecimal.ZERO;
	private BigDecimal guaranteeLimit;
	
	private BigDecimal guaranteeAmountBalance=BigDecimal.ZERO;
	
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean primaryGuarantor;

}
