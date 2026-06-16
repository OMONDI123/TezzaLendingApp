package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.enums.PaymentGateway;

@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<MPaymentGatewayConfig, Long>{
	
	List<MPaymentGatewayConfig> findByIsActiveAndAdOrgIDOrderByCreatedDesc(boolean isActive,long orgId);
	MPaymentGatewayConfig findTop1ByIsActiveAndAdOrgIDOrderByCreatedDesc(boolean isActive,long orgId);
	MPaymentGatewayConfig findTop1ByIsActiveAndAdOrgIDAndPaymentGatwayOrderByCreatedDesc(boolean b, long orgId,
			PaymentGateway mpesa);

}
