package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import co.ke.tezza.loanapp.entity.MPaymentMethod;

@Repository
public interface PaymentApprovalConfigurationRepository extends JpaRepository<MPaymentApprovalConfiguration, Long>{
	MPaymentApprovalConfiguration findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(boolean active,long adOrgId,MPaymentMethod paymentMethod);
	Page<MPaymentApprovalConfiguration> findByIsActiveAndAdOrgIDOrderByCreatedDesc(boolean active,long orgId,Pageable pageable);
	Page<MPaymentApprovalConfiguration> findByIsActiveAndAdOrgIDAndPaymentMethod_NameOrderByCreatedDesc(boolean active,long orgId,String name,Pageable pageable);

}
