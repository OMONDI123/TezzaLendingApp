package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.ke.tezza.loanapp.entity.ReportTemplates;



public interface ReportTemplatesRepository  extends JpaRepository<ReportTemplates, Long>{
	ReportTemplates findTop1ByIsActiveAndReportName(boolean active, String reportName);

}
