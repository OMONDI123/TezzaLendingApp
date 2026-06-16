package co.ke.tezza.loanapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MDocuments;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MGuarantorLoan;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;
import co.ke.tezza.loanapp.enums.RelationShipEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.LoanApplicationRequest;
import co.ke.tezza.loanapp.model.NextOfKins;
import co.ke.tezza.loanapp.repository.AttachmentRepository;
import co.ke.tezza.loanapp.repository.GroupBorrowersRepository;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.IndividualBorrowersRepository;
import co.ke.tezza.loanapp.repository.InstitutionBorrowersRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.LoanProductConfigRepository;
import co.ke.tezza.loanapp.repository.NextOfKinRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.LoanApplicationResponse;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoanApplicationService {

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private LoanProductConfigRepository loanProductConfigRepository;
	@Autowired
	private Utils utils;

	@Autowired
	private GroupBorrowersRepository groupBorrowerRepository;
	@Autowired
	private IndividualBorrowersRepository individualBorrowerRepository;

	@Autowired
	private InstitutionBorrowersRepository institutionBorrowerRepository;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private BorrowersServices borrowersServices;

	@Autowired
	private LoanInterestCalculatorService loanInterestCalculatorService;

	@Autowired
	private LoanProductConfigurationsService loanConfigService;

	@Autowired
	private LoanStatementService loanStatementService;

	@Autowired
	private SmsHandlersService remindersScheduler;

	@Autowired
	private GuarantorLoanRepository guarantorLoanRepository;

	@Autowired
	private NextOfKinRepository nextOfKinRepository;

	@Autowired
	private LoanApprovalWorkFlowService loanApprovalWorkFlowService;
	@Autowired
	private UserRepository userRepository;

	public ResponseEntity<LoanApplicationResponse> requestAmendment(long loanApplicationId) {
		MLoanApplication existingApplication = loanApplicationRepository.findById(loanApplicationId).orElse(null);
		if (existingApplication == null) {
			throw new SetUpExceptions("The application selected does not exists.");
		}

		String loanType = existingApplication.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";
		String message = "Amendment Request for " + loanType + " application (Ref: "
				+ existingApplication.getDocumentNo() + ") has been submitted successfully..";

		return new ResponseEntity<LoanApplicationResponse>(message, 200, mapLoanApplication(existingApplication));
	}

	@Transactional
	public ResponseEntity<List<LoanApplicationResponse>> processLoanApplicationsCsv(MultipartFile file) {
		List<LoanApplicationResponse> list = new ArrayList<>();
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Uploaded file is missing or empty");
		}

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			String line;
			String[] headers = null;
			boolean isFirstLine = true;

			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}

				if (isFirstLine) {
					headers = line.split(",", -1);
					isFirstLine = false;
					continue;
				}

				String[] row = line.split(",", -1);
				LoanApplicationRequest request = mapRowToRequest(row, headers);
				MLoanApplication existingApplication = null;
				List<DocStatus> docStatusList = List.of(DocStatus.APPROVED, DocStatus.SUBMITTED, DocStatus.DRAFT,
						DocStatus.PENDING, DocStatus.IN_PROGRESS, DocStatus.UNDER_REVIEW, DocStatus.POSTED,
						DocStatus.VERIFIED);

				if (request.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
					MGroupDebtors groupBorrower = groupBorrowerRepository.findById(request.getGroupBorrowerId())
							.orElseThrow(() -> new SetUpExceptions("Group borrower not found"));
					existingApplication = loanApplicationRepository
							.findTop1ByIsActiveAndBorrowerTypeAndGroupBorrowerAndDocStatusIn(true,
									BorrowerTypeEnum.GROUP, groupBorrower, docStatusList);

				} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
					MDebtor individualBorrower = individualBorrowerRepository
							.findById(request.getIndividualBorrowerId())
							.orElseThrow(() -> new SetUpExceptions("Individual borrower not found"));
					existingApplication = loanApplicationRepository
							.findTop1ByIsActiveAndBorrowerTypeAndIndividualBorrowerAndDocStatusIn(true,
									BorrowerTypeEnum.INDIVIDUAL, individualBorrower, docStatusList);

				} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
					MInstitutionBorrower institutionBorrower = institutionBorrowerRepository
							.findById(request.getInstitutionBorrowerId())
							.orElseThrow(() -> new SetUpExceptions("Institution borrower not found"));
					existingApplication = loanApplicationRepository
							.findTop1ByIsActiveAndBorrowerTypeAndInstitutionBorrowerAndDocStatusIn(true,
									BorrowerTypeEnum.INSTITUTION, institutionBorrower, docStatusList);
				}
				if (existingApplication != null && existingApplication.getLoanProductConfiguration()
						.getLoanProductConfigId() == request.getLoanProductId()) {
					continue;
				} else {
					list.add(mapLoanApplication(applyForLoanFromUpload(request)));
				}
			}

			return new ResponseEntity<List<LoanApplicationResponse>>(
					list.size() + " Applications Uploaded Successfully.", 200, list);

		} catch (Exception e) {
			throw new RuntimeException("Failed to process loan applications CSV", e);
		}
	}

	@Transactional
	public MLoanApplication applyForLoanFromUpload(LoanApplicationRequest request) {
		MLoanApplication existingApplication = null;
		List<MGuarantorLoan> guarantors = new ArrayList<>();
		List<DocStatus> docStatusList = List.of(DocStatus.APPROVED, DocStatus.SUBMITTED, DocStatus.DRAFT,
				DocStatus.PENDING, DocStatus.IN_PROGRESS, DocStatus.UNDER_REVIEW, DocStatus.POSTED, DocStatus.VERIFIED);

		MLoanApplication application = new MLoanApplication();
		application.setBorrowerType(request.getBorrowerType());
		MUser assignee = userRepository.findById(request.getLoanAssignedTo()).orElse(null);
		if (assignee != null) {
			application.setAssignee(assignee);
		}

		// === Resolve Borrower Type ===
		if (request.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
			MGroupDebtors groupBorrower = groupBorrowerRepository.findById(request.getGroupBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Group borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndGroupBorrowerAndDocStatusIn(true, BorrowerTypeEnum.GROUP,
							groupBorrower, docStatusList);
			application.setGroupBorrower(groupBorrower);

		} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
			MDebtor individualBorrower = individualBorrowerRepository.findById(request.getIndividualBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Individual borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndIndividualBorrowerAndDocStatusIn(true,
							BorrowerTypeEnum.INDIVIDUAL, individualBorrower, docStatusList);
			application.setIndividualBorrower(individualBorrower);

		} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
			MInstitutionBorrower institutionBorrower = institutionBorrowerRepository
					.findById(request.getInstitutionBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Institution borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndInstitutionBorrowerAndDocStatusIn(true,
							BorrowerTypeEnum.INSTITUTION, institutionBorrower, docStatusList);
			application.setInstitutionBorrower(institutionBorrower);

		} else {
			throw new SetUpExceptions("Unsupported borrower type: " + request.getBorrowerType());
		}

		application.setAppliedBy(utils.getLoggedInUser());

		if (existingApplication != null && existingApplication.getLoanProductConfiguration()
				.getLoanProductConfigId() == request.getLoanProductId()) {
			if (utils.isDebtor()) {
				throw new SetUpExceptions("Dear " + application.getAppliedBy().getFirstName() + " "
						+ application.getAppliedBy().getLastName()
						+ ", you currently have an existing loan of the same type. Please complete the repayment before reapplying.");
			}
			throw new SetUpExceptions("An active loan application of this type already exists for this borrower.");
		}

		// === Base Application Info ===
		MLoanProductConfiguration config = loanProductConfigRepository.findById(request.getLoanProductId())
				.orElseThrow(() -> new SetUpExceptions("Loan Product not found"));
		if (config == null || !config.isActive()) {
			throw new SetUpExceptions("Loan product configuration is missing or inactive.");
		}

		application.setLoanProductConfiguration(config);
		application.setAppliedAmount(request.getAppliedAmount());
		application.setTermInDays(request.getTermInDays());
		application.setExpectedDisbursementDate(request.getExpectedDisbursementDate());
		application.setActualDisbursementDate(request.getActualDisbursementDate());
		application.setPenaltyGracePeriod(config.getPenaltyGracePeriodDays());
		application.setExternalReferenceNo(request.getExternalReferenceNo());
		application.setDueDate(utils.getFutureDateUsingCalender(application.getExpectedDisbursementDate(),
				application.getTermInDays()));

		// === Validation ===
		if (request.getAppliedAmount().compareTo(config.getMinPrincipal()) < 0
				|| request.getAppliedAmount().compareTo(config.getMaxPrincipal()) > 0) {
			throw new SetUpExceptions(
					"Applied amount must be between " + config.getMinPrincipal() + " and " + config.getMaxPrincipal());
		}

		if (Boolean.TRUE.equals(config.getRequireCollateral()) && request.getCollateralValue() == null) {
			throw new SetUpExceptions("Collateral is required for this loan.");
		}

		// === Attach Collateral Documents ===
		if (request.getCollateralValue() != null && !request.getCollateralValue().isEmpty()) {
			Set<MDocuments> collateralDocs = request.getCollateralValue().stream().map(c -> {
				MDocuments doc = new MDocuments();
				if (c.getFileUpload() != null && !c.getFileUpload().isEmpty()) {
					FileUploads fileUpload = utils.uploadFileFromBase64(c.getFileUpload());
					if (fileUpload != null) {
						doc.setFileName(fileUpload.getFileName());
						doc.setActualFilePath(fileUpload.getFullFilePath());
						doc.setMimeType(fileUpload.getMimeType());
						doc.setFileSize(fileUpload.getFileSize());
						doc.setFilepath(utils.getFilePath() + doc.getFileName());
						doc.setAD_Document_UU(UUID.randomUUID().toString());
						doc.setColleteralOwner(c.getColleteralOwner());
						doc.setColleteralValue(c.getColleteralValue());
						doc.setExpiryDate(c.getExpiryDate());
						doc.setStorageDurationDaysOnLoanCompletion(c.getStorageDurationDaysOnLoanCompletion());
						doc.setColleteralNo(c.getColleteralNo());
						doc.setAttachment(attachmentRepository.findById(c.getAttachmentId()).get());
						return doc;
					}
				}
				return null;
			}).collect(Collectors.toSet());
			application.setCollateralAttachments(collateralDocs);
		}

		Set<MNextOfKin> attachedGuarantors = new HashSet<>();
		List<GuarantorTemp> tempGuarantors = new ArrayList<>();

		if (Boolean.TRUE.equals(config.getRequireGuarantors())) {
			if (request.getGuarantors() == null || request.getGuarantors().size() < config.getMinGuarantors()) {
				throw new SetUpExceptions("At least " + config.getMinGuarantors() + " guarantors required.");
			}

			for (NextOfKins g : request.getGuarantors()) {
				MNextOfKin guarantor = new MNextOfKin();
				guarantor.setFullName(g.getFullName());
				guarantor.setRelationship(g.getRelationship());
				guarantor.setPhoneNumber(g.getPhoneNumber());
				guarantor.setAddress(g.getAddress());
				guarantor.setPrimaryGuarantor(g.isPrimaryGuarantor());

				guarantor = nextOfKinRepository.save(guarantor);
				attachedGuarantors.add(guarantor);

				if (g.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0) {
					tempGuarantors.add(new GuarantorTemp(guarantor, g.getGuaranteeAmount(), g.getGuaranteeLimit(),
							g.isPrimaryGuarantor()));
				}
			}
			application.setGuarantors(attachedGuarantors);
		}

		application.setAdOrgID(utils.getAD_Org_ID());
		application.setDocStatus(DocStatus.DRAFT);
		application.setAD_LoanApplication_UU(UUID.randomUUID().toString());
		application.setBalance(BigDecimal.ZERO);
		application.setLoanState(LoanStateEnum.PENDING_APPROVAL);

		application.setDailyInterestRate(
				request.getDailyInterestRate() != null ? request.getDailyInterestRate() : BigDecimal.ZERO);
		application.setWeeklyInterestRate(
				request.getWeeklyInterestRate() != null ? request.getWeeklyInterestRate() : BigDecimal.ZERO);
		application.setMonthlyInterestRate(
				request.getMonthlyInterestRate() != null ? request.getMonthlyInterestRate() : BigDecimal.ZERO);
		application.setAnnualInterestRate(
				request.getAnnualInterestRate() != null ? request.getAnnualInterestRate() : BigDecimal.ZERO);
		application.setInteretsFlatRateAmount(
				request.getInteretsFlatRateAmount() != null ? request.getInteretsFlatRateAmount() : BigDecimal.ZERO);
		application.setInteretsFlatRate(
				request.getInteretsFlatRate() != null ? request.getInteretsFlatRate() : BigDecimal.ZERO);
		application.setCycle1FlatInterestPercent(
				request.getCycle1FlatInterestPercent() != null ? request.getCycle1FlatInterestPercent()
						: BigDecimal.ZERO);
		application.setCycle2DailyInterestPercent(
				request.getCycle2DailyInterestPercent() != null ? request.getCycle2DailyInterestPercent()
						: BigDecimal.ZERO);
		application.setCycle3PenaltyPercentPerPeriod(
				request.getCycle3PenaltyPercentPerPeriod() != null ? request.getCycle3PenaltyPercentPerPeriod()
						: BigDecimal.ZERO);

		application.setConsolidatedBillingGroupId(request.getConsolidatedBillingGroupId());
		application.setConsolidatedBilling(request.isConsolidatedBilling());


		// === Restructuring ===
		application.setRestructured(request.isRestructured());
		application.setRestructuringReason(request.getRestructuringReason());
		application.setOriginalLoanId(request.getOriginalLoanId());

		// === Additional Metadata ===
		application.setDisbursementNotes(request.getDisbursementNotes());
		application.setCancellationReason(request.getCancellationReason());
		application.setNotificationPreferences(request.getNotificationPreferences());
		application.setClosureNotes(request.getClosureNotes());

		// === Write-off Details ===
		application.setWriteOffReason(request.getWriteOffReason());
		if (request.getWriteOffApprovedBy() != null) {
			MUser writeOffApprovedBy = userRepository.findById(request.getWriteOffApprovedBy()).orElse(null);
			application.setWriteOffApprovedBy(writeOffApprovedBy);
		}
		if (request.getWrittenOffBy() != null) {
			MUser writtenOffBy = userRepository.findById(request.getWrittenOffBy()).orElse(null);
			application.setWrittenOffBy(writtenOffBy);
		}

		// === Reinstatement ===
		application.setReinstatementReason(request.getReinstatementReason());

		// === Cancellation ===
		if (request.getCancelledBy() != null) {
			MUser cancelledBy = userRepository.findById(request.getCancelledBy()).orElse(null);
			application.setCancelledBy(cancelledBy);
		}

		// === Last State Change Trigger ===
		application.setLastStateChangeTrigger(request.getLastStateChangeTrigger());

		// === Save Application FIRST ===
		application = loanApplicationRepository.save(application);

		// === Handle Consolidated Billing Relationships ===
		if (request.isConsolidatedBilling()) {
			// Case 1: This is a parent loan with existing children (by IDs)
			if (request.getChildConsolidatedLoanIds() != null && !request.getChildConsolidatedLoanIds().isEmpty()) {
				for (Long childId : request.getChildConsolidatedLoanIds()) {
					MLoanApplication child = loanApplicationRepository.findById(childId)
							.orElseThrow(() -> new SetUpExceptions("Child loan not found: " + childId));
					child.setParentConsolidatedLoan(application);
					child.setConsolidatedBillingGroupId(application.getConsolidatedBillingGroupId());
					child.setConsolidatedBilling(true);
					child.setDueDate(application.getDueDate());
					loanApplicationRepository.save(child);
				}
			}

			// Case 2: This is a parent loan with inline children
			if (request.getChildConsolidatedLoans() != null && !request.getChildConsolidatedLoans().isEmpty()) {
				for (LoanApplicationRequest childRequest : request.getChildConsolidatedLoans()) {
					childRequest.setConsolidatedBillingGroupId(application.getConsolidatedBillingGroupId());
					childRequest.setConsolidatedBilling(true);
					childRequest.setLoanProductId(request.getLoanProductId()); 
					MLoanApplication child = applyForLoanInternal(childRequest);
					child.setParentConsolidatedLoan(application);
					child.setDueDate(application.getDueDate());
					loanApplicationRepository.save(child);
				}
			}
		}

		// Handle parent reference if this is a child loan
		if (request.getParentConsolidatedLoanId() != null) {
			MLoanApplication parent = loanApplicationRepository.findById(request.getParentConsolidatedLoanId())
					.orElseThrow(() -> new SetUpExceptions("Parent loan not found"));
			application.setParentConsolidatedLoan(parent);
			application.setConsolidatedBillingGroupId(parent.getConsolidatedBillingGroupId());
			application.setConsolidatedBilling(true);
			application.setDueDate(parent.getDueDate());
			loanApplicationRepository.save(application);
		}

		if (!tempGuarantors.isEmpty()) {
			for (GuarantorTemp temp : tempGuarantors) {
				MGuarantorLoan existingGuarantorLoan = guarantorLoanRepository
						.findTop1ByLoanAndGuarantorAndIsActive(application, temp.guarantor, true);
				if (existingGuarantorLoan == null) {
					existingGuarantorLoan = new MGuarantorLoan();
				}

				existingGuarantorLoan.setGuarantor(temp.guarantor);
				existingGuarantorLoan.setLoan(application);
				existingGuarantorLoan.setGuaranteeAmount(temp.guaranteeAmount);
				existingGuarantorLoan.setGuaranteeLimit(temp.guaranteeLimit);
				existingGuarantorLoan.setGuaranteeAmountBalance(temp.guaranteeAmount);
				existingGuarantorLoan.setPrimaryGuarantor(temp.primaryGuarantor);
				guarantors.add(existingGuarantorLoan);

				guarantorLoanRepository.save(existingGuarantorLoan);
			}
		}

		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, application.getAdOrgID());
		String loanTypeCode = config.getIsDebtProduct() ? "DB" : "LN";
		String referenceNo = String.format("%s/%06d", loanTypeCode, application.getLoanApplicationId());
		application.setDocumentNo(referenceNo);

		// === Handle Approval Flow ===
		if (config.getRequiredApprovalSteps() > 0) {
			MApprovalSteps firstStep = config.getApprovalLevels().stream().filter(step -> step.getStep() == 1)
					.findFirst()
					.orElseThrow(() -> new SetUpExceptions("First approval step is not defined in configuration"));
			loanApprovalWorkFlowService.triggerApprovalStep(firstStep, application);
		} else {
			loanApprovalWorkFlowService.completeLoanApproval(application);
		}

		// === Handle Auto-Approved Loans ===
		if (application.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			BigDecimal interestExpected = loanInterestCalculatorService.calculateTotalInterest(application,
					application.getTermInDays());
			application.setTotalExpectedInterest(interestExpected);
			application.setTotalExpectedBalance(interestExpected.add(application.getAppliedAmount()));
			application.setDecliningInterest(application.getInterestsEarned());

			application.setBalance(application.getInterestsEarned().add(application.getAppliedAmount()));
			application.setLoanState(LoanStateEnum.OPEN);
			application.setStateChangeDate(new Date());

			loanApplicationRepository.save(application);
			Date expected = application.getExpectedDisbursementDate();

			LocalDateTime ldt = expected.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			loanStatementService.recordDisbursement(application.getLoanApplicationId(), null, application.getBalance(),
					application.getDocumentNo(), ldt);
			if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
				remindersScheduler.handleLoanApproval(application, null);
			}
		}

		// === Post-Commit Notifications ===
		if (sys != null && sys.isAllowSystemNotifications()) {
			MLoanApplication finalApp = application;
			loanApprovalWorkFlowService.notifyApplicantOnApproval(finalApp);

			if (finalApp.getApprovalStage().equals(ApprovalStage.APPROVED)) {
				if (!finalApp.getGuarantors().isEmpty()) {
					for (MNextOfKin guarantor : finalApp.getGuarantors()) {
						MGuarantorLoan gLoan = guarantorLoanRepository
								.findTop1ByLoanAndGuarantorAndIsActive(application, guarantor, true);
						BigDecimal guaranteeAmount = BigDecimal.ZERO;
						BigDecimal guaranteeLimit = BigDecimal.ZERO;
						if (gLoan != null && gLoan.getGuaranteeAmount() != null) {
							guaranteeAmount = gLoan.getGuaranteeAmount();
							guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit()
									: guaranteeAmount;
						}

						remindersScheduler.handleGuarantorLoanAssignmentNotification(guarantor, finalApp,
								application.getApprovalDate(),
								guaranteeAmount.compareTo(BigDecimal.ZERO) > 0 ? guaranteeAmount
										: application.getApprovedAmount(),
								guaranteeLimit.compareTo(BigDecimal.ZERO) > 0 ? guaranteeLimit
										: application.getApprovedAmount(),
								null);
					}
				}
			} else {
				if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
					remindersScheduler.handleLoanApplicationRegistration(finalApp, null);
				}
			}
		}

		return application;
	}

	@Transactional
	public ResponseEntity<LoanApplicationResponse> cancelLoan(Long loanId, String cancellationReason) {
	    MLoanApplication loan = loanApplicationRepository.findById(loanId)
	            .orElseThrow(() -> new SetUpExceptions("Loan/Debt with Id " + loanId + " not found."));
	    
	    if (loan.getLoanState() == LoanStateEnum.CLOSED) {
	        throw new SetUpExceptions("Cannot cancel a loan that is already closed.");
	    }
	    
	    if (loan.getLoanState() == LoanStateEnum.CANCELLED) {
	        throw new SetUpExceptions("Loan is already cancelled.");
	    }
	    
	    if (loan.getLoanState() == LoanStateEnum.WRITTEN_OFF) {
	        throw new SetUpExceptions("Cannot cancel a loan that has been written off.");
	    }
	    
	    if (loan.getLoanState() == LoanStateEnum.OPEN || loan.getLoanState() == LoanStateEnum.OVERDUE) {
	        if (loan.getActualDisbursementDate() != null) {
	            throw new SetUpExceptions("Cannot cancel a loan that has already been disbursed. Please use write-off instead.");
	        }
	    }
	    
	    MUser loggedInUser = utils.getLoggedInUser();
	    
	    loan.setLoanState(LoanStateEnum.CANCELLED);
	    loan.setCancelledDate(new Date());
	    loan.setCancelledBy(loggedInUser);
	    loan.setCancellationReason(cancellationReason);
	    loan.setStateChangeDate(new Date());
	    loan.setLastStateChangeTrigger("MANUAL_CANCELLATION");
	    
	    if (loan.getDocStatus() == DocStatus.DRAFT || 
	        loan.getDocStatus() == DocStatus.PENDING || 
	        loan.getDocStatus() == DocStatus.IN_PROGRESS) {
	        loan.setDocStatus(DocStatus.CANCELLED);
	    }
	    
	    loanApplicationRepository.save(loan);
	    
	    try {
	        MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
	                SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
	        if (sys != null && sys.isAllowSystemNotifications()) {
	            remindersScheduler.handleLoanCancellation(loan, cancellationReason);
	        }
	    } catch (Exception e) {
	        log.warn("Failed to send cancellation notification for loan: " + loanId, e);
	    }
	    
	    String message = "Loan (Ref: " + loan.getDocumentNo() + ") has been successfully cancelled. Reason: " + cancellationReason;
	    return new ResponseEntity<LoanApplicationResponse>(message, 200, mapLoanApplication(loan));
	}

	private LoanApplicationRequest mapRowToRequest(String[] row, String[] headers) {
		LoanApplicationRequest request = new LoanApplicationRequest();

		BorrowerTypeEnum borrowerType = BorrowerTypeEnum.fromValue(row[0]);
		request.setBorrowerType(borrowerType);

		String borrowerIdentifier = row[1];

		if (BorrowerTypeEnum.INDIVIDUAL.equals(borrowerType)) {
			MDebtor borrower = individualBorrowerRepository
					.findByIsActiveAndAdOrgIDAndDocumentNo(true, utils.getAD_Org_ID(), borrowerIdentifier)
					.orElseThrow(() -> new RuntimeException("Individual borrower not found: " + borrowerIdentifier));
			request.setIndividualBorrowerId(borrower.getIndividualBorrowerId());

		} else if (BorrowerTypeEnum.GROUP.equals(borrowerType)) {
			MGroupDebtors borrower = groupBorrowerRepository
					.findByIsActiveAndAdOrgIDAndDocumentNo(true, utils.getAD_Org_ID(), borrowerIdentifier)
					.orElseThrow(() -> new RuntimeException("Group borrower not found: " + borrowerIdentifier));
			request.setGroupBorrowerId(borrower.getGroupBorrowerId());

		} else if (BorrowerTypeEnum.INSTITUTION.equals(borrowerType)) {
			MInstitutionBorrower borrower = institutionBorrowerRepository
					.findByIsActiveAndAdOrgIDAndDocumentNo(true, utils.getAD_Org_ID(), borrowerIdentifier)
					.orElseThrow(() -> new RuntimeException("Institution borrower not found: " + borrowerIdentifier));
			request.setInstitutionBorrowerId(borrower.getInstitutionBorrowerId());
		}

		String loanProduct = row[2];
		Long loanProductId = 0L;
		if (row[2] != null && !row[2].trim().isEmpty()) {
			String value = row[2].trim();
			if (value.matches("\\d+")) {
				loanProductId = Long.valueOf(value);
			} else {
				loanProductId = 0L;
				loanProduct = value;
			}
		}
		MLoanProductConfiguration product = loanProductConfigRepository
				.findTop1ByLoanProductConfigIdOrDocumentNoOrNameOrderByLoanProductConfigIdDesc(loanProductId,
						loanProduct, loanProduct)
				.orElseThrow(() -> new RuntimeException("Loan product not found: " + row[2]));

		request.setLoanProductId(product.getLoanProductConfigId());

		request.setAppliedAmount(parseDecimal(row[3]));
		request.setApprovedAmount(parseDecimal(row[4]));
		request.setTermInDays(parseInt(row[5]));
		request.setExpectedDisbursementDate(parseDate(row[6]));
		request.setGracePeriodToFirstInstallment(parseInt(row[7]));
		request.setGraceperiod(parseInt(row[8]));
		request.setDailyInterestRate(parseDecimal(row[9]));
		request.setWeeklyInterestRate(parseDecimal(row[10]));
		request.setMonthlyInterestRate(parseDecimal(row[11]));
		request.setAnnualInterestRate(parseDecimal(row[12]));

		if (row.length > 13) {
			request.setExternalReferenceNo(row[13]);
		}

		List<NextOfKins> guarantors = new ArrayList<>();
		int columnIndex = 14;

		while (columnIndex < row.length) {
			if (columnIndex + 6 < row.length) {
				String fullName = row[columnIndex];

				if (fullName != null && !fullName.trim().isEmpty()) {
					NextOfKins kin = new NextOfKins();
					kin.setFullName(fullName.trim());

					if (columnIndex + 1 < row.length && row[columnIndex + 1] != null) {
						kin.setRelationship(RelationShipEnum.fromValue(row[columnIndex + 1]));
					}
					if (columnIndex + 2 < row.length) {
						kin.setPhoneNumber(row[columnIndex + 2]);
					}
					if (columnIndex + 3 < row.length) {
						kin.setAddress(row[columnIndex + 3]);
					}
					if (columnIndex + 4 < row.length) {
						kin.setGuaranteeAmount(parseDecimal(row[columnIndex + 4]));
					}
					if (columnIndex + 5 < row.length) {
						kin.setGuaranteeLimit(parseDecimal(row[columnIndex + 5]));
					}
					if (columnIndex + 6 < row.length) {
						kin.setPrimaryGuarantor(Boolean.parseBoolean(row[columnIndex + 6]));
					}

					guarantors.add(kin);
				}
			}

			columnIndex += 7;
		}

		request.setGuarantors(guarantors);

		return request;
	}

	private BigDecimal parseDecimal(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return new BigDecimal(value.trim());
	}

	private Integer parseInt(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return Integer.parseInt(value.trim());
	}

	private Date parseDate(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		LocalDate localDate = LocalDate.parse(value.trim());
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public void downloadLoanApplicationTemplate(HttpServletResponse response) throws IOException {
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=loan_application_template.csv");

		PrintWriter writer = response.getWriter();

		String[] headers = { "borrowerType", "borrowerIdentifier", "loanProduct", "appliedAmount", "approvedAmount",
				"termInDays", "expectedDisbursementDate", "gracePeriodToFirstInstallment", "graceperiod",
				"dailyInterestRate", "weeklyInterestRate", "monthlyInterestRate", "annualInterestRate",
				"externalReferenceNo", "guarantor1FullName", "guarantor1Relationship", "guarantor1Phone",
				"guarantor1Address", "guarantor1GuaranteeAmount", "guarantor1GuaranteeLimit", "guarantor1Primary",
				"guarantor2FullName", "guarantor2Relationship", "guarantor2Phone", "guarantor2Address",
				"guarantor2GuaranteeAmount", "guarantor2GuaranteeLimit", "guarantor2Primary", "guarantor3FullName",
				"guarantor3Relationship", "guarantor3Phone", "guarantor3Address", "guarantor3GuaranteeAmount",
				"guarantor3GuaranteeLimit", "guarantor3Primary" };

		writer.println(String.join(",", headers));

		String[] exampleRow = { "INDIVIDUAL", "IND-001", "PERSONAL_LOAN", "100000", "95000", "180", "2025-01-15", "14",
				"7", "0.005", "0.035", "0.15", "1.8", "EXT-REF-12345", "John Doe", "FATHER", "0711111111", "Nairobi",
				"40000", "50000", "true", "Jane Doe", "MOTHER", "0722222222", "Nairobi", "30000", "40000", "false", "",
				"", "", "", "", "", "" };

		writer.println(String.join(",", exampleRow));

		writer.println();
		writer.println("# Example 2: Group borrower with 1 guarantor");
		writer.println(
				"GROUP,GRP-001,BUSINESS_LOAN,500000,450000,365,2025-01-20,30,15,0.003,0.021,0.09,1.08,EXT-REF-67890,Peter Smith,BUSINESS_PARTNER,0733333333,Kisumu,250000,300000,true,,,,,,,,,,,,,,,,,,");

		writer.println();
		writer.println("# Example 3: Institution borrower with no guarantors");
		writer.println(
				"INSTITUTION,INST-001,ASSET_FINANCE,2000000,1800000,720,2025-01-25,45,30,0.002,0.014,0.06,0.72,EXT-REF-55555,,,,,,,,,,,,,,,,,,,,,,,,,,");

		writer.flush();
		writer.close();
	}

	public void downloadGuarantorInstructions(HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=guarantor_instructions.txt");

		PrintWriter writer = response.getWriter();

		writer.println("================= HOW TO ADD UNLIMITED GUARANTORS =================");
		writer.println();
		writer.println("OUR SYSTEM SUPPORTS UNLIMITED GUARANTORS!");
		writer.println("You are NOT limited to only 3 or 5 guarantors shown in the template.");
		writer.println();
		writer.println("FORMAT PATTERN:");
		writer.println("For EACH guarantor, you need these 7 fields (in exact order):");
		writer.println("1. guarantor[N]FullName    - Full name of the guarantor");
		writer.println("2. guarantor[N]Relationship- Relationship (FATHER, MOTHER, SPOUSE, etc.)");
		writer.println("3. guarantor[N]Phone       - Phone number");
		writer.println("4. guarantor[N]Address     - Physical address");
		writer.println("5. guarantor[N]GuaranteeAmount - Amount they guarantee");
		writer.println("6. guarantor[N]GuaranteeLimit  - Maximum guarantee limit");
		writer.println("7. guarantor[N]Primary     - true/false (at least one must be true)");
		writer.println();
		writer.println("EXAMPLES OF VALID COLUMN PATTERNS:");
		writer.println();
		writer.println("For 1 guarantor:");
		writer.println(
				"guarantor1FullName,guarantor1Relationship,guarantor1Phone,guarantor1Address,guarantor1GuaranteeAmount,guarantor1GuaranteeLimit,guarantor1Primary");
		writer.println();
		writer.println("For 3 guarantors:");
		writer.println(
				"guarantor1FullName,guarantor1Relationship,guarantor1Phone,guarantor1Address,guarantor1GuaranteeAmount,guarantor1GuaranteeLimit,guarantor1Primary,");
		writer.println(
				"guarantor2FullName,guarantor2Relationship,guarantor2Phone,guarantor2Address,guarantor2GuaranteeAmount,guarantor2GuaranteeLimit,guarantor2Primary,");
		writer.println(
				"guarantor3FullName,guarantor3Relationship,guarantor3Phone,guarantor3Address,guarantor3GuaranteeAmount,guarantor3GuaranteeLimit,guarantor3Primary");
		writer.println();
		writer.println("For 5 guarantors (just continue the pattern):");
		writer.println("guarantor1FullName,...,guarantor1Primary,");
		writer.println("guarantor2FullName,...,guarantor2Primary,");
		writer.println("guarantor3FullName,...,guarantor3Primary,");
		writer.println("guarantor4FullName,...,guarantor4Primary,");
		writer.println("guarantor5FullName,...,guarantor5Primary");

		writer.flush();
		writer.close();
	}

	@Transactional
	public ResponseEntity<LoanApplicationResponse> applyForLoan(LoanApplicationRequest request) {
		MLoanApplication existingApplication = null;
		List<MGuarantorLoan> guarantors = new ArrayList<>();
		List<DocStatus> docStatusList = List.of(DocStatus.APPROVED, DocStatus.SUBMITTED, DocStatus.DRAFT,
				DocStatus.PENDING, DocStatus.IN_PROGRESS, DocStatus.UNDER_REVIEW, DocStatus.POSTED, DocStatus.VERIFIED);

		MLoanApplication application = new MLoanApplication();
		application.setBorrowerType(request.getBorrowerType());
		MUser assignee = userRepository.findById(request.getLoanAssignedTo()).orElse(null);
		if (assignee != null) {
			application.setAssignee(assignee);
		}

		// === Resolve Borrower Type ===
		if (request.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
			MGroupDebtors groupBorrower = groupBorrowerRepository.findById(request.getGroupBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Group borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndGroupBorrowerAndDocStatusIn(true, BorrowerTypeEnum.GROUP,
							groupBorrower, docStatusList);
			application.setGroupBorrower(groupBorrower);

		} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
			MDebtor individualBorrower = individualBorrowerRepository.findById(request.getIndividualBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Individual borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndIndividualBorrowerAndDocStatusIn(true,
							BorrowerTypeEnum.INDIVIDUAL, individualBorrower, docStatusList);
			application.setIndividualBorrower(individualBorrower);

		} else if (request.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
			MInstitutionBorrower institutionBorrower = institutionBorrowerRepository
					.findById(request.getInstitutionBorrowerId())
					.orElseThrow(() -> new SetUpExceptions("Institution borrower not found"));
			existingApplication = loanApplicationRepository
					.findTop1ByIsActiveAndBorrowerTypeAndInstitutionBorrowerAndDocStatusIn(true,
							BorrowerTypeEnum.INSTITUTION, institutionBorrower, docStatusList);
			application.setInstitutionBorrower(institutionBorrower);

		} else {
			throw new SetUpExceptions("Unsupported borrower type: " + request.getBorrowerType());
		}

		application.setAppliedBy(utils.getLoggedInUser());

		if (existingApplication != null && existingApplication.getLoanProductConfiguration()
				.getLoanProductConfigId() == request.getLoanProductId()) {
			if (utils.isDebtor()) {
				throw new SetUpExceptions("Dear " + application.getAppliedBy().getFirstName() + " "
						+ application.getAppliedBy().getLastName()
						+ ", you currently have an existing loan of the same type. Please complete the repayment before reapplying.");
			}
		}

		// === Base Application Info ===
		MLoanProductConfiguration config = loanProductConfigRepository.findById(request.getLoanProductId())
				.orElseThrow(() -> new SetUpExceptions("Loan Product not found"));
		if (config == null || !config.isActive()) {
			throw new SetUpExceptions("Loan product configuration is missing or inactive.");
		}

		application.setLoanProductConfiguration(config);
		application.setAppliedAmount(request.getAppliedAmount());
		application.setTermInDays(request.getTermInDays());
		application.setExpectedDisbursementDate(request.getExpectedDisbursementDate());
		application.setActualDisbursementDate(request.getActualDisbursementDate());
		application.setPenaltyGracePeriod(config.getPenaltyGracePeriodDays());
		application.setExternalReferenceNo(request.getExternalReferenceNo());
		application.setDueDate(utils.getFutureDateUsingCalender(application.getExpectedDisbursementDate(),
				application.getTermInDays()));

		// === Validation ===
		if (request.getAppliedAmount().compareTo(config.getMinPrincipal()) < 0
				|| request.getAppliedAmount().compareTo(config.getMaxPrincipal()) > 0) {
			throw new SetUpExceptions(
					"Applied amount must be between " + config.getMinPrincipal() + " and " + config.getMaxPrincipal());
		}

		if (Boolean.TRUE.equals(config.getRequireCollateral()) && request.getCollateralValue() == null) {
			throw new SetUpExceptions("Collateral is required for this loan.");
		}

		// === Attach Collateral Documents ===
		if (request.getCollateralValue() != null && !request.getCollateralValue().isEmpty()) {
			Set<MDocuments> collateralDocs = request.getCollateralValue().stream().map(c -> {
				MDocuments doc = new MDocuments();
				if (c.getFileUpload() != null && !c.getFileUpload().isEmpty()) {
					FileUploads fileUpload = utils.uploadFileFromBase64(c.getFileUpload());
					doc.setFileName(fileUpload.getFileName());
					doc.setActualFilePath(fileUpload.getFullFilePath());
					doc.setMimeType(fileUpload.getMimeType());
					doc.setFileSize(fileUpload.getFileSize());
					doc.setFilepath(utils.getFilePath() + doc.getFileName());
					doc.setAD_Document_UU(UUID.randomUUID().toString());
					doc.setColleteralOwner(c.getColleteralOwner());
					doc.setColleteralValue(c.getColleteralValue());
					doc.setExpiryDate(c.getExpiryDate());
					doc.setStorageDurationDaysOnLoanCompletion(c.getStorageDurationDaysOnLoanCompletion());
					doc.setColleteralNo(c.getColleteralNo());
					doc.setAttachment(attachmentRepository.findById(c.getAttachmentId()).get());
				}
				return doc;
			}).collect(Collectors.toSet());
			application.setCollateralAttachments(collateralDocs);
		}

		// === Prepare Guarantors ===
		Set<MNextOfKin> attachedGuarantors = new HashSet<>();
		List<GuarantorTemp> tempGuarantors = new ArrayList<>();

		if (Boolean.TRUE.equals(config.getRequireGuarantors())) {
			if (request.getGuarantors() == null || request.getGuarantors().size() < config.getMinGuarantors()) {
				throw new SetUpExceptions("At least " + config.getMinGuarantors() + " guarantors required.");
			}

			for (NextOfKins g : request.getGuarantors()) {
				MNextOfKin guarantor = new MNextOfKin();
				guarantor.setFullName(g.getFullName());
				guarantor.setRelationship(g.getRelationship());
				guarantor.setPhoneNumber(g.getPhoneNumber());
				guarantor.setAddress(g.getAddress());
				guarantor.setPrimaryGuarantor(g.isPrimaryGuarantor());

				guarantor = nextOfKinRepository.save(guarantor);
				attachedGuarantors.add(guarantor);

				if (g.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0) {
					tempGuarantors.add(new GuarantorTemp(guarantor, g.getGuaranteeAmount(), g.getGuaranteeLimit(),
							g.isPrimaryGuarantor()));
				}
			}
			application.setGuarantors(attachedGuarantors);
		}

		// === Core Values ===
		application.setAdOrgID(utils.getAD_Org_ID());
		application.setDocStatus(DocStatus.DRAFT);
		application.setAD_LoanApplication_UU(UUID.randomUUID().toString());
		application.setBalance(BigDecimal.ZERO);
		application.setLoanState(LoanStateEnum.PENDING_APPROVAL);

		// Interest Rate Overrides
		application.setDailyInterestRate(
				request.getDailyInterestRate() != null ? request.getDailyInterestRate() : BigDecimal.ZERO);
		application.setWeeklyInterestRate(
				request.getWeeklyInterestRate() != null ? request.getWeeklyInterestRate() : BigDecimal.ZERO);
		application.setMonthlyInterestRate(
				request.getMonthlyInterestRate() != null ? request.getMonthlyInterestRate() : BigDecimal.ZERO);
		application.setAnnualInterestRate(
				request.getAnnualInterestRate() != null ? request.getAnnualInterestRate() : BigDecimal.ZERO);
		application.setInteretsFlatRateAmount(
				request.getInteretsFlatRateAmount() != null ? request.getInteretsFlatRateAmount() : BigDecimal.ZERO);
		application.setInteretsFlatRate(
				request.getInteretsFlatRate() != null ? request.getInteretsFlatRate() : BigDecimal.ZERO);
		application.setCycle1FlatInterestPercent(
				request.getCycle1FlatInterestPercent() != null ? request.getCycle1FlatInterestPercent()
						: BigDecimal.ZERO);
		application.setCycle2DailyInterestPercent(
				request.getCycle2DailyInterestPercent() != null ? request.getCycle2DailyInterestPercent()
						: BigDecimal.ZERO);
		application.setCycle3PenaltyPercentPerPeriod(
				request.getCycle3PenaltyPercentPerPeriod() != null ? request.getCycle3PenaltyPercentPerPeriod()
						: BigDecimal.ZERO);

		// === Consolidated Billing (IMPROVED) ===
		application.setConsolidatedBillingGroupId(request.getConsolidatedBillingGroupId());
		application.setConsolidatedBilling(request.isConsolidatedBilling());

		// Handle parent-child relationship - will be set after save if needed

		// === Restructuring ===
		application.setRestructured(request.isRestructured());
		application.setRestructuringReason(request.getRestructuringReason());
		application.setOriginalLoanId(request.getOriginalLoanId());

		// === Additional Metadata ===
		application.setDisbursementNotes(request.getDisbursementNotes());
		application.setCancellationReason(request.getCancellationReason());
		application.setNotificationPreferences(request.getNotificationPreferences());
		application.setClosureNotes(request.getClosureNotes());

		// === Write-off Details ===
		application.setWriteOffReason(request.getWriteOffReason());
		if (request.getWriteOffApprovedBy() != null) {
			MUser writeOffApprovedBy = userRepository.findById(request.getWriteOffApprovedBy()).orElse(null);
			application.setWriteOffApprovedBy(writeOffApprovedBy);
		}
		if (request.getWrittenOffBy() != null) {
			MUser writtenOffBy = userRepository.findById(request.getWrittenOffBy()).orElse(null);
			application.setWrittenOffBy(writtenOffBy);
		}

		// === Reinstatement ===
		application.setReinstatementReason(request.getReinstatementReason());

		// === Cancellation ===
		if (request.getCancelledBy() != null) {
			MUser cancelledBy = userRepository.findById(request.getCancelledBy()).orElse(null);
			application.setCancelledBy(cancelledBy);
		}

		// === Last State Change Trigger ===
		application.setLastStateChangeTrigger(request.getLastStateChangeTrigger());

		// === Save Application FIRST ===
		application = loanApplicationRepository.save(application);

		// === Handle Consolidated Billing Relationships ===
		if (request.isConsolidatedBilling()) {
			// Case 1: This is a parent loan with existing children (by IDs)
			if (request.getChildConsolidatedLoanIds() != null && !request.getChildConsolidatedLoanIds().isEmpty()) {
				for (Long childId : request.getChildConsolidatedLoanIds()) {
					MLoanApplication child = loanApplicationRepository.findById(childId)
							.orElseThrow(() -> new SetUpExceptions("Child loan not found: " + childId));
					child.setParentConsolidatedLoan(application);
					child.setConsolidatedBillingGroupId(application.getConsolidatedBillingGroupId());
					child.setConsolidatedBilling(true);
					child.setDueDate(application.getDueDate());
					loanApplicationRepository.save(child);
				}
			}

			// Case 2: This is a parent loan with inline children
			if (request.getChildConsolidatedLoans() != null && !request.getChildConsolidatedLoans().isEmpty()) {
				for (LoanApplicationRequest childRequest : request.getChildConsolidatedLoans()) {
					childRequest.setConsolidatedBillingGroupId(application.getConsolidatedBillingGroupId());
					childRequest.setConsolidatedBilling(true);
					childRequest.setLoanProductId(request.getLoanProductId());
					// Apply for child loan
					MLoanApplication child = applyForLoanInternal(childRequest);
					child.setParentConsolidatedLoan(application);
					child.setDueDate(application.getDueDate());
					loanApplicationRepository.save(child);
				}
			}
		}

		// Handle parent reference if this is a child loan
		if (request.getParentConsolidatedLoanId() != null) {
			MLoanApplication parent = loanApplicationRepository.findById(request.getParentConsolidatedLoanId())
					.orElseThrow(() -> new SetUpExceptions("Parent loan not found"));
			application.setParentConsolidatedLoan(parent);
			application.setConsolidatedBillingGroupId(parent.getConsolidatedBillingGroupId());
			application.setConsolidatedBilling(true);
			application.setDueDate(parent.getDueDate());
			loanApplicationRepository.save(application);
		}

		if (!tempGuarantors.isEmpty()) {
			for (GuarantorTemp temp : tempGuarantors) {
				MGuarantorLoan existingGuarantorLoan = guarantorLoanRepository
						.findTop1ByLoanAndGuarantorAndIsActive(application, temp.guarantor, true);
				if (existingGuarantorLoan == null) {
					existingGuarantorLoan = new MGuarantorLoan();
				}

				existingGuarantorLoan.setGuarantor(temp.guarantor);
				existingGuarantorLoan.setLoan(application);
				existingGuarantorLoan.setGuaranteeAmount(temp.guaranteeAmount);
				existingGuarantorLoan.setGuaranteeLimit(temp.guaranteeLimit);
				existingGuarantorLoan.setGuaranteeAmountBalance(temp.guaranteeAmount);
				existingGuarantorLoan.setPrimaryGuarantor(temp.primaryGuarantor);
				guarantors.add(existingGuarantorLoan);

				guarantorLoanRepository.save(existingGuarantorLoan);
			}
		}

		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, application.getAdOrgID());
		String loanTypeCode = config.getIsDebtProduct() ? "DB" : "LN";
		String referenceNo = String.format("%s/%06d", loanTypeCode, application.getLoanApplicationId());
		application.setDocumentNo(referenceNo);

		// === Handle Approval Flow ===
		if (config.getRequiredApprovalSteps() > 0) {
			MApprovalSteps firstStep = config.getApprovalLevels().stream().filter(step -> step.getStep() == 1)
					.findFirst()
					.orElseThrow(() -> new SetUpExceptions("First approval step is not defined in configuration"));
			loanApprovalWorkFlowService.triggerApprovalStep(firstStep, application);
		} else {
			loanApprovalWorkFlowService.completeLoanApproval(application);
		}

		// === Handle Auto-Approved Loans ===
		if (application.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			BigDecimal interestExpected = loanInterestCalculatorService.calculateTotalInterest(application,
					application.getTermInDays());
			application.setTotalExpectedInterest(interestExpected);
			application.setTotalExpectedBalance(interestExpected.add(application.getAppliedAmount()));
			application.setDecliningInterest(application.getInterestsEarned());

			application.setBalance(application.getInterestsEarned().add(application.getAppliedAmount()));
			application.setLoanState(LoanStateEnum.OPEN);
			application.setStateChangeDate(new Date());

			loanApplicationRepository.save(application);
			Date expected = application.getExpectedDisbursementDate();

			LocalDateTime ldt = expected.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			loanStatementService.recordDisbursement(application.getLoanApplicationId(), null, application.getBalance(),
					application.getDocumentNo(), ldt);
			if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
				remindersScheduler.handleLoanApproval(application, null);
			}
		}

		// === Post-Commit Notifications ===
		if (sys != null && sys.isAllowSystemNotifications()) {
			MLoanApplication finalApp = application;
			loanApprovalWorkFlowService.notifyApplicantOnApproval(finalApp);

			if (finalApp.getApprovalStage().equals(ApprovalStage.APPROVED)) {
				if (!finalApp.getGuarantors().isEmpty()) {
					for (MNextOfKin guarantor : finalApp.getGuarantors()) {
						MGuarantorLoan gLoan = guarantorLoanRepository
								.findTop1ByLoanAndGuarantorAndIsActive(application, guarantor, true);
						BigDecimal guaranteeAmount = BigDecimal.ZERO;
						BigDecimal guaranteeLimit = BigDecimal.ZERO;
						if (gLoan != null && gLoan.getGuaranteeAmount() != null) {
							guaranteeAmount = gLoan.getGuaranteeAmount();
							guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit()
									: guaranteeAmount;
						}

						remindersScheduler.handleGuarantorLoanAssignmentNotification(guarantor, finalApp,
								application.getApprovalDate(),
								guaranteeAmount.compareTo(BigDecimal.ZERO) > 0 ? guaranteeAmount
										: application.getApprovedAmount(),
								guaranteeLimit.compareTo(BigDecimal.ZERO) > 0 ? guaranteeLimit
										: application.getApprovedAmount(),
								null);
					}
				}
			} else {
				if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
					remindersScheduler.handleLoanApplicationRegistration(finalApp, null);
				}
			}
		}

		String loanType = application.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";

		String message = loanType + " application successful. Loan (Ref: " + application.getDocumentNo()
				+ ") has been submitted and is now under review. We will keep you updated throughout the approval process.";

		return new ResponseEntity<LoanApplicationResponse>(message, 200, mapLoanApplication(application));
	}
	
	/**
	 * Internal method for applying for a loan (used for child loans during consolidation)
	 * This is a simplified version that reuses the main application logic
	 * 
	 * @param request The loan application request
	 * @return The created MLoanApplication entity
	 */
	@Transactional
	private MLoanApplication applyForLoanInternal(LoanApplicationRequest request) {
	    // Reuse the main applyForLoan logic but bypass the response wrapper
	    // We need to create the loan but not return the ResponseEntity
	    
	    MLoanApplication existingApplication = null;
	    List<MGuarantorLoan> guarantors = new ArrayList<>();
	    List<DocStatus> docStatusList = List.of(DocStatus.APPROVED, DocStatus.SUBMITTED, DocStatus.DRAFT,
	            DocStatus.PENDING, DocStatus.IN_PROGRESS, DocStatus.UNDER_REVIEW, DocStatus.POSTED, DocStatus.VERIFIED);

	    MLoanApplication application = new MLoanApplication();
	    application.setBorrowerType(request.getBorrowerType());
	    MUser assignee = userRepository.findById(request.getLoanAssignedTo()).orElse(null);
	    if (assignee != null) {
	        application.setAssignee(assignee);
	    }

	    // === Resolve Borrower Type ===
	    if (request.getBorrowerType().equals(BorrowerTypeEnum.GROUP)) {
	        MGroupDebtors groupBorrower = groupBorrowerRepository.findById(request.getGroupBorrowerId())
	                .orElseThrow(() -> new SetUpExceptions("Group borrower not found"));
	        existingApplication = loanApplicationRepository
	                .findTop1ByIsActiveAndBorrowerTypeAndGroupBorrowerAndDocStatusIn(true, BorrowerTypeEnum.GROUP,
	                        groupBorrower, docStatusList);
	        application.setGroupBorrower(groupBorrower);

	    } else if (request.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
	        MDebtor individualBorrower = individualBorrowerRepository.findById(request.getIndividualBorrowerId())
	                .orElseThrow(() -> new SetUpExceptions("Individual borrower not found"));
	        existingApplication = loanApplicationRepository
	                .findTop1ByIsActiveAndBorrowerTypeAndIndividualBorrowerAndDocStatusIn(true,
	                        BorrowerTypeEnum.INDIVIDUAL, individualBorrower, docStatusList);
	        application.setIndividualBorrower(individualBorrower);

	    } else if (request.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
	        MInstitutionBorrower institutionBorrower = institutionBorrowerRepository
	                .findById(request.getInstitutionBorrowerId())
	                .orElseThrow(() -> new SetUpExceptions("Institution borrower not found"));
	        existingApplication = loanApplicationRepository
	                .findTop1ByIsActiveAndBorrowerTypeAndInstitutionBorrowerAndDocStatusIn(true,
	                        BorrowerTypeEnum.INSTITUTION, institutionBorrower, docStatusList);
	        application.setInstitutionBorrower(institutionBorrower);

	    } else {
	        throw new SetUpExceptions("Unsupported borrower type: " + request.getBorrowerType());
	    }

	    application.setAppliedBy(utils.getLoggedInUser());

	    if (existingApplication != null && existingApplication.getLoanProductConfiguration()
	            .getLoanProductConfigId() == request.getLoanProductId()) {
	        if (utils.isDebtor()) {
	            throw new SetUpExceptions("Dear " + application.getAppliedBy().getFirstName() + " "
	                    + application.getAppliedBy().getLastName()
	                    + ", you currently have an existing loan of the same type. Please complete the repayment before reapplying.");
	        }
	        throw new SetUpExceptions("An active loan application of this type already exists for this borrower.");
	    }

	    // === Base Application Info ===
	    MLoanProductConfiguration config = loanProductConfigRepository.findById(request.getLoanProductId())
	            .orElseThrow(() -> new SetUpExceptions("Loan Product not found"));
	    if (config == null || !config.isActive()) {
	        throw new SetUpExceptions("Loan product configuration is missing or inactive.");
	    }

	    application.setLoanProductConfiguration(config);
	    application.setAppliedAmount(request.getAppliedAmount());
	    application.setTermInDays(request.getTermInDays());
	    application.setExpectedDisbursementDate(request.getExpectedDisbursementDate());
	    application.setActualDisbursementDate(request.getActualDisbursementDate());
	    application.setPenaltyGracePeriod(config.getPenaltyGracePeriodDays());
	    application.setExternalReferenceNo(request.getExternalReferenceNo());
	    application.setDueDate(utils.getFutureDateUsingCalender(application.getExpectedDisbursementDate(),
	            application.getTermInDays()));

	    // === Validation ===
	    if (request.getAppliedAmount().compareTo(config.getMinPrincipal()) < 0
	            || request.getAppliedAmount().compareTo(config.getMaxPrincipal()) > 0) {
	        throw new SetUpExceptions(
	                "Applied amount must be between " + config.getMinPrincipal() + " and " + config.getMaxPrincipal());
	    }

	    if (Boolean.TRUE.equals(config.getRequireCollateral()) && request.getCollateralValue() == null) {
	        throw new SetUpExceptions("Collateral is required for this loan.");
	    }

	    // === Attach Collateral Documents ===
	    if (request.getCollateralValue() != null && !request.getCollateralValue().isEmpty()) {
	        Set<MDocuments> collateralDocs = request.getCollateralValue().stream().map(c -> {
	            MDocuments doc = new MDocuments();
	            if (c.getFileUpload() != null && !c.getFileUpload().isEmpty()) {
	                FileUploads fileUpload = utils.uploadFileFromBase64(c.getFileUpload());
	                doc.setFileName(fileUpload.getFileName());
	                doc.setActualFilePath(fileUpload.getFullFilePath());
	                doc.setMimeType(fileUpload.getMimeType());
	                doc.setFileSize(fileUpload.getFileSize());
	                doc.setFilepath(utils.getFilePath() + doc.getFileName());
	                doc.setAD_Document_UU(UUID.randomUUID().toString());
	                doc.setColleteralOwner(c.getColleteralOwner());
	                doc.setColleteralValue(c.getColleteralValue());
	                doc.setExpiryDate(c.getExpiryDate());
	                doc.setStorageDurationDaysOnLoanCompletion(c.getStorageDurationDaysOnLoanCompletion());
	                doc.setColleteralNo(c.getColleteralNo());
	                doc.setAttachment(attachmentRepository.findById(c.getAttachmentId()).get());
	            }
	            return doc;
	        }).collect(Collectors.toSet());
	        application.setCollateralAttachments(collateralDocs);
	    }

	    // === Prepare Guarantors ===
	    Set<MNextOfKin> attachedGuarantors = new HashSet<>();
	    List<GuarantorTemp> tempGuarantors = new ArrayList<>();

	    if (Boolean.TRUE.equals(config.getRequireGuarantors())) {
	        if (request.getGuarantors() == null || request.getGuarantors().size() < config.getMinGuarantors()) {
	            throw new SetUpExceptions("At least " + config.getMinGuarantors() + " guarantors required.");
	        }

	        for (NextOfKins g : request.getGuarantors()) {
	            MNextOfKin guarantor = new MNextOfKin();
	            guarantor.setFullName(g.getFullName());
	            guarantor.setRelationship(g.getRelationship());
	            guarantor.setPhoneNumber(g.getPhoneNumber());
	            guarantor.setAddress(g.getAddress());
	            guarantor.setPrimaryGuarantor(g.isPrimaryGuarantor());

	            guarantor = nextOfKinRepository.save(guarantor);
	            attachedGuarantors.add(guarantor);

	            if (g.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0) {
	                tempGuarantors.add(new GuarantorTemp(guarantor, g.getGuaranteeAmount(), g.getGuaranteeLimit(),
	                        g.isPrimaryGuarantor()));
	            }
	        }
	        application.setGuarantors(attachedGuarantors);
	    }

	    // === Core Values ===
	    application.setAdOrgID(utils.getAD_Org_ID());
	    application.setDocStatus(DocStatus.DRAFT);
	    application.setAD_LoanApplication_UU(UUID.randomUUID().toString());
	    application.setBalance(BigDecimal.ZERO);
	    application.setLoanState(LoanStateEnum.PENDING_APPROVAL);

	    // Interest Rate Overrides
	    application.setDailyInterestRate(
	            request.getDailyInterestRate() != null ? request.getDailyInterestRate() : BigDecimal.ZERO);
	    application.setWeeklyInterestRate(
	            request.getWeeklyInterestRate() != null ? request.getWeeklyInterestRate() : BigDecimal.ZERO);
	    application.setMonthlyInterestRate(
	            request.getMonthlyInterestRate() != null ? request.getMonthlyInterestRate() : BigDecimal.ZERO);
	    application.setAnnualInterestRate(
	            request.getAnnualInterestRate() != null ? request.getAnnualInterestRate() : BigDecimal.ZERO);
	    application.setInteretsFlatRateAmount(
	            request.getInteretsFlatRateAmount() != null ? request.getInteretsFlatRateAmount() : BigDecimal.ZERO);
	    application.setInteretsFlatRate(
	            request.getInteretsFlatRate() != null ? request.getInteretsFlatRate() : BigDecimal.ZERO);
	    application.setCycle1FlatInterestPercent(
	            request.getCycle1FlatInterestPercent() != null ? request.getCycle1FlatInterestPercent()
	                    : BigDecimal.ZERO);
	    application.setCycle2DailyInterestPercent(
	            request.getCycle2DailyInterestPercent() != null ? request.getCycle2DailyInterestPercent()
	                    : BigDecimal.ZERO);
	    application.setCycle3PenaltyPercentPerPeriod(
	            request.getCycle3PenaltyPercentPerPeriod() != null ? request.getCycle3PenaltyPercentPerPeriod()
	                    : BigDecimal.ZERO);

	    // === Consolidated Billing ===
	    application.setConsolidatedBillingGroupId(request.getConsolidatedBillingGroupId());
	    application.setConsolidatedBilling(request.isConsolidatedBilling());

	    // === Restructuring ===
	    application.setRestructured(request.isRestructured());
	    application.setRestructuringReason(request.getRestructuringReason());
	    application.setOriginalLoanId(request.getOriginalLoanId());

	    // === Additional Metadata ===
	    application.setDisbursementNotes(request.getDisbursementNotes());
	    application.setCancellationReason(request.getCancellationReason());
	    application.setNotificationPreferences(request.getNotificationPreferences());
	    application.setClosureNotes(request.getClosureNotes());

	    // === Write-off Details ===
	    application.setWriteOffReason(request.getWriteOffReason());
	    if (request.getWriteOffApprovedBy() != null) {
	        MUser writeOffApprovedBy = userRepository.findById(request.getWriteOffApprovedBy()).orElse(null);
	        application.setWriteOffApprovedBy(writeOffApprovedBy);
	    }
	    if (request.getWrittenOffBy() != null) {
	        MUser writtenOffBy = userRepository.findById(request.getWrittenOffBy()).orElse(null);
	        application.setWrittenOffBy(writtenOffBy);
	    }

	    // === Reinstatement ===
	    application.setReinstatementReason(request.getReinstatementReason());

	    // === Cancellation ===
	    if (request.getCancelledBy() != null) {
	        MUser cancelledBy = userRepository.findById(request.getCancelledBy()).orElse(null);
	        application.setCancelledBy(cancelledBy);
	    }

	    // === Last State Change Trigger ===
	    application.setLastStateChangeTrigger(request.getLastStateChangeTrigger());

	    // === Save Application ===
	    application = loanApplicationRepository.save(application);

	    // === Handle Guarantors ===
	    if (!tempGuarantors.isEmpty()) {
	        for (GuarantorTemp temp : tempGuarantors) {
	            MGuarantorLoan existingGuarantorLoan = guarantorLoanRepository
	                    .findTop1ByLoanAndGuarantorAndIsActive(application, temp.guarantor, true);
	            if (existingGuarantorLoan == null) {
	                existingGuarantorLoan = new MGuarantorLoan();
	            }

	            existingGuarantorLoan.setGuarantor(temp.guarantor);
	            existingGuarantorLoan.setLoan(application);
	            existingGuarantorLoan.setGuaranteeAmount(temp.guaranteeAmount);
	            existingGuarantorLoan.setGuaranteeLimit(temp.guaranteeLimit);
	            existingGuarantorLoan.setGuaranteeAmountBalance(temp.guaranteeAmount);
	            existingGuarantorLoan.setPrimaryGuarantor(temp.primaryGuarantor);
	            guarantors.add(existingGuarantorLoan);

	            guarantorLoanRepository.save(existingGuarantorLoan);
	        }
	    }

	    // === Generate Document Number ===
	    MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
	            SettingCategoriesEnum.GENERAL_SETTINGS, application.getAdOrgID());
	    String loanTypeCode = config.getIsDebtProduct() ? "DB" : "LN";
	    String referenceNo = String.format("%s/%06d", loanTypeCode, application.getLoanApplicationId());
	    application.setDocumentNo(referenceNo);

	    // === Handle Approval Flow ===
	    if (config.getRequiredApprovalSteps() > 0) {
	        MApprovalSteps firstStep = config.getApprovalLevels().stream().filter(step -> step.getStep() == 1)
	                .findFirst()
	                .orElseThrow(() -> new SetUpExceptions("First approval step is not defined in configuration"));
	        loanApprovalWorkFlowService.triggerApprovalStep(firstStep, application);
	    } else {
	        loanApprovalWorkFlowService.completeLoanApproval(application);
	    }

	    // === Handle Auto-Approved Loans ===
	    if (application.getApprovalStage().equals(ApprovalStage.APPROVED)) {
	        BigDecimal interestExpected = loanInterestCalculatorService.calculateTotalInterest(application,
	                application.getTermInDays());
	        application.setTotalExpectedInterest(interestExpected);
	        application.setTotalExpectedBalance(interestExpected.add(application.getAppliedAmount()));
	        application.setDecliningInterest(application.getInterestsEarned());

	        application.setBalance(application.getInterestsEarned().add(application.getAppliedAmount()));
	        application.setLoanState(LoanStateEnum.OPEN);
	        application.setStateChangeDate(new Date());

	        loanApplicationRepository.save(application);
	        Date expected = application.getExpectedDisbursementDate();

	        LocalDateTime ldt = expected.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

	        loanStatementService.recordDisbursement(application.getLoanApplicationId(), null, application.getBalance(),
	                application.getDocumentNo(), ldt);
	        if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
	            remindersScheduler.handleLoanApproval(application, null);
	        }
	    }

	    // === Post-Commit Notifications ===
	    if (sys != null && sys.isAllowSystemNotifications()) {
	        MLoanApplication finalApp = application;
	        loanApprovalWorkFlowService.notifyApplicantOnApproval(finalApp);

	        if (finalApp.getApprovalStage().equals(ApprovalStage.APPROVED)) {
	            if (!finalApp.getGuarantors().isEmpty()) {
	                for (MNextOfKin guarantor : finalApp.getGuarantors()) {
	                    MGuarantorLoan gLoan = guarantorLoanRepository
	                            .findTop1ByLoanAndGuarantorAndIsActive(application, guarantor, true);
	                    BigDecimal guaranteeAmount = BigDecimal.ZERO;
	                    BigDecimal guaranteeLimit = BigDecimal.ZERO;
	                    if (gLoan != null && gLoan.getGuaranteeAmount() != null) {
	                        guaranteeAmount = gLoan.getGuaranteeAmount();
	                        guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit()
	                                : guaranteeAmount;
	                    }

	                    remindersScheduler.handleGuarantorLoanAssignmentNotification(guarantor, finalApp,
	                            application.getApprovalDate(),
	                            guaranteeAmount.compareTo(BigDecimal.ZERO) > 0 ? guaranteeAmount
	                                    : application.getApprovedAmount(),
	                            guaranteeLimit.compareTo(BigDecimal.ZERO) > 0 ? guaranteeLimit
	                                    : application.getApprovedAmount(),
	                            null);
	                }
	            }
	        } else {
	            if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(application)) {
	                remindersScheduler.handleLoanApplicationRegistration(finalApp, null);
	            }
	        }
	    }

	    log.info("✅ Internal loan application created: {} (Ref: {})", application.getLoanApplicationId(), application.getDocumentNo());
	    return application;
	}

	// Helper class for temporary guarantor storage
	private static class GuarantorTemp {
		MNextOfKin guarantor;
		BigDecimal guaranteeAmount;
		BigDecimal guaranteeLimit;
		boolean primaryGuarantor;

		GuarantorTemp(MNextOfKin guarantor, BigDecimal guaranteeAmount, BigDecimal guaranteeLimit,
				boolean primaryGuarantor) {
			this.guarantor = guarantor;
			this.guaranteeAmount = guaranteeAmount;
			this.guaranteeLimit = guaranteeLimit;
			this.primaryGuarantor = primaryGuarantor;
		}
	}

	public Page<LoanApplicationResponse> getAllApplications(Date dateFrom, Date dateTo, int page, int size,
			String search) {

		if ((search != null && !search.trim().isEmpty())) {

			return loanApplicationRepository
					.searchLoanApplications(utils.getAD_Org_ID(), search, dateFrom, dateTo, PageRequest.of(page, size))
					.map(this::mapLoanApplication);
		}

		return loanApplicationRepository
				.findByIsActiveAndAdOrgIDAndCreatedBetweenAndAmmendAndIsApprovedAndApprovalStageOrderByLoanApplicationIdDesc(
						true, utils.getAD_Org_ID(), dateFrom, dateTo, false, true, ApprovalStage.APPROVED,
						PageRequest.of(page, size))
				.map(this::mapLoanApplication);
	}

	public Page<LoanApplicationResponse> getAllRejectedApplications(Date dateFrom, Date dateTo, int page, int size,
			String search) {

		if ((search != null && !search.trim().isEmpty())) {

			return loanApplicationRepository.searchRejectedLoanApplications(utils.getAD_Org_ID(), search, dateFrom,
					dateTo, PageRequest.of(page, size)).map(this::mapLoanApplication);
		}

		return loanApplicationRepository
				.findByIsActiveAndAdOrgIDAndCreatedBetweenAndAmmendAndIsApprovedAndApprovalStageOrderByLoanApplicationIdDesc(
						true, utils.getAD_Org_ID(), dateFrom, dateTo, false, false, ApprovalStage.REJECTED,
						PageRequest.of(page, size))
				.map(this::mapLoanApplication);
	}

	public Page<LoanApplicationResponse> getAllApplicationsPendingApprovals(Date dateFrom, Date dateTo, int page,
			int size, String search) {
		long adOrgId = utils.getAD_Org_ID();
		Set<MRoles> currentRoles = utils.getLogedInUserRoles();
		MRoles role = currentRoles.stream().findFirst().orElse(null);
		long requiredRoleId = role.getId();

		if ((search != null && !search.trim().isEmpty())) {

			return loanApplicationRepository.searchPendingApprovals(requiredRoleId, adOrgId, search, dateFrom, dateTo,
					PageRequest.of(page, size)).map(this::mapLoanApplication);
		}

		return loanApplicationRepository.findPendingApprovalsByRoleAndStep(requiredRoleId, dateFrom, dateTo, adOrgId,
				PageRequest.of(page, size)).map(this::mapLoanApplication);
	}

	public Page<LoanApplicationResponse> getAllAmendedApplications(Date dateFrom, Date dateTo, int page, int size,
			String search) {

		if ((search != null && !search.trim().isEmpty())) {

			return loanApplicationRepository.searchAmmendedLoanApplications(utils.getAD_Org_ID(), search, dateFrom,
					dateTo, PageRequest.of(page, size)).map(this::mapLoanApplication);
		}

		return loanApplicationRepository
				.findByIsActiveAndAdOrgIDAndCreatedBetweenAndAmmendAndIsApprovedAndApprovalStageOrderByLoanApplicationIdDesc(
						true, utils.getAD_Org_ID(), dateFrom, dateTo, true, false, ApprovalStage.AMENDED,
						PageRequest.of(page, size))
				.map(this::mapLoanApplication);
	}

	@Transactional
	public LoanApplicationResponse deleteApplication(Long id) {
		MLoanApplication app = loanApplicationRepository.findById(id)
				.orElseThrow(() -> new SetUpExceptions("Loan Application not found"));
		app.setActive(false);

		return mapLoanApplication(loanApplicationRepository.save(app));
	}

	public LoanApplicationResponse mapLoanApplication(MLoanApplication entity) {
		if (entity == null) {
			return null;
		}

		LoanApplicationResponse response = new LoanApplicationResponse();

		// Basic Info
		response.setLoanApplicationId(entity.getLoanApplicationId());
		response.setAD_LoanApplication_UU(entity.getAD_LoanApplication_UU());
		response.setBorrowerType(entity.getBorrowerType());
		response.setAppliedAmount(entity.getAppliedAmount());
		response.setApprovedAmount(entity.getApprovedAmount());
		response.setTermInDays(entity.getTermInDays());
		response.setExpectedDisbursementDate(entity.getExpectedDisbursementDate());
		response.setActualDisbursementDate(entity.getActualDisbursementDate());
		response.setLoanProductName(entity.getName());
		response.setDocStatus(entity.getDocStatus());
		response.setApprovalStage(entity.getApprovalStage());
		response.setDocumentNo(entity.getDocumentNo());
		response.setCreated(entity.getCreated());
		response.setUpdated(entity.getUpdated());
		response.setDueDate(entity.getDueDate());

		// User Actions
		response.setAppliedBy(utils.mapUser(entity.getAppliedBy()));
		response.setApprovedBy(utils.mapUser(entity.getApprovedBy()));
		response.setRejectedBy(utils.mapUser(entity.getRejectedBy()));
		response.setCancelledBy(utils.mapUser(entity.getCancelledBy()));

		// Reasons / Dates
		response.setReasonForRejection(entity.getReasonForRejection());
		response.setApprovalDate(entity.getApprovalDate());
		response.setRejectedDate(entity.getRejectedDate());

		// Financial Fields
		response.setInterestsEarned(entity.getInterestsEarned());
		response.setBalance(entity.getBalance());
		response.setPenaltyEarned(entity.getPenaltyEarned());
		response.setDecliningInterest(entity.getDecliningInterest());
		response.setDecliningPrincipal(entity.getDecliningPrincipal());
		response.setInstallmentDistributionBalance(entity.getInstallmentDistributionBalance());
		response.setInitialInstallmentBaseAmount(entity.getInitialInstallmentBaseAmount());
		response.setNoOfRemindersSent(entity.getNoOfRemindersSent());
		response.setLastReminderSent(entity.getLastReminderSent());
		response.setGracePeriodToFirstInstallment(entity.getGracePeriodToFirstInstallment());
		response.setGraceperiod(entity.getGraceperiod());
		response.setPenaltyGracePeriod(entity.getPenaltyGracePeriod());
		response.setHasInstallments(entity.isHasInstallments());
		response.setRepaymentStatus(entity.getRepaymentStatus());
		response.setTotalExpectedBalance(entity.getTotalExpectedBalance());
		response.setTotalExpectedInterest(entity.getTotalExpectedInterest());

		// Interest Rate Overrides
		response.setDailyInterestRate(entity.getDailyInterestRate());
		response.setWeeklyInterestRate(entity.getWeeklyInterestRate());
		response.setMonthlyInterestRate(entity.getMonthlyInterestRate());
		response.setAnnualInterestRate(entity.getAnnualInterestRate());
		response.setInteretsFlatRateAmount(entity.getInteretsFlatRateAmount());
		response.setInteretsFlatRate(entity.getInteretsFlatRate());
		response.setCycle1FlatInterestPercent(entity.getCycle1FlatInterestPercent());
		response.setCycle2DailyInterestPercent(entity.getCycle2DailyInterestPercent());
		response.setCycle3PenaltyPercentPerPeriod(entity.getCycle3PenaltyPercentPerPeriod());

		// Exemptions
		response.setExemptedAmount(entity.getExemptedAmount());
		response.setExemptedInterests(entity.getExemptedInterests());
		response.setExemptedPenalties(entity.getExemptedPenalties());
		response.setExempted(entity.isExempted());

		// Fee Tracking
		response.setServiceFeeCharged(entity.getServiceFeeCharged());
		response.setDailyFeeCharged(entity.getDailyFeeCharged());
		response.setServiceFeeWaived(entity.getServiceFeeWaived());
		response.setDailyFeeWaived(entity.getDailyFeeWaived());

		// Loan State Management
		response.setLoanState(entity.getLoanState());
		response.setStateChangeDate(entity.getStateChangeDate());
		response.setOverdueSinceDate(entity.getOverdueSinceDate());
		response.setWriteOffDate(entity.getWriteOffDate());
		response.setCancelledDate(entity.getCancelledDate());
		response.setClosedDate(entity.getClosedDate());
		response.setReinstatementDate(entity.getReinstatementDate());
		response.setReinstatementReason(entity.getReinstatementReason());
		response.setLastStateChangeTrigger(entity.getLastStateChangeTrigger());

		// Interest Calculation Dates
		response.setLastInterestCalculationDate(entity.getLastInterestCalculationDate());
		response.setNextInterestCalculationDate(entity.getNextInterestCalculationDate());
		response.setLastPenaltyCalculationDate(entity.getLastPenaltyCalculationDate());
		response.setNextPenaltyCalculationDate(entity.getNextPenaltyCalculationDate());

		// Write-off Details
		response.setWriteOffReason(entity.getWriteOffReason());
		response.setWriteOffApprovedBy(utils.mapUser(entity.getWriteOffApprovedBy()));
		response.setWrittenOffBy(utils.mapUser(entity.getWrittenOffBy()));

		// Restructuring
		response.setRestructured(entity.isRestructured());
		response.setRestructuringDate(entity.getRestructuringDate());
		response.setRestructuringReason(entity.getRestructuringReason());
		response.setOriginalLoanId(entity.getOriginalLoanId());

		// Consolidated Billing (IMPROVED)
		response.setConsolidatedBillingGroupId(entity.getConsolidatedBillingGroupId());
		response.setConsolidatedBilling(entity.isConsolidatedBilling());

		// Parent loan
		if (entity.getParentConsolidatedLoan() != null) {
			response.setParentConsolidatedLoanId(entity.getParentConsolidatedLoan().getLoanApplicationId());
			response.setParentConsolidatedLoan(mapLoanApplication(entity.getParentConsolidatedLoan()));
			response.setChildLoan(true);
			response.setParentLoan(false);
		}

		// Child loans
		if (entity.getChildConsolidatedLoans() != null && !entity.getChildConsolidatedLoans().isEmpty()) {
			Set<LoanApplicationResponse> childResponses = new HashSet<>();
			for (MLoanApplication child : entity.getChildConsolidatedLoans()) {
				childResponses.add(mapLoanApplication(child));
			}
			response.setChildConsolidatedLoans(childResponses);
			response.setParentLoan(true);
			response.setChildLoan(false);
			response.setTotalGroupBalance(entity.getTotalGroupBalance());
		}

		// Sweep Job Tracking
		response.setLastSweepRunDate(entity.getLastSweepRunDate());
		response.setSweepRunCount(entity.getSweepRunCount());
		response.setSweepNotes(entity.getSweepNotes());

		// Notification Tracking
		response.setLastNotificationSentDate(entity.getLastNotificationSentDate());
		response.setNotificationCount(entity.getNotificationCount());
		response.setNotificationPreferences(entity.getNotificationPreferences());

		// Additional Metadata
		response.setDisbursementNotes(entity.getDisbursementNotes());
		response.setClosureNotes(entity.getClosureNotes());
		response.setCancellationReason(entity.getCancellationReason());

		// Loan Product Configuration
		if (entity.getLoanProductConfiguration() != null) {
			response.setLoanProductConfigResponse(
					loanConfigService.mappLoanProductConfig(entity.getLoanProductConfiguration()));
		}

		// Collaterals
		if (entity.getCollateralAttachments() != null && !entity.getCollateralAttachments().isEmpty()) {
			response.setCollaterals(borrowersServices.mappBorrowerAttachments(entity.getCollateralAttachments()));
		}

		// Guarantors
		if (entity.getGuarantors() != null && !entity.getGuarantors().isEmpty()) {
			response.setGuarantors(borrowersServices.mappBorrowerNextOfKins(entity.getGuarantors()));
		}

		// Borrowers
		if (entity.getIndividualBorrower() != null) {
			response.setIndividualBorrowerResponse(
					borrowersServices.mapIndividualBorrowers(entity.getIndividualBorrower()));
		}
		if (entity.getGroupBorrower() != null) {
			response.setGroupBorrowerResponse(borrowersServices.mapGroupBorrower(entity.getGroupBorrower()));
		}
		if (entity.getInstitutionBorrower() != null) {
			response.setInstitutionBorrowerResponse(
					borrowersServices.mapInstitutionBorrower(entity.getInstitutionBorrower()));
		}
		response.setExternalReferenceNo(entity.getExternalReferenceNo());
		if (entity.getAssignee() != null) {
			response.setAssignee(utils.mapUser(entity.getAssignee()));
		}

		return response;
	}

	public Page<LoanApplicationResponse> getLoansWithBalances(Date dateFrom, Date dateTo, int page, int size,
			String search) {
		if (search != null && !search.isEmpty()) {
			if ((search != null && !search.trim().isEmpty())) {

				return loanApplicationRepository.searchLoanApplicationsWithBalances(utils.getAD_Org_ID(), search,
						dateFrom, dateTo, PageRequest.of(page, size)).map(this::mapLoanApplication);
			}
		}
		return loanApplicationRepository
				.findByIsActiveAndAdOrgIDAndBalanceGreaterThanAndApprovalStageOrderByLoanApplicationIdAsc(true,
						utils.getAD_Org_ID(), BigDecimal.ZERO, ApprovalStage.APPROVED, PageRequest.of(page, size))
				.map(this::mapLoanApplication);
	}

	public List<LoanApplicationResponse> getRecentApplications() {
		return loanApplicationRepository
				.findTop10ByIsActiveAndAdOrgIDOrderByLoanApplicationIdDesc(true, utils.getAD_Org_ID()).stream()
				.map(this::mapLoanApplication).collect(Collectors.toList());
	}

	@Autowired
	private RoleRepository roleRepository;

	public List<User> getLoanAssignees(String searchTerm) {
		Set<MRoles> roles = new HashSet<>();

		Set<MRoles> admin = roleRepository.findByNameAndIsActiveTrue("ROLE_ADMIN");
		Set<MRoles> collectors = roleRepository.findByNameAndIsActiveTrue("ROLE_DEBT_COLLECTOR");
		Set<MRoles> financialOfficers = roleRepository.findByNameAndIsActiveTrue("ROLE_FINANCIAL_OFFICER");
		Set<MRoles> managers = roleRepository.findByNameAndIsActiveTrue("ROLE_MANAGER");
		roles.addAll(managers);
		roles.addAll(financialOfficers);
		roles.addAll(collectors);
		roles.addAll(admin);

		System.out.println("Found roles====" + roles.size());

		if (searchTerm != null && !searchTerm.isEmpty()) {

			return userRepository.searchAssignees(roles, true, utils.getAD_Org_ID(), searchTerm).stream()
					.map(utils::mapUserBreif).collect(Collectors.toList());
		} else {
			return userRepository.findByRolesAndIsActiveAndOrganisation(roles, true, utils.getAD_Org_ID()).stream()
					.map(utils::mapUserBreif).collect(Collectors.toList());
		}
	}

	public ResponseEntity<LoanApplicationResponse> reAssignLoan(long loanId, long newAssigneeId) {
		MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
		if (loan == null) {
			throw new SetUpExceptions("Loan/Debt with Id " + loanId + " not found.");
		}
		MUser newAssignee = userRepository.findById(newAssigneeId).orElse(null);
		if (newAssignee == null) {
			throw new SetUpExceptions("Loan Collector with Id " + loanId + " not found.");
		}
		loan.setAssignee(newAssignee);
		loanApplicationRepository.save(loan);
		String loanType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";
		return new ResponseEntity<LoanApplicationResponse>(loanType + " Reference No.: " + loan.getDocumentNo()
				+ " has been successfully re-assigned to " + newAssignee.getFullName(), 0, mapLoanApplication(loan));
	}
}