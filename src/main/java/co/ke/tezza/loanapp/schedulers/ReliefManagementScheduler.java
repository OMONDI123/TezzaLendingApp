package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;

@Component
public class ReliefManagementScheduler {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private ChargeMonitoringService chargeMonitoringService;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Scheduled(cron = "5 * * * * *")
	@Transactional
	public void monitorInterestsAndPenaltyCaps() {
		List<MLoanApplication> loans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED,true);
		if (loans == null || loans.isEmpty()) {
			return;
		}
		for (MLoanApplication loan : loans) {
			chargeMonitoringService.exemptAmountOverChargesAboveAllowedPercentageCap(loan);

		}

	}

}
