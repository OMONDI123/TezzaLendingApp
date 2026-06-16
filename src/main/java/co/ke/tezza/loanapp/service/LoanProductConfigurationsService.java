package co.ke.tezza.loanapp.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.ApprovalStepsModel;
import co.ke.tezza.loanapp.model.LoanProductConfig;
import co.ke.tezza.loanapp.repository.ApprovalStepsRepository;
import co.ke.tezza.loanapp.repository.LoanProductConfigRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.ApprovalStepsResponse;
import co.ke.tezza.loanapp.response.LoanProductConfigResponse;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class LoanProductConfigurationsService {

    @Autowired
    private LoanProductConfigRepository loanProductConfigRepository;
    @Autowired
    private Utils utils;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApprovalStepsRepository approvalStepsRepository;

    @Transactional
    public ResponseEntity<LoanProductConfigResponse> createUpdateLoanProductConfig(LoanProductConfig request) {
        String message = "Loan/Debt Product Configuration has been created successfully.";
        int code = 200;

        MLoanProductConfiguration loanProductConfig = loanProductConfigRepository
                .findById(request.getLoanProductConfigId()).orElse(null);

        if (loanProductConfig == null) {
            MLoanProductConfiguration existingLoanProductConfig = loanProductConfigRepository
                    .findTop1ByIsActiveAndAdOrgIDAndNameContainingIgnoreCase(true, utils.getAD_Org_ID(),
                            request.getName());

            if (existingLoanProductConfig != null) {
                throw new SetUpExceptions("Loan Configuration " + existingLoanProductConfig.getName()
                        + " already exists, please update the existing one.");
            }

            if (request.getRequiredApprovalSteps() != request.getApprovalLevels().size()) {
                throw new SetUpExceptions("The Required Approval Levels must match the No. of Steps. Currently set to "
                        + request.getApprovalLevels().size() + " but required steps are "
                        + request.getRequiredApprovalSteps());
            }

            loanProductConfig = new MLoanProductConfiguration();
        }
        
        Set<MApprovalSteps> stepsToDelete = new HashSet<>(loanProductConfig.getApprovalLevels());

        if (!stepsToDelete.isEmpty()) {
            for (MApprovalSteps step : stepsToDelete) {
                if (!step.getResponsiblePersons().isEmpty()) {
                    step.getResponsiblePersons().clear();
                }
            }
            approvalStepsRepository.flush();
            approvalStepsRepository.deleteAll(stepsToDelete);
            approvalStepsRepository.flush();
        }

        // ------------------------------------------
        // 1. Classification
        // ------------------------------------------
        loanProductConfig.setIsDebtProduct(request.getIsDebtProduct());
        loanProductConfig.setDebtType(request.getDebtType());
        loanProductConfig.setBorrowerTypes(request.getBorrowerTypes());

        // ------------------------------------------
        // 2. Tenure Configuration (NEW)
        // ------------------------------------------
        loanProductConfig.setTenureType(request.getTenureType());
        loanProductConfig.setFixedTenureDays(request.getFixedTenureDays());
        loanProductConfig.setMinTenureDays(request.getMinTenureDays());
        loanProductConfig.setMaxTenureDays(request.getMaxTenureDays());
        loanProductConfig.setTenureUnit(request.getTenureUnit());

        // ------------------------------------------
        // 3. Principal Configuration
        // ------------------------------------------
        loanProductConfig.setName(request.getName());
        loanProductConfig.setDescription(request.getDescription());
        loanProductConfig.setMinPrincipal(request.getMinPrincipal());
        loanProductConfig.setMaxPrincipal(request.getMaxPrincipal());

        // ------------------------------------------
        // 4. Service Fee Configuration (NEW)
        // ------------------------------------------
        loanProductConfig.setEnableServiceFee(request.getEnableServiceFee());
        loanProductConfig.setServiceFeeType(request.getServiceFeeType());
        loanProductConfig.setServiceFeeAmount(request.getServiceFeeAmount());
        loanProductConfig.setServiceFeePercentage(request.getServiceFeePercentage());
        loanProductConfig.setServiceFeeTiming(request.getServiceFeeTiming());

        // ------------------------------------------
        // 5. Daily Fee Configuration (NEW)
        // ------------------------------------------
        loanProductConfig.setEnableDailyFee(request.getEnableDailyFee());
        loanProductConfig.setDailyFeeAmount(request.getDailyFeeAmount());
        loanProductConfig.setDailyFeeStartDay(request.getDailyFeeStartDay());

        // ------------------------------------------
        // 6. Interest Configuration
        // ------------------------------------------
        loanProductConfig.setInterestCalculationMethod(request.getInterestCalculationMethod());
        loanProductConfig.setAnnualInterestRate(request.getAnnualInterestRate());
        loanProductConfig.setDailyInterestRate(request.getDailyInterestRate());
        loanProductConfig.setWeeklyInterestRate(request.getWeeklyInterestRate());
        loanProductConfig.setMonthlyInterestRate(request.getMonthlyInterestRate());
        loanProductConfig.setInterestFrequency(request.getInterestFrequency());
        loanProductConfig.setGracePeriodDays(request.getGracePeriodDays());
        loanProductConfig.setGracePeriodBeforeFirstInstallment(request.getGracePeriodBeforeFirstInstallment());
        loanProductConfig.setFlatRateType(request.getFlatRateType());
        loanProductConfig.setInteretsFlatRateAmount(request.getInteretsFlatRateAmount());
        loanProductConfig.setInteretsFlatRate(request.getInteretsFlatRate());
        loanProductConfig.setEarlyRepaymentDiscountPercent(request.getEarlyRepaymentDiscountPercent());

        // ------------------------------------------
        // 7. Cycle-Based Interest
        // ------------------------------------------
        loanProductConfig.setCycle1DurationDays(request.getCycle1DurationDays());
        loanProductConfig.setCycle1FlatInterestPercent(request.getCycle1FlatInterestPercent());
        loanProductConfig.setCycle2DurationDays(request.getCycle2DurationDays());
        loanProductConfig.setCycle2DailyInterestPercent(request.getCycle2DailyInterestPercent());
        loanProductConfig.setCycle2StartsAfterDay(request.getCycle2StartsAfterDay());
        loanProductConfig.setCycle3PenaltyStartsAfterDay(request.getCycle3PenaltyStartsAfterDay());
        loanProductConfig.setCycle3PenaltyPercentPerPeriod(request.getCycle3PenaltyPercentPerPeriod());
        loanProductConfig.setCycle3PenaltyPeriodDays(request.getCycle3PenaltyPeriodDays());

        // ------------------------------------------
        // 8. Penalty / Late Fee Configuration
        // ------------------------------------------
        loanProductConfig.setPenaltyGracePeriodDays(request.getPenaltyGracePeriodDays());
        loanProductConfig.setPenaltyRatePercent(request.getPenaltyRatePercent());
        loanProductConfig.setPenaltyFrequencyDays(request.getPenaltyFrequencyDays());
        loanProductConfig.setMaxPenaltyCapPercentOfPrincipal(request.getMaxPenaltyCapPercentOfPrincipal());
        loanProductConfig.setPenaltyFlatRateAmount(request.getPenaltyFlatRateAmount());
        loanProductConfig.setAllowPartialRepayments(request.getAllowPartialRepayments());
        loanProductConfig.setDefaultPenaltyCalculationBase(request.getDefaultPenaltyCalculationBase());
        loanProductConfig.setAllowMaxPenaltyCap(request.isAllowMaxPenaltyCap());
        loanProductConfig.setInstallmentDueChargePenalty(request.isInstallmentDueChargePenalty());
        loanProductConfig.setPeriodPaymentStopPenalty(request.isPeriodPaymentStopPenalty());
        loanProductConfig.setPenaltyAppliesTo(request.getPenaltyAppliesTo());
        loanProductConfig.setLoanOverDueChargePenaltyInstallmentDue(request.isLoanOverDueChargePenaltyInstallmentDue());
        loanProductConfig.setPaymentReliefOnOverdueDebt(request.isPaymentReliefOnOverdueDebt());
        loanProductConfig.setAllowOveralChargesCap(request.isAllowOveralChargesCap());
        loanProductConfig.setAllowedOveralChargesCapPercentage(request.getAllowedOveralChargesCapPercentage());

        // ------------------------------------------
        // 9. Loan State Management Rules (NEW)
        // ------------------------------------------
        loanProductConfig.setDaysToWriteOff(request.getDaysToWriteOff());
        loanProductConfig.setDaysToCancel(request.getDaysToCancel());
        loanProductConfig.setAllowReinstatement(request.getAllowReinstatement());
        loanProductConfig.setReinstatementGracePeriodDays(request.getReinstatementGracePeriodDays());
        loanProductConfig.setAutoCloseOnFullPayment(request.getAutoCloseOnFullPayment());

        // ------------------------------------------
        // 10. Sweep Job Configuration (NEW)
        // ------------------------------------------
        loanProductConfig.setEnableAutoSweep(request.getEnableAutoSweep());
        loanProductConfig.setAutoSweepFrequencyHours(request.getAutoSweepFrequencyHours());
        loanProductConfig.setSweepUpdateState(request.getSweepUpdateState());
        loanProductConfig.setSweepApplyPenalties(request.getSweepApplyPenalties());
        loanProductConfig.setSweepSendNotifications(request.getSweepSendNotifications());

        // ------------------------------------------
        // 11. Repayment Schedule Configuration
        // ------------------------------------------
        loanProductConfig.setRepaymentScheduleType(request.getRepaymentScheduleType());
        loanProductConfig.setInstallmentFrequency(request.getInstallmentFrequency());

        // ------------------------------------------
        // 12. Security & Collateral Requirements
        // ------------------------------------------
        loanProductConfig.setRequireGuarantors(request.getRequireGuarantors());
        loanProductConfig.setMinGuarantors(request.getMinGuarantors());
        loanProductConfig.setRequireCollateral(request.getRequireCollateral());
        loanProductConfig.setCollateralValuePercentOfLoan(request.getCollateralValuePercentOfLoan());

        // ------------------------------------------
        // 13. Operational Flags
        // ------------------------------------------
        loanProductConfig.setAllowEarlyRepayment(request.getAllowEarlyRepayment());
        loanProductConfig.setAllowTopUpLoans(request.getAllowTopUpLoans());
        loanProductConfig.setIsDefaultLoanProductConfig(request.getIsDefaultLoanProductConfig());
        loanProductConfig.setRequiredApprovalSteps(request.getRequiredApprovalSteps());

        // ------------------------------------------
        // 14. Audit & Status
        // ------------------------------------------
        loanProductConfig.setActive(true);

        // ------------------------------------------
        // 15. Unique Identifier
        // ------------------------------------------
        if (loanProductConfig.getAD_LoanProductConfiguration_UU() == null) {
            loanProductConfig.setAD_LoanProductConfiguration_UU(UUID.randomUUID().toString());
        }

        // ------------------------------------------
        // 16. Approval Levels
        // ------------------------------------------
        if (request.getApprovalLevels() != null && !request.getApprovalLevels().isEmpty()) {
            Set<MApprovalSteps> existingSteps = loanProductConfig.getApprovalLevels();
            if (existingSteps != null) {
                existingSteps.clear();
            }
        } else if (loanProductConfig.getApprovalLevels() != null) {
            loanProductConfig.getApprovalLevels().clear();
        }

        // ------------------------------------------
        // 17. Save Entity
        // ------------------------------------------
        if (loanProductConfig.getDocumentNo() == null) {
            Long id = 1L;
            MLoanProductConfiguration latest = loanProductConfigRepository
                    .findTop1ByIsActiveTrueOrderByLoanProductConfigIdDesc();
            if (latest != null) {
                id = latest.getLoanProductConfigId() + 1;
            }
            loanProductConfig.setDocumentNo("LNT/TYP/" + Utils.getCurrentYear() + "/" + id);
        }
        
        loanProductConfig = loanProductConfigRepository.save(loanProductConfig);
        buildApprovalSteps(loanProductConfig, request);

        if (request.getLoanProductConfigId() > 0) {
            message = "Loan/Debt Product Configuration has been updated successfully.";
        }

        return new ResponseEntity<>(message, code, mappLoanProductConfig(loanProductConfig));
    }

    private Set<MApprovalSteps> buildApprovalSteps(MLoanProductConfiguration loanProductConfig,
            LoanProductConfig request) {
        Set<MApprovalSteps> approvalLevels = new HashSet<>();
        List<ApprovalStepsModel> stepModels = request.getApprovalLevels();
        int totalSteps = stepModels.size();
        int i = 1;
        
        for (ApprovalStepsModel step : request.getApprovalLevels()) {
            MApprovalSteps approval = new MApprovalSteps();
            approval.setLoanConfiguration(loanProductConfig);
            approval.setStep(step.getSteps());
            
            if (i > 1 && step.getSteps() == 1) {
                approval.setStep(i);
            }

            MRoles currentRole = roleRepository.findById(step.getApprovalRoleInvolvedId())
                    .orElseThrow(() -> new SetUpExceptions("Role not found for step " + step.getSteps()));
            approval.setRoleInvolved(currentRole);

            approval.setTrigureStatus(step.getTrigureStatus());
            approval.setDocStatus(step.getTrigureStatus());
            approval.setApprovalStage(step.getApprovalStage());
            approval.setRejectiontrigeredStatus(step.getRejectiontrigeredStatus());

            if (i > 0) {
                DocStatus previousStatus = stepModels.get(i - 1).getTrigureStatus();
                approval.setPreviousStatus(previousStatus);

                ApprovalStage previousApprovalStage = stepModels.get(i - 1).getApprovalStage();
                approval.setPreviousApprovalStage(previousApprovalStage);
            }

            if (i < totalSteps - 1) {
                Long nextRoleId = stepModels.get(i + 1).getApprovalRoleInvolvedId();
                MRoles nextRole = roleRepository.findById(nextRoleId)
                        .orElseThrow(() -> new SetUpExceptions("Next role not found for step " + 1));
                approval.setNextRoleinvolved(nextRole);
            }

            Set<MUser> users = new HashSet<>();
            for (Long id : step.getResponsiblePersonIds()) {
                MUser user = userRepository.findById(id).orElse(null);
                if (user != null) {
                    users.add(user);
                }
            }
            approval.setResponsiblePersons(users);

            approvalLevels.add(approval);
            i++;
        }
        approvalStepsRepository.saveAll(approvalLevels);
        return approvalLevels;
    }

    public Page<LoanProductConfigResponse> getAllLoanProductConfigs(int page, int size, String searchTerm) {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return loanProductConfigRepository
                    .searchLoanProduct(true, utils.getAD_Org_ID(), searchTerm, PageRequest.of(page, size))
                    .map(this::mappLoanProductConfig);
        }

        return loanProductConfigRepository.findByIsActiveAndAdOrgIDOrderByLoanProductConfigIdDesc(true,
                utils.getAD_Org_ID(), PageRequest.of(page, size)).map(this::mappLoanProductConfig);
    }

    @Transactional
    public ResponseEntity<LoanProductConfigResponse> deleteLoanProductConfig(Long id) {
        MLoanProductConfiguration loanProductConfig = loanProductConfigRepository.findById(id)
                .orElseThrow(() -> new SetUpExceptions("Loan product configuration not found"));
        loanProductConfig.setActive(false);
        loanProductConfig = loanProductConfigRepository.save(loanProductConfig);

        return new ResponseEntity<LoanProductConfigResponse>(
                "Loan product configuration has been deleted successfully.", 200,
                mappLoanProductConfig(loanProductConfig));
    }

    public LoanProductConfigResponse mappLoanProductConfig(MLoanProductConfiguration entity) {
        if (entity == null) {
            return null;
        }

        LoanProductConfigResponse response = new LoanProductConfigResponse();

        // ------------------------------------------
        // Basic & Identification
        // ------------------------------------------
        response.setLoanProductConfigId(entity.getLoanProductConfigId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setActive(entity.isActive());

        // ------------------------------------------
        // Classification
        // ------------------------------------------
        response.setIsDebtProduct(entity.getIsDebtProduct());
        response.setDebtType(entity.getDebtType());
        response.setBorrowerTypes(entity.getBorrowerTypes());

        // ------------------------------------------
        // Tenure Configuration (NEW)
        // ------------------------------------------
        response.setTenureType(entity.getTenureType());
        response.setFixedTenureDays(entity.getFixedTenureDays());
        response.setMinTenureDays(entity.getMinTenureDays());
        response.setMaxTenureDays(entity.getMaxTenureDays());
        response.setTenureUnit(entity.getTenureUnit());

        // ------------------------------------------
        // Principal Configuration
        // ------------------------------------------
        response.setMinPrincipal(entity.getMinPrincipal());
        response.setMaxPrincipal(entity.getMaxPrincipal());

        // ------------------------------------------
        // Service Fee Configuration (NEW)
        // ------------------------------------------
        response.setEnableServiceFee(entity.getEnableServiceFee());
        response.setServiceFeeType(entity.getServiceFeeType());
        response.setServiceFeeAmount(entity.getServiceFeeAmount());
        response.setServiceFeePercentage(entity.getServiceFeePercentage());
        response.setServiceFeeTiming(entity.getServiceFeeTiming());

        // ------------------------------------------
        // Daily Fee Configuration (NEW)
        // ------------------------------------------
        response.setEnableDailyFee(entity.getEnableDailyFee());
        response.setDailyFeeAmount(entity.getDailyFeeAmount());
        response.setDailyFeeStartDay(entity.getDailyFeeStartDay());

        // ------------------------------------------
        // Interest Configuration
        // ------------------------------------------
        response.setInterestCalculationMethod(entity.getInterestCalculationMethod());
        response.setAnnualInterestRate(entity.getAnnualInterestRate());
        response.setDailyInterestRate(entity.getDailyInterestRate());
        response.setWeeklyInterestRate(entity.getWeeklyInterestRate());
        response.setMonthlyInterestRate(entity.getMonthlyInterestRate());
        response.setInterestFrequency(entity.getInterestFrequency());
        response.setGracePeriodDays(entity.getGracePeriodDays());
        response.setGracePeriodBeforeFirstInstallment(entity.getGracePeriodBeforeFirstInstallment());
        response.setFlatRateType(entity.getFlatRateType());
        response.setInteretsFlatRateAmount(entity.getInteretsFlatRateAmount());
        response.setInteretsFlatRate(entity.getInteretsFlatRate());
        response.setEarlyRepaymentDiscountPercent(entity.getEarlyRepaymentDiscountPercent());

        // ------------------------------------------
        // Cycle-Based Interest
        // ------------------------------------------
        response.setCycle1DurationDays(entity.getCycle1DurationDays());
        response.setCycle1FlatInterestPercent(entity.getCycle1FlatInterestPercent());
        response.setCycle2DurationDays(entity.getCycle2DurationDays());
        response.setCycle2DailyInterestPercent(entity.getCycle2DailyInterestPercent());
        response.setCycle2StartsAfterDay(entity.getCycle2StartsAfterDay());
        response.setCycle3PenaltyStartsAfterDay(entity.getCycle3PenaltyStartsAfterDay());
        response.setCycle3PenaltyPercentPerPeriod(entity.getCycle3PenaltyPercentPerPeriod());
        response.setCycle3PenaltyPeriodDays(entity.getCycle3PenaltyPeriodDays());

        // ------------------------------------------
        // Penalty / Late Fee Configuration
        // ------------------------------------------
        response.setPenaltyGracePeriodDays(entity.getPenaltyGracePeriodDays());
        response.setPenaltyRatePercent(entity.getPenaltyRatePercent());
        response.setPenaltyFrequencyDays(entity.getPenaltyFrequencyDays());
        response.setMaxPenaltyCapPercentOfPrincipal(entity.getMaxPenaltyCapPercentOfPrincipal());
        response.setPenaltyFlatRateAmount(entity.getPenaltyFlatRateAmount());
        response.setAllowPartialRepayments(entity.getAllowPartialRepayments());
        response.setDefaultPenaltyCalculationBase(entity.getDefaultPenaltyCalculationBase());
        response.setAllowMaxPenaltyCap(entity.isAllowMaxPenaltyCap());
        response.setInstallmentDueChargePenalty(entity.isInstallmentDueChargePenalty());
        response.setPeriodPaymentStopPenalty(entity.isPeriodPaymentStopPenalty());
        response.setPenaltyAppliesTo(entity.getPenaltyAppliesTo());
        response.setLoanOverDueChargePenaltyInstallmentDue(entity.isLoanOverDueChargePenaltyInstallmentDue());
        response.setPaymentReliefOnOverdueDebt(entity.isPaymentReliefOnOverdueDebt());
        response.setAllowOveralChargesCap(entity.isAllowOveralChargesCap());
        response.setAllowedOveralChargesCapPercentage(entity.getAllowedOveralChargesCapPercentage());

        // ------------------------------------------
        // Loan State Management Rules (NEW)
        // ------------------------------------------
        response.setDaysToWriteOff(entity.getDaysToWriteOff());
        response.setDaysToCancel(entity.getDaysToCancel());
        response.setAllowReinstatement(entity.getAllowReinstatement());
        response.setReinstatementGracePeriodDays(entity.getReinstatementGracePeriodDays());
        response.setAutoCloseOnFullPayment(entity.getAutoCloseOnFullPayment());

        // ------------------------------------------
        // Sweep Job Configuration (NEW)
        // ------------------------------------------
        response.setEnableAutoSweep(entity.getEnableAutoSweep());
        response.setAutoSweepFrequencyHours(entity.getAutoSweepFrequencyHours());
        response.setSweepUpdateState(entity.getSweepUpdateState());
        response.setSweepApplyPenalties(entity.getSweepApplyPenalties());
        response.setSweepSendNotifications(entity.getSweepSendNotifications());

        // ------------------------------------------
        // Repayment Schedule Configuration
        // ------------------------------------------
        response.setRepaymentScheduleType(entity.getRepaymentScheduleType());
        response.setInstallmentFrequency(entity.getInstallmentFrequency());

        // ------------------------------------------
        // Security & Collateral Requirements
        // ------------------------------------------
        response.setRequireGuarantors(entity.getRequireGuarantors());
        response.setMinGuarantors(entity.getMinGuarantors());
        response.setRequireCollateral(entity.getRequireCollateral());
        response.setCollateralValuePercentOfLoan(entity.getCollateralValuePercentOfLoan());

        // ------------------------------------------
        // Operational Flags
        // ------------------------------------------
        response.setAllowEarlyRepayment(entity.getAllowEarlyRepayment());
        response.setAllowTopUpLoans(entity.getAllowTopUpLoans());
        response.setIsDefaultLoanProductConfig(entity.getIsDefaultLoanProductConfig());
        response.setRequiredApprovalSteps(entity.getRequiredApprovalSteps());

        // ------------------------------------------
        // Audit & Status
        // ------------------------------------------
        response.setDocumentNo(entity.getDocumentNo());
        response.setAD_LoanProductConfiguration_UU(entity.getAD_LoanProductConfiguration_UU());
        response.setDocStatus(entity.getDocStatus());
        response.setApprovalStage(entity.getApprovalStage());
        response.setCreated(entity.getCreated());
        response.setUpdated(entity.getUpdated());

        // ------------------------------------------
        // Approval Levels
        // ------------------------------------------
        if (entity.getApprovalLevels() != null && !entity.getApprovalLevels().isEmpty()) {
            Set<ApprovalStepsResponse> approvalStepsResponses = new HashSet<>();
            for (MApprovalSteps step : entity.getApprovalLevels()) {
                approvalStepsResponses.add(mappApprovalStepsResponse(step));
            }
            response.setApprovalLevels(approvalStepsResponses);
        }

        return response;
    }

    public ApprovalStepsResponse mappApprovalStepsResponse(MApprovalSteps m) {
        if (m == null) {
            return null;
        }
        ApprovalStepsResponse response = new ApprovalStepsResponse();
        response.setApprovalRoleInvolved(utils.mapRole(m.getRoleInvolved()));
        response.setId(m.getId());
        response.setSteps(m.getStep());
        response.setTrigureStatus(m.getTrigureStatus());
        response.setRejectiontrigeredStatus(m.getRejectiontrigeredStatus());

        if (m.getNextRoleinvolved() != null) {
            response.setNextRoleinvolved(utils.mapRole(m.getNextRoleinvolved()));
        }
        if (m.getPreviousStatus() != null) {
            response.setPreviousStatus(m.getPreviousStatus());
        }
        
        Set<User> responsiblePersons = new HashSet<>();
        if (!m.getResponsiblePersons().isEmpty()) {
            for (MUser user : m.getResponsiblePersons()) {
                responsiblePersons.add(utils.mapUserBreif(user));
            }
            response.setResponsiblePersons(responsiblePersons);
        }
        response.setApprovalStage(m.getApprovalStage());
        response.setPreviousApprovalStage(m.getPreviousApprovalStage());
        return response;
    }
}