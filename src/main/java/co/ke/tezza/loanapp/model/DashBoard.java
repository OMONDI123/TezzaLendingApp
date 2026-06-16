package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashBoard {
	private int totalDebtsRegistered;
	private int totalDebtsApproved;
	private int totalPendingApprovals;
	private int totalRejectedDebts;
	private int totalPartiallyPaidDebts;
	private int nonPaidDebts;
	private int totalFullyPaidDebts;
	private BigDecimal totalAmountApproved;
	private int totalOutstandingDebts;
	private BigDecimal totalOutstandingBalance;
	private BigDecimal totalPenaltiesCharged;
	private BigDecimal totalInterestEarned;
	private BigDecimal totalAmountWaived;
	private BigDecimal totalInterestsWaived;
	private BigDecimal penaltiesWaived;
	private BigDecimal totalAmountWrittenOff;
	private BigDecimal totalWaiverByCreditNote;
	private BigDecimal totalPayments;
	private BigDecimal totalUnAllocatedSecurityPayments;
	private BigDecimal totalAllocatedSecurityPayments;
	private BigDecimal totalSecurityPayments;
	private BigDecimal totalCompletedPayments;
	private List<TopOverdueDebtors> topOverdueDebtors=new ArrayList<>();
	private List<TopOverdueDebtors> topOverdueDebtorsByAmount=new ArrayList<>();
	private List<BestPerformingDebtors> bestPerformingDebtors=new ArrayList<>();
	private List<BestPerformingCollectors> bestPerformingCollectors=new ArrayList<>();
	private Map<String , BigDecimal> paymentStatisticsByPaymentMethod=new HashMap<>();
	private List<PaymentStatisticsByPaymentMethod> paymentStatisticsByPaymentMethodObj =new ArrayList<>();
	private Map<String , BigDecimal> totalRemindersByCategory=new HashMap<>();
	private List <PaymentDistribution> yearlyPaymentDistribution=new ArrayList<>();
	private List<PaymentDistribution> monthlyPaymentDist=new ArrayList<>();
	
	private List <PaymentDistribution> yearlyPaymentDistributionByPaymentMethod=new ArrayList<>();
	private List<PaymentDistribution> monthlyPaymentDistByPaymentMethod=new ArrayList<>();
	private List<BestPerformingDebts> bestPerformingDebts=new ArrayList<>();
	private List<BestPerformingDebts> bestPerformingDebtsByAllPayments=new ArrayList<>();
	private int remindersSent;
	private int remindersFailed;
	private int totalReminders;
	private BigDecimal smsBalance;
	private int registeredDebtors;
	private int individualDebtors;
	private int institutionDebtors;
	private int groupDebtors;
	private BigDecimal organisationConversionRate;
	private BigDecimal approvedAmountollectionRate;
	
	
	
	
	
	
	

}
