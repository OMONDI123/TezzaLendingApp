package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import co.ke.tezza.loanapp.entity.MAmendmentApprovalHistory;
import co.ke.tezza.loanapp.entity.MAmendmentApprovalSteps;
import co.ke.tezza.loanapp.entity.MAmendmentConfiguration;
import co.ke.tezza.loanapp.entity.MLoanAmendmentDetail;
import co.ke.tezza.loanapp.entity.MLoanAmendmentRequest;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.AmendmentType;
import co.ke.tezza.loanapp.enums.ApprovalAction;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.FlatRateType;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.repository.AmendmentApprovalHistoryRepository;
import co.ke.tezza.loanapp.repository.LoanAmendmentDetailRepository;
import co.ke.tezza.loanapp.repository.LoanAmendmentRequestRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.AmendmentDetailResponse;
import co.ke.tezza.loanapp.response.AmendmentRequestId;
import co.ke.tezza.loanapp.response.AmendmentsToBeApprovedResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class LoanAmendmentApprovalService {

	@Autowired
	private Utils utils;

	@Autowired
	private LoanAmendmentRequestRepository loanAmendmentRequestRepository;

	@Autowired
	private ObjectsMapper objectsMapper;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private LoanAmendmentDetailRepository loanAmendmentDetailRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AmendmentApprovalHistoryRepository amendmentApprovalHistoryRepository;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private LoanAmendmentRequestRepository amendmentRequestRepository;

	@Autowired
	private LoanInterestCalculatorService loanInterestCalculatorService;
	@Autowired
	private PenaltyCalculatorService penaltyCalculatorService;
	@Autowired
	private InstallmentPenaltyCalculatorService installmentPenaltyCalculatorService;

	@Autowired
	private LoanStatementService loanStatementService;

	public ResponseEntity<AmendmentDetailResponse> approveAmendmentDetail(long id) {
		MUser approvingUser = utils.getLoggedInUser();
		String message = null;
		MLoanAmendmentDetail detail = loanAmendmentDetailRepository.findById(id).orElse(null);
		if (detail != null) {
			MUser requestedBy = userRepository.findById(detail.getCreatedBy()).orElse(null);
			if (approvingUser.getUserId().equals(requestedBy.getUserId())) {
				throw new SetUpExceptions("Sorry, you cannot approve your own amendment request.");
			}
			MAmendmentConfiguration config = detail.getAmendmentConfiguration();
			Integer totalNumberOfRequiredSteps = config.getRequiredApprovalSteps();
			Integer currentApprovalStep = detail.getCurrentApprovalLevel();
			MRoles previousRequiredRole = null;
			MAmendmentApprovalSteps previousApprovalLevel = getCurrentApprovalLevel(currentApprovalStep - 1, config);
			if (previousApprovalLevel != null) {
				previousRequiredRole = previousApprovalLevel.getRequiredRole();
			}
			MAmendmentApprovalSteps currentApprovalLevel = getCurrentApprovalLevel(currentApprovalStep, config);
			MAmendmentApprovalSteps lastApprovalLevel = getCurrentApprovalLevel(totalNumberOfRequiredSteps, config);
			Set<MRoles> currentRoles = utils.getLogedInUserRoles();
			MRoles role = currentRoles.stream().findFirst().orElse(null);
			if (role.getId() != currentApprovalLevel.getRequiredRole().getId()) {
				throw new SetUpExceptions("This approval is not allowed for " + role.getFormattedName());
			}
			if (detail.getDocStatus().equals(lastApprovalLevel.getTrigureStatus())
					&& detail.getCurrentApprovalLevel() == totalNumberOfRequiredSteps) {
				throw new SetUpExceptions("This request has already been approved.");
			}
			if (detail.getDocStatus().equals(DocStatus.REJECTED)) {
				throw new SetUpExceptions("This request has already been rejected.");
			}
			DocStatus previousDocStatus = detail.getDocStatus();
			ApprovalStage previousApprovalStage = detail.getApprovalStage();

			if (currentApprovalStep < totalNumberOfRequiredSteps) {

				detail.setDocStatus(currentApprovalLevel.getTrigureStatus());
				detail.setApprovalStage(currentApprovalLevel.getApprovalStage());
				detail.setCurrentApprovalLevel(detail.getCurrentApprovalLevel() + 1);
				loanAmendmentDetailRepository.save(detail);
				DocStatus newDocStatus = detail.getDocStatus();
				ApprovalStage newApprovalStage = detail.getApprovalStage();
				recordAmendmentApprovalHistory(detail, true, currentApprovalLevel, totalNumberOfRequiredSteps,
						previousDocStatus, previousApprovalStage, previousRequiredRole, newDocStatus, newApprovalStage);
				notifyNextApprovers(currentApprovalLevel, utils.getLoggedInUser(),
						"AMD-" + detail.getAmendmentDetailId(), new Date(), detail.getAmendmentReason(),
						currentApprovalStep - 1);
				notifyRequester(detail, true, approvingUser.getFullName(), null, currentApprovalStep,
						totalNumberOfRequiredSteps);

				message = "Loan Amendment request number AMD-" + detail.getAmendmentDetailId()
						+ " has been successfully forwarded to " + currentApprovalLevel.getNextRole().getFormattedName()
						+ " for further review.";
			} else {
				detail.setDocStatus(currentApprovalLevel.getTrigureStatus());
				detail.setApprovalStage(currentApprovalLevel.getApprovalStage());
				loanAmendmentDetailRepository.save(detail);
				DocStatus newDocStatus = detail.getDocStatus();
				ApprovalStage newApprovalStage = detail.getApprovalStage();
				MLoanApplication loanToAmend = loanApplicationRepository.findById(detail.getLoanToAmendId())
						.orElse(null);
				MLoanApplication amendedLoan = new MLoanApplication();
				if (loanToAmend != null) {
					copyLoanDetailsBeforeAmendment(loanToAmend, amendedLoan, utils.getLoggedInUser().getUserId());
					updateLoanWithAmendedDetails(loanToAmend, detail);
				}

				recordAmendmentApprovalHistory(detail, true, currentApprovalLevel, totalNumberOfRequiredSteps,
						previousDocStatus, previousApprovalStage, previousRequiredRole, newDocStatus, newApprovalStage);
				message = "Loan Amendment request number AMD-" + detail.getAmendmentDetailId()
						+ " has been approved successfully.";
				notifyRequester(detail, true, approvingUser.getFullName(), null, currentApprovalStep,
						totalNumberOfRequiredSteps);

			}
			updateMainRequestStatusOnApproval(detail, approvingUser);

		}
		return new ResponseEntity<AmendmentDetailResponse>(message, 200, objectsMapper.mapAmendmentDetails(detail));

	}

	private void updateMainRequestStatusOnApproval(MLoanAmendmentDetail detail, MUser processedBy) {
		MLoanAmendmentRequest request = amendmentRequestRepository.findById(detail.getAmendmentRequestId())
				.orElse(null);

		int noOfApprovedDetails = 0;
		if (request != null) {
			if (!request.getAmendmentDetails().isEmpty()) {
				for (MLoanAmendmentDetail d : request.getAmendmentDetails()) {
					if (d.getDocStatus().equals(DocStatus.APPROVED)) {
						noOfApprovedDetails++;
					}

				}
				if (noOfApprovedDetails == request.getAmendmentDetails().size()) {
					request.setDocStatus(DocStatus.APPROVED);
				} else {
					request.setDocStatus(DocStatus.PARTIALLY_APPROVED);
				}
				request.setProcessedBy(processedBy);
				amendmentRequestRepository.save(request);
			}
		}
	}

	private void updateMainRequestStatusOnRejection(MLoanAmendmentDetail detail, MUser processedBy) {
		MLoanAmendmentRequest request = amendmentRequestRepository.findById(detail.getAmendmentRequestId())
				.orElse(null);

		int noOfRejectedDetails = 0;
		int noOfApprovedDetails = 0;
		if (request != null) {
			if (!request.getAmendmentDetails().isEmpty()) {
				for (MLoanAmendmentDetail d : request.getAmendmentDetails()) {
					if (d.getDocStatus().equals(DocStatus.REJECTED)) {
						noOfRejectedDetails++;
					}
					if (d.getDocStatus().equals(DocStatus.APPROVED)) {
						noOfApprovedDetails++;
					}

				}
				if (noOfRejectedDetails == request.getAmendmentDetails().size()) {
					request.setDocStatus(DocStatus.REJECTED);
				}
				if (noOfApprovedDetails > 0) {
					request.setDocStatus(DocStatus.PARTIALLY_APPROVED);
				}
				request.setProcessedBy(processedBy);
				amendmentRequestRepository.save(request);
			}
		}
	}

	public ResponseEntity<AmendmentDetailResponse> rejectAmendmentDetail(long id, String reason) {
		MUser rejectingUser = utils.getLoggedInUser();
		MLoanAmendmentDetail detail = loanAmendmentDetailRepository.findById(id).orElse(null);
		if (detail != null) {
			MUser requestedBy = userRepository.findById(detail.getCreatedBy()).orElse(null);
			if (rejectingUser.getUserId().equals(requestedBy.getUserId())){
				throw new SetUpExceptions("Sorry, you cannot reject your own amendment request.");
			}
			MAmendmentConfiguration config = detail.getAmendmentConfiguration();
			Integer totalNumberOfRequiredSteps = config.getRequiredApprovalSteps();
			Integer currentApprovalStep = detail.getCurrentApprovalLevel();
			MRoles previousRequiredRole = null;
			MAmendmentApprovalSteps previousApprovalLevel = getCurrentApprovalLevel(currentApprovalStep - 1, config);
			if (previousApprovalLevel != null) {
				previousRequiredRole = previousApprovalLevel.getRequiredRole();
			}

			MAmendmentApprovalSteps currentApprovalLevel = getCurrentApprovalLevel(currentApprovalStep, config);
			MAmendmentApprovalSteps lastApprovalLevel = getCurrentApprovalLevel(totalNumberOfRequiredSteps, config);
			Set<MRoles> currentRoles = utils.getLogedInUserRoles();
			MRoles role = currentRoles.stream().findFirst().orElse(null);
			if (role.getId() != currentApprovalLevel.getRequiredRole().getId()) {
				throw new SetUpExceptions("This action is not allowed for " + role.getFormattedName());
			}
			if (detail.getDocStatus().equals(lastApprovalLevel.getDocStatus())
					&& detail.getCurrentApprovalLevel() == totalNumberOfRequiredSteps) {
				throw new SetUpExceptions("This request has already been approved.");
			}
			if (detail.getDocStatus().equals(DocStatus.REJECTED)) {
				throw new SetUpExceptions("This request has already been rejected.");
			}
			DocStatus previousDocStatus = detail.getDocStatus();
			ApprovalStage previousApprovalStage = detail.getApprovalStage();
			if (currentApprovalStep < totalNumberOfRequiredSteps) {

				detail.setDocStatus(DocStatus.REJECTED);
				detail.setApprovalStage(ApprovalStage.REJECTED);
				detail.setRejectedDate(new Date());
				detail.setReject(true);
				detail.setRejectReason(reason);
				loanAmendmentDetailRepository.save(detail);
				DocStatus newDocStatus = detail.getDocStatus();
				ApprovalStage newApprovalStage = detail.getApprovalStage();
				recordAmendmentApprovalHistory(detail, true, currentApprovalLevel, totalNumberOfRequiredSteps,
						previousDocStatus, previousApprovalStage, previousRequiredRole, newDocStatus, newApprovalStage);
			} else {
				detail.setDocStatus(DocStatus.REJECTED);
				detail.setApprovalStage(ApprovalStage.REJECTED);
				detail.setRejectedDate(new Date());
				detail.setReject(true);
				detail.setRejectReason(reason);
				loanAmendmentDetailRepository.save(detail);
				DocStatus newDocStatus = detail.getDocStatus();
				ApprovalStage newApprovalStage = detail.getApprovalStage();

				recordAmendmentApprovalHistory(detail, true, currentApprovalLevel, totalNumberOfRequiredSteps,
						previousDocStatus, previousApprovalStage, previousRequiredRole, newDocStatus, newApprovalStage);

			}
			notifyRequester(detail, false, rejectingUser.getFullName(), reason, currentApprovalStep,
					totalNumberOfRequiredSteps);
		}
		updateMainRequestStatusOnRejection(detail, rejectingUser);

		return new ResponseEntity<AmendmentDetailResponse>("Loan Amendment request number AMD-"
				+ detail.getAmendmentDetailId() + " has been rejected successfully.", 200,
				objectsMapper.mapAmendmentDetails(detail));
	}

	public void copyLoanDetailsBeforeAmendment(MLoanApplication source, MLoanApplication target, Long currentUserId) {
		if (source == null || target == null) {
			throw new IllegalArgumentException("Source and target cannot be null");
		}

		Date now = new Date();

		// 1. Copy simple fields manually to avoid collection reference issues
		target.setBorrowerType(source.getBorrowerType());

		// 2. Copy entity references (these are safe to share)
		target.setIndividualBorrower(source.getIndividualBorrower());
		target.setInstitutionBorrower(source.getInstitutionBorrower());
		target.setGroupBorrower(source.getGroupBorrower());
		target.setLoanProductConfiguration(source.getLoanProductConfiguration());
		target.setAppliedBy(source.getAppliedBy());
		target.setApprovedBy(source.getApprovedBy());
		target.setRejectedBy(source.getRejectedBy());

		// 3. Copy BigDecimal fields
		target.setAppliedAmount(source.getAppliedAmount());
		target.setApprovedAmount(source.getApprovedAmount());
		target.setBalance(source.getBalance());
		target.setInterestsEarned(source.getInterestsEarned());
		target.setInstallmentDistributionBalance(source.getInstallmentDistributionBalance());
		target.setDecliningInterest(source.getDecliningInterest());
		target.setDecliningPrincipal(source.getDecliningPrincipal());
		target.setTotalExpectedBalance(source.getTotalExpectedBalance());
		target.setTotalExpectedInterest(source.getTotalExpectedInterest());
		target.setDailyInterestRate(source.getDailyInterestRate());
		target.setWeeklyInterestRate(source.getWeeklyInterestRate());
		target.setMonthlyInterestRate(source.getMonthlyInterestRate());
		target.setAnnualInterestRate(source.getAnnualInterestRate());
		target.setInteretsFlatRateAmount(source.getInteretsFlatRateAmount());
		target.setInteretsFlatRate(source.getInteretsFlatRate());
		target.setCycle1FlatInterestPercent(source.getCycle1FlatInterestPercent());
		target.setCycle2DailyInterestPercent(source.getCycle2DailyInterestPercent());
		target.setCycle3PenaltyPercentPerPeriod(source.getCycle3PenaltyPercentPerPeriod());
		target.setInitialInstallmentBaseAmount(source.getInitialInstallmentBaseAmount());
		target.setPenaltyEarned(source.getPenaltyEarned());
		target.setExemptedAmount(source.getExemptedAmount());
		target.setExemptedInterests(source.getExemptedInterests());
		target.setExemptedPenalties(source.getExemptedPenalties());

		// 4. Copy Integer fields
		target.setTermInDays(source.getTermInDays());
		target.setNoOfRemindersSent(source.getNoOfRemindersSent());
		target.setGracePeriodToFirstInstallment(source.getGracePeriodToFirstInstallment());
		target.setGraceperiod(source.getGraceperiod());
		target.setPenaltyGracePeriod(source.getPenaltyGracePeriod());

		// 5. Copy Date fields (create new Date objects to avoid reference sharing)
		target.setExpectedDisbursementDate(
				source.getExpectedDisbursementDate() != null ? new Date(source.getExpectedDisbursementDate().getTime())
						: null);
		target.setApprovalDate(source.getApprovalDate() != null ? new Date(source.getApprovalDate().getTime()) : null);
		target.setRejectedDate(source.getRejectedDate() != null ? new Date(source.getRejectedDate().getTime()) : null);
		target.setLastReminderSent(
				source.getLastReminderSent() != null ? new Date(source.getLastReminderSent().getTime()) : null);
		target.setDueDate(source.getDueDate() != null ? new Date(source.getDueDate().getTime()) : null);
		target.setLastInterestCalculationDate(source.getLastInterestCalculationDate() != null
				? new Date(source.getLastInterestCalculationDate().getTime())
				: null);
		target.setNextInterestCalculationDate(source.getNextInterestCalculationDate() != null
				? new Date(source.getNextInterestCalculationDate().getTime())
				: null);
		target.setLastPenaltyCalculationDate(source.getLastPenaltyCalculationDate() != null
				? new Date(source.getLastPenaltyCalculationDate().getTime())
				: null);
		target.setNextPenaltyCalculationDate(source.getNextPenaltyCalculationDate() != null
				? new Date(source.getNextPenaltyCalculationDate().getTime())
				: null);

		// 6. Copy String and enum fields
		target.setReasonForRejection(source.getReasonForRejection());
		target.setExternalReferenceNo(source.getExternalReferenceNo());
		target.setRepaymentStatus(source.getRepaymentStatus());

		// 7. Copy boolean fields
		target.setHasInstallments(source.isHasInstallments());
		target.setExempted(source.isExempted());

		// 8. Handle ManyToMany collections - create new Sets but share entity
		// references
		// This is safe because MDocuments and MNextOfKin are separate entities
		if (source.getCollateralAttachments() != null && !source.getCollateralAttachments().isEmpty()) {
			target.setCollateralAttachments(new HashSet<>(source.getCollateralAttachments()));
		} else {
			target.setCollateralAttachments(new HashSet<>());
		}

		if (source.getGuarantors() != null && !source.getGuarantors().isEmpty()) {
			target.setGuarantors(new HashSet<>(source.getGuarantors()));
		} else {
			target.setGuarantors(new HashSet<>());
		}

		// 9. Set new IDs and audit fields for the copied entity
		target.setLoanApplicationId(null); // This will generate a new ID
		target.setAD_LoanApplication_UU(UUID.randomUUID().toString());

		// 10. Set amendment-specific fields
		target.setDocStatus(DocStatus.AMENDED);
		target.setApprovalStage(ApprovalStage.AMENDED);
		target.setAmmend(true);

		// 11. Set audit timestamps
		target.setUpdated(now);
		target.setCreated(now);
		target.setCreatedBy(currentUserId);
		target.setUpdatedBy(currentUserId);
		target.setDocumentNo(source.getDocumentNo());
		// 12. Save the entity
		try {
			loanApplicationRepository.save(target);
		} catch (Exception e) {
			throw new RuntimeException("Failed to save copied loan application for amendment", e);
		}
	}

	private void updateLoanWithAmendedDetails(MLoanApplication loan, MLoanAmendmentDetail detail) {

		BigDecimal interestExpected = loanInterestCalculatorService.calculateTotalInterest(loan, loan.getTermInDays());
		loan.setTotalExpectedInterest(interestExpected);
		loan.setTotalExpectedBalance(interestExpected.add(loan.getAppliedAmount()));
		if (detail == null) {
			return;
		}

		if (detail.getNewFlatRateAmount().compareTo(BigDecimal.ZERO) > 0
				|| detail.getNewInterestRate().compareTo(BigDecimal.ZERO) > 0) {
			if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
					&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
							.equals(InterestCalculationMethodEnum.FLAT)
					&& loan.getLoanProductConfiguration().getFlatRateType().equals(FlatRateType.AMOUNT_BASED)) {
				loan.setInteretsFlatRateAmount(detail.getNewFlatRateAmount());
			} else if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
					&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
							.equals(InterestCalculationMethodEnum.FLAT)
					&& loan.getLoanProductConfiguration().getFlatRateType().equals(FlatRateType.PERCENTAGE_BASED)) {
				loan.setInteretsFlatRate(detail.getNewInterestRate());
			} else {
				if (loan.getLoanProductConfiguration().getInterestFrequency() != null) {
					switch (loan.getLoanProductConfiguration().getInterestFrequency()) {
					case DAILY:
						loan.setDailyInterestRate(detail.getNewInterestRate());
					case WEEKLY:
						loan.setWeeklyInterestRate(detail.getNewInterestRate());
					case MONTHLY:
						loan.setMonthlyInterestRate(detail.getNewInterestRate());
					case YEARLY:
						loan.setAnnualInterestRate(detail.getNewInterestRate());

					default:
						loan.setMonthlyInterestRate(detail.getNewInterestRate());

					}
				}

			}
		}
		if (detail.getNewLoanProduct() != null) {
			loan.setLoanProductConfiguration(detail.getNewLoanProduct());

		}
		if (detail.getNewPrincipalAmount() != null && detail.getNewPrincipalAmount().compareTo(BigDecimal.ZERO) > 0
				&& detail.getAmendmentType().equals(AmendmentType.TOP_UP)) {

			if (loan.getLoanProductConfiguration() != null && loan.getLoanProductConfiguration()
					.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
				BigDecimal newInstallmentDistributionBalance = loan.getInstallmentDistributionBalance()
						.add(detail.getNewPrincipalAmount());
				BigDecimal newInstallmentDistributionBaseAmnt = loan.getInitialInstallmentBaseAmount()
						.add(detail.getNewPrincipalAmount());
				loan.setInstallmentDistributionBalance(newInstallmentDistributionBalance);
				loan.setInitialInstallmentBaseAmount(newInstallmentDistributionBaseAmnt);
			}

			BigDecimal newBalance = loan.getBalance().add(detail.getNewPrincipalAmount());
			BigDecimal principalAmount = loan.getAppliedAmount();
			loan.setApprovedAmount(detail.getNewPrincipalAmount().add(principalAmount));
			loan.setBalance(newBalance);

			LocalDateTime ldt = detail.getEffectiveDate();

			loanStatementService.recordDisbursement(loan.getLoanApplicationId(), null, detail.getNewPrincipalAmount(),
					loan.getDocumentNo(), ldt);

		}
		if (detail.getNewPrincipalAmount() != null && detail.getNewPrincipalAmount().compareTo(BigDecimal.ZERO) > 0
				&& detail.getAmendmentType().equals(AmendmentType.PRINCIPAL_REDUCTION)) {

			if (loan.getLoanProductConfiguration() != null && loan.getLoanProductConfiguration()
					.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
				BigDecimal newInstallmentDistributionBalance = loan.getInstallmentDistributionBalance()
						.subtract(detail.getNewPrincipalAmount());
				BigDecimal newInstallmentDistributionBaseAmnt = loan.getInitialInstallmentBaseAmount()
						.subtract(detail.getNewPrincipalAmount());
				loan.setInstallmentDistributionBalance(newInstallmentDistributionBalance);
				loan.setInitialInstallmentBaseAmount(newInstallmentDistributionBaseAmnt);
			}

			BigDecimal newBalance = loan.getBalance().subtract(detail.getNewPrincipalAmount());
			BigDecimal principalAmount = loan.getAppliedAmount();
			loan.setApprovedAmount(principalAmount.subtract(detail.getNewPrincipalAmount()));
			loan.setBalance(newBalance);
			LocalDateTime ldt = detail.getEffectiveDate();

			loanStatementService.recordPrincipleReduction(loan.getLoanApplicationId(), null,
					detail.getNewPrincipalAmount(), loan.getDocumentNo(), ldt);

		}

		if (detail.getNewPrincipalAmount() != null && detail.getNewPrincipalAmount().compareTo(BigDecimal.ZERO) > 0
				&& detail.getAmendmentType().equals(AmendmentType.PRINCIPAL_RESTRUCTURING)) {
			LocalDateTime ldt = detail.getEffectiveDate();
			if (loan.getLoanProductConfiguration() != null && loan.getLoanProductConfiguration()
					.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
				BigDecimal newInstallmentDistributionBalance = loan.getInstallmentDistributionBalance()
						.subtract(loan.getApprovedAmount());
				BigDecimal newInstallmentDistributionBaseAmnt = loan.getInitialInstallmentBaseAmount()
						.subtract(loan.getApprovedAmount());
				loan.setInstallmentDistributionBalance(
						newInstallmentDistributionBalance.add(detail.getNewPrincipalAmount()));
				loan.setInitialInstallmentBaseAmount(
						newInstallmentDistributionBaseAmnt.add(detail.getNewPrincipalAmount()));
			}

			BigDecimal newBalance = loan.getBalance().subtract(loan.getApprovedAmount());
			loanStatementService.recordPrincipleReduction(loan.getLoanApplicationId(), null, loan.getAppliedAmount(),
					loan.getDocumentNo(), ldt);
			loan.setApprovedAmount(detail.getNewPrincipalAmount());
			loan.setBalance(newBalance.add(detail.getNewPrincipalAmount()));

			loanStatementService.recordDisbursement(loan.getLoanApplicationId(), null, detail.getNewPrincipalAmount(),
					loan.getDocumentNo(), ldt);

		}
		if (detail.getNewTermInDays() > 0) {
			loan.setTermInDays(detail.getNewTermInDays());
		}
		loanApplicationRepository.save(loan);
	}

	private void notifyRequester(MLoanAmendmentDetail detail, boolean isApproved, String actionedBy, String reason,
			Integer currentStep, Integer totalSteps) {

// Get the requester (user who created the amendment request)
		MUser requester = userRepository.findById(detail.getCreatedBy()).orElse(null);
		if (requester == null) {
			return; // Requester not found
		}

		String referenceNo = "AMD-" + detail.getAmendmentDetailId();
		String smsMessage = null;
		String emailSubject = null;
		String emailMessage = null;

		if (isApproved) {
			if (currentStep < totalSteps) {
// Forwarded to next level (not final approval)
				smsMessage = "Dear " + requester.getFullName() + ", your loan amendment request " + referenceNo
						+ " has been approved at level " + currentStep + " by " + actionedBy
						+ " and forwarded for further review.";

				emailSubject = "Update: Loan Amendment Request Forwarded - " + referenceNo;
				emailMessage = buildRequesterForwardedEmailContent(requester, referenceNo, actionedBy, currentStep,
						totalSteps, detail);
			} else {
// Final approval
				smsMessage = "Dear " + requester.getFullName() + ", your loan amendment request " + referenceNo
						+ " has been fully approved by " + actionedBy + ". The loan has been updated accordingly.";

				emailSubject = "Approved: Loan Amendment Request - " + referenceNo;
				emailMessage = buildRequesterApprovedEmailContent(requester, referenceNo, actionedBy, detail);
			}
		} else {
// Rejected
			smsMessage = "Dear " + requester.getFullName() + ", your loan amendment request " + referenceNo
					+ " has been rejected by " + actionedBy + ". Reason: " + reason;

			emailSubject = "Rejected: Loan Amendment Request - " + referenceNo;
			emailMessage = buildRequesterRejectedEmailContent(requester, referenceNo, actionedBy, reason, detail);
		}

// Send notifications
		if (requester.getPhoneNumber() != null && !requester.getPhoneNumber().trim().isEmpty()) {
			utils.saveSmsLoanAmendmentAprrovalSms(requester.getPhoneNumber(), smsMessage, utils.getAD_Org_ID(),
					utils.getAD_Client_ID(), LocalDateTime.now());
		}

		utils.sendEmail(requester, emailMessage, emailSubject);
	}

	private String buildRequesterForwardedEmailContent(MUser requester, String referenceNo, String actionedBy,
			Integer currentStep, Integer totalSteps, MLoanAmendmentDetail detail) {
		StringBuilder emailContent = new StringBuilder();
		emailContent.append(
				"Your loan amendment request has been reviewed and forwarded to the next approval level.<br><br>");

		emailContent.append(
				"<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
		emailContent.append("<p><strong>Reference Number:</strong> ").append(referenceNo).append("</p>");
		emailContent.append("<p><strong>Actioned By:</strong> ").append(actionedBy).append("</p>");
		emailContent.append("<p><strong>Current Status:</strong> Approved at Level ").append(currentStep).append(" of ")
				.append(totalSteps).append("</p>");
		if (detail.getAmendmentReason() != null) {
			emailContent.append("<p><strong>Your Reason:</strong> ").append(detail.getAmendmentReason()).append("</p>");
		}
		emailContent.append("</div>");

		emailContent.append("<p>The request is now pending review at the next approval level.</p>");
		emailContent.append("<p>You will be notified once a final decision is made.</p>");

		return emailContent.toString();
	}

	private String buildRequesterApprovedEmailContent(MUser requester, String referenceNo, String actionedBy,
			MLoanAmendmentDetail detail) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		StringBuilder emailContent = new StringBuilder();
		emailContent.append("Congratulations! Your loan amendment request has been fully approved.<br><br>");

		emailContent.append(
				"<div style=\"background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
		emailContent.append("<p><strong>Reference Number:</strong> ").append(referenceNo).append("</p>");
		emailContent.append("<p><strong>Approved By:</strong> ").append(actionedBy).append("</p>");
		emailContent.append("<p><strong>Approval Date:</strong> ").append(dateFormat.format(new Date())).append("</p>");
		if (detail.getAmendmentReason() != null) {
			emailContent.append("<p><strong>Your Reason:</strong> ").append(detail.getAmendmentReason()).append("</p>");
		}
		emailContent.append(
				"<p><strong>Status:</strong> <span style=\"color: #28a745; font-weight: bold;\">APPROVED</span></p>");
		emailContent.append("</div>");

		emailContent.append("<p>The loan details have been updated as per your request.</p>");
		emailContent.append("<p>You can now view the updated loan information in your account.</p>");

		return emailContent.toString();
	}

	private String buildRequesterRejectedEmailContent(MUser requester, String referenceNo, String actionedBy,
			String reason, MLoanAmendmentDetail detail) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		StringBuilder emailContent = new StringBuilder();
		emailContent.append("Your loan amendment request has been reviewed and requires your attention.<br><br>");

		emailContent.append(
				"<div style=\"background-color: #fff0f0; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
		emailContent.append("<p><strong>Reference Number:</strong> ").append(referenceNo).append("</p>");
		emailContent.append("<p><strong>Actioned By:</strong> ").append(actionedBy).append("</p>");
		emailContent.append("<p><strong>Action Date:</strong> ").append(dateFormat.format(new Date())).append("</p>");
		emailContent.append(
				"<p><strong>Status:</strong> <span style=\"color: #dc3545; font-weight: bold;\">REJECTED</span></p>");
		if (detail.getAmendmentReason() != null) {
			emailContent.append("<p><strong>Your Reason:</strong> ").append(detail.getAmendmentReason()).append("</p>");
		}
		emailContent.append("<p><strong>Rejection Reason:</strong> ").append(reason != null ? reason : "Not specified")
				.append("</p>");
		emailContent.append("</div>");

		emailContent.append("<p>You may need to review and resubmit the amendment request with corrections.</p>");
		emailContent.append("<p>If you have any questions, please contact the approval team.</p>");

		return emailContent.toString();
	}

	public void notifyNextApprovers(MAmendmentApprovalSteps nextStep, MUser loggedInUser, String referenceNo, Date dateActioned,
			String reason, Integer previousStep) {

//		List<MUser> usersWithNextRole = userRepository.findByRolesAndIsActiveAndAdOrgId(nextRole, true,
//				utils.getAD_Org_ID());
//
//		if (usersWithNextRole.isEmpty()) {
//			return;
//		}
		Set<MUser> usersWithRole = nextStep.getResponsiblePersons();

		String smsMessage = null;
		String emailSubject = null;
		String emailMessage = null;

		for (MUser user : usersWithRole) {
			if (user.isActive()) {
				if (previousStep == 0) {
					smsMessage = "Dear " + user.getFullName() + ", " + loggedInUser.getFullName()
							+ " has submitted a loan amendment request (Ref: " + referenceNo + "). "
							+ "Please review and take appropriate action promptly.";

					emailSubject = "New Loan Amendment Request - " + referenceNo;
					emailMessage = buildInitialRequestEmailContent(user, loggedInUser, referenceNo, dateActioned,
							reason);

				} else {
					smsMessage = "Dear " + user.getFullName() + ", " + loggedInUser.getFullName()
							+ " has forwarded loan amendment request " + referenceNo + " for your review. "
							+ "Please process at your earliest convenience.";

					emailSubject = "Forwarded: Loan Amendment Request - " + referenceNo;
					emailMessage = buildForwardedRequestEmailContent(user, loggedInUser, referenceNo, dateActioned,
							reason);
				}

				utils.sendEmail(user, emailMessage, emailSubject);
				utils.saveSmsLoanAmendmentAprrovalSms(user.getPhoneNumber(), smsMessage, utils.getAD_Org_ID(),
						utils.getAD_Client_ID(), LocalDateTime.now());

			}

		}
	}

	private String buildInitialRequestEmailContent(MUser recipient, MUser requester, String referenceNo,
			Date dateActioned, String reason) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		StringBuilder emailContent = new StringBuilder();
		emailContent.append("A new loan amendment request has been submitted for your review and approval.<br><br>");
		emailContent.append(
				"<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
		emailContent.append("<p><strong>Reference Number:</strong> ").append(referenceNo).append("</p>");
		emailContent.append("<p><strong>Submitted By:</strong> ").append(requester.getFullName()).append("</p>");
		emailContent.append("<p><strong>Submission Date:</strong> ").append(dateFormat.format(dateActioned))
				.append("</p>");
		emailContent.append("<p><strong>Reason for Amendment:</strong> ").append(reason).append("</p>");
		emailContent.append("</div>");
		emailContent.append("<p>Please log in to the system to review this request and take appropriate action.</p>");
		emailContent.append("<p>Kindly process this request at your earliest convenience.</p>");

		return emailContent.toString();
	}

	private String buildForwardedRequestEmailContent(MUser recipient, MUser requester, String referenceNo,
			Date dateActioned, String reason) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		StringBuilder emailContent = new StringBuilder();
		emailContent
				.append("A loan amendment request has been forwarded to you for further review and approval.<br><br>");
		emailContent.append(
				"<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;\">");
		emailContent.append("<p><strong>Reference Number:</strong> ").append(referenceNo).append("</p>");
		emailContent.append("<p><strong>Forwarded By:</strong> ").append(requester.getFullName()).append("</p>");
		emailContent.append("<p><strong>Forward Date:</strong> ").append(dateFormat.format(dateActioned))
				.append("</p>");
		emailContent.append("<p><strong>Amendment Reason:</strong> ").append(reason).append("</p>");
		emailContent.append("</div>");
		emailContent.append(
				"<p>Please log in to the system to review this request and proceed with the next approval step.</p>");
		emailContent.append("<p>Your prompt attention to this matter is appreciated.</p>");

		return emailContent.toString();
	}

	public void recordAmendmentApprovalHistory(MLoanAmendmentDetail detail, boolean approved,
			MAmendmentApprovalSteps currentApprovalLevel, Integer maximumApprovalLevel, DocStatus previousDocStatus,
			ApprovalStage previousApprovalStage, MRoles previousRequiredRole, DocStatus newDocStatus,
			ApprovalStage newApprovalStage) {
		MAmendmentApprovalHistory history = new MAmendmentApprovalHistory();

		// Set action based on approval status
		history.setAction(approved ? ApprovalAction.APPROVE : ApprovalAction.REJECT);

		// Set dates
		Date currentDate = new Date();
		history.setActionDate(currentDate);

		// Use the detail's creation date as received date
		Date receivedDate = detail.getCreated() != null ? detail.getCreated() : currentDate;
		history.setReceivedDate(receivedDate);

		// Set user and role
		MUser loggedInUser = utils.getLoggedInUser();
		history.setActionedBy(loggedInUser);

		// Set required role from parameter
		if (previousRequiredRole != null) {
			history.setRequiredRole(previousRequiredRole);
		} else if (currentApprovalLevel != null && currentApprovalLevel.getRequiredRole() != null) {
			// Fallback to current approval level's role
			history.setRequiredRole(currentApprovalLevel.getRequiredRole());
		} else {
			// Fallback to logged-in user's role
			Set<MRoles> userRoles = utils.getLogedInUserRoles();
			if (userRoles != null && !userRoles.isEmpty()) {
				history.setRequiredRole(userRoles.iterator().next());
			}
		}

		// Set loan association
		MLoanApplication loan = loanApplicationRepository.findById(detail.getLoanToAmendId()).orElse(null);
		if (loan != null) {
			history.setLoan(loan);
		}

		// Set the amendment detail
		history.setAmendmentRequest(detail);

		// Set step information
		Integer stepNumber = null;
		if (currentApprovalLevel != null) {
			stepNumber = currentApprovalLevel.getStepNumber();
		} else if (detail.getCurrentApprovalLevel() != null) {
			// For rejection cases where we might not have currentApprovalLevel
			stepNumber = detail.getCurrentApprovalLevel() - 1; // Previous step
		}
		history.setStepNumber(stepNumber);

		// Set previous and new statuses from parameters
		history.setPreviousDocStatus(previousDocStatus);
		history.setPreviousApprovalStage(previousApprovalStage);
		history.setNewDocStatus(newDocStatus);
		history.setNewApprovalStage(newApprovalStage);

		// Set processing time (calculate time since the detail was created/request
		// received)
		if (receivedDate != null) {
			long diffInMillis = currentDate.getTime() - receivedDate.getTime();
			int hours = diffInMillis > 0 ? (int) (diffInMillis / (1000 * 60 * 60)) : 0;
			history.setProcessingTimeHours(hours);
		}

		// Set audit information
		history.setDocStatus(DocStatus.CO); // History records are always completed
		history.setDigitalSignature(generateDigitalSignature(loggedInUser, detail));
		history.setIpAddress(getClientIpAddress());
		history.setUserAgent(getUserAgent());

		// Set comments based on approval status
		if (!approved) {
			history.setComments("Amendment detail rejected");
			history.setRequiresCorrection(true);
			history.setCorrectionInstructions("Please review and correct the amendment details");
		} else {
			String stepInfo = stepNumber != null ? "at step " + stepNumber : "";
			history.setComments("Amendment detail approved " + stepInfo);
			history.setRequiresCorrection(false);
			history.setCorrectionInstructions(null);
		}

		// Save the history record
		try {
			amendmentApprovalHistoryRepository.save(history);
		} catch (Exception e) {
			// Log the error but don't throw to prevent interrupting the main flow
			System.err.println("Error saving amendment approval history: " + e.getMessage());
			// Consider using a proper logger: log.error("Error saving amendment approval
			// history", e);
		}
	}

	private String generateDigitalSignature(MUser user, MLoanAmendmentDetail detail) {
		if (user == null)
			return null;
		// Generate a simple digital signature
		return user.getUserId() + "-" + detail.getAmendmentDetailId() + "-" + System.currentTimeMillis();
	}

	private String getClientIpAddress() {
		// Get client IP address from request context
		// This depends on your framework (Spring Security, Servlet API, etc.)
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
					.getRequest();
			return request.getRemoteAddr();
		} catch (Exception e) {
			return "127.0.0.1";
		}
	}

	private String getUserAgent() {
		// Get user agent from request
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
					.getRequest();
			return request.getHeader("User-Agent");
		} catch (Exception e) {
			return "Unknown";
		}
	}

	public MAmendmentApprovalSteps getCurrentApprovalLevel(Integer currentApprovalStep,
			MAmendmentConfiguration config) {
		Set<MAmendmentApprovalSteps> steps = config.getApprovalWorkflow();
		if (!steps.isEmpty()) {
			for (MAmendmentApprovalSteps step : steps) {
				if (step.getStepNumber() == currentApprovalStep) {
					return step;
				}
			}
		}
		return null;
	}

	public Page<AmendmentsToBeApprovedResponse> getAllAmendmentsReadyForApproval(String searchTerm, Date dateFrom,
			Date dateTo, int page, int size) {

		long adOrgId = utils.getAD_Org_ID();
		Set<MRoles> currentRoles = utils.getLogedInUserRoles();
		MRoles role = currentRoles.stream().findFirst().orElse(null);
		long requiredRoleId = role.getId();

		// Get total count for pagination
		long totalCount = getTotalCount(searchTerm, dateFrom, dateTo, requiredRoleId, adOrgId);

		// Get paginated IDs
		List<AmendmentRequestId> ids = getIds(page, size, searchTerm, dateFrom, dateTo, requiredRoleId, adOrgId);

		List<AmendmentsToBeApprovedResponse> requests = new ArrayList<>();

		if (!ids.isEmpty()) {
			for (AmendmentRequestId id : ids) {
				AmendmentsToBeApprovedResponse response = new AmendmentsToBeApprovedResponse();

				if (id.getAmendmentRequestId() > 0) {
					MLoanAmendmentRequest request = loanAmendmentRequestRepository.findById(id.getAmendmentRequestId())
							.orElse(null);
					if (request != null) {
						response.setAmendmentRequest(objectsMapper.mapLoanAmendmentRequest(request));
					}
				}

				if (id.getAmendmentDetailId() > 0) {
					MLoanAmendmentDetail detail = loanAmendmentDetailRepository.findById(id.getAmendmentDetailId())
							.orElse(null);
					if (detail != null) {
						response.setCurrentAmendmentDetail(objectsMapper.mapAmendmentDetails(detail));
					}
				}

				requests.add(response);
			}
		}

		// Create Page object
		PageRequest pageRequest = PageRequest.of(page, size);
		return new PageImpl<>(requests, pageRequest, totalCount);
	}

	private List<AmendmentRequestId> getIds(int page, int size, String searchTerm, Date dateFrom, Date dateTo,
			Long requiredRoleId, long adOrgId) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("dateFrom", dateFrom);
		parameters.addValue("adOrgId", adOrgId);
		parameters.addValue("dateTo", dateTo);
		parameters.addValue("requiredRoleId", requiredRoleId);
		parameters.addValue("offset", page * size);
		parameters.addValue("limit", size);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT r.AD_Loan_Amendment_Request_ID, d.AD_Loan_Amendment_Detail_ID ");
		sql.append("FROM AD_Loan_Amendment_Detail d ");
		sql.append(
				"INNER JOIN AD_Request_Amendment_Details rd ON rd.AD_Loan_Amendment_Detail_ID = d.AD_Loan_Amendment_Detail_ID ");
		sql.append(
				"INNER JOIN AD_Loan_Amendment_Request r ON rd.AD_Loan_Amendment_Request_ID = r.AD_Loan_Amendment_Request_ID ");
		sql.append(
				"INNER JOIN AD_Amendment_Configuration c ON c.AD_Amendment_Configuration_ID = d.AD_Amendment_Configuration_ID ");
		sql.append(
				"INNER JOIN amendment_config_approval_steps amstp ON amstp.amendment_config_id = c.AD_Amendment_Configuration_ID ");
		sql.append("INNER JOIN AD_Amendment_Approval_Steps stp ON stp.id = amstp.approval_step_id ");
		sql.append("INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = r.AD_Loan_Application_ID ");

		// Updated LEFT JOINs for search functionality
		sql.append("LEFT JOIN AD_Debtor ind ON ind.AD_Debtor_ID = l.AD_Debtor_ID ");
		sql.append(
				"LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID ");
		sql.append("LEFT JOIN AD_Group_Borrower grp ON grp.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID ");

		sql.append("WHERE stp.step_number = d.current_approval_level ");
		sql.append("AND stp.AD_Role_ID = :requiredRoleId ");
		sql.append("AND d.isactive = true ");
		sql.append("AND d.created BETWEEN :dateFrom AND :dateTo ");
		sql.append("AND d.AD_Org_ID = :adOrgId ");

		// Add search condition if searchTerm is provided
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			parameters.addValue("searchTerm", "%" + searchTerm.toLowerCase() + "%");

			sql.append("AND ( ");
			sql.append("   LOWER(r.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(r.request_Reason) LIKE :searchTerm OR ");
			sql.append("   LOWER(l.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(d.amendment_type) LIKE :searchTerm OR ");
			sql.append("   LOWER(d.amendment_Reason) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.first_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.last_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.national_Id) LIKE :searchTerm OR ");
			sql.append("   ind.phone LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.institution_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.registration_Number) LIKE :searchTerm OR ");
			sql.append("   LOWER(grp.group_Name) LIKE :searchTerm ");
			sql.append(") ");
		}

		sql.append("ORDER BY d.updated ASC ");
		sql.append("LIMIT :limit OFFSET :offset");

		return namedParameterJdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) -> {
			AmendmentRequestId id = new AmendmentRequestId();
			id.setAmendmentRequestId(rs.getLong("AD_Loan_Amendment_Request_ID"));
			id.setAmendmentDetailId(rs.getLong("AD_Loan_Amendment_Detail_ID"));
			return id;
		});
	}

	private long getTotalCount(String searchTerm, Date dateFrom, Date dateTo, Long requiredRoleId, long adOrgId) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("dateFrom", dateFrom);
		parameters.addValue("adOrgId", adOrgId);
		parameters.addValue("dateTo", dateTo);
		parameters.addValue("requiredRoleId", requiredRoleId);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(DISTINCT d.AD_Loan_Amendment_Detail_ID) ");
		sql.append("FROM AD_Loan_Amendment_Detail d ");
		sql.append(
				"INNER JOIN AD_Request_Amendment_Details rd ON rd.AD_Loan_Amendment_Detail_ID = d.AD_Loan_Amendment_Detail_ID ");
		sql.append(
				"INNER JOIN AD_Loan_Amendment_Request r ON rd.AD_Loan_Amendment_Request_ID = r.AD_Loan_Amendment_Request_ID ");
		sql.append(
				"INNER JOIN AD_Amendment_Configuration c ON c.AD_Amendment_Configuration_ID = d.AD_Amendment_Configuration_ID ");
		sql.append(
				"INNER JOIN amendment_config_approval_steps amstp ON amstp.amendment_config_id = c.AD_Amendment_Configuration_ID ");
		sql.append("INNER JOIN AD_Amendment_Approval_Steps stp ON stp.id = amstp.approval_step_id ");
		sql.append("INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = r.AD_Loan_Application_ID ");

		// Updated LEFT JOINs for search functionality
		sql.append("LEFT JOIN AD_Debtor ind ON ind.AD_Debtor_ID = l.AD_Debtor_ID ");
		sql.append(
				"LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID ");
		sql.append("LEFT JOIN AD_Group_Borrower grp ON grp.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID ");

		sql.append("WHERE stp.step_number = d.current_approval_level ");
		sql.append("AND stp.AD_Role_ID = :requiredRoleId ");
		sql.append("AND d.isactive = true ");
		sql.append("AND d.created BETWEEN :dateFrom AND :dateTo ");
		sql.append("AND d.AD_Org_ID = :adOrgId ");

		// Add search condition if searchTerm is provided
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			parameters.addValue("searchTerm", "%" + searchTerm.toLowerCase() + "%");

			sql.append("AND ( ");
			sql.append("   LOWER(r.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(r.request_Reason) LIKE :searchTerm OR ");
			sql.append("   LOWER(l.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(d.amendment_type) LIKE :searchTerm OR ");
			sql.append("   LOWER(d.amendment_Reason) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.first_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.last_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.national_Id) LIKE :searchTerm OR ");
			sql.append("   ind.phone LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.institution_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.registration_Number) LIKE :searchTerm OR ");
			sql.append("   LOWER(grp.group_Name) LIKE :searchTerm ");
			sql.append(") ");
		}

		Long count = namedParameterJdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count != null ? count : 0L;
	}

}