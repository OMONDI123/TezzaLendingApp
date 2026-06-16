package co.ke.tezza.loanapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.*;
import co.ke.tezza.loanapp.enums.*;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.AmendmentApprovalStepRequest;
import co.ke.tezza.loanapp.model.AmendmentConfigurationRequest;
import co.ke.tezza.loanapp.repository.*;
import co.ke.tezza.loanapp.response.LoanAmendmentConfigResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AmendmentConfigurationService {

	@Autowired
	private AmendmentConfigurationRepository amendmentConfigRepository;

	@Autowired
	private LoanProductConfigRepository loanProductConfigRepository;

	@Autowired
	private AmendmentApprovalStepsRepository amendmentApprovalStepsRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private LoanAmendmentRequestRepository amendmentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ObjectsMapper objectsMapper;
	


	/**
	 * Create or update amendment configuration with orphan cleanup
	 */
	@Transactional
	public ResponseEntity<LoanAmendmentConfigResponse> createUpdateConfiguration(
			AmendmentConfigurationRequest request) {
		try {
			// Validate request
			validateConfigurationRequest(request);

			// Check if this is an update
			boolean isUpdate = request.getAmendmentConfigId() > 0;
			MAmendmentConfiguration config;

			if (isUpdate) {
				// Update existing configuration
				config = amendmentConfigRepository.findById(request.getAmendmentConfigId())
						.orElseThrow(() -> new SetUpExceptions(
								"Configuration not found with id: " + request.getAmendmentConfigId()));

				// Check if configuration is active (can't update deactivated configs)
				if (!config.isActive()) {
					throw new SetUpExceptions("Cannot update deactivated configuration. Please activate it first.");
				}

				// Clear orphan relationships BEFORE setting new ones
				clearOrphanRelationshipsForUpdate(config);
			} else {
				// Create new configuration
				config = new MAmendmentConfiguration();
			}

			// Set basic information
			request.setConfigurationName(request.getAmendmentType().getDescription());
			request.setConfigurationDescription(request.getConfigurationName());

			config.setConfigurationName(request.getConfigurationName());
			config.setConfigurationDescription(request.getConfigurationDescription());
			config.setAmendmentType(request.getAmendmentType());
			config.setIsDefaultConfiguration(
					request.getIsDefaultConfiguration() != null ? request.getIsDefaultConfiguration() : false);

			// Set applicable loan products - clear existing and set new
			if (request.getApplicableLoanProductIds() != null && !request.getApplicableLoanProductIds().isEmpty()) {
				Set<MLoanProductConfiguration> products = request.getApplicableLoanProductIds().stream()
						.map(id -> loanProductConfigRepository.findById(id)
								.orElseThrow(() -> new SetUpExceptions("Loan product not found: " + id)))
						.collect(Collectors.toSet());
				if (config.getApplicableLoanProducts() == null) {
					config.setApplicableLoanProducts(new HashSet<>());
				} else {
					config.getApplicableLoanProducts().clear();
				}
				config.getApplicableLoanProducts().addAll(products);
			} else {
				if (config.getApplicableLoanProducts() == null) {
					config.setApplicableLoanProducts(new HashSet<>());
				} else {
					config.getApplicableLoanProducts().clear();
				}
			}

			// Set allowed borrower types - clear existing and set new
			if (request.getAllowedBorrowerTypes() != null && !request.getAllowedBorrowerTypes().isEmpty()) {
				if (config.getAllowedBorrowerTypes() == null) {
					config.setAllowedBorrowerTypes(new HashSet<>());
				} else {
					config.getAllowedBorrowerTypes().clear();
				}
				config.getAllowedBorrowerTypes().addAll(request.getAllowedBorrowerTypes());
			} else {
				// Default to all borrower types
				if (config.getAllowedBorrowerTypes() == null) {
					config.setAllowedBorrowerTypes(new HashSet<>());
				} else {
					config.getAllowedBorrowerTypes().clear();
				}
				config.getAllowedBorrowerTypes().addAll(Arrays.asList(BorrowerTypeEnum.values()));
			}

			// Set eligibility criteria
			config.setRequireMinimumLoanAge(request.getRequireMinimumLoanAge());
			config.setMinimumLoanAgeDays(request.getMinimumLoanAgeDays() != null ? request.getMinimumLoanAgeDays() : 0);
			config.setRequireMaximumLoanAge(request.getRequireMaximumLoanAge());
			config.setMaximumLoanAgeDays(
					request.getMaximumLoanAgeDays() != null ? request.getMaximumLoanAgeDays() : 3650);
			config.setRequireMinimumOutstandingBalance(request.getRequireMinimumOutstandingBalance());
			config.setMinimumOutstandingBalance(
					request.getMinimumOutstandingBalance() != null ? request.getMinimumOutstandingBalance()
							: BigDecimal.ZERO);
			config.setRequireMaximumOutstandingBalance(request.getRequireMaximumOutstandingBalance());
			config.setMaximumOutstandingBalance(
					request.getMaximumOutstandingBalance() != null ? request.getMaximumOutstandingBalance()
							: BigDecimal.valueOf(1000000000));
			config.setRequireGoodRepaymentHistory(request.getRequireGoodRepaymentHistory());
			config.setMinimumOnTimePayments(
					request.getMinimumOnTimePayments() != null ? request.getMinimumOnTimePayments() : 0);
			config.setAllowForOverdueLoans(request.getAllowForOverdueLoans());
			config.setMaximumDaysOverdueAllowed(
					request.getMaximumDaysOverdueAllowed() != null ? request.getMaximumDaysOverdueAllowed() : 0);
			config.setRequireNoActiveLegalCases(
					request.getRequireNoActiveLegalCases() != null ? request.getRequireNoActiveLegalCases() : true);

			// Set amendment limits based on type
			configureAmendmentLimits(config, request);

			// Set allowed calculation methods - clear existing and set new
			if (request.getAllowedCalculationMethods() != null && !request.getAllowedCalculationMethods().isEmpty()) {
				if (config.getAllowedCalculationMethods() == null) {
					config.setAllowedCalculationMethods(new HashSet<>());
				} else {
					config.getAllowedCalculationMethods().clear();
				}
				config.getAllowedCalculationMethods().addAll(request.getAllowedCalculationMethods());
			} else {
				if (config.getAllowedCalculationMethods() != null) {
					config.getAllowedCalculationMethods().clear();
				}
			}

			// Set allowed borrower type changes - clear existing and set new
			if (request.getAllowedBorrowerTypeChanges() != null && !request.getAllowedBorrowerTypeChanges().isEmpty()) {
				if (config.getAllowedBorrowerTypeChanges() == null) {
					config.setAllowedBorrowerTypeChanges(new HashSet<>());
				} else {
					config.getAllowedBorrowerTypeChanges().clear();
				}
				config.getAllowedBorrowerTypeChanges().addAll(request.getAllowedBorrowerTypeChanges());
			} else {
				if (config.getAllowedBorrowerTypeChanges() != null) {
					config.getAllowedBorrowerTypeChanges().clear();
				}
			}

			// Set allowed target products - clear existing and set new
			if (request.getAllowedTargetProductIds() != null && !request.getAllowedTargetProductIds().isEmpty()) {
				Set<MLoanProductConfiguration> targetProducts = request.getAllowedTargetProductIds().stream()
						.map(id -> loanProductConfigRepository.findById(id)
								.orElseThrow(() -> new SetUpExceptions("Target loan product not found: " + id)))
						.collect(Collectors.toSet());
				if (config.getAllowedTargetProducts() == null) {
					config.setAllowedTargetProducts(new HashSet<>());
				} else {
					config.getAllowedTargetProducts().clear();
				}
				config.getAllowedTargetProducts().addAll(targetProducts);
			} else {
				if (config.getAllowedTargetProducts() == null) {
					config.setAllowedTargetProducts(new HashSet<>());
				} else {
					config.getAllowedTargetProducts().clear();
				}
			}

			// Set approval configuration
			config.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : true);
			config.setRequiredApprovalSteps(
					request.getRequiredApprovalSteps() != null ? request.getRequiredApprovalSteps() : 1);
			config.setAllowAutoApproval(request.getAllowAutoApproval());
			config.setAutoApprovalMaximumAmount(
					request.getAutoApprovalMaximumAmount() != null ? request.getAutoApprovalMaximumAmount()
							: BigDecimal.ZERO);
			config.setAutoApprovalMaximumTermChange(
					request.getAutoApprovalMaximumTermChange() != null ? request.getAutoApprovalMaximumTermChange()
							: BigDecimal.ZERO);
			config.setAutoApprovalMaximumInterestChange(request.getAutoApprovalMaximumInterestChange() != null
					? request.getAutoApprovalMaximumInterestChange()
					: BigDecimal.ZERO);
			config.setAllowQuickApproval(request.getAllowQuickApproval());
			config.setQuickApprovalMaximumAmount(
					request.getQuickApprovalMaximumAmount() != null ? request.getQuickApprovalMaximumAmount()
							: BigDecimal.ZERO);
			config.setQuickApprovalSteps(request.getQuickApprovalSteps() != null ? request.getQuickApprovalSteps() : 1);

			// Set fees & charges
			config.setChargeAmendmentFee(request.getChargeAmendmentFee());
			config.setAmendmentFeeAmount(
					request.getAmendmentFeeAmount() != null ? request.getAmendmentFeeAmount() : BigDecimal.ZERO);
			config.setAmendmentFeePercentage(
					request.getAmendmentFeePercentage() != null ? request.getAmendmentFeePercentage()
							: BigDecimal.ZERO);
			config.setAmendmentFeeType(
					request.getAmendmentFeeType() != null ? request.getAmendmentFeeType() : AmendmentFeeType.NONE);
			config.setChargeProcessingFee(request.getChargeProcessingFee());
			config.setProcessingFeeAmount(
					request.getProcessingFeeAmount() != null ? request.getProcessingFeeAmount() : BigDecimal.ZERO);
			config.setChargeLegalFee(request.getChargeLegalFee());
			config.setLegalFeeAmount(
					request.getLegalFeeAmount() != null ? request.getLegalFeeAmount() : BigDecimal.ZERO);
			config.setAllowFeeWaiver(request.getAllowFeeWaiver());
			config.setFeeWaiverApprovalSteps(
					request.getFeeWaiverApprovalSteps() != null ? request.getFeeWaiverApprovalSteps() : 0);

			// Set documentation requirements
			config.setRequireNewApplicationForm(request.getRequireNewApplicationForm());
			config.setRequireUpdatedContract(
					request.getRequireUpdatedContract() != null ? request.getRequireUpdatedContract() : true);
			config.setRequireBorrowerConsent(
					request.getRequireBorrowerConsent() != null ? request.getRequireBorrowerConsent() : true);
			config.setRequireGuarantorConsent(request.getRequireGuarantorConsent());
			config.setRequireCollateralRevaluation(request.getRequireCollateralRevaluation());
			config.setRequireCreditReassessment(request.getRequireCreditReassessment());
			config.setRequireLegalReview(request.getRequireLegalReview());
			config.setRequireBoardApproval(request.getRequireBoardApproval());
			config.setBoardApprovalThresholdAmount(
					request.getBoardApprovalThresholdAmount() != null ? request.getBoardApprovalThresholdAmount()
							: BigDecimal.valueOf(1000000));

			// Set notification & communication
			config.setNotifyBorrower(request.getNotifyBorrower() != null ? request.getNotifyBorrower() : true);
			config.setNotifyGuarantors(request.getNotifyGuarantors());
			config.setRequireBorrowerAcknowledgement(
					request.getRequireBorrowerAcknowledgement() != null ? request.getRequireBorrowerAcknowledgement()
							: true);
			config.setSendFormalLetter(request.getSendFormalLetter() != null ? request.getSendFormalLetter() : true);
			config.setUpdateCreditBureau(request.getUpdateCreditBureau());
			config.setRequireRegulatoryNotification(request.getRequireRegulatoryNotification());

			// Set risk controls
			config.setTriggerRiskReview(request.getTriggerRiskReview());
			config.setRiskReviewThresholdAmount(
					request.getRiskReviewThresholdAmount() != null ? request.getRiskReviewThresholdAmount()
							: BigDecimal.valueOf(500000));
			config.setRequireCommitteeApproval(request.getRequireCommitteeApproval());
			config.setCommitteeApprovalThresholdAmount(request.getCommitteeApprovalThresholdAmount() != null
					? request.getCommitteeApprovalThresholdAmount()
					: BigDecimal.valueOf(1000000));
			config.setRequireCEOApproval(request.getRequireCEOApproval());
			config.setCeoApprovalThresholdAmount(
					request.getCeoApprovalThresholdAmount() != null ? request.getCeoApprovalThresholdAmount()
							: BigDecimal.valueOf(5000000));
			config.setAllowMultipleAmendments(request.getAllowMultipleAmendments());
			config.setMaximumAmendmentsPerLoan(
					request.getMaximumAmendmentsPerLoan() != null ? request.getMaximumAmendmentsPerLoan() : 3);
			config.setCoolingPeriodDays(request.getCoolingPeriodDays() != null ? request.getCoolingPeriodDays() : 0);

			// Set system & integration
			config.setAutoUpdateLoanSystem(
					request.getAutoUpdateLoanSystem() != null ? request.getAutoUpdateLoanSystem() : true);
			config.setGenerateAmendmentNumber(
					request.getGenerateAmendmentNumber() != null ? request.getGenerateAmendmentNumber() : true);
			config.setAmendmentNumberPrefix(
					request.getAmendmentNumberPrefix() != null ? request.getAmendmentNumberPrefix() : "AMEND");
			config.setCreateAuditTrail(request.getCreateAuditTrail() != null ? request.getCreateAuditTrail() : true);
			config.setUpdateCollateralRegistry(request.getUpdateCollateralRegistry());
			config.setSyncWithCoreBanking(request.getSyncWithCoreBanking());

			// Set validation rules
			config.setValidateCreditLimit(
					request.getValidateCreditLimit() != null ? request.getValidateCreditLimit() : true);
			config.setValidateDebtServiceRatio(
					request.getValidateDebtServiceRatio() != null ? request.getValidateDebtServiceRatio() : true);
			config.setMaximumDebtServiceRatio(
					request.getMaximumDebtServiceRatio() != null ? request.getMaximumDebtServiceRatio()
							: BigDecimal.valueOf(0.7));
			config.setValidateTotalExposure(
					request.getValidateTotalExposure() != null ? request.getValidateTotalExposure() : true);
			config.setMaximumTotalExposure(request.getMaximumTotalExposure() != null ? request.getMaximumTotalExposure()
					: BigDecimal.valueOf(10000000));

			// Set audit fields
			config.setAdOrgID(utils.getAD_Org_ID());
			if (!isUpdate) {
				config.setAD_Amendment_Configuration_UU(UUID.randomUUID().toString());
				config.setActive(true);
				config.setCreatedBy(utils.getLoggedInUser().getUserId());
				config.setCreated(new Date());
			} else {
				config.setUpdatedBy(utils.getLoggedInUser().getUserId());
				config.setUpdated(new Date());
			}

			// Save configuration first
			config = amendmentConfigRepository.save(config);

			// Handle approval workflow
			handleApprovalWorkflow(config, request.getApprovalWorkflow(), isUpdate);

			// Re-fetch to get the workflow
			config = amendmentConfigRepository.findById(config.getAmendmentConfigId())
					.orElseThrow(() -> new SetUpExceptions("Configuration not found after saving"));

			String message = isUpdate ? "Amendment configuration updated successfully"
					: "Amendment configuration created successfully";
			return new ResponseEntity<>(message, 200, objectsMapper.mapLoanAmendmentConfig(config));
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error creating/updating amendment configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to create/update amendment configuration: " + e.getMessage());
		}
	}

	/**
	 * Clear orphan relationships for update
	 */
	private void clearOrphanRelationshipsForUpdate(MAmendmentConfiguration config) {
		// Clear all ManyToMany and ElementCollection relationships
		// to prevent orphan records in join tables

		// Clear applicable loan products (ManyToMany)
		if (config.getApplicableLoanProducts() != null) {
			config.getApplicableLoanProducts().clear();
		}

		// Clear allowed target products (ManyToMany)
		if (config.getAllowedTargetProducts() != null) {
			config.getAllowedTargetProducts().clear();
		}

		Set<MAmendmentApprovalSteps> currentSteps = config.getApprovalWorkflow();
		
		if (!config.getApprovalWorkflow().isEmpty()) {
			for (MAmendmentApprovalSteps step : config.getApprovalWorkflow()) {
				if (!step.getResponsiblePersons().isEmpty()) {
					step.getResponsiblePersons().clear();
				}
			}
			amendmentApprovalStepsRepository.deleteAll(currentSteps);
			amendmentApprovalStepsRepository.flush();
		}
		// Clear ElementCollections
		if (config.getAllowedBorrowerTypes() != null) {

			config.getAllowedBorrowerTypes().clear();
		}

		if (config.getAllowedCalculationMethods() != null) {
			config.getAllowedCalculationMethods().clear();
		}

		if (config.getAllowedBorrowerTypeChanges() != null) {
			config.getAllowedBorrowerTypeChanges().clear();
		}

		// Save to persist cleared collections
		amendmentConfigRepository.save(config);
		amendmentConfigRepository.flush();
	}

	/**
	 * Handle approval workflow creation/update with orphan cleanup
	 */
	private void handleApprovalWorkflow(MAmendmentConfiguration config,
			List<co.ke.tezza.loanapp.model.AmendmentApprovalStepRequest> workflowRequests, boolean isUpdate) {

		if (config.getRequiresApproval() && config.getRequiredApprovalSteps() > 0) {
			// Validate approval workflow
			if (workflowRequests == null || workflowRequests.isEmpty()) {
				throw new SetUpExceptions(
						"Approval workflow is required when approval is enabled. Please provide approval steps with roles.");
			}

			// Validate step count matches required steps
			if (workflowRequests.size() != config.getRequiredApprovalSteps()) {
				throw new SetUpExceptions("Number of approval steps provided (" + workflowRequests.size()
						+ ") does not match required approval steps (" + config.getRequiredApprovalSteps() + ")");
			}

			// If update, delete existing steps
			if (isUpdate) {
				Set<MAmendmentApprovalSteps> existingSteps = config.getApprovalWorkflow();
				if (!existingSteps.isEmpty()) {
					// First clear the ManyToMany relationship
					if (config.getApprovalWorkflow() != null) {
						for (MAmendmentApprovalSteps step : config.getApprovalWorkflow()) {
							if (step.getResponsiblePersons() != null && !step.getResponsiblePersons().isEmpty()) {
								step.getResponsiblePersons().clear();
							}
						}
						config.getApprovalWorkflow().clear();
						amendmentConfigRepository.save(config);
					}
					// Then delete the orphaned approval steps
					amendmentApprovalStepsRepository.deleteAll(existingSteps);
					amendmentApprovalStepsRepository.flush(); // Ensure immediate deletion
				}
			}

			// Create new workflow
			createApprovalWorkflow(config, workflowRequests);
		} else if (isUpdate && !config.getRequiresApproval()) {
			// If updating and approval is disabled, remove any existing steps
			Set<MAmendmentApprovalSteps> existingSteps = config.getApprovalWorkflow();
			if (!existingSteps.isEmpty()) {
				// First clear the ManyToMany relationship
				if (config.getApprovalWorkflow() != null) {
					config.getApprovalWorkflow().clear();
					amendmentConfigRepository.save(config); // Save to clear join table
				}
				// Then delete the orphaned approval steps
				amendmentApprovalStepsRepository.deleteAll(existingSteps);
				amendmentApprovalStepsRepository.flush(); // Ensure immediate deletion
			}
		}
	}

	/**
	 * Create approval workflow with proper ManyToMany relationship setup
	 */
	private void createApprovalWorkflow(MAmendmentConfiguration config,
			List<co.ke.tezza.loanapp.model.AmendmentApprovalStepRequest> workflowRequests) {

		// Validate workflow requests
		if (workflowRequests == null || workflowRequests.isEmpty()) {
			throw new SetUpExceptions(
					"Approval workflow is required when approval is enabled. Please provide approval steps with roles.");
		}

		// Create custom workflow
		int stepNumber = 1;
		Set<MAmendmentApprovalSteps> steps = new HashSet<>();

		for (co.ke.tezza.loanapp.model.AmendmentApprovalStepRequest stepRequest : workflowRequests) {
			Set<MUser> users = new HashSet<>();
			MAmendmentApprovalSteps step = new MAmendmentApprovalSteps();

			step.setStepNumber(stepNumber);

			// Set required role - MANDATORY
			if (stepRequest.getApprovalRoleId() == null) {
				throw new SetUpExceptions("Required role ID is mandatory for approval step " + stepNumber);
			}

			MRoles role = roleRepository.findById(stepRequest.getApprovalRoleId()).orElseThrow(
					() -> new SetUpExceptions("Role not found with ID: " + stepRequest.getApprovalRoleId()));
			step.setRequiredRole(role);

			// Set next role if provided
			if (stepRequest.getNextApprovalRoleId() != null) {
				MRoles nextRole = roleRepository.findById(stepRequest.getNextApprovalRoleId()).orElse(null);

				step.setNextRole(nextRole);
			}

			step.setTrigureStatus(stepRequest.getTrigureStatus());
			step.setApprovalStage(stepRequest.getApprovalStage());
			// Set requirements
			step.setRequireDocumentReview(
					stepRequest.getRequireDocumentReview() != null ? stepRequest.getRequireDocumentReview() : false);

			step.setRequireDigitalSignature(
					stepRequest.getRequireDigitalSignature() != null ? stepRequest.getRequireDigitalSignature()
							: false);

			// Set audit fields
			step.setCreatedBy(utils.getLoggedInUser().getUserId());
			step.setCreated(new Date());
			for (Long id : stepRequest.getResponsiblePersonIds()) {
				MUser user = userRepository.findById(id).orElse(null);
				if (user != null) {
					users.add(user);
				}
			}
			step.setResponsiblePersons(users); 
			step = amendmentApprovalStepsRepository.save(step);

			steps.add(step);
			stepNumber++;
		}

		// Set the workflow on the configuration
		config.setApprovalWorkflow(steps);
		amendmentConfigRepository.save(config);
	}

	/**
	 * DELETE method to completely remove a configuration and all its relationships
	 */
	@Transactional
	public ResponseEntity<Void> deleteConfiguration(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			// Check if configuration is in use
			if (isConfigurationInUse(config)) {
				throw new SetUpExceptions(
						"Cannot delete configuration as it is currently in use by loan amendments. Please deactivate it instead.");
			}

			// Clear all relationships before deletion to avoid foreign key constraints
			clearAllConfigurationRelationships(config);

			// Delete the configuration
			amendmentConfigRepository.delete(config);
			amendmentConfigRepository.flush(); // Ensure immediate deletion

			return new ResponseEntity<>("Configuration deleted successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error deleting configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to delete configuration: " + e.getMessage());
		}
	}

	/**
	 * Check if configuration is in use
	 */
	private boolean isConfigurationInUse(MAmendmentConfiguration config) {
		// Check if there are any loan amendments using this configuration
		// You'll need to implement this based on your LoanAmendmentRequest entity
		// For example:
		// return loanAmendmentRepository.existsByAmendmentConfiguration(config);

		// For now, return false as a placeholder
		return false;
	}

	/**
	 * Clear all relationships before deletion
	 */
	private void clearAllConfigurationRelationships(MAmendmentConfiguration config) {
		// 1. Clear ManyToMany relationships from join tables

		// Clear applicable loan products
		if (config.getApplicableLoanProducts() != null && !config.getApplicableLoanProducts().isEmpty()) {
			config.getApplicableLoanProducts().clear();
		}

		// Clear allowed target products
		if (config.getAllowedTargetProducts() != null && !config.getAllowedTargetProducts().isEmpty()) {
			config.getAllowedTargetProducts().clear();
		}

		// 2. Handle approval workflow (ManyToMany)
		if (config.getApprovalWorkflow() != null && !config.getApprovalWorkflow().isEmpty()) {
			// Since it's ManyToMany, we need to clear the collection first
			// and then delete the orphaned steps

			// Get existing steps before clearing
			List<MAmendmentApprovalSteps> existingSteps = new ArrayList<>(config.getApprovalWorkflow());

			// Clear the collection to remove entries from join table
			config.getApprovalWorkflow().clear();

			// Save to clear the join table
			amendmentConfigRepository.save(config);

			// Delete the orphaned approval steps
			if (!existingSteps.isEmpty()) {
				amendmentApprovalStepsRepository.deleteAll(existingSteps);
				amendmentApprovalStepsRepository.flush();
			}
		}

		// 3. Clear ElementCollections
		// These are automatically cleared when parent is deleted, but we clear
		// explicitly

		if (config.getAllowedBorrowerTypes() != null) {
			config.getAllowedBorrowerTypes().clear();
		}

		if (config.getAllowedCalculationMethods() != null) {
			config.getAllowedCalculationMethods().clear();
		}

		if (config.getAllowedBorrowerTypeChanges() != null) {
			config.getAllowedBorrowerTypeChanges().clear();
		}

		// Save to persist the cleared collections
		amendmentConfigRepository.save(config);
		amendmentConfigRepository.flush();
	}

	/**
	 * Deactivate configuration with relationship cleanup
	 */
	@Transactional
	public ResponseEntity<Void> deactivateConfiguration(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			if (!config.isActive()) {
				return new ResponseEntity<>("Configuration is already deactivated", 200, null);
			}

			// Check if this is a default configuration
			if (config.getIsDefaultConfiguration()) {
				throw new SetUpExceptions(
						"Cannot deactivate default configuration. Please set another configuration as default first.");
			}

			// Clear all ManyToMany relationships to prevent orphan records
			clearAllManyToManyRelationships(config);

			// Deactivate the configuration
			config.setActive(false);
			config.setUpdatedBy(utils.getLoggedInUser().getUserId());
			config.setUpdated(new Date());

			amendmentConfigRepository.save(config);

			return new ResponseEntity<>("Configuration deactivated successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error deactivating configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to deactivate configuration: " + e.getMessage());
		}
	}

	/**
	 * Clear all ManyToMany relationships for deactivation
	 */
	private void clearAllManyToManyRelationships(MAmendmentConfiguration config) {
		// Clear applicable loan products
		if (config.getApplicableLoanProducts() != null && !config.getApplicableLoanProducts().isEmpty()) {
			config.getApplicableLoanProducts().clear();
		}

		// Clear allowed target products
		if (config.getAllowedTargetProducts() != null && !config.getAllowedTargetProducts().isEmpty()) {
			config.getAllowedTargetProducts().clear();
		}

		// Handle approval workflow
		if (config.getApprovalWorkflow() != null && !config.getApprovalWorkflow().isEmpty()) {
			// Get existing steps before clearing
			List<MAmendmentApprovalSteps> existingSteps = new ArrayList<>(config.getApprovalWorkflow());

			// Clear the collection
			config.getApprovalWorkflow().clear();

			// Delete the orphaned approval steps
			if (!existingSteps.isEmpty()) {
				amendmentApprovalStepsRepository.deleteAll(existingSteps);
				amendmentApprovalStepsRepository.flush();
			}
		}

		// Save to persist changes
		amendmentConfigRepository.save(config);
	}

	// ============ REST OF THE EXISTING METHODS (UNCHANGED) ============

	private void configureAmendmentLimits(MAmendmentConfiguration config, AmendmentConfigurationRequest request) {
		// Reset all flags first
		resetAmendmentLimits(config);

		// Set common default values
		setCommonDefaultLimits(config, request);

		// Configure type-specific limits
		switch (config.getAmendmentType()) {
		case TOP_UP:
			config.setAllowTopUp(true);
			config.setMinimumTopUpAmount(
					request.getMinimumTopUpAmount() != null ? request.getMinimumTopUpAmount() : BigDecimal.ZERO);
			config.setMaximumTopUpAmount(request.getMaximumTopUpAmount() != null ? request.getMaximumTopUpAmount()
					: BigDecimal.valueOf(1000000));
			config.setMaximumTopUpPercentage(
					request.getMaximumTopUpPercentage() != null ? request.getMaximumTopUpPercentage()
							: BigDecimal.valueOf(50));
			break;

		case PRINCIPAL_REDUCTION:
			config.setAllowPrincipalReduction(true);
			config.setMaximumPrincipalReductionPercentage(request.getMaximumPrincipalReductionPercentage() != null
					? request.getMaximumPrincipalReductionPercentage()
					: BigDecimal.valueOf(100));
			break;

		case PRINCIPAL_RESTRUCTURING:
			config.setAllowPrincipalReduction(true);
			config.setMaximumRestructuringAmount(
					request.getMaximumRestructuringAmount() != null ? request.getMaximumRestructuringAmount()
							: BigDecimal.valueOf(500000));
			config.setAllowExtendedRepaymentTerms(
					request.getAllowExtendedRepaymentTerms() != null ? request.getAllowExtendedRepaymentTerms() : true);
			break;

		case TERM_EXTENSION:
			config.setAllowTermExtension(true);
			config.setMinimumTermExtensionDays(
					request.getMinimumTermExtensionDays() != null ? request.getMinimumTermExtensionDays() : 0);
			config.setMaximumTermExtensionDays(
					request.getMaximumTermExtensionDays() != null ? request.getMaximumTermExtensionDays() : 365);
			break;

		case TERM_REDUCTION:
			config.setAllowTermExtension(true);
			config.setMinimumTermReductionDays(
					request.getMinimumTermReductionDays() != null ? request.getMinimumTermReductionDays() : 0);
			config.setMaximumTermReductionDays(
					request.getMaximumTermReductionDays() != null ? request.getMaximumTermReductionDays() : 180);
			break;

		case GRACE_PERIOD_EXTENSION:
			config.setAllowGracePeriodExtension(true);
			config.setMaximumGracePeriodExtensionDays(
					request.getMaximumGracePeriodExtensionDays() != null ? request.getMaximumGracePeriodExtensionDays()
							: 30);
			break;

		case INTEREST_RATE_CHANGE:
			config.setAllowInterestRateChange(true);
			config.setMinimumInterestRateChange(
					request.getMinimumInterestRateChange() != null ? request.getMinimumInterestRateChange()
							: BigDecimal.ZERO);
			config.setMaximumInterestRateChange(
					request.getMaximumInterestRateChange() != null ? request.getMaximumInterestRateChange()
							: BigDecimal.valueOf(100));
			config.setAllowRateDecrease(request.getAllowRateDecrease() != null ? request.getAllowRateDecrease() : true);
			config.setAllowRateIncrease(request.getAllowRateIncrease() != null ? request.getAllowRateIncrease() : true);
			break;

		case FLAT_RATE_CHANGE:
			config.setAllowFlatRateChange(true);
			config.setMinimumFlatRateChange(
					request.getMinimumFlatRateChange() != null ? request.getMinimumFlatRateChange() : BigDecimal.ZERO);
			config.setMaximumFlatRateChange(
					request.getMaximumFlatRateChange() != null ? request.getMaximumFlatRateChange()
							: BigDecimal.valueOf(50));
			break;

		case INTEREST_CALCULATION_CHANGE:
			config.setAllowInterestCalculationChange(true);
			config.setAllowedCalculationMethods(request.getAllowedCalculationMethods() != null
					? new HashSet<>(request.getAllowedCalculationMethods())
					: getAllCalculationMethods());
			break;

		case BORROWER_CHANGE:
			config.setAllowBorrowerChange(true);
			config.setRequireNewCreditCheck(
					request.getRequireNewCreditCheck() != null ? request.getRequireNewCreditCheck() : true);
			config.setRequireNewGuarantors(
					request.getRequireNewGuarantors() != null ? request.getRequireNewGuarantors() : true);
			break;

		case BORROWER_TYPE_CHANGE:
			config.setAllowBorrowerTypeChange(true);
			config.setAllowedBorrowerTypeChanges(request.getAllowedBorrowerTypeChanges() != null
					? new HashSet<>(request.getAllowedBorrowerTypeChanges())
					: getAllBorrowerTypeChanges());
			break;

		case PRODUCT_CHANGE:
			Set<MLoanProductConfiguration> allowedTargetProducts = new HashSet<>();
			if (request.getApplicableLoanProductIds() != null && !request.getApplicableLoanProductIds().isEmpty()) {
				for (Long id : request.getApplicableLoanProductIds()) {
					MLoanProductConfiguration targetProduct = loanProductConfigRepository.findById(id).orElse(null);
					if (targetProduct != null) {
						allowedTargetProducts.add(targetProduct);
					}
				}
			}
			config.setAllowProductChange(true);
			config.setAllowedTargetProducts(allowedTargetProducts);
			config.setRequireProductCompatibilityCheck(request.getRequireProductCompatibilityCheck() != null
					? request.getRequireProductCompatibilityCheck()
					: true);
			break;

		case CONTRACT_TERMS_CHANGE:
			config.setAllowContractTermsChange(true);
			config.setMaximumContractTermChanges(
					request.getMaximumContractTermChanges() != null ? request.getMaximumContractTermChanges() : 5);
			config.setRequireLegalReview(
					request.getRequireLegalReview() != null ? request.getRequireLegalReview() : true);
			break;

		case COLLATERAL_CHANGE:
			config.setAllowCollateralChange(true);
			config.setMaximumCollateralChanges(
					request.getMaximumCollateralChanges() != null ? request.getMaximumCollateralChanges() : 3);
			config.setRequireCollateralRevaluation(
					request.getRequireCollateralRevaluation() != null ? request.getRequireCollateralRevaluation()
							: true);
			break;

		case GUARANTOR_CHANGE:
			config.setAllowGuarantorChange(true);
			config.setMaximumGuarantorChanges(
					request.getMaximumGuarantorChanges() != null ? request.getMaximumGuarantorChanges() : 3);
			config.setRequireNewGuarantorApproval(
					request.getRequireNewGuarantorApproval() != null ? request.getRequireNewGuarantorApproval() : true);
			break;

		case SECURITY_ENHANCEMENT:
			config.setAllowSecurityEnhancement(true);
			config.setMinimumSecurityCoverageIncrease(
					request.getMinimumSecurityCoverageIncrease() != null ? request.getMinimumSecurityCoverageIncrease()
							: BigDecimal.valueOf(10));
			config.setRequireLegalDocumentation(
					request.getRequireLegalDocumentation() != null ? request.getRequireLegalDocumentation() : true);
			break;

		case PENALTY_WAIVER:
			config.setAllowPenaltyWaiver(true);
			config.setMaximumPenaltyWaiverPercentage(
					request.getMaximumPenaltyWaiverPercentage() != null ? request.getMaximumPenaltyWaiverPercentage()
							: BigDecimal.valueOf(100));
			config.setRequireJustificationDocument(
					request.getRequireJustificationDocument() != null ? request.getRequireJustificationDocument()
							: true);
			break;

		case FEE_WAIVER:
			config.setAllowFeeWaiver(true);
			config.setMaximumFeeWaiverAmount(
					request.getMaximumFeeWaiverAmount() != null ? request.getMaximumFeeWaiverAmount()
							: BigDecimal.valueOf(10000));
			config.setMaximumFeeWaiverPercentage(
					request.getMaximumFeeWaiverPercentage() != null ? request.getMaximumFeeWaiverPercentage()
							: BigDecimal.valueOf(100));
			break;

		case PENALTY_RATE_CHANGE:
			config.setAllowPenaltyRateChange(true);
			config.setMinimumPenaltyRateChange(
					request.getMinimumPenaltyRateChange() != null ? request.getMinimumPenaltyRateChange()
							: BigDecimal.ZERO);
			config.setMaximumPenaltyRateChange(
					request.getMaximumPenaltyRateChange() != null ? request.getMaximumPenaltyRateChange()
							: BigDecimal.valueOf(100));
			break;

		case REPAYMENT_SCHEDULE_CHANGE:
			config.setAllowRepaymentScheduleChange(true);
			config.setMaximumScheduleChanges(
					request.getMaximumScheduleChanges() != null ? request.getMaximumScheduleChanges() : 12);
			config.setRequirePaymentCapacityAnalysis(
					request.getRequirePaymentCapacityAnalysis() != null ? request.getRequirePaymentCapacityAnalysis()
							: true);
			break;

		case INSTALLMENT_RESCHEDULING:
			config.setAllowInstallmentRescheduling(true);
			config.setMaximumRescheduledInstallments(
					request.getMaximumRescheduledInstallments() != null ? request.getMaximumRescheduledInstallments()
							: 6);
			config.setAllowInstallmentAmountChange(
					request.getAllowInstallmentAmountChange() != null ? request.getAllowInstallmentAmountChange()
							: true);
			break;

		case PAYMENT_HOLIDAY:
			config.setAllowPaymentHoliday(true);
			config.setMaximumPaymentHolidayDays(
					request.getMaximumPaymentHolidayDays() != null ? request.getMaximumPaymentHolidayDays() : 90);
			config.setAllowInterestCapitalization(
					request.getAllowInterestCapitalization() != null ? request.getAllowInterestCapitalization() : true);
			break;

		case DOCUMENT_UPDATE:
			config.setAllowDocumentUpdate(true);
			config.setMaximumDocumentUpdates(
					request.getMaximumDocumentUpdates() != null ? request.getMaximumDocumentUpdates() : 10);
			config.setRequireVersionControl(
					request.getRequireVersionControl() != null ? request.getRequireVersionControl() : true);
			break;

		case ADMINISTRATIVE_CORRECTION:
			config.setAllowAdministrativeCorrection(true);
			config.setMaximumCorrectionAmount(
					request.getMaximumCorrectionAmount() != null ? request.getMaximumCorrectionAmount()
							: BigDecimal.valueOf(100000));
			config.setRequireAuditTrail(request.getRequireAuditTrail() != null ? request.getRequireAuditTrail() : true);
			break;

		case EMERGENCY_AMENDMENT:
			config.setAllowEmergencyAmendment(true);
			config.setEmergencyApprovalThreshold(
					request.getEmergencyApprovalThreshold() != null ? request.getEmergencyApprovalThreshold()
							: BigDecimal.valueOf(500000));
			config.setEmergencyResponseTimeHours(
					request.getEmergencyResponseTimeHours() != null ? request.getEmergencyResponseTimeHours() : 24);
			break;

		case LOAN_RENEWAL:
			config.setAllowLoanRenewal(true);
			config.setMaximumRenewalCount(
					request.getMaximumRenewalCount() != null ? request.getMaximumRenewalCount() : 3);
			config.setRequireFullReassessment(
					request.getRequireFullReassessment() != null ? request.getRequireFullReassessment() : true);
			break;

		case LOAN_RESCHEDULING:
			config.setAllowLoanRescheduling(true);
			config.setMaximumReschedulingMonths(
					request.getMaximumReschedulingMonths() != null ? request.getMaximumReschedulingMonths() : 24);
			config.setRequireDebtSustainabilityAnalysis(request.getRequireDebtSustainabilityAnalysis() != null
					? request.getRequireDebtSustainabilityAnalysis()
					: true);
			break;

		case FORBEARANCE:
			config.setAllowForbearance(true);
			config.setMaximumForbearanceMonths(
					request.getMaximumForbearanceMonths() != null ? request.getMaximumForbearanceMonths() : 6);
			config.setRequireFinancialHardshipProof(
					request.getRequireFinancialHardshipProof() != null ? request.getRequireFinancialHardshipProof()
							: true);
			break;

		default:
			// For any other amendment types, set generic defaults
			config.setAllowGenericAmendment(true);
			break;
		}
	}

	private void resetAmendmentLimits(MAmendmentConfiguration config) {
		// Reset all boolean flags to false
		config.setAllowTopUp(false);
		config.setAllowPrincipalReduction(false);
		config.setAllowPrincipalReduction(false);
		config.setAllowTermExtension(false);
		config.setAllowTermExtension(false);
		config.setAllowGracePeriodExtension(false);
		config.setAllowInterestRateChange(false);
		config.setAllowFlatRateChange(false);
		config.setAllowInterestCalculationChange(false);
		config.setAllowBorrowerChange(false);
		config.setAllowBorrowerTypeChange(false);
		config.setAllowProductChange(false);
		config.setAllowContractTermsChange(false);
		config.setAllowCollateralChange(false);
		config.setAllowGuarantorChange(false);
		config.setAllowSecurityEnhancement(false);
		config.setAllowPenaltyWaiver(false);
		config.setAllowFeeWaiver(false);
		config.setAllowPenaltyRateChange(false);
		config.setAllowRepaymentScheduleChange(false);
		config.setAllowInstallmentRescheduling(false);
		config.setAllowPaymentHoliday(false);
		config.setAllowDocumentUpdate(false);
		config.setAllowAdministrativeCorrection(false);
		config.setAllowEmergencyAmendment(false);
		config.setAllowLoanRenewal(false);
		config.setAllowLoanRescheduling(false);
		config.setAllowForbearance(false);
		config.setAllowGenericAmendment(false);
	}

	private void setCommonDefaultLimits(MAmendmentConfiguration config, AmendmentConfigurationRequest request) {
		// Set common default values that apply to most amendment types
		config.setMinimumAmountChange(
				request.getMinimumAmountChange() != null ? request.getMinimumAmountChange() : BigDecimal.ZERO);
		config.setMaximumAmountChange(request.getMaximumAmountChange() != null ? request.getMaximumAmountChange()
				: BigDecimal.valueOf(1000000));
		config.setMinimumPercentageChange(
				request.getMinimumPercentageChange() != null ? request.getMinimumPercentageChange() : BigDecimal.ZERO);
		config.setMaximumPercentageChange(
				request.getMaximumPercentageChange() != null ? request.getMaximumPercentageChange()
						: BigDecimal.valueOf(100));
		config.setMinimumDaysChange(request.getMinimumDaysChange() != null ? request.getMinimumDaysChange() : 0);
		config.setMaximumDaysChange(request.getMaximumDaysChange() != null ? request.getMaximumDaysChange() : 365);

		// Common approval settings
		config.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : true);
		config.setAllowAutoApproval(request.getAllowAutoApproval() != null ? request.getAllowAutoApproval() : false);
		config.setAllowQuickApproval(request.getAllowQuickApproval() != null ? request.getAllowQuickApproval() : false);
	}

	private Set<String> getAllCalculationMethods() {
		return new HashSet<>(Arrays.asList("DAILY_COMPOUND", "MONTHLY_COMPOUND", "ANNUAL_COMPOUND", "SIMPLE_INTEREST",
				"REDUCING_BALANCE", "FLAT_RATE"));
	}

	private Set<BorrowerTypeEnum> getAllBorrowerTypeChanges() {
		return new HashSet<>(
				Arrays.asList(BorrowerTypeEnum.INDIVIDUAL, BorrowerTypeEnum.GROUP, BorrowerTypeEnum.INSTITUTION));
	}

	private void validateConfigurationRequest(AmendmentConfigurationRequest request) {
		if (request.getConfigurationName() == null || request.getConfigurationName().trim().isEmpty()) {
			throw new SetUpExceptions("Configuration name is required");
		}

		if (request.getAmendmentType() == null) {
			throw new SetUpExceptions("Amendment type is required");
		}

		// Check for duplicate configuration name (only for new configurations)
		if (request.getAmendmentConfigId() <= 0) {
			List<MAmendmentConfiguration> existingConfigs = amendmentConfigRepository
					.findByIsActiveAndAdOrgIDOrderByConfigurationNameAsc(true, utils.getAD_Org_ID());
			boolean nameExists = existingConfigs.stream()
					.anyMatch(config -> config.getConfigurationName().equalsIgnoreCase(request.getConfigurationName()));

			if (nameExists) {
				throw new SetUpExceptions("Configuration name already exists: " + request.getConfigurationName());
			}
		}

		// Validate fee configuration
		if (request.getChargeAmendmentFee() != null && request.getChargeAmendmentFee()) {
			if (request.getAmendmentFeeType() == null) {
				throw new SetUpExceptions("Amendment fee type is required when charging amendment fee");
			}

			if (request.getAmendmentFeeType() == AmendmentFeeType.FLAT_AMOUNT
					&& (request.getAmendmentFeeAmount() == null
							|| request.getAmendmentFeeAmount().compareTo(BigDecimal.ZERO) <= 0)) {
				throw new SetUpExceptions("Amendment fee amount must be greater than zero for flat amount fee type");
			}

			if (request.getAmendmentFeeType() == AmendmentFeeType.PERCENTAGE
					&& (request.getAmendmentFeePercentage() == null
							|| request.getAmendmentFeePercentage().compareTo(BigDecimal.ZERO) <= 0)) {
				throw new SetUpExceptions("Amendment fee percentage must be greater than zero for percentage fee type");
			}
		}

		// Validate approval steps
		if (request.getRequiresApproval() != null && request.getRequiresApproval()) {
			if (request.getRequiredApprovalSteps() == null || request.getRequiredApprovalSteps() < 1) {
				throw new SetUpExceptions("Required approval steps must be at least 1 when approval is required");
			}

			// Validate approval workflow
			if (request.getApprovalWorkflow() == null || request.getApprovalWorkflow().isEmpty()) {
				throw new SetUpExceptions(
						"Approval workflow is required when approval is enabled. Please provide approval steps with roles.");
			}

			// Validate each step has required role
			if (!request.getApprovalWorkflow().isEmpty() && request.getRequiredApprovalSteps() > 0) {

				for (AmendmentApprovalStepRequest step : request.getApprovalWorkflow()) {
					if (step.getApprovalRoleId() == null) {
						throw new SetUpExceptions(
								"Required role ID is mandatory for approval step " + step.getStepNumber());
					}

					// Validate role exists
					if (!roleRepository.existsById(step.getApprovalRoleId())) {
						throw new SetUpExceptions("Role not found with ID: " + step.getApprovalRoleId() + " for step "
								+ step.getStepNumber());
					}
					// Validate next role if provided (only for non-last steps)
					if (step.getNextApprovalRoleId() != null && step.getNextApprovalRoleId() > 0) {
						// Only validate next role if this is not the last step
						if (step.getStepNumber() < request.getRequiredApprovalSteps()) {
							if (!roleRepository.existsById(step.getNextApprovalRoleId())) {
								throw new SetUpExceptions("Next role not found with ID: " + step.getNextApprovalRoleId()
										+ " for step " + step.getStepNumber());
							}
						} else {
							// Last step shouldn't have a next role - we'll ignore it
							log.warn("Next role specified for last approval step {} will be ignored",
									step.getStepNumber());
						}
					}

				}
			}

		}

		// Validate amendment limits
		validateAmendmentLimits(request);
	}

	private void validateAmendmentLimits(AmendmentConfigurationRequest request) {
		switch (request.getAmendmentType()) {
		case TOP_UP:
			if (request.getMinimumTopUpAmount() != null && request.getMaximumTopUpAmount() != null
					&& request.getMinimumTopUpAmount().compareTo(request.getMaximumTopUpAmount()) > 0) {
				throw new SetUpExceptions("Minimum top-up amount cannot be greater than maximum top-up amount");
			}
			if (request.getMaximumTopUpPercentage() != null
					&& (request.getMaximumTopUpPercentage().compareTo(BigDecimal.ZERO) <= 0
							|| request.getMaximumTopUpPercentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
				throw new SetUpExceptions("Maximum top-up percentage must be between 0 and 100");
			}
			break;

		case TERM_EXTENSION:
			if (request.getMinimumTermExtensionDays() != null && request.getMaximumTermExtensionDays() != null
					&& request.getMinimumTermExtensionDays() > request.getMaximumTermExtensionDays()) {
				throw new SetUpExceptions(
						"Minimum term extension days cannot be greater than maximum term extension days");
			}
			break;

		case INTEREST_RATE_CHANGE:
			if (request.getMinimumInterestRateChange() != null && request.getMaximumInterestRateChange() != null
					&& request.getMinimumInterestRateChange().compareTo(request.getMaximumInterestRateChange()) > 0) {
				throw new SetUpExceptions(
						"Minimum interest rate change cannot be greater than maximum interest rate change");
			}
			break;

		default:
			// For other amendment types, implement specific validations
			break;
		}
	}

	/**
	 * Get amendment configuration for a specific type and loan product
	 */
	public ResponseEntity<MAmendmentConfiguration> getConfigurationForAmendment(AmendmentType amendmentType,
			Long loanProductId) {
		try {
			// First try to get configuration specific to this loan product
			List<MAmendmentConfiguration> configs = amendmentConfigRepository
					.findByAmendmentTypeAndApplicableLoanProducts_LoanProductConfigId(amendmentType, loanProductId,
							utils.getAD_Org_ID());

			if (!configs.isEmpty()) {
				// Filter for active configurations
				List<MAmendmentConfiguration> activeConfigs = configs.stream().filter(MAmendmentConfiguration::isActive)
						.collect(Collectors.toList());

				if (!activeConfigs.isEmpty()) {
					return new ResponseEntity<>("Configuration found", 200, activeConfigs.get(0));
				}
			}

			// If not found, try to get default configuration for this amendment type
			configs = amendmentConfigRepository.findByAmendmentTypeAndIsActiveAndIsDefaultConfiguration(amendmentType,
					true, true);

			if (!configs.isEmpty()) {
				return new ResponseEntity<>("Default configuration found", 200, configs.get(0));
			}

			// If still not found, return error
			return new ResponseEntity<>("No configuration found for amendment type: " + amendmentType, 404, null);
		} catch (Exception e) {
			log.error("Error getting configuration for amendment: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error getting configuration: " + e.getMessage(), 500, null);
		}
	}

	/**
	 * Get configuration by ID
	 */
	public ResponseEntity<MAmendmentConfiguration> getConfigurationById(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			return new ResponseEntity<>("Configuration retrieved successfully", 200, config);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting configuration by ID: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to retrieve configuration: " + e.getMessage());
		}
	}

	/**
	 * Validate if an amendment request meets configuration requirements
	 */
	public ResponseEntity<Void> validateAmendmentRequest(MLoanApplication loan, AmendmentType amendmentType,
			Map<String, Object> amendmentDetails) {

		ResponseEntity<MAmendmentConfiguration> configResponse = getConfigurationForAmendment(amendmentType,
				loan.getLoanProductConfiguration().getLoanProductConfigId());

		if (configResponse.getEntity() == null) {
			return new ResponseEntity<>(configResponse.getMessage(), 400, null);
		}

		MAmendmentConfiguration config = configResponse.getEntity();

		// Check eligibility
		if (!isEligibleForAmendment(loan, config)) {
			return new ResponseEntity<>("Loan is not eligible for this amendment type", 400, null);
		}

		// Check amendment limits
		if (!withinAmendmentLimits(loan, config, amendmentDetails)) {
			return new ResponseEntity<>("Amendment request exceeds allowed limits", 400, null);
		}

		// Check borrower type
		if (!config.getAllowedBorrowerTypes().contains(loan.getBorrowerType())) {
			return new ResponseEntity<>("Borrower type not allowed for this amendment", 400, null);
		}

		// Check cooling period
		if (!withinCoolingPeriod(loan, config)) {
			return new ResponseEntity<>("Cooling period has not elapsed since last amendment", 400, null);
		}

		// Check maximum amendments
		if (!withinMaximumAmendments(loan, config)) {
			return new ResponseEntity<>("Maximum number of amendments reached for this loan", 400, null);
		}

		// Check if approval is required and get approval steps
		if (config.getRequiresApproval()) {
			Set<MAmendmentApprovalSteps> approvalSteps = config.getApprovalWorkflow();
			if (approvalSteps.isEmpty()) {
				return new ResponseEntity<>("Approval workflow not configured for this amendment type", 400, null);
			}
		}

		return new ResponseEntity<>("Amendment request is valid", 200, null);
	}

	private boolean isEligibleForAmendment(MLoanApplication loan, MAmendmentConfiguration config) {
		Date now = new Date();

		// Check minimum loan age
		if (config.getRequireMinimumLoanAge()) {
			long loanAgeDays = (now.getTime() - loan.getCreated().getTime()) / (1000 * 60 * 60 * 24);
			if (loanAgeDays < config.getMinimumLoanAgeDays()) {
				return false;
			}
		}

		// Check maximum loan age
		if (config.getRequireMaximumLoanAge()) {
			long loanAgeDays = (now.getTime() - loan.getCreated().getTime()) / (1000 * 60 * 60 * 24);
			if (loanAgeDays > config.getMaximumLoanAgeDays()) {
				return false;
			}
		}

		// Check outstanding balance
		BigDecimal outstandingBalance = loan.getBalance();
		if (config.getRequireMinimumOutstandingBalance()
				&& outstandingBalance.compareTo(config.getMinimumOutstandingBalance()) < 0) {
			return false;
		}

		if (config.getRequireMaximumOutstandingBalance()
				&& outstandingBalance.compareTo(config.getMaximumOutstandingBalance()) > 0) {
			return false;
		}

		// Check overdue status
		if (!config.getAllowForOverdueLoans() && isLoanOverdue(loan)) {
			if (config.getMaximumDaysOverdueAllowed() > 0) {
				long overdueDays = getOverdueDays(loan);
				if (overdueDays > config.getMaximumDaysOverdueAllowed()) {
					return false;
				}
			} else {
				return false;
			}
		}

		// Check repayment history (simplified - you'd need actual repayment history)
		if (config.getRequireGoodRepaymentHistory() && config.getMinimumOnTimePayments() > 0) {
			// Implementation would depend on your repayment tracking system
			// For now, return true
		}

		return true;
	}

	private boolean withinAmendmentLimits(MLoanApplication loan, MAmendmentConfiguration config,
			Map<String, Object> amendmentDetails) {

		switch (config.getAmendmentType()) {
		case TOP_UP:
			BigDecimal topUpAmount = (BigDecimal) amendmentDetails.get("topUpAmount");
			if (topUpAmount != null) {
				// Check amount limits
				if (topUpAmount.compareTo(config.getMinimumTopUpAmount()) < 0) {
					return false;
				}
				if (topUpAmount.compareTo(config.getMaximumTopUpAmount()) > 0) {
					return false;
				}
				// Check percentage limit
				BigDecimal originalAmount = loan.getAppliedAmount();
				if (originalAmount.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal topUpPercentage = topUpAmount.divide(originalAmount, 4, RoundingMode.HALF_UP)
							.multiply(BigDecimal.valueOf(100));
					if (topUpPercentage.compareTo(config.getMaximumTopUpPercentage()) > 0) {
						return false;
					}
				}
			}
			break;

		case TERM_EXTENSION:
			Integer extensionDays = (Integer) amendmentDetails.get("extensionDays");
			if (extensionDays != null) {
				if (extensionDays < config.getMinimumTermExtensionDays()) {
					return false;
				}
				if (extensionDays > config.getMaximumTermExtensionDays()) {
					return false;
				}
			}
			break;

		case INTEREST_RATE_CHANGE:
			BigDecimal rateChange = (BigDecimal) amendmentDetails.get("rateChange");
			if (rateChange != null) {
				if (rateChange.compareTo(config.getMinimumInterestRateChange()) < 0) {
					return false;
				}
				if (rateChange.compareTo(config.getMaximumInterestRateChange()) > 0) {
					return false;
				}
			}
			break;

		default:
			// For other amendment types, implement specific validations
			break;
		}

		return true;
	}

	private boolean withinCoolingPeriod(MLoanApplication loan, MAmendmentConfiguration config) {
		if (config.getCoolingPeriodDays() <= 0) {
			return true;
		}

		// Get the last amendment date for this loan
		Date lastAmendmentDate = getLastAmendmentDate(loan);
		if (lastAmendmentDate == null) {
			return true; // No previous amendments
		}

		long daysSinceLastAmendment = (new Date().getTime() - lastAmendmentDate.getTime()) / (1000 * 60 * 60 * 24);
		return daysSinceLastAmendment >= config.getCoolingPeriodDays();
	}

	private boolean withinMaximumAmendments(MLoanApplication loan, MAmendmentConfiguration config) {
		if (!config.getAllowMultipleAmendments()) {
			return getAmendmentCount(loan) == 0;
		}

		return getAmendmentCount(loan) < config.getMaximumAmendmentsPerLoan();
	}

	// Helper methods
	private boolean isLoanOverdue(MLoanApplication loan) {
		if (loan.getDueDate() == null)
			return false;
		return new Date().after(loan.getDueDate());
	}

	private long getOverdueDays(MLoanApplication loan) {
		if (!isLoanOverdue(loan))
			return 0;
		return (new Date().getTime() - loan.getDueDate().getTime()) / (1000 * 60 * 60 * 24);
	}

	private Date getLastAmendmentDate(MLoanApplication loan) {
		List<MLoanAmendmentRequest> amendments = amendmentRepository.findByLoanToAmendOrderByCreatedDesc(loan);
		if (!amendments.isEmpty()) {
			return amendments.get(0).getCreated();
		}
		return null;
	}

	private int getAmendmentCount(MLoanApplication loan) {
		return amendmentRepository.countByLoanToAmend(loan);
	}

	/**
	 * Get all amendment configurations with pagination and search
	 */
	public Page<LoanAmendmentConfigResponse> getAllAmendmentConfigs(int page, int size, String searchTerm) {
		try {

			// Create pageable object
			Pageable pageable = PageRequest.of(page, size);

			Long adOrgID = utils.getAD_Org_ID();

			Page<MAmendmentConfiguration> configs;

			if (searchTerm != null && !searchTerm.trim().isEmpty()) {
				// Use the comprehensive search query
				configs = amendmentConfigRepository.searchByAdOrgID(adOrgID, true, searchTerm, pageable);
			} else {
				// Get all active configurations for the organization with pagination
				configs = amendmentConfigRepository.findByIsActiveAndAdOrgIDOrderByConfigurationNameAsc(true, adOrgID,
						pageable);
			}

			return configs.map(objectsMapper::mapLoanAmendmentConfig);

		} catch (Exception e) {
			log.error("Error retrieving amendment configurations: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get configurations by amendment type
	 */
	public ResponseEntity<List<MAmendmentConfiguration>> getConfigurationsByType(AmendmentType amendmentType) {
		try {
			List<MAmendmentConfiguration> configs = amendmentConfigRepository
					.findByAmendmentTypeAndIsActiveAndAdOrgIDOrderByConfigurationNameAsc(amendmentType, true,
							utils.getAD_Org_ID());

			return new ResponseEntity<>("Configurations retrieved successfully", 200, configs);
		} catch (Exception e) {
			log.error("Error retrieving configurations by type: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error retrieving configurations: " + e.getMessage(), 500, null);
		}
	}

	/**
	 * Activate configuration
	 */
	@Transactional
	public ResponseEntity<Void> activateConfiguration(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			if (config.isActive()) {
				return new ResponseEntity<>("Configuration is already active", 200, null);
			}

			config.setActive(true);
			config.setUpdatedBy(utils.getLoggedInUser().getUserId());
			config.setUpdated(new Date());
			amendmentConfigRepository.save(config);

			return new ResponseEntity<>("Configuration activated successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error activating configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to activate configuration: " + e.getMessage());
		}
	}

	/**
	 * Set configuration as default
	 */
	@Transactional
	public ResponseEntity<Void> setAsDefaultConfiguration(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			if (!config.isActive()) {
				throw new SetUpExceptions("Cannot set inactive configuration as default");
			}

			// Clear default flag from other configurations of the same amendment type
			List<MAmendmentConfiguration> defaultConfigs = amendmentConfigRepository
					.findByAmendmentTypeAndIsActiveAndIsDefaultConfiguration(config.getAmendmentType(), true, true);

			for (MAmendmentConfiguration defaultConfig : defaultConfigs) {
				defaultConfig.setIsDefaultConfiguration(false);
				defaultConfig.setUpdatedBy(utils.getLoggedInUser().getUserId());
				defaultConfig.setUpdated(new Date());
				amendmentConfigRepository.save(defaultConfig);
			}

			// Set this configuration as default
			config.setIsDefaultConfiguration(true);
			config.setUpdatedBy(utils.getLoggedInUser().getUserId());
			config.setUpdated(new Date());
			amendmentConfigRepository.save(config);

			return new ResponseEntity<>("Configuration set as default successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error setting configuration as default: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to set configuration as default: " + e.getMessage());
		}
	}

	/**
	 * Clone existing configuration
	 */
	@Transactional
	public ResponseEntity<MAmendmentConfiguration> cloneConfiguration(Long configId, String newConfigName) {
		try {
			MAmendmentConfiguration originalConfig = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			// Validate new configuration name
			if (newConfigName == null || newConfigName.trim().isEmpty()) {
				throw new SetUpExceptions("New configuration name is required");
			}

			// Check for duplicate name
			List<MAmendmentConfiguration> existingConfigs = amendmentConfigRepository
					.findByIsActiveAndAdOrgIDOrderByConfigurationNameAsc(true, utils.getAD_Org_ID());
			boolean nameExists = existingConfigs.stream()
					.anyMatch(config -> config.getConfigurationName().equalsIgnoreCase(newConfigName));

			if (nameExists) {
				throw new SetUpExceptions("Configuration name already exists: " + newConfigName);
			}

			// Create new configuration
			MAmendmentConfiguration newConfig = new MAmendmentConfiguration();

			// Copy all fields except ID and UUID
			newConfig.setConfigurationName(newConfigName);
			newConfig.setConfigurationDescription(originalConfig.getConfigurationDescription() + " (Clone)");
			newConfig.setAmendmentType(originalConfig.getAmendmentType());
			newConfig.setActive(true);
			newConfig.setIsDefaultConfiguration(false); // Clones should not be default

			// Copy collections
			if (originalConfig.getApplicableLoanProducts() != null) {
				newConfig.setApplicableLoanProducts(new HashSet<>(originalConfig.getApplicableLoanProducts()));
			}
			if (originalConfig.getAllowedBorrowerTypes() != null) {
				newConfig.setAllowedBorrowerTypes(new HashSet<>(originalConfig.getAllowedBorrowerTypes()));
			}

			// Copy eligibility criteria
			newConfig.setRequireMinimumLoanAge(originalConfig.getRequireMinimumLoanAge());
			newConfig.setMinimumLoanAgeDays(originalConfig.getMinimumLoanAgeDays());
			newConfig.setRequireMaximumLoanAge(originalConfig.getRequireMaximumLoanAge());
			newConfig.setMaximumLoanAgeDays(originalConfig.getMaximumLoanAgeDays());
			newConfig.setRequireMinimumOutstandingBalance(originalConfig.getRequireMinimumOutstandingBalance());
			newConfig.setMinimumOutstandingBalance(originalConfig.getMinimumOutstandingBalance());
			newConfig.setRequireMaximumOutstandingBalance(originalConfig.getRequireMaximumOutstandingBalance());
			newConfig.setMaximumOutstandingBalance(originalConfig.getMaximumOutstandingBalance());
			newConfig.setRequireGoodRepaymentHistory(originalConfig.getRequireGoodRepaymentHistory());
			newConfig.setMinimumOnTimePayments(originalConfig.getMinimumOnTimePayments());
			newConfig.setAllowForOverdueLoans(originalConfig.getAllowForOverdueLoans());
			newConfig.setMaximumDaysOverdueAllowed(originalConfig.getMaximumDaysOverdueAllowed());
			newConfig.setRequireNoActiveLegalCases(originalConfig.getRequireNoActiveLegalCases());

			// Copy amendment limits
			newConfig.setAllowTopUp(originalConfig.getAllowTopUp());
			newConfig.setMinimumTopUpAmount(originalConfig.getMinimumTopUpAmount());
			newConfig.setMaximumTopUpAmount(originalConfig.getMaximumTopUpAmount());
			newConfig.setMaximumTopUpPercentage(originalConfig.getMaximumTopUpPercentage());
			newConfig.setAllowPrincipalReduction(originalConfig.getAllowPrincipalReduction());
			newConfig.setMaximumPrincipalReductionPercentage(originalConfig.getMaximumPrincipalReductionPercentage());
			newConfig.setAllowTermExtension(originalConfig.getAllowTermExtension());
			newConfig.setMinimumTermExtensionDays(originalConfig.getMinimumTermExtensionDays());
			newConfig.setMaximumTermExtensionDays(originalConfig.getMaximumTermExtensionDays());
			newConfig.setAllowInterestRateChange(originalConfig.getAllowInterestRateChange());
			newConfig.setMinimumInterestRateChange(originalConfig.getMinimumInterestRateChange());
			newConfig.setMaximumInterestRateChange(originalConfig.getMaximumInterestRateChange());
			newConfig.setAllowGracePeriodExtension(originalConfig.getAllowGracePeriodExtension());
			newConfig.setMaximumGracePeriodExtensionDays(originalConfig.getMaximumGracePeriodExtensionDays());

			// Copy approval configuration
			newConfig.setRequiresApproval(originalConfig.getRequiresApproval());
			newConfig.setRequiredApprovalSteps(originalConfig.getRequiredApprovalSteps());
			newConfig.setAllowAutoApproval(originalConfig.getAllowAutoApproval());
			newConfig.setAutoApprovalMaximumAmount(originalConfig.getAutoApprovalMaximumAmount());
			newConfig.setAutoApprovalMaximumTermChange(originalConfig.getAutoApprovalMaximumTermChange());
			newConfig.setAutoApprovalMaximumInterestChange(originalConfig.getAutoApprovalMaximumInterestChange());
			newConfig.setAllowQuickApproval(originalConfig.getAllowQuickApproval());
			newConfig.setQuickApprovalMaximumAmount(originalConfig.getQuickApprovalMaximumAmount());
			newConfig.setQuickApprovalSteps(originalConfig.getQuickApprovalSteps());

			// Copy fees & charges
			newConfig.setChargeAmendmentFee(originalConfig.getChargeAmendmentFee());
			newConfig.setAmendmentFeeAmount(originalConfig.getAmendmentFeeAmount());
			newConfig.setAmendmentFeePercentage(originalConfig.getAmendmentFeePercentage());
			newConfig.setAmendmentFeeType(originalConfig.getAmendmentFeeType());
			newConfig.setChargeProcessingFee(originalConfig.getChargeProcessingFee());
			newConfig.setProcessingFeeAmount(originalConfig.getProcessingFeeAmount());
			newConfig.setChargeLegalFee(originalConfig.getChargeLegalFee());
			newConfig.setLegalFeeAmount(originalConfig.getLegalFeeAmount());
			newConfig.setAllowFeeWaiver(originalConfig.getAllowFeeWaiver());
			newConfig.setFeeWaiverApprovalSteps(originalConfig.getFeeWaiverApprovalSteps());

			// Copy documentation requirements
			newConfig.setRequireNewApplicationForm(originalConfig.getRequireNewApplicationForm());
			newConfig.setRequireUpdatedContract(originalConfig.getRequireUpdatedContract());
			newConfig.setRequireBorrowerConsent(originalConfig.getRequireBorrowerConsent());
			newConfig.setRequireGuarantorConsent(originalConfig.getRequireGuarantorConsent());
			newConfig.setRequireCollateralRevaluation(originalConfig.getRequireCollateralRevaluation());
			newConfig.setRequireCreditReassessment(originalConfig.getRequireCreditReassessment());
			newConfig.setRequireLegalReview(originalConfig.getRequireLegalReview());
			newConfig.setRequireBoardApproval(originalConfig.getRequireBoardApproval());
			newConfig.setBoardApprovalThresholdAmount(originalConfig.getBoardApprovalThresholdAmount());

			// Copy notification & communication
			newConfig.setNotifyBorrower(originalConfig.getNotifyBorrower());
			newConfig.setNotifyGuarantors(originalConfig.getNotifyGuarantors());
			newConfig.setRequireBorrowerAcknowledgement(originalConfig.getRequireBorrowerAcknowledgement());
			newConfig.setSendFormalLetter(originalConfig.getSendFormalLetter());
			newConfig.setUpdateCreditBureau(originalConfig.getUpdateCreditBureau());
			newConfig.setRequireRegulatoryNotification(originalConfig.getRequireRegulatoryNotification());

			// Copy risk controls
			newConfig.setTriggerRiskReview(originalConfig.getTriggerRiskReview());
			newConfig.setRiskReviewThresholdAmount(originalConfig.getRiskReviewThresholdAmount());
			newConfig.setRequireCommitteeApproval(originalConfig.getRequireCommitteeApproval());
			newConfig.setCommitteeApprovalThresholdAmount(originalConfig.getCommitteeApprovalThresholdAmount());
			newConfig.setRequireCEOApproval(originalConfig.getRequireCEOApproval());
			newConfig.setCeoApprovalThresholdAmount(originalConfig.getCeoApprovalThresholdAmount());
			newConfig.setAllowMultipleAmendments(originalConfig.getAllowMultipleAmendments());
			newConfig.setMaximumAmendmentsPerLoan(originalConfig.getMaximumAmendmentsPerLoan());
			newConfig.setCoolingPeriodDays(originalConfig.getCoolingPeriodDays());

			// Copy system & integration
			newConfig.setAutoUpdateLoanSystem(originalConfig.getAutoUpdateLoanSystem());
			newConfig.setGenerateAmendmentNumber(originalConfig.getGenerateAmendmentNumber());
			newConfig.setAmendmentNumberPrefix(originalConfig.getAmendmentNumberPrefix());
			newConfig.setCreateAuditTrail(originalConfig.getCreateAuditTrail());
			newConfig.setUpdateCollateralRegistry(originalConfig.getUpdateCollateralRegistry());
			newConfig.setSyncWithCoreBanking(originalConfig.getSyncWithCoreBanking());

			// Copy validation rules
			newConfig.setValidateCreditLimit(originalConfig.getValidateCreditLimit());
			newConfig.setValidateDebtServiceRatio(originalConfig.getValidateDebtServiceRatio());
			newConfig.setMaximumDebtServiceRatio(originalConfig.getMaximumDebtServiceRatio());
			newConfig.setValidateTotalExposure(originalConfig.getValidateTotalExposure());
			newConfig.setMaximumTotalExposure(originalConfig.getMaximumTotalExposure());

			// Set audit fields
			newConfig.setAdOrgID(utils.getAD_Org_ID());
			newConfig.setAD_Amendment_Configuration_UU(UUID.randomUUID().toString());
			newConfig.setCreatedBy(utils.getLoggedInUser().getUserId());
			newConfig.setCreated(new Date());

			// Save new configuration
			newConfig = amendmentConfigRepository.save(newConfig);

			// Clone approval workflow
			Set<MAmendmentApprovalSteps> originalSteps = originalConfig.getApprovalWorkflow();

			if (originalSteps != null && !originalSteps.isEmpty()) {
				for (MAmendmentApprovalSteps originalStep : originalSteps) {
					MAmendmentApprovalSteps newStep = new MAmendmentApprovalSteps();

					// Copy step properties
					newStep.setStepNumber(originalStep.getStepNumber());

					newStep.setRequiredRole(originalStep.getRequiredRole());
					newStep.setNextRole(originalStep.getNextRole());

					newStep.setApprovalStage(originalStep.getApprovalStage());
					newStep.setApprovalStage(originalStep.getApprovalStage());

					newStep.setRequireDocumentReview(originalStep.getRequireDocumentReview());
					newStep.setTrigureStatus(originalStep.getTrigureStatus());
					newStep.setRequireDigitalSignature(originalStep.getRequireDigitalSignature());
					newStep.setCreatedBy(utils.getLoggedInUser().getUserId());
					newStep.setCreated(new Date());

					amendmentApprovalStepsRepository.save(newStep);
				}
			}

			return new ResponseEntity<>("Configuration cloned successfully", 200, newConfig);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error cloning configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to clone configuration: " + e.getMessage());
		}
	}

	/**
	 * Get eligible loan products for configuration
	 */
	public ResponseEntity<List<MLoanProductConfiguration>> getEligibleLoanProducts(Long configId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			List<MLoanProductConfiguration> allProducts = loanProductConfigRepository
					.findByIsActiveAndAdOrgIDOrderByNameAsc(true, utils.getAD_Org_ID());

			// Filter out products already in the configuration
			Set<MLoanProductConfiguration> configuredProducts = config.getApplicableLoanProducts();
			List<MLoanProductConfiguration> eligibleProducts = allProducts.stream()
					.filter(product -> !configuredProducts.contains(product)).collect(Collectors.toList());

			return new ResponseEntity<>("Eligible loan products retrieved successfully", 200, eligibleProducts);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting eligible loan products: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to retrieve eligible loan products: " + e.getMessage());
		}
	}

	/**
	 * Add loan product to configuration
	 */
	@Transactional
	public ResponseEntity<Void> addLoanProductToConfig(Long configId, Long productId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			MLoanProductConfiguration product = loanProductConfigRepository.findById(productId)
					.orElseThrow(() -> new SetUpExceptions("Loan product not found with id: " + productId));

			if (config.getApplicableLoanProducts().contains(product)) {
				return new ResponseEntity<>("Loan product already added to configuration", 200, null);
			}

			config.getApplicableLoanProducts().add(product);
			config.setUpdatedBy(utils.getLoggedInUser().getUserId());
			config.setUpdated(new Date());
			amendmentConfigRepository.save(config);

			return new ResponseEntity<>("Loan product added to configuration successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error adding loan product to configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to add loan product to configuration: " + e.getMessage());
		}
	}

	/**
	 * Remove loan product from configuration
	 */
	@Transactional
	public ResponseEntity<Void> removeLoanProductFromConfig(Long configId, Long productId) {
		try {
			MAmendmentConfiguration config = amendmentConfigRepository.findById(configId)
					.orElseThrow(() -> new SetUpExceptions("Configuration not found with id: " + configId));

			MLoanProductConfiguration product = loanProductConfigRepository.findById(productId)
					.orElseThrow(() -> new SetUpExceptions("Loan product not found with id: " + productId));

			if (!config.getApplicableLoanProducts().contains(product)) {
				return new ResponseEntity<>("Loan product not found in configuration", 200, null);
			}

			config.getApplicableLoanProducts().remove(product);
			config.setUpdatedBy(utils.getLoggedInUser().getUserId());
			config.setUpdated(new Date());
			amendmentConfigRepository.save(config);

			return new ResponseEntity<>("Loan product removed from configuration successfully", 200, null);
		} catch (SetUpExceptions e) {
			throw e;
		} catch (Exception e) {
			log.error("Error removing loan product from configuration: {}", e.getMessage(), e);
			throw new SetUpExceptions("Failed to remove loan product from configuration: " + e.getMessage());
		}
	}

	/**
	 * Check if amendment type is configured for loan product
	 */
	public ResponseEntity<Boolean> isAmendmentTypeConfigured(AmendmentType amendmentType, Long loanProductId) {
		try {
			List<MAmendmentConfiguration> configs = amendmentConfigRepository
					.findByAmendmentTypeAndApplicableLoanProducts_LoanProductConfigId(amendmentType, loanProductId,
							utils.getAD_Org_ID());

			boolean isConfigured = configs.stream()
					.anyMatch(config -> config.isActive() && config.getApplicableLoanProducts().stream()
							.anyMatch(product -> product.getLoanProductConfigId().equals(loanProductId)));

			return new ResponseEntity<>("Check completed", 200, isConfigured);
		} catch (Exception e) {
			log.error("Error checking if amendment type is configured: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error checking configuration: " + e.getMessage(), 500, false);
		}
	}

	/**
	 * Get statistics for amendment configurations
	 */
	public ResponseEntity<Map<String, Object>> getConfigurationStatistics() {
		try {
			Map<String, Object> stats = new HashMap<>();
			Long adOrgID = utils.getAD_Org_ID();

			// Total active configurations
			long totalConfigs = amendmentConfigRepository.countByIsActiveAndAdOrgID(true, adOrgID);
			stats.put("totalConfigurations", totalConfigs);

			// Configurations by amendment type
			Map<AmendmentType, Long> configsByType = new HashMap<>();
			for (AmendmentType type : AmendmentType.values()) {
				long count = amendmentConfigRepository.countByAmendmentTypeAndIsActiveAndAdOrgID(type, true, adOrgID);
				configsByType.put(type, count);
			}
			stats.put("configurationsByType", configsByType);

			// Default configurations count
			long defaultConfigs = amendmentConfigRepository.countByIsActiveAndIsDefaultConfigurationAndAdOrgID(true,
					true, adOrgID);
			stats.put("defaultConfigurations", defaultConfigs);

			// Configurations with approval workflow
			long withApproval = amendmentConfigRepository.countByRequiresApprovalAndIsActiveAndAdOrgID(true, true,
					adOrgID);
			stats.put("configurationsWithApproval", withApproval);

			// Configurations without approval workflow
			long withoutApproval = amendmentConfigRepository.countByRequiresApprovalAndIsActiveAndAdOrgID(false, true,
					adOrgID);
			stats.put("configurationsWithoutApproval", withoutApproval);

			return new ResponseEntity<>("Statistics retrieved successfully", 200, stats);
		} catch (Exception e) {
			log.error("Error getting configuration statistics: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error retrieving statistics: " + e.getMessage(), 500, null);
		}
	}
}