package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MGuarantorLoan;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DebtTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.FeeTimingEnum;
import co.ke.tezza.loanapp.enums.FeeTypeEnum;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.LoanApplicationResponse;
import co.ke.tezza.loanapp.schedulers.ManageInstallments;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class LoanApprovalWorkFlowService {
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LoanInterestCalculatorService loanInterestCalculatorService;

	@Autowired
	private LoanStatementService loanStatementService;

	@Autowired
	private SmsHandlersService remindersScheduler;

	@Autowired
	private GuarantorLoanRepository guarantorLoanRepository;

	@Autowired
	private ManageInstallments manageInstallments;

	@Autowired
	private LoanApplicationService loanApplicationService;

	@Autowired
	private ObjectsMapper objectsMapper;
	
	@Autowired
    private FeeCalculatorService feeCalculatorService;
	@Autowired
    private InstallmentRepository installmentRepository;

	public void triggerApprovalStep(MApprovalSteps step, MLoanApplication application) {
		MRoles role = step.getRoleInvolved();
		if (role == null) {
			throw new SetUpExceptions("Approval step does not have a role assigned.");
		}

		notifyNextApprovers(step, utils.getLoggedInUser(), application);

		application.setCurrentApprovalLevel(step.getStep());
		loanApplicationRepository.save(application);

		System.out.println(
				"Triggered approval step " + step.getStep() + " for application " + application.getDocumentNo());
	}

	public ResponseEntity<LoanApplicationResponse> rejectLoan(Long applicationId, String reason) {
		String message = "The loan application has been rejected successfully, and a notification has been sent to the applicant with the reason for the rejection.";
		int code = 200;
		MLoanApplication app = loanApplicationRepository.findById(applicationId)
				.orElseThrow(() -> new SetUpExceptions("Loan Application not found"));

		MUser currentUser = utils.getLoggedInUser();
		System.out.println("Rejected By ID: " + currentUser.getUserId() + " Created By ID: " + app.getCreatedBy());
		if (currentUser.getUserId().equals(app.getCreatedBy())) {
			throw new SetUpExceptions("You are not allowed to reject your own application request.");
		}

		if (app.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			throw new SetUpExceptions("This loan has already been approved.");
		}

		if (app.getApprovalStage().equals(ApprovalStage.REJECTED)) {
			throw new SetUpExceptions("This loan has already been rejected.");
		}

		// Find the configuration and current step
		MLoanProductConfiguration config = app.getLoanProductConfiguration();
		List<MApprovalSteps> steps = config.getApprovalLevels().stream()
				.sorted(Comparator.comparingInt(MApprovalSteps::getStep)).collect(Collectors.toList());

		MApprovalSteps currentStep = steps.stream().filter(s -> s.getStep() == app.getCurrentApprovalLevel())
				.findFirst().orElseThrow(() -> new SetUpExceptions("No approval step matching current status"));

		MApprovalSteps previousStep = objectsMapper.getCurrentApprovalLevel(app.getCurrentApprovalLevel() - 1, config);
		MRoles previousRole = previousStep != null ? previousStep.getRoleInvolved() : null;
		DocStatus previousDocStatus = app.getDocStatus();
		ApprovalStage previousApprovalStage = app.getApprovalStage();

		if (!currentUser.getRoles().contains(currentStep.getRoleInvolved())) {
			throw new SetUpExceptions("You are not authorised to reject at this step");
		}

		// Mark rejection
		app.setRejectedBy(currentUser);
		app.setReasonForRejection(reason);
		app.setRejectReason(reason);
		app.setDocStatus(currentStep.getRejectiontrigeredStatus());
		app.setApprovalStage(ApprovalStage.REJECTED);
		loanApplicationRepository.save(app);

		DocStatus newDocStatus = app.getDocStatus();
		ApprovalStage newApprovalStage = app.getApprovalStage();
		Integer maximumSteps = app.getLoanProductConfiguration().getRequiredApprovalSteps();

		objectsMapper.recordLoanApplicationApprovalHistory(app, true, currentStep, maximumSteps, previousDocStatus,
				previousApprovalStage, previousRole, newDocStatus, newApprovalStage);

		// Get system configuration
		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, app.getAdOrgID());

		// Send SMS notifications if enabled
		if (sys != null && sys.isAllowSystemNotifications() && sys.isSendMessageToclientOnRejection()) {
			remindersScheduler.handleLoanRejection(app, reason, null);

			// Additional SMS notification
			if (sys.isAllowSystemNotifications() && sys.isSendMessageToclientOnRejection()) {
				sendSmsNotificationOnRejection(app, reason, currentUser);
			}
		}

		return new ResponseEntity<LoanApplicationResponse>(message, code,
				loanApplicationService.mapLoanApplication(app));
	}

	 public void completeLoanApproval(MLoanApplication application) {
	        application.setApprovalDate(new Date());
	        application.setApproved(true);
	        application.setApprovedAmount(application.getAppliedAmount());
	        application.setExpectedDisbursementDate(application.getExpectedDisbursementDate());
	        application.setApprovalStage(ApprovalStage.APPROVED);
	        application.setDocStatus(DocStatus.APPROVED);
	        application.setRepaymentStatus(LoanRepaymentStatus.PENDING);
	        application.setDecliningPrincipal(application.getApprovedAmount());

	        application.setLoanState(LoanStateEnum.APPROVED);
	        application.setStateChangeDate(new Date());
	        application.setLastStateChangeTrigger("APPROVAL");

	        // Calculate initial interest
	        BigDecimal initialInterest = calculateInitialInterest(application);
	        application.setInterestsEarned(initialInterest);

	        // ========== USE FeeCalculatorService ==========
	        // Check if service fee should be charged at ORIGINATION
	        if (feeCalculatorService.shouldChargeServiceFeeNow(application, FeeTimingEnum.ORIGINATION)) {
	            BigDecimal serviceFee = feeCalculatorService.calculateServiceFee(application);
	            application.setServiceFeeCharged(serviceFee);
	            application.setLastServiceFeeCalculationDate(new Date());
	        } else {
	            // Don't charge at origination - will be charged later by scheduler
	            application.setServiceFeeCharged(BigDecimal.ZERO);
	        }

	        application.setDailyFeeCharged(BigDecimal.ZERO);

	        BigDecimal totalBalance = application.getApprovedAmount()
	                .add(initialInterest != null ? initialInterest : BigDecimal.ZERO)
	                .add(application.getServiceFeeCharged() != null ? application.getServiceFeeCharged() : BigDecimal.ZERO);
	        application.setBalance(totalBalance);

	        if (application.getLoanProductConfiguration().getRepaymentScheduleType() != null
	                && application.getLoanProductConfiguration().getRepaymentScheduleType()
	                        .equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
	            application.setInstallmentDistributionBalance(application.getApprovedAmount());
	            manageInstallments.generateFirstInstallmentForLoan(application);
	            manageInstallments.updateFirstInstallmentInterest(application);
	            
	            if (application.getServiceFeeCharged() != null && application.getServiceFeeCharged().compareTo(BigDecimal.ZERO) > 0) {
	                MInstallments firstInstallment = installmentRepository
	                        .findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true,BigDecimal.ZERO, application);
	                if (firstInstallment != null) {
	                    firstInstallment.setBalance(firstInstallment.getBalance().add(application.getServiceFeeCharged()));
	                    firstInstallment.setServiceFeeCharged(application.getServiceFeeCharged());
	                    installmentRepository.save(firstInstallment);
	                }
	            }
	        }

	        application.setPenaltyGracePeriod(application.getLoanProductConfiguration().getPenaltyGracePeriodDays());
	    }
	

	/**
	 * Approve a loan application at the current workflow step. Advances to the next
	 * approver or finalises the application.
	 */
	public ResponseEntity<LoanApplicationResponse> approveLoan(Long applicationId) {
		int code = 200;
		MLoanApplication application = loanApplicationRepository.findById(applicationId)
				.orElseThrow(() -> new SetUpExceptions("Loan Application not found"));

		if (application.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			throw new SetUpExceptions("This loan has already been approved.");
		}

		if (application.getApprovalStage().equals(ApprovalStage.REJECTED)) {
			throw new SetUpExceptions("This loan has already been rejected.");
		}

		MUser currentUser = utils.getLoggedInUser();
		if (currentUser.getUserId().equals(application.getCreatedBy())) {
			throw new SetUpExceptions("You are not allowed to approve your own application request.");
		}

		MLoanProductConfiguration config = application.getLoanProductConfiguration();
		List<MApprovalSteps> steps = config.getApprovalLevels().stream()
				.sorted(Comparator.comparingInt(MApprovalSteps::getStep)).collect(Collectors.toList());

		String message = "Loan has been successfully approved. The approved amount is KES "
				+ application.getApprovedAmount() + ". Disbursement will follow shortly.";

		MApprovalSteps currentStep = steps.stream().filter(s -> s.getStep() == application.getCurrentApprovalLevel())
				.findFirst().orElseThrow(() -> new SetUpExceptions("No approval step matching current status"));

		MApprovalSteps previousStep = objectsMapper.getCurrentApprovalLevel(application.getCurrentApprovalLevel() - 1,
				config);
		MRoles previousRole = previousStep != null ? previousStep.getRoleInvolved() : null;
		DocStatus previousDocStatus = application.getDocStatus();
		ApprovalStage previousApprovalStage = application.getApprovalStage();

		if (!currentUser.getRoles().contains(currentStep.getRoleInvolved())) {
			throw new SetUpExceptions("You are not authorised to approve at this step");
		}

		if (currentUser.getUserId() == application.getCreatedBy()) {
			throw new SetUpExceptions("You are not allowed to approve your own application request.");
		}

		application.setApprovedBy(currentUser);

		int nextStepNumber = currentStep.getStep() + 1;
		MApprovalSteps nextStep = steps.stream().filter(s -> s.getStep() == nextStepNumber).findFirst().orElse(null);

		if (nextStep != null) {
			message = "Dear " + currentUser.getFullName() + ",\n\n"
					+ "You have successfully approved the loan at level " + currentStep.getStep() + ".\n"
					+ "The loan application has now been forwarded to " + nextStep.getRoleInvolved().getFormattedName()
					+ " for the next level of approval.\n\n" + "Thank you.";

			triggerApprovalStep(nextStep, application);
		} else {
			// Final approval
			completeLoanApproval(application);

			// Calculate expected interest for the full term
			BigDecimal interestExpected = loanInterestCalculatorService.calculateTotalInterest(application,
					application.getTermInDays());
			application.setTotalExpectedInterest(interestExpected);

			// ========== NEW: Include fees in total expected balance ==========
			BigDecimal totalExpectedBalance = application.getApprovedAmount()
					.add(interestExpected != null ? interestExpected : BigDecimal.ZERO)
					.add(application.getServiceFeeCharged() != null ? application.getServiceFeeCharged()
							: BigDecimal.ZERO);
			application.setTotalExpectedBalance(totalExpectedBalance);

			application.setDecliningInterest(application.getInterestsEarned());

			loanApplicationRepository.save(application);

			// Record disbursement
			Date expected = application.getExpectedDisbursementDate();
			LocalDateTime ldt = expected.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			loanStatementService.recordDisbursement(application.getLoanApplicationId(), null, application.getBalance(),
					application.getDocumentNo(), ldt);

			// Send approval notifications
			if (application.getBalance().compareTo(BigDecimal.ZERO) > 0) {
				notifyApplicantOnApproval(application);

				MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
						SettingCategoriesEnum.GENERAL_SETTINGS, application.getAdOrgID());

				if (sys != null && sys.isAllowSystemNotifications()) {
					if (utils.isBorrowerEligible(application)) {
						remindersScheduler.handleLoanApproval(application, null);
						sendSmsNotificationOnApproval(application);
					}

					if (!application.getGuarantors().isEmpty()) {
						for (MNextOfKin guarantor : application.getGuarantors()) {
							MGuarantorLoan gLoan = guarantorLoanRepository
									.findTop1ByLoanAndGuarantorAndIsActive(application, guarantor, true);

							BigDecimal guaranteeAmount = BigDecimal.ZERO;
							BigDecimal guaranteeLimit = BigDecimal.ZERO;

							if (gLoan != null && gLoan.getGuaranteeAmount() != null) {
								guaranteeAmount = gLoan.getGuaranteeAmount();
								guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit()
										: guaranteeAmount;
							}

							if (sys.isAllowSystemNotifications()) {
								remindersScheduler.handleGuarantorLoanAssignmentNotification(guarantor, application,
										application.getApprovalDate(),
										guaranteeAmount.compareTo(BigDecimal.ZERO) > 0 ? guaranteeAmount
												: application.getApprovedAmount(),
										guaranteeLimit.compareTo(BigDecimal.ZERO) > 0 ? guaranteeLimit
												: application.getApprovedAmount(),
										null);
							}
						}
					}
				}
			}

			message = "Loan has been successfully approved. The approved amount is KES "
					+ application.getApprovedAmount() + ". Disbursement will follow shortly.";
		}

		DocStatus newDocStatus = application.getDocStatus();
		ApprovalStage newApprovalStage = application.getApprovalStage();
		Integer maximumSteps = application.getLoanProductConfiguration().getRequiredApprovalSteps();

		objectsMapper.recordLoanApplicationApprovalHistory(application, true, currentStep, maximumSteps,
				previousDocStatus, previousApprovalStage, previousRole, newDocStatus, newApprovalStage);

		return new ResponseEntity<LoanApplicationResponse>(message, code,
				loanApplicationService.mapLoanApplication(application));
	}

	/**
	 * Notify the next approvers in the workflow
	 */
	private void notifyNextApprovers(MApprovalSteps nextStep, MUser currentUser, MLoanApplication application) {

		// Get all users with the next role
		Set<MUser> nextApprovers = nextStep.getResponsiblePersons();

		if (nextApprovers.isEmpty()) {
			return;
		}

		// Get system configuration
		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, application.getAdOrgID());

		boolean allowEmail = sys != null && sys.isAllowSystemNotifications();
		boolean allowSms = sys != null && sys.isAllowSystemNotifications();

		String applicantName = getApplicantName(application);
		String loanRef = application.getDocumentNo();
		String loanAmount = "KES " + application.getAppliedAmount();
		String currentStepName = String.valueOf(nextStep.getStep() - 1);
		String nextStepName = String.valueOf(nextStep.getStep());

		for (MUser approver : nextApprovers) {
			try {
				// Email notification
				if (allowEmail) {
					String emailSubject = "Loan Application Forwarded for Approval - " + loanRef;

					StringBuilder emailContent = new StringBuilder();
					emailContent.append("Dear ").append(approver.getFullName()).append(",\n\n")
							.append("A loan application has been forwarded to you for further review and approval.\n\n")
							.append("Loan Application No: ").append(loanRef).append("\n").append("Applicant: ")
							.append(applicantName).append("\n").append("Requested Amount: ").append(loanAmount)
							.append("\n").append("Forwarded By: ").append(currentUser.getFullName()).append("\n")
							.append("Current Step: ").append(currentStepName).append("\n").append("Your Step: ")
							.append(nextStepName).append("\n\n")
							.append("Please log in to the system to review this request.\n\n").append("Thank you,\n")
							.append("Loan Management System");

					utils.sendEmail(approver, emailContent.toString(), emailSubject);
				}

				// SMS notification
				if (allowSms && approver.getPhoneNumber() != null && !approver.getPhoneNumber().trim().isEmpty()) {
					String smsMessage = "Dear " + approver.getFullName() + ", loan application " + loanRef + " for "
							+ applicantName + " has been forwarded to you by " + currentUser.getFullName()
							+ " for step " + nextStepName + " approval. Please review.";

					utils.saveSmsLoanAmendmentAprrovalSms(approver.getPhoneNumber(), smsMessage,
							application.getAdOrgID(), application.getAdClientId(), LocalDateTime.now());
				}
			} catch (Exception e) {
				System.err.println("Failed to notify next approver: " + approver.getEmail() + " - " + e.getMessage());
			}
		}
	}

	/**
	 * Send SMS notification when loan is approved
	 */
	private void sendSmsNotificationOnApproval(MLoanApplication app) {
		try {
			String applicantName = getApplicantName(app);
			String loanRef = app.getDocumentNo();
			String approvedAmount = "KES " + app.getApprovedAmount();
			String phoneNumber = getApplicantPhoneNumber(app);

			if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
				String smsMessage = "Dear " + applicantName + ", your loan application " + loanRef
						+ " has been approved for " + approvedAmount
						+ ". Disbursement details will follow shortly. Thank you.";

				utils.saveSmsLoanAmendmentAprrovalSms(phoneNumber, smsMessage, app.getAdOrgID(), app.getAdClientId(),
						LocalDateTime.now());
			}
		} catch (Exception e) {
			System.err.println("Failed to send SMS notification on approval: " + e.getMessage());
		}
	}

	/**
	 * Send SMS notification when loan is rejected
	 */
	private void sendSmsNotificationOnRejection(MLoanApplication app, String reason, MUser rejectedBy) {
		try {
			String applicantName = getApplicantName(app);
			String loanRef = app.getDocumentNo();
			String requestedAmount = "KES " + app.getAppliedAmount();
			String phoneNumber = getApplicantPhoneNumber(app);

			if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
				String smsMessage = "Dear " + applicantName + ", your loan application " + loanRef + " for "
						+ requestedAmount + " has been rejected. Reason: " + reason
						+ ". Contact customer service for assistance.";

				utils.saveSmsLoanAmendmentAprrovalSms(phoneNumber, smsMessage, app.getAdOrgID(), app.getAdClientId(),
						LocalDateTime.now());
			}
		} catch (Exception e) {
			System.err.println("Failed to send SMS notification on rejection: " + e.getMessage());
		}
	}

	public void notifyApplicantOnApproval(MLoanApplication app) {
		// Notify applicant
		try {
			// Determine borrower details
			String applicantName;
			String emailAddress = "";
			switch (app.getBorrowerType()) {
			case INDIVIDUAL:
				applicantName = app.getIndividualBorrower().getFirstName() + " "
						+ app.getIndividualBorrower().getLastName();
				emailAddress = app.getIndividualBorrower().getEmail();
				break;
			case GROUP:
				applicantName = app.getGroupBorrower().getGroupName();
				emailAddress = app.getGroupBorrower().getContactEmail();
				break;
			case INSTITUTION:
				applicantName = app.getInstitutionBorrower().getRegistrationNumber() + " - "
						+ app.getInstitutionBorrower().getOrganizationName();
				emailAddress = app.getInstitutionBorrower().getContactEmail();
				break;
			default:
				applicantName = "Valued Customer";
				break;
			}

			StringBuilder messageContent = new StringBuilder();
			messageContent.append("Dear ").append(applicantName).append(",\n\n")
					.append("Congratulations! Your loan application has been approved.\n\n")
					.append("Loan Reference Number: ").append(app.getDocumentNo()).append("\n")
					.append("Approved Amount: KES ").append(app.getApprovedAmount()).append("\n")
					.append("Current Loan Balance: KES ").append(app.getBalance()).append("\n").append("Loan Product: ")
					.append(app.getLoanProductConfiguration().getName()).append("\n\n")
					.append("You will receive the disbursement details shortly.\n\n")
					.append("Thank you for choosing our services.\n\n").append("Best regards,\nLoan Management Team");

			String subject = "Loan Application Approved";

			utils.sendEmail(applicantName, "", emailAddress, messageContent.toString(), subject);

		} catch (Exception e) {
			throw new SetUpExceptions("Failed to notify applicant of approval: " + e.getMessage());
		}
	}

	/**
	 * Calculate initial interest for a newly approved loan
	 */
	public BigDecimal calculateInitialInterest(MLoanApplication application) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();

		if (config.getIsDebtProduct()) {
			if (config.getDebtType().equals(DebtTypeEnum.INTERESTED)) {
				InterestCalculationMethodEnum method = config.getInterestCalculationMethod();

				if (method == InterestCalculationMethodEnum.FLAT || method == InterestCalculationMethodEnum.CYCLE_BASED
						|| method == InterestCalculationMethodEnum.DECLINING_BALANCE_EMI) {
					return loanInterestCalculatorService.calculateTotalInterest(application,
							application.getTermInDays());
				} else {
					return BigDecimal.ZERO;
				}
			} else {
				return BigDecimal.ZERO;
			}
		} else {
			InterestCalculationMethodEnum method = config.getInterestCalculationMethod();

			if (method == InterestCalculationMethodEnum.FLAT || method == InterestCalculationMethodEnum.CYCLE_BASED) {
				return loanInterestCalculatorService.calculateTotalInterest(application, application.getTermInDays());
			} else {
				return BigDecimal.ZERO;
			}
		}
	}

	public void notifyApplicantOnRejection(MLoanApplication app) {
		// Notify applicant
		try {
			// Determine borrower details
			String applicantName;
			String emailAddress = "";
			switch (app.getBorrowerType()) {
			case INDIVIDUAL:
				applicantName = app.getIndividualBorrower().getFirstName() + " "
						+ app.getIndividualBorrower().getLastName();
				emailAddress = app.getIndividualBorrower().getEmail();
				break;
			case GROUP:
				applicantName = app.getGroupBorrower().getGroupName();
				emailAddress = app.getGroupBorrower().getContactEmail();
				break;
			case INSTITUTION:
				applicantName = app.getInstitutionBorrower().getRegistrationNumber() + " - "
						+ app.getInstitutionBorrower().getOrganizationName();
				emailAddress = app.getInstitutionBorrower().getContactEmail();
				break;
			default:
				applicantName = "Valued Customer";
				break;
			}

			StringBuilder messageContent = new StringBuilder();
			messageContent.append("Dear ").append(applicantName).append(",\n\n")
					.append("We regret to inform you that your recent loan application has not been approved.\n\n")
					.append("Loan Reference Number: ").append(app.getDocumentNo()).append("\n")
					.append("Requested Amount: KES ").append(app.getAppliedAmount()).append("\n").append("Reason: ")
					.append(app.getReasonForRejection()).append("\n\n")
					.append("For further assistance or clarification, kindly contact our customer service team.\n\n")
					.append("Thank you for considering our services.\n\n")
					.append("Best regards,\nLoan Management Team");

			String subject = "Loan Application Outcome";

			utils.sendEmail(applicantName, "", emailAddress, messageContent.toString(), subject);

		} catch (Exception e) {
			throw new SetUpExceptions("Failed to notify applicant of rejection: " + e.getMessage());
		}
	}

	/**
	 * Helper method to get applicant name
	 */
	private String getApplicantName(MLoanApplication application) {
		switch (application.getBorrowerType()) {
		case INDIVIDUAL:
			return application.getIndividualBorrower().getFirstName() + " "
					+ application.getIndividualBorrower().getLastName();
		case GROUP:
			return application.getGroupBorrower().getGroupName();
		case INSTITUTION:
			return application.getInstitutionBorrower().getOrganizationName();
		default:
			return "Applicant";
		}
	}

	/**
	 * Helper method to get applicant phone number
	 */
	private String getApplicantPhoneNumber(MLoanApplication application) {
		switch (application.getBorrowerType()) {
		case INDIVIDUAL:
			return application.getIndividualBorrower().getPhone();
		case GROUP:
			return application.getGroupBorrower().getContactPhone();
		case INSTITUTION:
			return application.getInstitutionBorrower().getContactPhone();
		default:
			return null;
		}
	}

	/**
	 * Calculate service fee for the loan
	 */
	private BigDecimal calculateServiceFee(MLoanApplication application) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();

		if (config.getEnableServiceFee() == null || !config.getEnableServiceFee()) {
			return BigDecimal.ZERO;
		}

		BigDecimal principal = application.getApprovedAmount();

		if (config.getServiceFeeType() == FeeTypeEnum.FIXED) {
			return config.getServiceFeeAmount() != null ? config.getServiceFeeAmount() : BigDecimal.ZERO;
		} else if (config.getServiceFeeType() == FeeTypeEnum.PERCENTAGE) {
			if (config.getServiceFeePercentage() != null) {
				return principal.multiply(config.getServiceFeePercentage()).divide(BigDecimal.valueOf(100), 2,
						RoundingMode.HALF_UP);
			}
		}

		return BigDecimal.ZERO;
	}

	/**
	 * Calculate daily fee for the loan
	 */
	private BigDecimal calculateDailyFee(MLoanApplication application) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();

		if (config.getEnableDailyFee() == null || !config.getEnableDailyFee()) {
			return BigDecimal.ZERO;
		}

		return BigDecimal.ZERO;
	}
}