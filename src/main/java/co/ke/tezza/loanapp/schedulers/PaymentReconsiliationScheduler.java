package co.ke.tezza.loanapp.schedulers;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.repository.PaymentRepository;
import co.ke.tezza.loanapp.service.PaymentsService;

@Component
public class PaymentReconsiliationScheduler {
	@Autowired
	private PaymentsService paymentsService;
	@Autowired
	private PaymentRepository paymentRepository;
	Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Run every 5 minutes to check pending M-Pesa payments This will automatically
	 * call your queryPendingMpesaPayments method
	 */
	@Scheduled(fixedRate = 300000)
	@Transactional
	public void scheduledMpesaStatusCheck() {
		try {
			paymentsService.queryPendingMpesaPayments();
		} catch (Exception e) {
		}
	}

	@Scheduled(cron = "0 59 23 * * *")
	@Transactional
	public void allocateSecurityPayments() {
		List<MPayments> pendingAllocations = paymentRepository
				.findByIsActiveAndSecurityPaymentAndDocStatusAndExpectedAllocationDateLessThanEqual(true, true,
						DocStatus.PENDING_ALLOCATION, LocalDateTime.now());
		if (pendingAllocations.isEmpty()) {
			return;
		}
		for (MPayments payment : pendingAllocations) {
			if (payment == null) {
				continue;
			}
			boolean isMpesa=payment.getCheckoutrequest()!=null;
			paymentsService.processUnifiedPaymentAllocationPendingSecurityPayments(payment, isMpesa);

		}

	}

}
