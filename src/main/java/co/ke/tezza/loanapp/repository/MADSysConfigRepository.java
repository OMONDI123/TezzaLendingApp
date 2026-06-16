package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;

@Repository
public interface MADSysConfigRepository extends JpaRepository<MADSysConfig, Long> {

	MADSysConfig findTopByIsActiveAndAdOrgIDAndSettingCategory(boolean isActive, long aD_Org_ID,
			SettingCategoriesEnum settingCategory);

	List<MADSysConfig> findByIsActiveAndAdOrgID(boolean b, long aD_Org_ID);
	Page<MADSysConfig> findByIsActiveAndAdOrgIDOrderByIdDesc(boolean b, long aD_Org_ID,Pageable pageable);

}
