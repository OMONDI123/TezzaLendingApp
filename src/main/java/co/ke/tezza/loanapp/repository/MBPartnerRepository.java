package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MBPartner;

@Repository
public interface MBPartnerRepository extends JpaRepository<MBPartner, Long> {

}
