package co.ke.tezza.loanapp.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.ApprovalStepsModel;
import co.ke.tezza.loanapp.model.PaymentAppprovalConfigModel;
import co.ke.tezza.loanapp.repository.ApprovalStepsRepository;
import co.ke.tezza.loanapp.repository.PaymentApprovalConfigurationRepository;
import co.ke.tezza.loanapp.repository.PaymentMethodRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class PaymentApprovalService {

	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private Utils utils;
	@Autowired
	private PaymentApprovalConfigurationRepository paymentApprovalConfigurationRepository;

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	@Autowired
	private ApprovalStepsRepository approvalStepsRepository;

	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private UserRepository userRepository;
	
	

	public ResponseEntity<MPaymentApprovalConfiguration> createUpdatePaymentApprovalConfig(
			PaymentAppprovalConfigModel request) {
		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
				.findById(request.getPaymentApprovalConfigId()).orElse(null);
		if (config == null) {
			config = new MPaymentApprovalConfiguration();
		}
		MPaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId()).orElse(null);
		if (paymentMethod == null) {
			throw new SetUpExceptions(
					"You have not selected Payment method. Please make sure you select a valid payment method.");
		}
		MPaymentApprovalConfiguration existingConfig = paymentApprovalConfigurationRepository
				.findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(true, utils.getAD_Org_ID(), paymentMethod);

		if ((existingConfig != null && request.getPaymentApprovalConfigId() == 0)
				|| (existingConfig != null && request.getPaymentApprovalConfigId() > 0
						&& request.getPaymentApprovalConfigId() != existingConfig.getPaymentApprovalConfigId())) {

			throw new SetUpExceptions("Approval Configuration for this Payment method already exists.");
		}

		config.setPaymentMethod(paymentMethod);
		config.setRequiredAprrovalSteps(request.getRequiredAprrovalSteps());
		Set<MApprovalSteps> currentSteps = config.getApprovalLevels();
		
		if (!config.getApprovalLevels().isEmpty()) {
			for (MApprovalSteps step : config.getApprovalLevels()) {
				if (!step.getResponsiblePersons().isEmpty()) {
					step.getResponsiblePersons().clear();
				}
			}
			approvalStepsRepository.deleteAll(currentSteps);
			approvalStepsRepository.flush();
		}
		if (!request.getApprovalLevels().isEmpty()) {
			Set<MApprovalSteps> steps = new HashSet<>();
			for (ApprovalStepsModel m : request.getApprovalLevels()) {
				MApprovalSteps st = new MApprovalSteps();
				st.setStep(m.getSteps());
				st.setApprovalStage(m.getApprovalStage());
				st.setTrigureStatus(m.getTrigureStatus());
				MRoles currentRole = roleRepository.findById(m.getApprovalRoleInvolvedId()).orElse(null);
				if (currentRole == null) {
					throw new SetUpExceptions("Current Approval Role is not provided for step " + m.getSteps());
				}
				Set<MUser> users = new HashSet<>();

				for (Long id : m.getResponsiblePersonIds()) {
					MUser user = userRepository.findById(id).orElse(null);
					if (user != null) {
						users.add(user);
					}
				}
				st.setResponsiblePersons(users);
				st.setRoleInvolved(currentRole);
				st.setNextRoleinvolved(roleRepository.findById(m.getNextApprovalRoleId()).orElse(null));
				st.setRejectiontrigeredStatus(m.getRejectiontrigeredStatus());
				steps.add(st);

			}
			config.setApprovalLevels(steps);
			paymentApprovalConfigurationRepository.save(config);
		}
		return new ResponseEntity<MPaymentApprovalConfiguration>("Payment Approval Configuration Created Successfully.",
				200, config);

	}

	public Page<MPaymentApprovalConfiguration> getPaymentApprovalConfiguration(int page, int size, String searchTerm) {
		if (searchTerm != null && !searchTerm.isEmpty()) {
			return paymentApprovalConfigurationRepository
					.findByIsActiveAndAdOrgIDAndPaymentMethod_NameOrderByCreatedDesc(true, utils.getAD_Org_ID(),
							searchTerm, PageRequest.of(page, size));
		} else {
			return paymentApprovalConfigurationRepository.findByIsActiveAndAdOrgIDOrderByCreatedDesc(true,
					utils.getAD_Org_ID(), PageRequest.of(page, size));
		}
	}

	public ResponseEntity<MPaymentApprovalConfiguration> deletePaymentApprovalConfig(Long id) {
		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository.findById(id).orElse(null);
		if (config == null) {
			throw new SetUpExceptions("Payment Approval Setup Not Found");
		}
		config.setActive(false);
		paymentApprovalConfigurationRepository.save(config);
		return new ResponseEntity<MPaymentApprovalConfiguration>("Payment Approval Configuration deleted successfully.",
				200, config);
	}

	public MUser getPaymentRequestedBy(MPayments payment) {
		MUser requester = userRepository.findById(payment.getCreatedBy()).orElse(null);
		return requester;
	}

}
