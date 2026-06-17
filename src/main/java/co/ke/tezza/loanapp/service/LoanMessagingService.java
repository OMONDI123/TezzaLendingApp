package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanComment;
import co.ke.tezza.loanapp.entity.MMessagingCenter;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.MessageForm;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.enums.SupportedClientsEnum;
import co.ke.tezza.loanapp.model.BorrowerDetails;
import co.ke.tezza.loanapp.model.DBConnect;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.Guarantors;
import co.ke.tezza.loanapp.model.LoanCommentRequest;
import co.ke.tezza.loanapp.model.MessageRequest;
import co.ke.tezza.loanapp.repository.GroupBorrowersRepository;
import co.ke.tezza.loanapp.repository.IndividualBorrowersRepository;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.InstitutionBorrowersRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.LoanCommentRepository;
import co.ke.tezza.loanapp.repository.MMessagingCenterRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.BorrowerWithMessagesResponse;
import co.ke.tezza.loanapp.response.CardexBorrowers;
import co.ke.tezza.loanapp.response.LoanCommentResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoanMessagingService {

	@Autowired
	private Utils utils;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private LoanCommentRepository loanCommentRepository;

	@Autowired
	private GroupBorrowersRepository groupBorrowersRepository;
	@Autowired
	private IndividualBorrowersRepository individualBorrowersRepository;
	@Autowired
	private InstitutionBorrowersRepository institutionBorrowersRepository;

	@Autowired
	private MMessagingCenterRepository messagingCenterRepository;

	@Autowired
	private NamedParameterJdbcTemplate jdbc;

	@Autowired
	private SmsRepository smsRepository;

	/**
	 * Create or update a loan comment (Cardex entry).
	 */
	public ResponseEntity<LoanCommentResponse> saveOrUpdate(LoanCommentRequest request) {
		DBConnect connect = utils.getDBConnect();
		if (connect != null) {
			switch (connect.getClient()) {

			case SMART_DEBT:
				return this.saveOrUpdateLoanComment(request);
			default:
				break;
			}
		}
		return this.saveOrUpdateLoanComment(request);
	}

	/**
	 * Internal method for SMART_DEBT client to create/update loan comment
	 */
	private ResponseEntity<LoanCommentResponse> saveOrUpdateLoanComment(LoanCommentRequest request) {
		MLoanComment entity = (request.getCommentId() != null)
				? loanCommentRepository.findById(request.getCommentId()).orElse(new MLoanComment())
				: new MLoanComment();

		// Basic fields
		entity.setNotes(request.getNotes());
		entity.setStatus(request.getStatus());
		entity.setActionDate(request.getActionDate());
		entity.setCallDateTime(request.getCallDateTime());
		entity.setNextCallDate(request.getNextCallDate());

		// New fields
		entity.setPriority(request.getPriority());
		entity.setContactMethod(request.getContactMethod());
		entity.setCallDuration(request.getCallDuration());
		entity.setPromiseAmount(request.getPromiseAmount());

		// Relations
		if (request.getLoanId() != null) {
			MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId()).orElse(null);
			entity.setLoan(loan);
		}

		if (request.getInstallmentId() != null) {
			MInstallments installment = installmentRepository.findById(request.getInstallmentId()).orElse(null);
			entity.setInstallment(installment);
		}

		// Automatically set user creating/updating the note
		entity.setNotesTakenBy(utils.getLoggedInUser());

		// Organization & audit
		entity.setAdOrgID(utils.getAD_Org_ID());

		// Save
		MLoanComment createdComment = loanCommentRepository.save(entity);

		String message = (createdComment != null) ? "Cardex recorded successfully"
				: "Failed to add cardex for this loan.";

		int code = (createdComment != null) ? 200 : 500;

		// Save and map to response
		return new ResponseEntity<LoanCommentResponse>(message, code, objectsMapper.mapCardex(createdComment));
	}

	public org.springframework.http.ResponseEntity<MessageResponse> sendManualMessages(MessageRequest request) {
		DBConnect connect = utils.getDBConnect();
		if (connect != null) {
			switch (connect.getClient()) {
			
			case SMART_DEBT:
				return this.sendManualMessagesForSmartDebt(request);
			default:
				break;
			}
		}
		return this.sendManualMessagesForSmartDebt(request);
	}

	/**
	 * Handle manual messaging for SMART_DEBT client. Now supports SMS, Email, or
	 * Both based on MessageForm.
	 */
	private org.springframework.http.ResponseEntity<MessageResponse> sendManualMessagesForSmartDebt(
			MessageRequest request) {
		try {
			// Upload file and get FileUploads object (contains file path and name)
			FileUploads uploadedFile = processFileUpload(request.getFile());

			switch (request.getReceiverCategory()) {
			case SPECIFIC_OR_INDIVIDUAL_BORROWER:
				processSpecificBorrowers(request, uploadedFile);
				break;
			case ALL:
				processAllBorrowers(request, uploadedFile);
				break;
			case BORROWERS_WITH_BALANCES:
				processBorrowersWithBalances(request, uploadedFile);
				break;
			default:
				throw new IllegalArgumentException("Unknown receiver category: " + request.getReceiverCategory());
			}

			return org.springframework.http.ResponseEntity
					.ok(new MessageResponse("Message has been staged for processing", request));

		} catch (Exception e) {
			return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new MessageResponse("Failed to process message: " + e.getMessage(), null));
		}
	}

	/**
	 * Upload file and return FileUploads object with full details.
	 */
	private FileUploads processFileUpload(String fileBase64) {
		return fileBase64 != null ? utils.uploadFileFromBase64(fileBase64) : null;
	}

	// ------------------------------------------------------------------------
	// Helper methods to decide channels
	// ------------------------------------------------------------------------

	private boolean shouldSendSms(MessageForm messageForm) {
		return messageForm == MessageForm.SMS || messageForm == MessageForm.BOTH;
	}

	private boolean shouldSendEmail(MessageForm messageForm) {
		return messageForm == MessageForm.EMAIL || messageForm == MessageForm.BOTH;
	}

	// ------------------------------------------------------------------------
	// Processing methods for each category
	// ------------------------------------------------------------------------

	private void processSpecificBorrowers(MessageRequest request, FileUploads uploadedFile) {
		processIndividualBorrowers(request, uploadedFile, request.getIndividualBorrowerId());
		processGroupBorrowers(request, uploadedFile, request.getGroupBorrowerId());
		processInstitutionBorrowers(request, uploadedFile, request.getInstitutionBorrowerId());
	}

	private void processIndividualBorrowers(MessageRequest request, FileUploads uploadedFile, List<Long> receiverIds) {
		if (receiverIds == null || receiverIds.isEmpty())
			return;

		List<MDebtor> debtors = individualBorrowersRepository.findAllById(receiverIds);
		List<MMessagingCenter> smsCenters = new ArrayList<>();

		for (MDebtor debtor : debtors) {
			// SMS
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, debtor));
			}
			// Email
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, debtor.getEmail(),
						debtor.getFirstName() + " " + debtor.getLastName(), debtor.getAdOrgID());
			}

			// Guarantors
			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("INDIVIDUAL", debtor.getIndividualBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, debtor));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		processMessageCenters(smsCenters, request.isScheduledMessage());
	}

	private void processGroupBorrowers(MessageRequest request, FileUploads uploadedFile, List<Long> groupIds) {
		if (groupIds == null || groupIds.isEmpty())
			return;

		List<MGroupDebtors> groups = groupBorrowersRepository.findAllById(groupIds);
		List<MMessagingCenter> smsCenters = new ArrayList<>();

		for (MGroupDebtors group : groups) {
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, group));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, group.getContactEmail(), group.getGroupName(),
						group.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("GROUP", group.getGroupBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, group));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		processMessageCenters(smsCenters, request.isScheduledMessage());
	}

	private void processInstitutionBorrowers(MessageRequest request, FileUploads uploadedFile,
			List<Long> institutionIds) {
		if (institutionIds == null || institutionIds.isEmpty())
			return;

		List<MInstitutionBorrower> institutions = institutionBorrowersRepository.findAllById(institutionIds);
		List<MMessagingCenter> smsCenters = new ArrayList<>();

		for (MInstitutionBorrower institution : institutions) {
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, institution));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, institution.getContactEmail(),
						institution.getInstitutionName(), institution.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("INSTITUTION",
						institution.getInstitutionBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, institution));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		processMessageCenters(smsCenters, request.isScheduledMessage());
	}

	private void processAllBorrowers(MessageRequest request, FileUploads uploadedFile) {
		long orgId = utils.getAD_Org_ID();
		List<MMessagingCenter> smsCenters = new ArrayList<>();

		// Process individual borrowers
		List<MDebtor> debtors = individualBorrowersRepository.findByIsActiveAndAdOrgID(true, orgId);
		for (MDebtor debtor : debtors) {
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, debtor));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, debtor.getEmail(),
						debtor.getFirstName() + " " + debtor.getLastName(), debtor.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("INDIVIDUAL", debtor.getIndividualBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, debtor));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		// Process group borrowers
		List<MGroupDebtors> groups = groupBorrowersRepository.findByIsActiveAndAdOrgID(true, orgId);
		for (MGroupDebtors group : groups) {
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, group));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, group.getContactEmail(), group.getGroupName(),
						group.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("GROUP", group.getGroupBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, group));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		// Process institution borrowers
		List<MInstitutionBorrower> institutions = institutionBorrowersRepository.findByIsActiveAndAdOrgID(true, orgId);
		for (MInstitutionBorrower institution : institutions) {
			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenter(request, uploadedFile, institution));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, institution.getContactEmail(),
						institution.getInstitutionName(), institution.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors("INSTITUTION",
						institution.getInstitutionBorrowerId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenter(request, uploadedFile, guarantor, institution));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		processMessageCenters(smsCenters, request.isScheduledMessage());
	}

	private void processBorrowersWithBalances(MessageRequest request, FileUploads uploadedFile) {
		List<MLoanApplication> loans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED, true);

		List<MMessagingCenter> smsCenters = new ArrayList<>();

		for (MLoanApplication loan : loans) {
			BorrowerInfo borrowerInfo = extractBorrowerInfo(loan);

			if (shouldSendSms(request.getMessageForm())) {
				smsCenters.add(createMessageCenterFromLoan(request, uploadedFile, loan));
			}
			if (shouldSendEmail(request.getMessageForm())) {
				sendEmailForBorrower(request, uploadedFile, borrowerInfo.getEmail(), borrowerInfo.getReceiverName(),
						loan.getAdOrgID());
			}

			if (request.isSendAlsoGuarantors()) {
				List<Guarantors> guarantors = getBorrowerGuarantors(borrowerInfo.getBorrowerType().name(),
						borrowerInfo.getReceiverId());
				for (Guarantors guarantor : guarantors) {
					if (shouldSendSms(request.getMessageForm())) {
						smsCenters.add(createGuarantorMessageCenterFromLoan(request, uploadedFile, guarantor, loan));
					}
					if (shouldSendEmail(request.getMessageForm())) {
						sendEmailForGuarantor(request, uploadedFile, guarantor);
					}
				}
			}
		}

		processMessageCenters(smsCenters, request.isScheduledMessage());
	}

	// ------------------------------------------------------------------------
	// Email helper methods
	// ------------------------------------------------------------------------

	private void sendEmailForBorrower(MessageRequest request, FileUploads uploadedFile, String emailAddress,
			String recipientName, Long orgId) {
		if (emailAddress == null || emailAddress.isBlank()) {
			log.warn("Cannot send email: no email address for borrower {}", recipientName);
			return;
		}

		String messageText = request.getMessage();
		String subject = request.getSubject() != null ? request.getSubject() : "Message from System";

		String filePath = uploadedFile != null ? uploadedFile.getFullFilePath() : null;
		String fileName = uploadedFile != null ? uploadedFile.getFileName() : null;

		LocalDateTime sendTime = null;
		if (request.isScheduledMessage() && request.getDateToSendMessage() != null) {
			sendTime = request.getDateToSendMessage().toInstant().atZone(java.time.ZoneId.systemDefault())
					.toLocalDateTime();
		}

		utils.sendEmailWithAttachment(emailAddress, recipientName, "", messageText, subject, filePath, null, fileName,
				orgId, sendTime);
	}

	private void sendEmailForGuarantor(MessageRequest request, FileUploads uploadedFile, Guarantors guarantor) {
		if (guarantor == null || guarantor.getEmail() == null || guarantor.getEmail().isBlank()) {
			log.warn("Cannot send email to guarantor: no email address");
			return;
		}

		String messageText = request.getMessage();
		String subject = request.getSubject() != null ? request.getSubject() : "Message from System (Guarantor)";

		String filePath = uploadedFile != null ? uploadedFile.getFullFilePath() : null;
		String fileName = uploadedFile != null ? uploadedFile.getFileName() : null;

		LocalDateTime sendTime = null;
		if (request.isScheduledMessage() && request.getDateToSendMessage() != null) {
			sendTime = request.getDateToSendMessage().toInstant().atZone(java.time.ZoneId.systemDefault())
					.toLocalDateTime();
		}

		// Use the same email utility; pass orgId from request (or default)
		utils.sendEmailWithAttachment(guarantor.getEmail(), guarantor.getFullName(), "", messageText, subject, filePath,
				null, fileName, utils.getAD_Org_ID(), sendTime);
	}

	// ------------------------------------------------------------------------
	// SMS message center creation methods (unchanged but accept FileUploads)
	// ------------------------------------------------------------------------

	private MMessagingCenter createMessageCenter(MessageRequest request, FileUploads uploadedFile, MDebtor debtor) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(debtor.getPhone());
		center.setReceiverEmail(debtor.getEmail());
		center.setReceiverId(debtor.getIndividualBorrowerId());
		center.setReceiverName(debtor.getFirstName() + " " + debtor.getLastName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.INDIVIDUAL);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setIndividualBorrowerId(debtor.getIndividualBorrowerId());
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createMessageCenter(MessageRequest request, FileUploads uploadedFile,
			MGroupDebtors group) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(group.getContactPhone());
		center.setReceiverEmail(group.getContactEmail());
		center.setReceiverId(group.getGroupBorrowerId());
		center.setReceiverName(group.getGroupName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.GROUP);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setGroupBorrowerId(group.getGroupBorrowerId());
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createMessageCenter(MessageRequest request, FileUploads uploadedFile,
			MInstitutionBorrower institution) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(institution.getContactPhone());
		center.setReceiverEmail(institution.getContactEmail());
		center.setReceiverId(institution.getInstitutionBorrowerId());
		center.setReceiverName(institution.getInstitutionName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.INSTITUTION);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setInstitutionBorrowerId(institution.getInstitutionBorrowerId());
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createMessageCenterFromLoan(MessageRequest request, FileUploads uploadedFile,
			MLoanApplication loan) {
		BorrowerInfo borrowerInfo = extractBorrowerInfo(loan);

		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setReceiverEmail(borrowerInfo.getEmail());
		center.setReceiverId(borrowerInfo.getReceiverId());
		center.setReceiverName(borrowerInfo.getReceiverName());
		center.setPhoneNumber(borrowerInfo.getPhoneNumber());
		center.setBorrowerType(borrowerInfo.getBorrowerType());
		center.setIndividualBorrowerId(borrowerInfo.getIndividualReceiverId());
		center.setGroupBorrowerId(borrowerInfo.getGroupReceiverId());
		center.setInstitutionBorrowerId(borrowerInfo.getInstitutionReceiverId());
		center.setMessageStatus(MessageStatus.PENDING);
		center.setMessagingTime(getMessagingTime(request));

		return center;
	}

	private MMessagingCenter createGuarantorMessageCenter(MessageRequest request, FileUploads uploadedFile,
			Guarantors guarantor, MDebtor borrower) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(guarantor.getPhoneNumber());
		center.setReceiverEmail(guarantor.getEmail());
		center.setReceiverName(guarantor.getFullName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.INDIVIDUAL);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setIndividualBorrowerId(borrower.getIndividualBorrowerId());
		center.setGroupBorrowerId(0);
		center.setInstitutionBorrowerId(0);
		center.setReceiverId(0);
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createGuarantorMessageCenter(MessageRequest request, FileUploads uploadedFile,
			Guarantors guarantor, MGroupDebtors group) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(guarantor.getPhoneNumber());
		center.setReceiverEmail(guarantor.getEmail());
		center.setReceiverName(guarantor.getFullName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.GROUP);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setGroupBorrowerId(group.getGroupBorrowerId());
		center.setIndividualBorrowerId(0);
		center.setInstitutionBorrowerId(0);
		center.setReceiverId(0);
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createGuarantorMessageCenter(MessageRequest request, FileUploads uploadedFile,
			Guarantors guarantor, MInstitutionBorrower institution) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(guarantor.getPhoneNumber());
		center.setReceiverEmail(guarantor.getEmail());
		center.setReceiverName(guarantor.getFullName());
		center.setMessagingTime(getMessagingTime(request));
		center.setBorrowerType(BorrowerTypeEnum.INSTITUTION);
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setInstitutionBorrowerId(institution.getInstitutionBorrowerId());
		center.setIndividualBorrowerId(0);
		center.setGroupBorrowerId(0);
		center.setReceiverId(0);
		center.setMessageStatus(MessageStatus.PENDING);
		return center;
	}

	private MMessagingCenter createGuarantorMessageCenterFromLoan(MessageRequest request, FileUploads uploadedFile,
			Guarantors guarantor, MLoanApplication loan) {
		MMessagingCenter center = new MMessagingCenter();
		center.setMessage(request.getMessage());
		center.setFilepath(uploadedFile != null ? uploadedFile.getFullFilePath() : null);
		center.setPhoneNumber(guarantor.getPhoneNumber());
		center.setReceiverEmail(guarantor.getEmail());
		center.setReceiverName(guarantor.getFullName());
		center.setMessagingTime(getMessagingTime(request));
		center.setApprovalDate(new Date());
		center.setApprovalStage(ApprovalStage.APPROVED);
		center.setMessageStatus(MessageStatus.PENDING);
		center.setReceiverId(0);

		if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
			center.setBorrowerType(BorrowerTypeEnum.INDIVIDUAL);
			center.setIndividualBorrowerId(loan.getIndividualBorrower().getIndividualBorrowerId());
			center.setGroupBorrowerId(0);
			center.setInstitutionBorrowerId(0);
		} else if (loan.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
			center.setBorrowerType(BorrowerTypeEnum.GROUP);
			center.setGroupBorrowerId(loan.getGroupBorrower().getGroupBorrowerId());
			center.setIndividualBorrowerId(0);
			center.setInstitutionBorrowerId(0);
		} else {
			center.setBorrowerType(BorrowerTypeEnum.INSTITUTION);
			center.setInstitutionBorrowerId(loan.getInstitutionBorrower().getInstitutionBorrowerId());
			center.setIndividualBorrowerId(0);
			center.setGroupBorrowerId(0);
		}

		return center;
	}

	private void processMessageCenters(List<MMessagingCenter> centers, boolean isScheduled) {
		if (centers.isEmpty())
			return;

		List<MMessagingCenter> savedCenters = messagingCenterRepository.saveAll(centers);

		if (!isScheduled) {
			savedCenters.forEach(this::processImmediateMessage);
		} else {
			savedCenters.forEach(this::processScheduledMessages);
		}
	}

	private void processImmediateMessage(MMessagingCenter center) {
		try {
			utils.saveManualSms(center.getIndividualBorrowerId(), center.getGroupBorrowerId(),
					center.getInstitutionBorrowerId(), center.getPhoneNumber(), center.getMessage(),
					center.getAdOrgID(), center.getAdClientId(), SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER,
					center.getMessagingId());

			center.setMessageStatus(MessageStatus.PROCESSING);
			messagingCenterRepository.save(center);

		} catch (Exception e) {
			center.setMessageStatus(MessageStatus.FAILED);
			messagingCenterRepository.save(center);
		}
	}

	private void processScheduledMessages(MMessagingCenter center) {
		center.setMessageStatus(MessageStatus.SCHEDULED);
		messagingCenterRepository.save(center);
	}

	private Date getMessagingTime(MessageRequest request) {
		return request.getDateToSendMessage() != null ? request.getDateToSendMessage() : new Date();
	}

	private List<Guarantors> getBorrowerGuarantors(String borrowerType, Long borrowerId) {
		return utils.getGuarantorsByBorrowerType(borrowerType, borrowerId);
	}

	// DTO for borrower information
	@Getter
	@Setter
	private static class BorrowerInfo {
		private long individualReceiverId;
		private long groupReceiverId;
		private long institutionReceiverId;
		private long receiverId;
		private String receiverName;
		private String phoneNumber;
		private String email;
		private BorrowerTypeEnum borrowerType;
	}

	private BorrowerInfo extractBorrowerInfo(MLoanApplication loan) {
		BorrowerInfo info = new BorrowerInfo();

		if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
			MDebtor debtor = loan.getIndividualBorrower();
			info.setIndividualReceiverId(debtor.getIndividualBorrowerId());
			info.setReceiverId(debtor.getIndividualBorrowerId());
			info.setReceiverName(debtor.getFirstName() + " " + debtor.getLastName());
			info.setPhoneNumber(debtor.getPhone());
			info.setBorrowerType(BorrowerTypeEnum.INDIVIDUAL);
			info.setEmail(debtor.getEmail());
		} else if (loan.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
			MGroupDebtors group = loan.getGroupBorrower();
			info.setGroupReceiverId(group.getGroupBorrowerId());
			info.setReceiverId(group.getGroupBorrowerId());
			info.setReceiverName(group.getGroupName());
			info.setPhoneNumber(group.getContactPhone());
			info.setBorrowerType(BorrowerTypeEnum.GROUP);
			info.setEmail(group.getContactEmail());
		} else {
			MInstitutionBorrower institution = loan.getInstitutionBorrower();
			info.setInstitutionReceiverId(institution.getInstitutionBorrowerId());
			info.setReceiverId(institution.getInstitutionBorrowerId());
			info.setReceiverName(institution.getInstitutionName());
			info.setPhoneNumber(institution.getContactPhone());
			info.setBorrowerType(BorrowerTypeEnum.INSTITUTION);
			info.setEmail(institution.getContactEmail());
		}

		return info;
	}

	// Response DTO
	@Getter
	@Setter
	public static class MessageResponse {
		private String message;
		private MessageRequest request;

		public MessageResponse(String message, MessageRequest request) {
			this.message = message;
			this.request = request;
		}
	}

	/**
	 * Get all active cardex (loan comments) for a given loan.
	 */
	public List<LoanCommentResponse> getByLoan(Long loanId) {
		MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
		return loanCommentRepository.findByIsActiveAndAdOrgIDAndLoan(true, utils.getAD_Org_ID(), loan).stream()
				.map(objectsMapper::mapCardex).collect(Collectors.toList());
	}

	public Page<LoanCommentResponse> getCardexByBorrower(String borrowerType, Long borrowerId, String searchTerm,
			Long addedByUserId, int page, int size) {

		Long orgId = utils.getAD_Org_ID();
		Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
		Page<MLoanComment> cardexPage;

		// CASE 1: All three parameters provided (borrowerType, borrowerId, searchTerm)
		if (borrowerType != null && borrowerId != null && searchTerm != null && !searchTerm.trim().isEmpty()) {
			Long userId = (addedByUserId != null) ? addedByUserId : 0L;

			switch (borrowerType.toUpperCase()) {
			case "INDIVIDUAL":
				cardexPage = loanCommentRepository.findByIndividualBorrowerWithSearch(borrowerId, true, orgId,
						"%" + searchTerm.toLowerCase() + "%", userId, pageable);
				break;
			case "GROUP":
				cardexPage = loanCommentRepository.findByGroupBorrowerWithSearch(borrowerId, true, orgId,
						"%" + searchTerm.toLowerCase() + "%", userId, pageable);
				break;
			case "INSTITUTION":
				cardexPage = loanCommentRepository.findByInstitutionBorrowerWithSearch(borrowerId, true, orgId,
						"%" + searchTerm.toLowerCase() + "%", userId, pageable);
				break;
			default:
				cardexPage = loanCommentRepository.enhancedComprehensiveSearch(true, orgId, null, null,
						"%" + searchTerm.toLowerCase() + "%", userId, pageable);
			}
		}
		// CASE 2: Only search term is provided
		else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			Long userId = (addedByUserId != null) ? addedByUserId : 0L;
			cardexPage = loanCommentRepository.enhancedComprehensiveSearch(true, orgId, null, null,
					"%" + searchTerm.toLowerCase() + "%", userId, pageable);

		}
		// CASE 3: Only added by user is specified without search term
		else if (addedByUserId != null) {
			cardexPage = loanCommentRepository.findByAddedBy(true, orgId, addedByUserId, pageable);
		}
		// CASE 4: Only borrower type and ID are specified
		else if (borrowerType != null && borrowerId != null) {
			switch (borrowerType.toUpperCase()) {
			case "INDIVIDUAL":
				cardexPage = loanCommentRepository.findByIndividualBorrower(borrowerId, true, orgId, pageable);
				break;
			case "GROUP":
				cardexPage = loanCommentRepository.findByGroupBorrower(borrowerId, true, orgId, pageable);
				break;
			case "INSTITUTION":
				cardexPage = loanCommentRepository.findByInstitutionBorrower(borrowerId, true, orgId, pageable);
				break;
			default:
				cardexPage = loanCommentRepository.findByIsActiveAndAdOrgID(true, orgId, pageable);
			}
		}
		// CASE 5: Default: return all cardex for org
		else {
			cardexPage = loanCommentRepository.findByIsActiveAndAdOrgID(true, orgId, pageable);
		}

		// Map to response DTO
		return cardexPage.map(objectsMapper::mapCardex);
	}

	/**
	 * Get all active cardex (loan comments) for a given installment.
	 */
	public List<LoanCommentResponse> getByInstallment(Long installmentId) {
		MInstallments installment = installmentRepository.findById(installmentId).orElse(null);
		return loanCommentRepository.findByIsActiveAndAdOrgIDAndInstallment(true, utils.getAD_Org_ID(), installment)
				.stream().map(objectsMapper::mapCardex).collect(Collectors.toList());
	}

	public List<BorrowerDetails> fetchBorrowerDetails(long adOrgId) {

		String sql = " WITH params AS (\n" + " SELECT :adOrgId AS AD_Org_ID \n" + "\n" + "),\n" + "\n"
				+ "allDebtors AS (\n" + "    SELECT \n" + "        AD_Debtor_ID AS borrower_id,\n"
				+ "        AD_Debtor_ID AS individual_borrower_id,\n" + "        0 AS group_borrower_id,\n"
				+ "        0 AS institution_borrower_id,\n"
				+ "        CONCAT(first_name, ' ', last_name) AS borrower_name,\n" + "        email,\n"
				+ "        phone,\n" + "        'INDIVIDUAL' AS borrower_type\n" + "    FROM AD_Debtor \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n" + "\n" + "    UNION ALL\n"
				+ "\n" + "    SELECT \n" + "        AD_Group_Borrower_ID AS borrower_id,\n"
				+ "        0 AS individual_borrower_id,\n" + "        AD_Group_Borrower_ID AS group_borrower_id,\n"
				+ "        0 AS institution_borrower_id,\n" + "        group_name AS borrower_name,\n"
				+ "        contact_email AS email,\n" + "        contact_phone AS phone,\n"
				+ "        'GROUP' AS borrower_type\n" + "    FROM AD_Group_Borrower \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n" + "\n" + "    UNION ALL\n"
				+ "\n" + "    SELECT \n" + "        AD_Institution_Borrower_ID AS borrower_id,\n"
				+ "        0 AS individual_borrower_id,\n" + "        0 AS group_borrower_id,\n"
				+ "        AD_Institution_Borrower_ID AS institution_borrower_id,\n"
				+ "        institution_name AS borrower_name,\n" + "        contact_email AS email,\n"
				+ "        contact_phone AS phone,\n" + "        'INSTITUTION' AS borrower_type\n"
				+ "    FROM AD_Institution_Borrower \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n" + "),\n" + "\n"
				+ "latestLoan AS (\n" + "    SELECT \n" + "        l.*,\n" + "        ROW_NUMBER() OVER (\n"
				+ "            PARTITION BY COALESCE(l.AD_Debtor_ID, l.AD_Group_Borrower_ID, l.AD_Institution_Borrower_ID)\n"
				+ "            ORDER BY l.AD_Loan_Application_ID DESC\n" + "        ) AS rn\n"
				+ "    FROM AD_Loan_Application l WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params)\n" + "),\n" + "\n"
				+ "activeLoans AS (\n" + "    SELECT \n"
				+ "        COALESCE(AD_Debtor_ID, AD_Group_Borrower_ID, AD_Institution_Borrower_ID) AS borrower_id,\n"
				+ "        COUNT(*) AS total_active_loans\n" + "    FROM AD_Loan_Application\n"
				+ "    WHERE balance > 0 AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n"
				+ "    GROUP BY COALESCE(AD_Debtor_ID, AD_Group_Borrower_ID, AD_Institution_Borrower_ID)\n" + ")\n"
				+ "\n" + "SELECT \n" + "    d.borrower_id,\n" + "d.individual_borrower_id,\n"
				+ "	d.group_borrower_id,\n" + "	d.institution_borrower_id,    d.borrower_name,\n" + "    d.email,\n"
				+ "    d.phone,\n" + "    d.borrower_type,\n" + "\n" + "    -- Latest Loan (1 per borrower)\n"
				+ "    l.documentno,\n" + "    l.applied_amount,\n" + "    l.approved_amount,\n"
				+ "    l.interests_earned,\n" + "    l.penalty_earned,\n" + "    l.balance,\n" + "    l.due_date,\n"
				+ "\n" + "    -- Active loans per borrower (NULL becomes 0)\n"
				+ "    COALESCE(a.total_active_loans, 0) AS total_active_loans\n" + "\n" + "FROM allDebtors d\n"
				+ "LEFT JOIN latestLoan l\n" + "    ON l.rn = 1\n" + "    AND (\n"
				+ "        (d.individual_borrower_id     = l.AD_Debtor_ID)\n"
				+ "        OR (d.group_borrower_id       = l.AD_Group_Borrower_ID)\n"
				+ "        OR (d.institution_borrower_id = l.AD_Institution_Borrower_ID)\n" + "    )\n"
				+ "LEFT JOIN activeLoans a\n" + "   ON d.borrower_id = a.borrower_id\n" + "\n"
				+ "ORDER BY d.borrower_name;\n" + " ";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);

		return jdbc.query(sql, params, (rs, rowNum) -> {

			BorrowerDetails b = new BorrowerDetails();

			b.setBorrowerId(rs.getLong("borrower_id"));
			b.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			b.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			b.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));

			b.setBorrowerName(rs.getString("borrower_name"));
			b.setPhoneNumber(rs.getString("phone"));
			b.setEmail(rs.getString("email"));

			b.setBorrowerType(BorrowerTypeEnum.valueOf(rs.getString("borrower_type")));

			b.setLoanDocumentNo(rs.getString("documentno"));
			b.setAmountApplied(rs.getBigDecimal("applied_amount"));
			b.setAmountApproved(rs.getBigDecimal("approved_amount"));
			b.setBalance(rs.getBigDecimal("balance"));
			b.setInterestAccrued(rs.getBigDecimal("interests_earned"));
			b.setPenaltyCharged(rs.getBigDecimal("penalty_earned"));
			b.setDueDate(rs.getTimestamp("due_date"));

			b.setTotalActiveloans(rs.getInt("total_active_loans"));

			return b;
		});
	}

	public List<BorrowerDetails> searchBorrower(long adOrgId, String searchTerm) {

		String sql = "WITH params AS (\n" + " SELECT :adOrgId AS AD_Org_ID ,:searchTerm AS searchTerm\n" + "\n" + "),\n"
				+ "\n" + "allDebtors AS (\n" + "    SELECT \n" + "        AD_Debtor_ID AS borrower_id,\n"
				+ "        AD_Debtor_ID AS individual_borrower_id,\n" + "        0 AS group_borrower_id,\n"
				+ "        0 AS institution_borrower_id,\n"
				+ "        CONCAT(first_name, ' ', last_name) AS borrower_name,\n" + "        email,\n"
				+ "        phone,\n" + "        'INDIVIDUAL' AS borrower_type\n" + "    FROM AD_Debtor \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
				+ "	AND( documentno ILIKE (SELECT searchTerm FROM params) OR email ILIKE (SELECT searchTerm FROM params) \n"
				+ "	OR phone ILIKE (SELECT searchTerm FROM params) OR first_name ILIKE (SELECT searchTerm FROM params) OR last_name ILIKE (SELECT searchTerm FROM params)\n"
				+ "	OR CONCAT(first_name, ' ', last_name) ILIKE (SELECT searchTerm FROM params) OR external_refrence_no ILIKE (SELECT searchTerm FROM params) OR \n"
				+ "	national_id ILIKE (SELECT searchTerm FROM params))\n" + "\n" + "    UNION ALL\n" + "\n"
				+ "    SELECT \n" + "        AD_Group_Borrower_ID AS borrower_id,\n"
				+ "        0 AS individual_borrower_id,\n" + "        AD_Group_Borrower_ID AS group_borrower_id,\n"
				+ "        0 AS institution_borrower_id,\n" + "        group_name AS borrower_name,\n"
				+ "        contact_email AS email,\n" + "        contact_phone AS phone,\n"
				+ "        'GROUP' AS borrower_type\n" + "    FROM AD_Group_Borrower \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
				+ "	AND (documentno ILIKE (SELECT searchTerm FROM params) OR group_name ILIKE (SELECT searchTerm FROM params)\n"
				+ "	OR contact_email ILIKE (SELECT searchTerm FROM params) OR contact_phone ILIKE (SELECT searchTerm FROM params)\n"
				+ "	OR external_refrence_no ILIKE (SELECT searchTerm FROM params) OR group_type ILIKE (SELECT searchTerm FROM params) \n"
				+ "	OR registration_number ILIKE (SELECT searchTerm FROM params))\n" + "\n" + "    UNION ALL\n" + "\n"
				+ "    SELECT \n" + "        AD_Institution_Borrower_ID AS borrower_id,\n"
				+ "        0 AS individual_borrower_id,\n" + "        0 AS group_borrower_id,\n"
				+ "        AD_Institution_Borrower_ID AS institution_borrower_id,\n"
				+ "        institution_name AS borrower_name,\n" + "        contact_email AS email,\n"
				+ "        contact_phone AS phone,\n" + "        'INSTITUTION' AS borrower_type\n"
				+ "    FROM AD_Institution_Borrower \n"
				+ "    WHERE isactive = TRUE AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n"
				+ "	AND( documentno ILIKE (SELECT searchTerm FROM params) OR institution_name ILIKE (SELECT searchTerm FROM params) OR\n"
				+ "	contact_email ILIKE (SELECT searchTerm FROM params) OR contact_phone ILIKE (SELECT searchTerm FROM params) OR\n"
				+ "	registration_number ILIKE (SELECT searchTerm FROM params) OR external_refrence_no ILIKE (SELECT searchTerm FROM params))\n"
				+ "),\n" + "\n" + "latestLoan AS (\n" + "    SELECT \n" + "        l.*,\n"
				+ "        ROW_NUMBER() OVER (\n"
				+ "            PARTITION BY COALESCE(l.AD_Debtor_ID, l.AD_Group_Borrower_ID, l.AD_Institution_Borrower_ID)\n"
				+ "            ORDER BY l.AD_Loan_Application_ID DESC\n" + "        ) AS rn\n"
				+ "    FROM AD_Loan_Application l WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params)\n" + "),\n" + "\n"
				+ "activeLoans AS (\n" + "    SELECT \n"
				+ "        COALESCE(AD_Debtor_ID, AD_Group_Borrower_ID, AD_Institution_Borrower_ID) AS borrower_id,\n"
				+ "        COUNT(*) AS total_active_loans\n" + "    FROM AD_Loan_Application\n"
				+ "    WHERE balance > 0 AND AD_Org_ID=(SELECT AD_Org_ID FROM params)\n"
				+ "    GROUP BY COALESCE(AD_Debtor_ID, AD_Group_Borrower_ID, AD_Institution_Borrower_ID)\n" + ")\n"
				+ "\n" + "SELECT \n" + "    d.borrower_id,\n" + "	d.individual_borrower_id,\n"
				+ "	d.group_borrower_id,\n" + "	d.institution_borrower_id,\n" + "    d.borrower_name,\n"
				+ "    d.email,\n" + "    d.phone,\n" + "    d.borrower_type,\n" + "\n"
				+ "    -- Latest Loan (1 per borrower)\n" + "    l.documentno,\n" + "    l.applied_amount,\n"
				+ "    l.approved_amount,\n" + "    l.interests_earned,\n" + "    l.penalty_earned,\n"
				+ "    l.balance,\n" + "    l.due_date,\n" + "\n"
				+ "    -- Active loans per borrower (NULL becomes 0)\n"
				+ "    COALESCE(a.total_active_loans, 0) AS total_active_loans\n" + "\n" + "FROM allDebtors d\n"
				+ "LEFT JOIN latestLoan l\n" + "    ON l.rn = 1\n" + "    AND (\n"
				+ "        (d.individual_borrower_id     = l.AD_Debtor_ID)\n"
				+ "        OR (d.group_borrower_id       = l.AD_Group_Borrower_ID)\n"
				+ "        OR (d.institution_borrower_id = l.AD_Institution_Borrower_ID)\n" + "    )\n"
				+ "LEFT JOIN activeLoans a\n" + "   ON d.borrower_id = a.borrower_id\n" + "\n"
				+ "ORDER BY d.borrower_name;\n" + "";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("searchTerm", "%" + searchTerm + "%");

		return jdbc.query(sql, params, (rs, rowNum) -> {

			BorrowerDetails b = new BorrowerDetails();

			b.setBorrowerId(rs.getLong("borrower_id"));
			b.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			b.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			b.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));

			b.setBorrowerName(rs.getString("borrower_name"));
			b.setPhoneNumber(rs.getString("phone"));
			b.setEmail(rs.getString("email"));

			b.setBorrowerType(BorrowerTypeEnum.valueOf(rs.getString("borrower_type")));

			b.setLoanDocumentNo(rs.getString("documentno"));
			b.setAmountApplied(rs.getBigDecimal("applied_amount"));
			b.setAmountApproved(rs.getBigDecimal("approved_amount"));
			b.setBalance(rs.getBigDecimal("balance"));
			b.setInterestAccrued(rs.getBigDecimal("interests_earned"));
			b.setPenaltyCharged(rs.getBigDecimal("penalty_earned"));
			b.setDueDate(rs.getTimestamp("due_date"));

			b.setTotalActiveloans(rs.getInt("total_active_loans"));

			return b;
		});
	}

	public Page<BorrowerDetails> getBorrowers(int page, int size, String searchTerm) {
		List<BorrowerDetails> all = new ArrayList<>();
		if (searchTerm != null && !searchTerm.isEmpty()) {
			searchTerm = "%" + searchTerm + "%";
			all = searchBorrower(utils.getAD_Org_ID(), searchTerm);
		} else {
			all = fetchBorrowerDetails(utils.getAD_Org_ID());
		}

		return utils.paginate(all, page, size);
	}

	public Page<BorrowerWithMessagesResponse> getBorrowersWithMessages(int page, int size, String searchTerm,
			Date dateFrom, Date dateTo) {
		Calendar cal = Calendar.getInstance();

		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}
		List<BorrowerWithMessagesResponse> all = new ArrayList<>();

		if (searchTerm != null && !searchTerm.isEmpty()) {
			searchTerm = "%" + searchTerm + "%";
			all = searchBorrowersWithMessages(utils.getAD_Org_ID(), dateFrom, dateTo, searchTerm);
		} else {
			all = fetchBorrowersWithMessages(utils.getAD_Org_ID(), dateFrom, dateTo);
		}

		return utils.paginate(all, page, size);
	}

	public List<BorrowerWithMessagesResponse> fetchBorrowersWithMessages(long adOrgId, Date dateFrom, Date dateTo) {
		String sql = "WITH params AS (SELECT :adOrgId AS ad_org_id, :dateFrom AS date_from, :dateTo AS date_to),\n"
				+ "\n" + "message_counts AS (\n" + "    SELECT \n"
				+ "        -- Get the borrower ID (whichever is not zero)\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        -- Determine borrower type\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN 'INDIVIDUAL'\n"
				+ "            WHEN group_borrower_id > 0 THEN 'GROUP'\n"
				+ "            WHEN institution_borrower_id > 0 THEN 'INSTITUTION'\n" + "            ELSE 'UNKNOWN'\n"
				+ "        END AS borrower_type,\n" + "        \n" + "        -- Get actual ID values for joining\n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id,\n" + "        \n"
				+ "        -- Get the message count (only within date range)\n"
				+ "        COUNT(AD_Message_center_ID) AS messages_sent\n" + "        \n"
				+ "    FROM AD_Message_center\n" + "    WHERE ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND isactive = true\n" + "        AND messaging_time::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            individual_borrower_id > 0 \n" + "            OR group_borrower_id > 0 \n"
				+ "            OR institution_borrower_id > 0\n" + "        )\n" + "    GROUP BY \n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id\n" + "),\n" + "\n" + "last_message_sent AS (\n" + "    SELECT \n"
				+ "        CASE \n" + "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        messaging_time,\n" + "        message,\n"
				+ "        ROW_NUMBER() OVER(\n" + "            PARTITION BY \n" + "                CASE \n"
				+ "                    WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "                    WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "                    WHEN institution_borrower_id > 0 THEN institution_borrower_id\n"
				+ "                    ELSE 0\n" + "                END \n"
				+ "            ORDER BY messaging_time DESC\n" + "        ) AS rn \n" + "    FROM AD_Message_center \n"
				+ "    WHERE isactive = true\n" + "        AND ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND messaging_time::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "),\n" + "\n" + "loan_balance AS (\n"
				+ "    SELECT \n" + "        -- Determine which ID to use for joining\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n"
				+ "        END AS borrower_id,\n" + "        borrower_type,\n" + "        SUM(balance) AS balance\n"
				+ "    FROM AD_Loan_Application \n" + "    WHERE isactive = true \n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND balance > 0 \n"
				+ "        AND approvalstage = 'APPROVED' \n" + "        AND isapproved = true \n"
				+ "        AND ammend = false\n"
				+ "        AND borrower_type IN ('INDIVIDUAL', 'GROUP', 'INSTITUTION')\n" + "    GROUP BY \n"
				+ "        borrower_type,\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n" + "        END\n"
				+ ")\n" + "\n" + "SELECT \n" + "    mc.borrower_id,\n" + "    mc.borrower_type,\n"
				+ "    mc.messages_sent,\n" + "    mc.individual_borrower_id,\n" + "    mc.institution_borrower_id,\n"
				+ "    mc.group_borrower_id,\n" + "    \n" + "    -- Get borrower details based on type\n"
				+ "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "        WHEN 'GROUP' THEN g.group_name\n" + "        WHEN 'INSTITUTION' THEN i.institution_name\n"
				+ "        ELSE 'Unknown'\n" + "    END AS borrower_name,\n" + "    \n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.email\n" + "        WHEN 'GROUP' THEN g.contact_email\n"
				+ "        WHEN 'INSTITUTION' THEN i.contact_email\n" + "        ELSE NULL\n" + "    END AS email,\n"
				+ "    \n" + "    CASE mc.borrower_type\n" + "        WHEN 'INDIVIDUAL' THEN d.phone\n"
				+ "        WHEN 'GROUP' THEN g.contact_phone\n" + "        WHEN 'INSTITUTION' THEN i.contact_phone\n"
				+ "        ELSE NULL\n" + "    END AS phone,\n" + "    \n"
				+ "    ls.messaging_time AS last_messaging_time,\n" + "    ls.message AS last_message,\n"
				+ "    COALESCE(lb.balance, 0) AS balance\n" + "\n" + "FROM message_counts mc\n" + "\n"
				+ "-- Join with appropriate borrower tables\n" + "LEFT JOIN AD_Debtor d \n"
				+ "    ON mc.borrower_type = 'INDIVIDUAL' \n" + "    AND d.AD_Debtor_ID = mc.individual_borrower_id\n"
				+ "    AND d.isactive = true\n" + "    \n" + "LEFT JOIN AD_Group_Borrower g \n"
				+ "    ON mc.borrower_type = 'GROUP' \n" + "    AND g.AD_Group_Borrower_ID = mc.group_borrower_id\n"
				+ "    AND g.isactive = true\n" + "    \n" + "LEFT JOIN AD_Institution_Borrower i \n"
				+ "    ON mc.borrower_type = 'INSTITUTION' \n"
				+ "    AND i.AD_Institution_Borrower_ID = mc.institution_borrower_id\n" + "    AND i.isactive = true\n"
				+ "\n"
				+ "-- Join for last message time (INNER JOIN to only show borrowers with messages in date range)\n"
				+ "INNER JOIN last_message_sent ls \n" + "    ON mc.borrower_id = ls.borrower_id \n"
				+ "    AND ls.rn = 1\n" + "\n"
				+ "-- Join for loan balance - Using borrower_id for consistent matching\n"
				+ "LEFT JOIN loan_balance lb \n" + "    ON mc.borrower_type = lb.borrower_type \n"
				+ "    AND mc.borrower_id = lb.borrower_id\n" + "\n" + "ORDER BY mc.messages_sent DESC;";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("dateFrom", dateFrom);
		params.put("dateTo", dateTo);

		return jdbc.query(sql, params, (rs, rowNum) -> {
			BorrowerWithMessagesResponse response = new BorrowerWithMessagesResponse();

			response.setBorrowerId(rs.getLong("borrower_id"));
			response.setBorrowerType(rs.getString("borrower_type"));
			response.setMessagesSent(rs.getInt("messages_sent"));
			response.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			response.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			response.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			response.setBorrowerName(rs.getString("borrower_name"));
			response.setEmail(rs.getString("email"));
			response.setPhone(rs.getString("phone"));

			if (rs.getTimestamp("last_messaging_time") != null) {
				response.setLastMessageTime(rs.getTimestamp("last_messaging_time").toLocalDateTime());
			}

			response.setLastMessage(rs.getString("last_message"));
			response.setBalance(rs.getDouble("balance"));

			return response;
		});
	}

	public List<BorrowerWithMessagesResponse> searchBorrowersWithMessages(long adOrgId, Date dateFrom, Date dateTo,
			String searchTerm) {
		String sql = "WITH params AS (\n" + "    SELECT :adOrgId AS ad_org_id, \n"
				+ "           :searchTerm AS search_term,\n" + "           :dateFrom AS date_from,\n"
				+ "           :dateTo AS date_to\n" + "),\n" + "\n" + "message_counts AS (\n" + "    SELECT \n"
				+ "        -- Get the borrower ID (whichever is not zero)\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        -- Determine borrower type\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN 'INDIVIDUAL'\n"
				+ "            WHEN group_borrower_id > 0 THEN 'GROUP'\n"
				+ "            WHEN institution_borrower_id > 0 THEN 'INSTITUTION'\n" + "            ELSE 'UNKNOWN'\n"
				+ "        END AS borrower_type,\n" + "        \n" + "        -- Get actual ID values for joining\n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id,\n" + "        \n"
				+ "        -- Get the message count (only within date range)\n"
				+ "        COUNT(AD_Message_center_ID) AS messages_sent\n" + "        \n"
				+ "    FROM AD_Message_center mc\n" + "    WHERE mc.ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND mc.isactive = true\n" + "        AND mc.messaging_time::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            mc.individual_borrower_id > 0 \n" + "            OR mc.group_borrower_id > 0 \n"
				+ "            OR mc.institution_borrower_id > 0\n" + "        )\n" + "    GROUP BY \n"
				+ "        mc.individual_borrower_id,\n" + "        mc.group_borrower_id,\n"
				+ "        mc.institution_borrower_id\n" + "),\n" + "\n" + "last_message_sent AS (\n" + "    SELECT \n"
				+ "        CASE \n" + "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        messaging_time,\n" + "        message,\n"
				+ "        ROW_NUMBER() OVER(\n" + "            PARTITION BY \n" + "                CASE \n"
				+ "                    WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "                    WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "                    WHEN institution_borrower_id > 0 THEN institution_borrower_id\n"
				+ "                    ELSE 0\n" + "                END \n"
				+ "            ORDER BY messaging_time DESC\n" + "        ) AS rn \n" + "    FROM AD_Message_center \n"
				+ "    WHERE isactive = true\n" + "        AND ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND messaging_time::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "),\n" + "\n" + "loan_balance AS (\n"
				+ "    SELECT \n" + "        -- Determine which ID to use for joining\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n"
				+ "        END AS borrower_id,\n" + "        borrower_type,\n" + "        SUM(balance) AS balance\n"
				+ "    FROM AD_Loan_Application \n" + "    WHERE isactive = true \n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND balance > 0 \n"
				+ "        AND approvalstage = 'APPROVED' \n" + "        AND isapproved = true \n"
				+ "        AND ammend = false\n"
				+ "        AND borrower_type IN ('INDIVIDUAL', 'GROUP', 'INSTITUTION')\n" + "    GROUP BY \n"
				+ "        borrower_type,\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n" + "        END\n"
				+ ")\n" + "\n" + "SELECT \n" + "    mc.borrower_id,\n" + "    mc.borrower_type,\n"
				+ "    mc.messages_sent,\n" + "    mc.individual_borrower_id,\n" + "    mc.institution_borrower_id,\n"
				+ "    mc.group_borrower_id,\n" + "    \n" + "    -- Get borrower details based on type\n"
				+ "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "        WHEN 'GROUP' THEN g.group_name\n" + "        WHEN 'INSTITUTION' THEN i.institution_name\n"
				+ "        ELSE 'Unknown'\n" + "    END AS borrower_name,\n" + "    \n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.email\n" + "        WHEN 'GROUP' THEN g.contact_email\n"
				+ "        WHEN 'INSTITUTION' THEN i.contact_email\n" + "        ELSE NULL\n" + "    END AS email,\n"
				+ "    \n" + "    CASE mc.borrower_type\n" + "        WHEN 'INDIVIDUAL' THEN d.phone\n"
				+ "        WHEN 'GROUP' THEN g.contact_phone\n" + "        WHEN 'INSTITUTION' THEN i.contact_phone\n"
				+ "        ELSE NULL\n" + "    END AS phone,\n" + "    \n"
				+ "    ls.messaging_time AS last_messaging_time,\n" + "    ls.message AS last_message,\n"
				+ "    COALESCE(lb.balance, 0) AS balance\n" + "\n" + "FROM message_counts mc\n" + "\n"
				+ "-- Join with appropriate borrower tables\n" + "LEFT JOIN AD_Debtor d \n"
				+ "    ON mc.borrower_type = 'INDIVIDUAL' \n" + "    AND d.AD_Debtor_ID = mc.individual_borrower_id\n"
				+ "    AND d.isactive = true\n" + "    \n" + "LEFT JOIN AD_Group_Borrower g \n"
				+ "    ON mc.borrower_type = 'GROUP' \n" + "    AND g.AD_Group_Borrower_ID = mc.group_borrower_id\n"
				+ "    AND g.isactive = true\n" + "    \n" + "LEFT JOIN AD_Institution_Borrower i \n"
				+ "    ON mc.borrower_type = 'INSTITUTION' \n"
				+ "    AND i.AD_Institution_Borrower_ID = mc.institution_borrower_id\n" + "    AND i.isactive = true\n"
				+ "\n"
				+ "-- Join for last message time (INNER JOIN to only show borrowers with messages in date range)\n"
				+ "INNER JOIN last_message_sent ls \n" + "    ON mc.borrower_id = ls.borrower_id \n"
				+ "    AND ls.rn = 1\n" + "\n"
				+ "-- Join for loan balance - Using borrower_id for consistent matching\n"
				+ "LEFT JOIN loan_balance lb \n" + "    ON mc.borrower_type = lb.borrower_type \n"
				+ "    AND mc.borrower_id = lb.borrower_id\n" + "\n" + "-- Apply search filter\n" + "WHERE \n"
				+ "    (mc.borrower_type = 'INDIVIDUAL' AND (\n"
				+ "        d.first_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.last_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        CONCAT(d.first_name, ' ', d.last_name) ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.phone ILIKE (SELECT search_term FROM params)\n" + "    )) OR\n"
				+ "    (mc.borrower_type = 'GROUP' AND (\n"
				+ "        g.group_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        g.contact_email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        g.contact_phone ILIKE (SELECT search_term FROM params)\n" + "    )) OR\n"
				+ "    (mc.borrower_type = 'INSTITUTION' AND (\n"
				+ "        i.institution_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        i.contact_email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        i.contact_phone ILIKE (SELECT search_term FROM params)\n" + "    ))\n" + "\n"
				+ "ORDER BY mc.messages_sent DESC;";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("dateFrom", dateFrom);
		params.put("dateTo", dateTo);
		params.put("searchTerm", "%" + searchTerm + "%");

		return jdbc.query(sql, params, (rs, rowNum) -> {
			BorrowerWithMessagesResponse response = new BorrowerWithMessagesResponse();

			response.setBorrowerId(rs.getLong("borrower_id"));
			response.setBorrowerType(rs.getString("borrower_type"));
			response.setMessagesSent(rs.getInt("messages_sent"));
			response.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			response.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			response.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			response.setBorrowerName(rs.getString("borrower_name"));
			response.setEmail(rs.getString("email"));
			response.setPhone(rs.getString("phone"));

			if (rs.getTimestamp("last_messaging_time") != null) {
				response.setLastMessageTime(rs.getTimestamp("last_messaging_time").toLocalDateTime());
			}

			response.setLastMessage(rs.getString("last_message"));
			response.setBalance(rs.getDouble("balance"));

			return response;
		});
	}

	// Add these methods to your existing LoanMessagingService class

	public List<CardexBorrowers> fetchCardexBorrowers(long adOrgId) {
		String sql = "WITH \n" + "active_loans AS (\n" + "    SELECT * FROM AD_Loan_Application l\n"
				+ "    WHERE l.balance > 0 \n" + "        AND l.AD_Org_ID = :adOrgId\n"
				+ "        AND l.isactive = true\n" + "),\n" + "\n" + "borrower_base AS (\n" + "    SELECT \n"
				+ "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.group_name\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.institution_name\n"
				+ "        END AS borrower_name,\n" + "        l.borrower_type,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.phone\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.contact_phone\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.contact_phone\n"
				+ "        END AS phone_number,\n" + "        l.AD_Loan_Application_ID,\n" + "        l.balance,\n"
				+ "        l.approved_amount,\n" + "        l.AD_Debtor_ID AS individual_borrower_id,\n"
				+ "        l.AD_Group_Borrower_ID AS group_borrower_id,\n"
				+ "        l.AD_Institution_Borrower_ID AS institution_borrower_id,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.documentno\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.documentno\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.documentno\n"
				+ "        END AS borrower_no,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.ad_debtor_id\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.ad_group_borrower_id\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.ad_institution_borrower_id\n"
				+ "        END AS borrower_id\n" + "    FROM active_loans l\n"
				+ "    LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL'\n"
				+ "    LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP'\n"
				+ "    LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION'\n"
				+ "),\n" + "\n" + "payments_agg AS (\n" + "    SELECT \n" + "        p.AD_Loan_Application_ID,\n"
				+ "        SUM(p.amount) AS paid_amount\n" + "    FROM AD_Payment p\n"
				+ "    WHERE p.docstatus = 'COMPLETED' \n" + "        AND p.AD_Org_ID = :adOrgId\n"
				+ "    GROUP BY p.AD_Loan_Application_ID\n" + "),\n" + "\n" + "cardex_agg AS (\n" + "    SELECT \n"
				+ "        c.AD_Loan_Application_ID,\n" + "        COUNT(*) AS cardex_count,\n"
				+ "        MAX(c.created) AS last_cardex_date\n" + "    FROM AD_Loan_Comment c\n"
				+ "    WHERE c.isactive = true \n" + "        AND c.AD_Org_ID = :adOrgId\n"
				+ "    GROUP BY c.AD_Loan_Application_ID\n" + "),\n" + "\n" + "latest_cardex_details AS (\n"
				+ "    SELECT DISTINCT ON (c.AD_Loan_Application_ID)\n" + "        c.AD_Loan_Application_ID,\n"
				+ "        c.notes AS latest_notes,\n" + "        c.created AS latest_date,\n"
				+ "        CONCAT(u.first_name, ' ', u.last_name) AS recorded_by\n" + "    FROM AD_Loan_Comment c\n"
				+ "    LEFT JOIN AD_User u ON u.AD_User_ID = c.notes_taken_by\n" + "    WHERE c.isactive = true \n"
				+ "        AND c.AD_Org_ID = :adOrgId\n"
				+ "    ORDER BY c.AD_Loan_Application_ID, c.created DESC, c.AD_Loan_Comment_ID DESC\n" + "),\n" + "\n"
				+ "borrower_latest_feedback AS (\n" + "    SELECT \n" + "        bb.borrower_name,\n"
				+ "        bb.borrower_type,\n" + "        bb.phone_number,\n" + "        bb.borrower_no,\n"
				+ "        lcd.latest_notes AS latest_feedback,\n"
				+ "        lcd.recorded_by AS last_cardex_recorded_by,\n"
				+ "        MAX(lcd.latest_date) AS latest_feedback_date\n" + "    FROM borrower_base bb\n"
				+ "    JOIN latest_cardex_details lcd ON lcd.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "    GROUP BY bb.borrower_name, bb.borrower_type, bb.phone_number, bb.borrower_no, lcd.latest_notes, lcd.recorded_by\n"
				+ ")\n" + "\n" + "SELECT \n" + "    bb.borrower_name,\n" + "    bb.borrower_no,\n"
				+ "    bb.borrower_type,\n" + "    bb.phone_number,\n" + "    MAX(bb.borrower_id) AS borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'INDIVIDUAL' THEN bb.individual_borrower_id ELSE NULL END) AS individual_borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'GROUP' THEN bb.group_borrower_id ELSE NULL END) AS group_borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'INSTITUTION' THEN bb.institution_borrower_id ELSE NULL END) AS institution_borrower_id,\n"
				+ "    COUNT(DISTINCT bb.AD_Loan_Application_ID) AS total_active_loans,\n"
				+ "    SUM(bb.balance) AS total_outstanding_balance,\n"
				+ "    SUM(bb.approved_amount) AS total_approved_amount,\n"
				+ "    COALESCE(SUM(pa.paid_amount), 0) AS total_paid_amount,\n"
				+ "    COALESCE(SUM(ca.cardex_count), 0) AS total_cardex_entries,\n"
				+ "    MAX(ca.last_cardex_date) AS last_cardex_date,\n"
				+ "    MAX(blf.latest_feedback) AS latest_feedback,\n"
				+ "    MAX(blf.last_cardex_recorded_by) AS last_cardex_recorded_by\n" + "FROM borrower_base bb\n"
				+ "LEFT JOIN payments_agg pa ON pa.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "INNER JOIN cardex_agg ca ON ca.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "LEFT JOIN borrower_latest_feedback blf ON blf.borrower_name = bb.borrower_name\n"
				+ "    AND blf.borrower_type = bb.borrower_type\n" + "    AND blf.phone_number = bb.phone_number\n"
				+ "    AND (blf.borrower_no = bb.borrower_no OR (blf.borrower_no IS NULL AND bb.borrower_no IS NULL))\n"
				+ "GROUP BY bb.borrower_name, bb.borrower_type, bb.phone_number, bb.borrower_no\n"
				+ "HAVING COUNT(DISTINCT bb.AD_Loan_Application_ID) > 0\n"
				+ "    AND COALESCE(SUM(ca.cardex_count), 0) > 0\n"
				+ "ORDER BY total_outstanding_balance DESC, bb.borrower_name";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);

		return jdbc.query(sql, params, (rs, rowNum) -> {
			CardexBorrowers cardex = new CardexBorrowers();

			cardex.setBorrowerName(rs.getString("borrower_name"));
			cardex.setBorrowerNo(rs.getString("borrower_no"));
			cardex.setBorrowerType(rs.getString("borrower_type"));
			cardex.setPhoneNumber(rs.getString("phone_number"));
			cardex.setBorrowerId(rs.getLong("borrower_id"));
			cardex.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			cardex.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			cardex.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			cardex.setTotalActiveLoans(rs.getInt("total_active_loans"));
			cardex.setTotalOutstandingBalance(rs.getBigDecimal("total_outstanding_balance"));
			cardex.setTotalApprovedAmount(rs.getBigDecimal("total_approved_amount"));
			cardex.setTotalPaidAmount(rs.getBigDecimal("total_paid_amount"));
			cardex.setTotalCardexEntries(rs.getInt("total_cardex_entries"));

			java.sql.Timestamp lastCardexTimestamp = rs.getTimestamp("last_cardex_date");
			cardex.setLastCardexDate(lastCardexTimestamp != null ? lastCardexTimestamp.toString() : null);

			cardex.setLatestFeedback(rs.getString("latest_feedback"));
			cardex.setLastCardexRecordedBy(rs.getString("last_cardex_recorded_by"));

			return cardex;
		});
	}

	public List<CardexBorrowers> searchCardexBorrowers(long adOrgId, String searchTerm) {
		String sql = "WITH \n" + "active_loans AS (\n" + "    SELECT * FROM AD_Loan_Application l\n"
				+ "    WHERE l.balance > 0 \n" + "        AND l.AD_Org_ID = :adOrgId\n"
				+ "        AND l.isactive = true\n" + "),\n" + "\n" + "borrower_base AS (\n" + "    SELECT \n"
				+ "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.group_name\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.institution_name\n"
				+ "        END AS borrower_name,\n" + "        l.borrower_type,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.phone\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.contact_phone\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.contact_phone\n"
				+ "        END AS phone_number,\n" + "        l.AD_Loan_Application_ID,\n" + "        l.balance,\n"
				+ "        l.approved_amount,\n" + "        l.AD_Debtor_ID AS individual_borrower_id,\n"
				+ "        l.AD_Group_Borrower_ID AS group_borrower_id,\n"
				+ "        l.AD_Institution_Borrower_ID AS institution_borrower_id,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.documentno\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.documentno\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.documentno\n"
				+ "        END AS borrower_no,\n" + "        CASE \n"
				+ "            WHEN l.borrower_type = 'INDIVIDUAL' THEN d.ad_debtor_id\n"
				+ "            WHEN l.borrower_type = 'GROUP' THEN g.ad_group_borrower_id\n"
				+ "            WHEN l.borrower_type = 'INSTITUTION' THEN inst.ad_institution_borrower_id\n"
				+ "        END AS borrower_id\n" + "    FROM active_loans l\n"
				+ "    LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL'\n"
				+ "    LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP'\n"
				+ "    LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION'\n"
				+ "    WHERE (\n" + "        (l.borrower_type = 'INDIVIDUAL' AND (\n"
				+ "            d.first_name ILIKE :searchTerm OR \n" + "            d.last_name ILIKE :searchTerm OR \n"
				+ "            CONCAT(d.first_name, ' ', d.last_name) ILIKE :searchTerm OR\n"
				+ "            d.phone ILIKE :searchTerm OR\n" + "            d.documentno ILIKE :searchTerm\n"
				+ "        )) OR\n" + "        (l.borrower_type = 'GROUP' AND (\n"
				+ "            g.group_name ILIKE :searchTerm OR\n"
				+ "            g.contact_phone ILIKE :searchTerm OR\n" + "            g.documentno ILIKE :searchTerm\n"
				+ "        )) OR\n" + "        (l.borrower_type = 'INSTITUTION' AND (\n"
				+ "            inst.institution_name ILIKE :searchTerm OR\n"
				+ "            inst.contact_phone ILIKE :searchTerm OR\n"
				+ "            inst.documentno ILIKE :searchTerm\n" + "        )) OR\n"
				+ "        l.documentno ILIKE :searchTerm\n" + "    )\n" + "),\n" + "\n" + "payments_agg AS (\n"
				+ "    SELECT \n" + "        p.AD_Loan_Application_ID,\n" + "        SUM(p.amount) AS paid_amount\n"
				+ "    FROM AD_Payment p\n" + "    WHERE p.docstatus = 'COMPLETED' \n"
				+ "        AND p.AD_Org_ID = :adOrgId\n" + "    GROUP BY p.AD_Loan_Application_ID\n" + "),\n" + "\n"
				+ "cardex_agg AS (\n" + "    SELECT \n" + "        c.AD_Loan_Application_ID,\n"
				+ "        COUNT(*) AS cardex_count,\n" + "        MAX(c.created) AS last_cardex_date\n"
				+ "    FROM AD_Loan_Comment c\n" + "    WHERE c.isactive = true \n"
				+ "        AND c.AD_Org_ID = :adOrgId\n" + "    GROUP BY c.AD_Loan_Application_ID\n" + "),\n" + "\n"
				+ "latest_cardex_details AS (\n" + "    SELECT DISTINCT ON (c.AD_Loan_Application_ID)\n"
				+ "        c.AD_Loan_Application_ID,\n" + "        c.notes AS latest_notes,\n"
				+ "        c.created AS latest_date,\n"
				+ "        CONCAT(u.first_name, ' ', u.last_name) AS recorded_by\n" + "    FROM AD_Loan_Comment c\n"
				+ "    LEFT JOIN AD_User u ON u.AD_User_ID = c.notes_taken_by\n" + "    WHERE c.isactive = true \n"
				+ "        AND c.AD_Org_ID = :adOrgId\n"
				+ "    ORDER BY c.AD_Loan_Application_ID, c.created DESC, c.AD_Loan_Comment_ID DESC\n" + "),\n" + "\n"
				+ "borrower_latest_feedback AS (\n" + "    SELECT \n" + "        bb.borrower_name,\n"
				+ "        bb.borrower_type,\n" + "        bb.phone_number,\n" + "        bb.borrower_no,\n"
				+ "        lcd.latest_notes AS latest_feedback,\n"
				+ "        lcd.recorded_by AS last_cardex_recorded_by,\n"
				+ "        MAX(lcd.latest_date) AS latest_feedback_date\n" + "    FROM borrower_base bb\n"
				+ "    JOIN latest_cardex_details lcd ON lcd.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "    GROUP BY bb.borrower_name, bb.borrower_type, bb.phone_number, bb.borrower_no, lcd.latest_notes, lcd.recorded_by\n"
				+ ")\n" + "\n" + "SELECT \n" + "    bb.borrower_name,\n" + "    bb.borrower_no,\n"
				+ "    bb.borrower_type,\n" + "    bb.phone_number,\n" + "    MAX(bb.borrower_id) AS borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'INDIVIDUAL' THEN bb.individual_borrower_id ELSE NULL END) AS individual_borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'GROUP' THEN bb.group_borrower_id ELSE NULL END) AS group_borrower_id,\n"
				+ "    MAX(CASE WHEN bb.borrower_type = 'INSTITUTION' THEN bb.institution_borrower_id ELSE NULL END) AS institution_borrower_id,\n"
				+ "    COUNT(DISTINCT bb.AD_Loan_Application_ID) AS total_active_loans,\n"
				+ "    SUM(bb.balance) AS total_outstanding_balance,\n"
				+ "    SUM(bb.approved_amount) AS total_approved_amount,\n"
				+ "    COALESCE(SUM(pa.paid_amount), 0) AS total_paid_amount,\n"
				+ "    COALESCE(SUM(ca.cardex_count), 0) AS total_cardex_entries,\n"
				+ "    MAX(ca.last_cardex_date) AS last_cardex_date,\n"
				+ "    MAX(blf.latest_feedback) AS latest_feedback,\n"
				+ "    MAX(blf.last_cardex_recorded_by) AS last_cardex_recorded_by\n" + "FROM borrower_base bb\n"
				+ "LEFT JOIN payments_agg pa ON pa.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "INNER JOIN cardex_agg ca ON ca.AD_Loan_Application_ID = bb.AD_Loan_Application_ID\n"
				+ "LEFT JOIN borrower_latest_feedback blf ON blf.borrower_name = bb.borrower_name\n"
				+ "    AND blf.borrower_type = bb.borrower_type\n" + "    AND blf.phone_number = bb.phone_number\n"
				+ "    AND (blf.borrower_no = bb.borrower_no OR (blf.borrower_no IS NULL AND bb.borrower_no IS NULL))\n"
				+ "GROUP BY bb.borrower_name, bb.borrower_type, bb.phone_number, bb.borrower_no\n"
				+ "HAVING COUNT(DISTINCT bb.AD_Loan_Application_ID) > 0\n"
				+ "    AND COALESCE(SUM(ca.cardex_count), 0) > 0\n"
				+ "ORDER BY total_outstanding_balance DESC, bb.borrower_name";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("searchTerm", "%" + searchTerm + "%");

		return jdbc.query(sql, params, (rs, rowNum) -> {
			CardexBorrowers cardex = new CardexBorrowers();

			cardex.setBorrowerName(rs.getString("borrower_name"));
			cardex.setBorrowerNo(rs.getString("borrower_no"));
			cardex.setBorrowerType(rs.getString("borrower_type"));
			cardex.setPhoneNumber(rs.getString("phone_number"));
			cardex.setBorrowerId(rs.getLong("borrower_id"));
			cardex.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			cardex.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			cardex.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			cardex.setTotalActiveLoans(rs.getInt("total_active_loans"));
			cardex.setTotalOutstandingBalance(rs.getBigDecimal("total_outstanding_balance"));
			cardex.setTotalApprovedAmount(rs.getBigDecimal("total_approved_amount"));
			cardex.setTotalPaidAmount(rs.getBigDecimal("total_paid_amount"));
			cardex.setTotalCardexEntries(rs.getInt("total_cardex_entries"));

			java.sql.Timestamp lastCardexTimestamp = rs.getTimestamp("last_cardex_date");
			cardex.setLastCardexDate(lastCardexTimestamp != null ? lastCardexTimestamp.toString() : null);

			cardex.setLatestFeedback(rs.getString("latest_feedback"));
			cardex.setLastCardexRecordedBy(rs.getString("last_cardex_recorded_by"));

			return cardex;
		});
	}

	public Page<CardexBorrowers> getCardexBorrowers(int page, int size, String searchTerm) {
		List<CardexBorrowers> all = new ArrayList<>();
		long orgId = utils.getAD_Org_ID();

		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			all = searchCardexBorrowers(orgId, searchTerm);
		} else {
			all = fetchCardexBorrowers(orgId);
		}

		return utils.paginate(all, page, size);
	}

	public Page<MMessagingCenter> getAllMessageCenter(int page, int size, String searchTerm, String messageStatus,
			Date dateFrom, Date dateTo) {
		DBConnect connect = utils.getDBConnect();

		Calendar cal = Calendar.getInstance();

		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}

		boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
		boolean hasStatus = messageStatus != null && !messageStatus.trim().isEmpty();

		MessageStatus statusEnum = null;
		if (hasStatus) {
			try {
				statusEnum = MessageStatus.fromValue(messageStatus);
			} catch (Exception ex) {
				statusEnum = null;
				hasStatus = false;
			}
		}

		long orgId = 0;
		try {
			orgId = utils.getAD_Org_ID();
		} catch (Exception e) {
			orgId = 0;
		}

		PageRequest pageable = PageRequest.of(page, size);

		if (hasSearch && hasStatus && statusEnum != null) {
			return messagingCenterRepository.searchMessages(true, orgId, statusEnum, searchTerm.trim(), dateFrom,
					dateTo, pageable);
		}

		if (hasSearch && !hasStatus) {
			return messagingCenterRepository.searchMessagesNoStatus(true, orgId, searchTerm.trim(), dateFrom, dateTo,
					pageable);
		}

		if (!hasSearch && hasStatus && statusEnum != null) {
			return messagingCenterRepository.findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetween(true, orgId,
					statusEnum, dateFrom, dateTo, pageable);
		}

		return messagingCenterRepository.findByIsActiveAndAdOrgIDAndCreatedBetween(true, orgId, dateFrom, dateTo,
				pageable);
	}

	public Page<MMessagingCenter> getMessagesSentByBorrower(int page, int size, String searchTerm, String messageStatus,
			Date dateFrom, Date dateTo, long receiverId, String borrowerType) {
		Calendar cal = Calendar.getInstance();

		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}

		boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
		boolean hasStatus = messageStatus != null && !messageStatus.trim().isEmpty();

		MessageStatus statusEnum = null;
		BorrowerTypeEnum borrowerTypeEnum = null;
		if (borrowerType != null) {
			borrowerTypeEnum = BorrowerTypeEnum.fromValue(borrowerType);
		}

		if (hasStatus) {
			try {
				statusEnum = MessageStatus.fromValue(messageStatus);
			} catch (Exception ex) {
				statusEnum = null;
				hasStatus = false;
			}
		}

		long orgId = 0;
		try {
			orgId = utils.getAD_Org_ID();
		} catch (Exception e) {
			orgId = 0;
		}

		PageRequest pageable = PageRequest.of(page, size);

		if (hasSearch && hasStatus && statusEnum != null) {
			return messagingCenterRepository.searchMessagesByBorrowerId(true, orgId, statusEnum, searchTerm.trim(),
					dateFrom, dateTo, borrowerTypeEnum, receiverId, pageable);
		}

		if (hasSearch && !hasStatus) {
			return messagingCenterRepository.searchMessagesNoStatusByBorrowerId(true, orgId, searchTerm.trim(),
					dateFrom, dateTo, borrowerTypeEnum, receiverId, pageable);
		}

		if (!hasSearch && hasStatus && statusEnum != null) {
			return messagingCenterRepository
					.findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetweenAndReceiverIdAndBorrowerTypeOrderByMessagingTimeDesc(
							true, orgId, statusEnum, dateFrom, dateTo, receiverId, borrowerTypeEnum, pageable);
		}

		return messagingCenterRepository
				.findByIsActiveAndAdOrgIDAndCreatedBetweenAndReceiverIdAndBorrowerTypeOrderByMessagingTimeDesc(true,
						orgId, dateFrom, dateTo, receiverId, borrowerTypeEnum, pageable);
	}

	public Page<BorrowerWithMessagesResponse> getBorrowersWithSmsReminders(int page, int size, String searchTerm,
			Date dateFrom, Date dateTo) {
		Calendar cal = Calendar.getInstance();

		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}

		List<BorrowerWithMessagesResponse> all = new ArrayList<>();

		if (searchTerm != null && !searchTerm.isEmpty()) {
			searchTerm = "%" + searchTerm + "%";
			all = searchBorrowersWithSmsReminders(utils.getAD_Org_ID(), dateFrom, dateTo, searchTerm);
		} else {
			all = fetchBorrowersWithSmsReminders(utils.getAD_Org_ID(), dateFrom, dateTo);
		}

		return utils.paginate(all, page, size);
	}

	public List<BorrowerWithMessagesResponse> fetchBorrowersWithSmsReminders(long adOrgId, Date dateFrom, Date dateTo) {
		String sql = "WITH params AS (SELECT :adOrgId AS ad_org_id, :dateFrom AS date_from, :dateTo AS date_to),\n"
				+ "\n" + "message_counts AS (\n" + "    SELECT \n"
				+ "        -- Get the borrower ID (whichever is not zero)\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        -- Determine borrower type\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN 'INDIVIDUAL'\n"
				+ "            WHEN group_borrower_id > 0 THEN 'GROUP'\n"
				+ "            WHEN institution_borrower_id > 0 THEN 'INSTITUTION'\n" + "            ELSE 'UNKNOWN'\n"
				+ "        END AS borrower_type,\n" + "        \n" + "        -- Get actual ID values for joining\n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id,\n" + "        \n"
				+ "        -- Get the message count (only within date range)\n"
				+ "        COUNT(AD_Sms_ID) AS messages_sent\n" + "        \n" + "    FROM AD_Sms\n"
				+ "    WHERE ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND isactive = true\n"
				+ "        AND created::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            individual_borrower_id > 0 \n" + "            OR group_borrower_id > 0 \n"
				+ "            OR institution_borrower_id > 0\n" + "        )\n" + "    GROUP BY \n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id\n" + "),\n" + "\n" + "last_message_sent AS (\n" + "    SELECT \n"
				+ "        CASE \n" + "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        created AS message_time,\n" + "        message,\n"
				+ "        ROW_NUMBER() OVER(\n" + "            PARTITION BY \n" + "                CASE \n"
				+ "                    WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "                    WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "                    WHEN institution_borrower_id > 0 THEN institution_borrower_id\n"
				+ "                    ELSE 0\n" + "                END \n" + "            ORDER BY created DESC\n"
				+ "        ) AS rn \n" + "    FROM AD_Sms \n" + "    WHERE isactive = true\n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND created::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            individual_borrower_id > 0 \n" + "            OR group_borrower_id > 0 \n"
				+ "            OR institution_borrower_id > 0\n" + "        )\n" + "),\n" + "\n" + "loan_balance AS (\n"
				+ "    SELECT \n" + "        -- Determine which ID to use for joining\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n"
				+ "        END AS borrower_id,\n" + "        borrower_type,\n" + "        SUM(balance) AS balance\n"
				+ "    FROM AD_Loan_Application \n" + "    WHERE isactive = true \n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND balance > 0 \n"
				+ "        AND approvalstage = 'APPROVED' \n" + "        AND isapproved = true \n"
				+ "        AND ammend = false\n"
				+ "        AND borrower_type IN ('INDIVIDUAL', 'GROUP', 'INSTITUTION')\n" + "    GROUP BY \n"
				+ "        borrower_type,\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n" + "        END\n"
				+ ")\n" + "\n" + "SELECT \n" + "    mc.borrower_id,\n" + "    mc.borrower_type,\n"
				+ "    mc.messages_sent,\n" + "    mc.individual_borrower_id,\n" + "    mc.institution_borrower_id,\n"
				+ "    mc.group_borrower_id,\n" + "    \n" + "    -- Get borrower details based on type\n"
				+ "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "        WHEN 'GROUP' THEN g.group_name\n" + "        WHEN 'INSTITUTION' THEN i.institution_name\n"
				+ "        ELSE 'Unknown'\n" + "    END AS borrower_name,\n" + "    \n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.email\n" + "        WHEN 'GROUP' THEN g.contact_email\n"
				+ "        WHEN 'INSTITUTION' THEN i.contact_email\n" + "        ELSE NULL\n" + "    END AS email,\n"
				+ "    \n" + "    CASE mc.borrower_type\n" + "        WHEN 'INDIVIDUAL' THEN d.phone\n"
				+ "        WHEN 'GROUP' THEN g.contact_phone\n" + "        WHEN 'INSTITUTION' THEN i.contact_phone\n"
				+ "        ELSE NULL\n" + "    END AS phone,\n" + "    \n"
				+ "    ls.message_time AS last_message_time,\n" + "    ls.message AS last_message,\n"
				+ "    COALESCE(lb.balance, 0) AS balance,\n" + "    \n"
				+ "    -- Additional fields from AD_Sms that might be useful\n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.isactive\n" + "        WHEN 'GROUP' THEN g.isactive\n"
				+ "        WHEN 'INSTITUTION' THEN i.isactive\n" + "        ELSE false\n"
				+ "    END AS borrower_is_active\n" + "\n" + "FROM message_counts mc\n" + "\n"
				+ "-- Join with appropriate borrower tables\n" + "LEFT JOIN AD_Debtor d \n"
				+ "    ON mc.borrower_type = 'INDIVIDUAL' \n" + "    AND d.AD_Debtor_ID = mc.individual_borrower_id\n"
				+ "    AND d.isactive = true\n" + "    \n" + "LEFT JOIN AD_Group_Borrower g \n"
				+ "    ON mc.borrower_type = 'GROUP' \n" + "    AND g.AD_Group_Borrower_ID = mc.group_borrower_id\n"
				+ "    AND g.isactive = true\n" + "    \n" + "LEFT JOIN AD_Institution_Borrower i \n"
				+ "    ON mc.borrower_type = 'INSTITUTION' \n"
				+ "    AND i.AD_Institution_Borrower_ID = mc.institution_borrower_id\n" + "    AND i.isactive = true\n"
				+ "\n"
				+ "-- Join for last message time (INNER JOIN to only show borrowers with messages in date range)\n"
				+ "INNER JOIN last_message_sent ls \n" + "    ON mc.borrower_id = ls.borrower_id \n"
				+ "    AND ls.rn = 1\n" + "\n"
				+ "-- Join for loan balance - Using borrower_id for consistent matching\n"
				+ "LEFT JOIN loan_balance lb \n" + "    ON mc.borrower_type = lb.borrower_type \n"
				+ "    AND mc.borrower_id = lb.borrower_id\n" + "\n" + "ORDER BY mc.messages_sent DESC;";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("dateFrom", dateFrom);
		params.put("dateTo", dateTo);

		return jdbc.query(sql, params, (rs, rowNum) -> {
			BorrowerWithMessagesResponse response = new BorrowerWithMessagesResponse();

			response.setBorrowerId(rs.getLong("borrower_id"));
			response.setBorrowerType(rs.getString("borrower_type"));
			response.setMessagesSent(rs.getInt("messages_sent"));
			response.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			response.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			response.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			response.setBorrowerName(rs.getString("borrower_name"));
			response.setEmail(rs.getString("email"));
			response.setPhone(rs.getString("phone"));

			if (rs.getTimestamp("last_message_time") != null) {
				response.setLastMessageTime(rs.getTimestamp("last_message_time").toLocalDateTime());
			}

			response.setLastMessage(rs.getString("last_message"));
			response.setBalance(rs.getDouble("balance"));

			return response;
		});
	}

	public List<BorrowerWithMessagesResponse> searchBorrowersWithSmsReminders(long adOrgId, Date dateFrom, Date dateTo,
			String searchTerm) {
		String sql = "WITH params AS (\n" + "    SELECT :adOrgId AS ad_org_id, \n"
				+ "           :searchTerm AS search_term,\n" + "           :dateFrom AS date_from,\n"
				+ "           :dateTo AS date_to\n" + "),\n" + "\n" + "message_counts AS (\n" + "    SELECT \n"
				+ "        -- Get the borrower ID (whichever is not zero)\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        -- Determine borrower type\n" + "        CASE \n"
				+ "            WHEN individual_borrower_id > 0 THEN 'INDIVIDUAL'\n"
				+ "            WHEN group_borrower_id > 0 THEN 'GROUP'\n"
				+ "            WHEN institution_borrower_id > 0 THEN 'INSTITUTION'\n" + "            ELSE 'UNKNOWN'\n"
				+ "        END AS borrower_type,\n" + "        \n" + "        -- Get actual ID values for joining\n"
				+ "        individual_borrower_id,\n" + "        group_borrower_id,\n"
				+ "        institution_borrower_id,\n" + "        \n"
				+ "        -- Get the message count (only within date range)\n"
				+ "        COUNT(AD_Sms_ID) AS messages_sent\n" + "        \n" + "    FROM AD_Sms sms\n"
				+ "    WHERE sms.ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND sms.isactive = true\n"
				+ "        AND sms.created::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            sms.individual_borrower_id > 0 \n" + "            OR sms.group_borrower_id > 0 \n"
				+ "            OR sms.institution_borrower_id > 0\n" + "        )\n" + "    GROUP BY \n"
				+ "        sms.individual_borrower_id,\n" + "        sms.group_borrower_id,\n"
				+ "        sms.institution_borrower_id\n" + "),\n" + "\n" + "last_message_sent AS (\n" + "    SELECT \n"
				+ "        CASE \n" + "            WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "            WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "            WHEN institution_borrower_id > 0 THEN institution_borrower_id\n" + "            ELSE 0\n"
				+ "        END AS borrower_id,\n" + "        created AS message_time,\n" + "        message,\n"
				+ "        ROW_NUMBER() OVER(\n" + "            PARTITION BY \n" + "                CASE \n"
				+ "                    WHEN individual_borrower_id > 0 THEN individual_borrower_id\n"
				+ "                    WHEN group_borrower_id > 0 THEN group_borrower_id\n"
				+ "                    WHEN institution_borrower_id > 0 THEN institution_borrower_id\n"
				+ "                    ELSE 0\n" + "                END \n" + "            ORDER BY created DESC\n"
				+ "        ) AS rn \n" + "    FROM AD_Sms \n" + "    WHERE isactive = true\n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n"
				+ "        AND created::timestamp BETWEEN \n"
				+ "            (SELECT date_from::timestamp FROM params) AND \n"
				+ "            (SELECT date_to::timestamp FROM params)\n" + "        AND (\n"
				+ "            individual_borrower_id > 0 \n" + "            OR group_borrower_id > 0 \n"
				+ "            OR institution_borrower_id > 0\n" + "        )\n" + "),\n" + "\n" + "loan_balance AS (\n"
				+ "    SELECT \n" + "        -- Determine which ID to use for joining\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n"
				+ "        END AS borrower_id,\n" + "        borrower_type,\n" + "        SUM(balance) AS balance\n"
				+ "    FROM AD_Loan_Application \n" + "    WHERE isactive = true \n"
				+ "        AND ad_org_id = (SELECT ad_org_id FROM params)\n" + "        AND balance > 0 \n"
				+ "        AND approvalstage = 'APPROVED' \n" + "        AND isapproved = true \n"
				+ "        AND ammend = false\n"
				+ "        AND borrower_type IN ('INDIVIDUAL', 'GROUP', 'INSTITUTION')\n" + "    GROUP BY \n"
				+ "        borrower_type,\n" + "        CASE \n"
				+ "            WHEN borrower_type = 'INDIVIDUAL' THEN ad_debtor_id\n"
				+ "            WHEN borrower_type = 'GROUP' THEN ad_group_borrower_id\n"
				+ "            WHEN borrower_type = 'INSTITUTION' THEN ad_institution_borrower_id\n" + "        END\n"
				+ ")\n" + "\n" + "SELECT \n" + "    mc.borrower_id,\n" + "    mc.borrower_type,\n"
				+ "    mc.messages_sent,\n" + "    mc.individual_borrower_id,\n" + "    mc.institution_borrower_id,\n"
				+ "    mc.group_borrower_id,\n" + "    \n" + "    -- Get borrower details based on type\n"
				+ "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name)\n"
				+ "        WHEN 'GROUP' THEN g.group_name\n" + "        WHEN 'INSTITUTION' THEN i.institution_name\n"
				+ "        ELSE 'Unknown'\n" + "    END AS borrower_name,\n" + "    \n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.email\n" + "        WHEN 'GROUP' THEN g.contact_email\n"
				+ "        WHEN 'INSTITUTION' THEN i.contact_email\n" + "        ELSE NULL\n" + "    END AS email,\n"
				+ "    \n" + "    CASE mc.borrower_type\n" + "        WHEN 'INDIVIDUAL' THEN d.phone\n"
				+ "        WHEN 'GROUP' THEN g.contact_phone\n" + "        WHEN 'INSTITUTION' THEN i.contact_phone\n"
				+ "        ELSE NULL\n" + "    END AS phone,\n" + "    \n"
				+ "    ls.message_time AS last_message_time,\n" + "    ls.message AS last_message,\n"
				+ "    COALESCE(lb.balance, 0) AS balance,\n" + "    \n"
				+ "    -- Additional fields from AD_Sms that might be useful\n" + "    CASE mc.borrower_type\n"
				+ "        WHEN 'INDIVIDUAL' THEN d.isactive\n" + "        WHEN 'GROUP' THEN g.isactive\n"
				+ "        WHEN 'INSTITUTION' THEN i.isactive\n" + "        ELSE false\n"
				+ "    END AS borrower_is_active\n" + "\n" + "FROM message_counts mc\n" + "\n"
				+ "-- Join with appropriate borrower tables\n" + "LEFT JOIN AD_Debtor d \n"
				+ "    ON mc.borrower_type = 'INDIVIDUAL' \n" + "    AND d.AD_Debtor_ID = mc.individual_borrower_id\n"
				+ "    AND d.isactive = true\n" + "    \n" + "LEFT JOIN AD_Group_Borrower g \n"
				+ "    ON mc.borrower_type = 'GROUP' \n" + "    AND g.AD_Group_Borrower_ID = mc.group_borrower_id\n"
				+ "    AND g.isactive = true\n" + "    \n" + "LEFT JOIN AD_Institution_Borrower i \n"
				+ "    ON mc.borrower_type = 'INSTITUTION' \n"
				+ "    AND i.AD_Institution_Borrower_ID = mc.institution_borrower_id\n" + "    AND i.isactive = true\n"
				+ "\n"
				+ "-- Join for last message time (INNER JOIN to only show borrowers with messages in date range)\n"
				+ "INNER JOIN last_message_sent ls \n" + "    ON mc.borrower_id = ls.borrower_id \n"
				+ "    AND ls.rn = 1\n" + "\n"
				+ "-- Join for loan balance - Using borrower_id for consistent matching\n"
				+ "LEFT JOIN loan_balance lb \n" + "    ON mc.borrower_type = lb.borrower_type \n"
				+ "    AND mc.borrower_id = lb.borrower_id\n" + "\n" + "-- Apply search filter\n" + "WHERE \n"
				+ "    (mc.borrower_type = 'INDIVIDUAL' AND (\n"
				+ "        d.first_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.last_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        CONCAT(d.first_name, ' ', d.last_name) ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        d.phone ILIKE (SELECT search_term FROM params)\n" + "    )) OR\n"
				+ "    (mc.borrower_type = 'GROUP' AND (\n"
				+ "        g.group_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        g.contact_email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        g.contact_phone ILIKE (SELECT search_term FROM params)\n" + "    )) OR\n"
				+ "    (mc.borrower_type = 'INSTITUTION' AND (\n"
				+ "        i.institution_name ILIKE (SELECT search_term FROM params) OR\n"
				+ "        i.contact_email ILIKE (SELECT search_term FROM params) OR\n"
				+ "        i.contact_phone ILIKE (SELECT search_term FROM params)\n" + "    ))\n" + "\n"
				+ "ORDER BY mc.messages_sent DESC;";

		Map<String, Object> params = new HashMap<>();
		params.put("adOrgId", adOrgId);
		params.put("dateFrom", dateFrom);
		params.put("dateTo", dateTo);
		params.put("searchTerm", searchTerm);

		return jdbc.query(sql, params, (rs, rowNum) -> {
			BorrowerWithMessagesResponse response = new BorrowerWithMessagesResponse();

			response.setBorrowerId(rs.getLong("borrower_id"));
			response.setBorrowerType(rs.getString("borrower_type"));
			response.setMessagesSent(rs.getInt("messages_sent"));
			response.setIndividualBorrowerId(rs.getLong("individual_borrower_id"));
			response.setInstitutionBorrowerId(rs.getLong("institution_borrower_id"));
			response.setGroupBorrowerId(rs.getLong("group_borrower_id"));
			response.setBorrowerName(rs.getString("borrower_name"));
			response.setEmail(rs.getString("email"));
			response.setPhone(rs.getString("phone"));

			if (rs.getTimestamp("last_message_time") != null) {
				response.setLastMessageTime(rs.getTimestamp("last_message_time").toLocalDateTime());
			}

			response.setLastMessage(rs.getString("last_message"));
			response.setBalance(rs.getDouble("balance"));

			return response;
		});
	}

	public Page<MSms> getSmsRemindersSent(int page, int size, String searchTerm, String messageStatus, Date dateFrom,
			Date dateTo) {
		DBConnect connect = utils.getDBConnect();

		Calendar cal = Calendar.getInstance();

		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}

		boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
		boolean hasStatus = messageStatus != null && !messageStatus.trim().isEmpty();

		MessageStatus statusEnum = null;
		if (hasStatus) {
			try {
				statusEnum = MessageStatus.fromValue(messageStatus);
			} catch (Exception ex) {
				statusEnum = null;
				hasStatus = false;
			}
		}

		long orgId = 0;
		try {
			orgId = utils.getAD_Org_ID();
		} catch (Exception e) {
			orgId = 0;
		}

		PageRequest pageable = PageRequest.of(page, size);

		if (hasSearch && hasStatus && statusEnum != null) {
			return smsRepository.searchMessages(true, orgId, statusEnum, searchTerm.trim(), dateFrom, dateTo, pageable);
		}

		if (hasSearch && !hasStatus) {
			return smsRepository.searchMessagesNoStatus(true, orgId, searchTerm.trim(), dateFrom, dateTo, pageable);
		}

		if (!hasSearch && hasStatus && statusEnum != null) {
			return smsRepository.findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(true,
					orgId, statusEnum, dateFrom, dateTo, pageable);
		}

		return smsRepository.findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(true, orgId, dateFrom, dateTo,
				pageable);
	}

	public Page<MSms> getSmsRemindersSentByBorrowerId(int page, int size, String searchTerm, String messageStatus,
			Date dateFrom, Date dateTo, long borrowerId, String borrowerType) {

		BorrowerTypeEnum borrowerTypeEnum = null;
		if (borrowerType != null) {
			try {
				borrowerTypeEnum = BorrowerTypeEnum.fromValue(borrowerType);
			} catch (Exception e) {
				borrowerTypeEnum = null;
			}
		}

		Calendar cal = Calendar.getInstance();
		if (dateFrom == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -100);
			dateFrom = cal.getTime();
		}

		if (dateTo == null) {
			cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 100);
			dateTo = cal.getTime();
		}

		if (dateFrom.after(dateTo)) {
			Date temp = dateFrom;
			dateFrom = dateTo;
			dateTo = temp;
		}

		boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
		boolean hasStatus = messageStatus != null && !messageStatus.trim().isEmpty();
		MessageStatus statusEnum = null;

		if (hasStatus) {
			try {
				statusEnum = MessageStatus.fromValue(messageStatus);
			} catch (Exception ex) {
				statusEnum = null;
				hasStatus = false;
			}
		}

		long orgId = 0;
		try {
			orgId = utils.getAD_Org_ID();
		} catch (Exception e) {
			orgId = 0;
		}

		PageRequest pageable = PageRequest.of(page, size);

		// Handle different borrower types
		if (borrowerTypeEnum != null) {
			switch (borrowerTypeEnum) {
			case INDIVIDUAL:
				if (hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.searchByIndividualBorrowerIdWithStatus(orgId, borrowerId, statusEnum,
							searchTerm.trim(), dateFrom, dateTo, pageable);
				} else if (hasSearch && !hasStatus) {
					return smsRepository.searchByIndividualBorrowerId(orgId, borrowerId, searchTerm.trim(), dateFrom,
							dateTo, pageable);
				} else if (!hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.findByIndividualBorrowerIdWithStatus(orgId, borrowerId, statusEnum, dateFrom,
							dateTo, pageable);
				} else {
					return smsRepository.findByIndividualBorrowerId(orgId, borrowerId, dateFrom, dateTo, pageable);
				}

			case GROUP:
				if (hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.searchByGroupBorrowerIdWithStatus(orgId, borrowerId, statusEnum,
							searchTerm.trim(), dateFrom, dateTo, pageable);
				} else if (hasSearch && !hasStatus) {
					return smsRepository.searchByGroupBorrowerId(orgId, borrowerId, searchTerm.trim(), dateFrom, dateTo,
							pageable);
				} else if (!hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.findByGroupBorrowerIdWithStatus(orgId, borrowerId, statusEnum, dateFrom,
							dateTo, pageable);
				} else {
					return smsRepository.findByGroupBorrowerId(orgId, borrowerId, dateFrom, dateTo, pageable);
				}

			case INSTITUTION:
				if (hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.searchByInstitutionBorrowerIdWithStatus(orgId, borrowerId, statusEnum,
							searchTerm.trim(), dateFrom, dateTo, pageable);
				} else if (hasSearch && !hasStatus) {
					return smsRepository.searchByInstitutionBorrowerId(orgId, borrowerId, searchTerm.trim(), dateFrom,
							dateTo, pageable);
				} else if (!hasSearch && hasStatus && statusEnum != null) {
					return smsRepository.findByInstitutionBorrowerIdWithStatus(orgId, borrowerId, statusEnum, dateFrom,
							dateTo, pageable);
				} else {
					return smsRepository.findByInstitutionBorrowerId(orgId, borrowerId, dateFrom, dateTo, pageable);
				}

			default:
				// Fall back to general search if borrower type is unknown
				return Page.empty();
			}
		}
		return Page.empty();

	}

}