package co.ke.tezza.loanapp.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MMessagingCenter;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.repository.MMessagingCenterRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;
import co.ke.tezza.loanapp.util.Utils;

@Service
@EnableScheduling
public class SmsService {

	@Autowired
	private Utils utils;

	@Autowired
	private SmsRepository smsRepository;

	@Autowired
	private MMessagingCenterRepository mMessagingCenterRepository;

	@Scheduled(fixedDelay = 30000)
	public void smsTask() {

		List<MSms> smsList = smsRepository.findByProcessedAndIsActiveAndSmsTypeNotAndTimesTosendLessThanEqualOrderBySmsIdAsc(false, true,
				SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER,LocalDateTime.now());

		for (MSms sms : smsList) {

			try {
				utils.sendBulkSms(sms);

				sms.setProcessed(true);
				smsRepository.save(sms);

			} catch (Exception e) {

				System.err.println("❌ Failed to send SMS ID " + sms.getSmsId() + ": " + e.getMessage());
				e.printStackTrace();

				sms.setDocStatus(DocStatus.REJECTED);
				smsRepository.save(sms);

			}
		}
	}

	@Scheduled(fixedRate = 40000)
	public void smsTaskSendManual() {

		List<MMessagingCenter> pendingMessages = mMessagingCenterRepository
				.findByIsActiveAndMessageStatusAndMessagingTimeLessThanEqual(true, MessageStatus.SCHEDULED,
						new Date());

		for (MMessagingCenter center : pendingMessages) {

			try {
				utils.saveManualSms(center.getIndividualBorrowerId(), center.getGroupBorrowerId(),
						center.getInstitutionBorrowerId(), center.getPhoneNumber(), center.getMessage(),
						center.getAdOrgID(), center.getAdClientId(), SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER,
						center.getMessagingId());

				center.setMessageStatus(MessageStatus.PROCESSING);
				mMessagingCenterRepository.save(center);

			} catch (Exception e) {
				center.setMessageStatus(MessageStatus.FAILED);
				mMessagingCenterRepository.save(center);
			}
		}
	}

	@Scheduled(fixedRate = 60000)
	public void smsTaskManual() {

		List<MSms> smsList = smsRepository.findByProcessedAndIsActiveAndSmsTypeOrderBySmsIdAsc(false, true,
				SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER);

		for (MSms sms : smsList) {
			MMessagingCenter center = mMessagingCenterRepository.findById(sms.getMessageCenterId()).get();

			try {
				utils.sendBulkSms(sms);
				sms.setProcessed(true);
				smsRepository.save(sms);
				
				center.setMessageStatus(sms.getMessageStatus());
				center.setDocStatus(sms.getDocStatus());
				center.setApprovalStage(sms.getApprovalStage());
				mMessagingCenterRepository.save(center);

			} catch (Exception e) {

				System.err.println("❌ Failed to send SMS ID " + sms.getSmsId() + ": " + e.getMessage());
				e.printStackTrace();

				sms.setDocStatus(DocStatus.REJECTED);

				smsRepository.save(sms);
				center.setMessageStatus(MessageStatus.FAILED);
				center.setDocStatus(DocStatus.REJECTED);
				mMessagingCenterRepository.save(center);

			}
		}
	}
}
