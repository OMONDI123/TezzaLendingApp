package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.enums.PaymentType;

@Repository
public interface PaymentMethodRepository extends JpaRepository<MPaymentMethod, Long>{
	Page<MPaymentMethod> findByIsActiveAndAdOrgID(boolean active, long adOrgId,Pageable pageable);
	MPaymentMethod findTop1ByIsActiveAndAdOrgIDAndName(boolean active,long adOrgId,String name);
	MPaymentMethod findTop1ByIsActiveAndAdOrgIDAndPaymentType(boolean active,long adOrgId,PaymentType type);
	List<MPaymentMethod> findByIsActiveAndAdOrgID(boolean isActive,long orgId);
	

}
