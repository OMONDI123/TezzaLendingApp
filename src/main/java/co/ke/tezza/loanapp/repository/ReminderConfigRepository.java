package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MRemindersConfiguration;
import co.ke.tezza.loanapp.entity.MSmsSetup;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;

@Repository
public interface ReminderConfigRepository extends JpaRepository<MRemindersConfiguration, Long>{

	List<MRemindersConfiguration> findByIsActiveAndAdOrgIDOrderByReminderIdDesc(boolean b, long ad_Org_ID);
	MRemindersConfiguration findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
            long adOrgID, boolean isActive, SmsTypeEnum smsType);
	MRemindersConfiguration findTop1ByIsActiveAndAdOrgIDAndSmsMessageTemplate(boolean b, long ad_Org_ID, MSmsSetup temp);
	

}
