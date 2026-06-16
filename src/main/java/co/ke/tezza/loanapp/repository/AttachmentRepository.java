package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MAttachment;
import co.ke.tezza.loanapp.enums.AttachmentType;

@Repository
public interface AttachmentRepository extends JpaRepository<MAttachment, Long> {
	Page<MAttachment> findByIsActiveAndAdOrgID(boolean isActive,long adOrgId,Pageable pageable);
	List<MAttachment> findByIsActiveAndAdOrgIDAndAttachmentType(boolean isActive,long adOrgId,AttachmentType attachmentType);

}
