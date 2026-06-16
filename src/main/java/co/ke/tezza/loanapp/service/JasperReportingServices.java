package co.ke.tezza.loanapp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.FileOutputUrl;
import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.ReportTemplates;
import co.ke.tezza.loanapp.enums.ReportType;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.repository.FileOutputUrlRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.ReportTemplatesRepository;
import co.ke.tezza.loanapp.response.FileOutPut;
import co.ke.tezza.loanapp.response.FileOutPutResponse;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

@Service
public class JasperReportingServices {

	@Autowired
	private DataSource dataSource;
	@Autowired
	private FileOutputUrlRepository fileOutputUrlRepository;
	@Autowired
	private ReportTemplatesRepository reportTemplatesRepository;
	@Autowired
	private Utils utils;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	public ResponseEntity<FileOutPutResponse> generateTopOverdueDebtsReport(ReportParams params, boolean byDays) {
		FileOutPut fileOutPut = null;
		String reportName = "Dashboard Reports";
		String jasperReportName = "TopOverdueDebtsByDays.jasper";
		String message = "Top Overdue Debts By Days Generated Successfully.";
		String fileName = "top_overdue_debts_by_days";

		if (!byDays) {
			jasperReportName = "TopOverdueDebtsByAmount.jasper";
			message = "Top Overdue Debts By Amount Generated Successfully.";
			fileName = "top_overdue_debts_by_amount";

		}

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		// If dateFrom is null → set to Jan 1st of current year
		if (params.getDateFrom() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			params.setDateFrom(cal.getTime());
		}

		// If dateTo is null → set to Dec 31st of current year
		if (params.getDateTo() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.DECEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			params.setDateTo(cal.getTime());
		}
		Date dateFrom = utils.getStartOfDay(params.getDateFrom());
		Date dateTo = utils.getEndOfDay(params.getDateTo());

		long orgId = 0;
		if (params.getAd_Org_ID() == 0) {
			orgId = utils.getAD_Org_ID();
		}
		if (params.getReportType().equals(ReportType.CSV)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("csv", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generateCsvDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		} else if (params.getReportType().equals(ReportType.EXCEL)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("excel", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generateExcelDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		} else {
			fileOutPut = returnOutPutStreamDynamicFileNames("pdf", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generatePDFDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		}

		FileOutPutResponse response = new FileOutPutResponse();
//		String encrypted=null;
//		try {
//			encrypted = utils.encryptPdfWithPassword(fileOutPut.getFilePath(), "11111");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());

		return new ResponseEntity<FileOutPutResponse>(message, 200, response);

	}

	public ResponseEntity<FileOutPutResponse> generateBestPerformingDebtsReport(ReportParams params,
			boolean netPayments) {
		FileOutPut fileOutPut = null;
		String reportName = "Dashboard Reports";
		String jasperReportName = "BestPerformingDebtsAllPayments.jasper";
		String fileName = "best_performing_debts_all_payments";
		if (netPayments) {
			jasperReportName = "BestPerformingDebtsNetPayments.jasper";
			fileName = "best_performing_debts_net_payments";
		}

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		// If dateFrom is null → set to Jan 1st of current year
		if (params.getDateFrom() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			params.setDateFrom(cal.getTime());
		}

		// If dateTo is null → set to Dec 31st of current year
		if (params.getDateTo() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.DECEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			params.setDateTo(cal.getTime());
		}
		Date dateFrom = utils.getStartOfDay(params.getDateFrom());
		Date dateTo = utils.getEndOfDay(params.getDateTo());

		long orgId = 0;
		if (params.getAd_Org_ID() == 0) {
			orgId = utils.getAD_Org_ID();
		}
		if (params.getReportType().equals(ReportType.CSV)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("csv", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generateCsvDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		} else if (params.getReportType().equals(ReportType.EXCEL)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("excel", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generateExcelDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		} else {
			fileOutPut = returnOutPutStreamDynamicFileNames("pdf", fileName,orgId);
			FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();
			generatePDFDateRangeReportsRaw(dateFrom, dateTo, fileOutputStream, reportName, jasperReportName, orgId);
		}

		FileOutPutResponse response = new FileOutPutResponse();
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());
		String message = "Best Performing Debts By All Payments Generated Successfully.";
		if (netPayments) {
			message = "Best Performing Debts By Net Payments Generated Successfully.";
		}

		return new ResponseEntity<FileOutPutResponse>(message, 200, response);

	}

	public ResponseEntity<FileOutPutResponse> generateCustomerInvoice(long billId, long proformaInvoiceId, long adOrgId,
			String fileName) {
		FileOutPut fileOutPut = returnOutPutStreamDynamicFileNames("pdf", fileName,adOrgId);
		String reportName = "Invoice and Proforma Invoice";
		String jasperReportName = null;
		if (billId > 0) {
			jasperReportName = "Invoice.jasper";
			generatePDFByGenericIdRaw(fileOutPut.getFileOutputStream(), reportName, jasperReportName, adOrgId, billId);
		}
		if (proformaInvoiceId > 0) {
			jasperReportName = "ProformaInvoice.jasper";
			generatePDFByGenericIdRaw(fileOutPut.getFileOutputStream(), reportName, jasperReportName, adOrgId,
					proformaInvoiceId);

		}

		FileOutPutResponse response = new FileOutPutResponse();
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());

		return new ResponseEntity<FileOutPutResponse>("Invoice  Generated Successfully", 200, response);

	}

	public ResponseEntity<FileOutPutResponse> printLoanStatement(ReportParams params) {
		FileOutPut fileOutPut = null;
		String reportName = "Loan Statement Report";
		String jasperReportName = "SummaryLoanStatement.jasper";

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		// If dateFrom is null → set to Jan 1st of current year
		if (params.getDateFrom() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			params.setDateFrom(cal.getTime());
		}

		// If dateTo is null → set to Dec 31st of current year
		if (params.getDateTo() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.DECEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			params.setDateTo(cal.getTime());
		}
		Date dateFrom = utils.getStartOfDay(params.getDateFrom());
		Date dateTo = utils.getEndOfDay(params.getDateTo());

		long orgId = 0;
		long loanId = params.getLoanId();
		long individualBorrowerId = params.getIndividualBorrowerId();
		long groupBorrowerId = params.getGroupBorrowerId();
		long institutionBorrowerId = params.getInstitutionBorrowerId();
		long membershipAccountId = params.getMembershipAccountId();

		if (params.getLoanId() > 0) {
			MLoanApplication app = loanApplicationRepository.findById(params.getLoanId()).orElse(null);
			if (app != null) {
				individualBorrowerId = app.getIndividualBorrower() != null
						? app.getIndividualBorrower().getIndividualBorrowerId()
						: 0;
				groupBorrowerId = app.getGroupBorrower() != null ? app.getGroupBorrower().getGroupBorrowerId() : 0;
				institutionBorrowerId = app.getInstitutionBorrower() != null
						? app.getInstitutionBorrower().getInstitutionBorrowerId()
						: 0;
			}
			jasperReportName = "LoanStatementReportByLoanId.jasper";
		} else if (params.getIndividualBorrowerId() > 0 || params.getGroupBorrowerId() > 0
				|| params.getInstitutionBorrowerId() > 0) {
			jasperReportName = "LoanStatementReportByBorrowerId.jasper";
		} else {
			jasperReportName = "SummaryLoanStatement.jasper";
		}

		if (params.getAd_Org_ID() > 0) {
			orgId = params.getAd_Org_ID();
		} else {
			orgId = utils.getAD_Org_ID();
		}
		System.out.println("Date From: " + dateFrom + ", Date  To: " + dateTo + ", AD_ORG_ID: " + orgId
				+ ", INDIVIDUAL_DEBTOR_ID: " + individualBorrowerId + ", GROUP_DEBTOR_ID: " + groupBorrowerId
				+ ", INSTITUTION_DEBTOR_ID: " + institutionBorrowerId + ", LOAN_ID:" + loanId);

		if (params.getReportType() == null) {
			params.setReportType(ReportType.PDF);
		}

		if (params.getReportType().equals(ReportType.CSV)) {
			fileOutPut = returnOutPutStream("csv",orgId);
		} else if (params.getReportType().equals(ReportType.EXCEL)) {
			fileOutPut = returnOutPutStream("excel",orgId);
		} else {
			fileOutPut = returnOutPutStream("pdf",orgId);
		}

		FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();

		// Use the new method that accepts all parameters
		switch (params.getReportType()) {
		case CSV:
			generateCSVLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		case EXCEL:
			generateExcelLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		default:
			generatePDFLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
		}

		FileOutPutResponse response = new FileOutPutResponse();
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());

		return new ResponseEntity<FileOutPutResponse>("Loan Statement Report Generated Successfully", 200, response);
	}

	public FileOutPutResponse generateLoanStatement(ReportParams params) {
		FileOutPut fileOutPut = null;
		String reportName = "Loan Statement Report";
		String jasperReportName = "SummaryLoanStatement.jasper";

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		// If dateFrom is null → set to Jan 1st of current year
		if (params.getDateFrom() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			params.setDateFrom(cal.getTime());
		}

		// If dateTo is null → set to Dec 31st of current year
		if (params.getDateTo() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.DECEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			params.setDateTo(cal.getTime());
		}
		Date dateFrom = utils.getStartOfDay(params.getDateFrom());
		Date dateTo = utils.getEndOfDay(params.getDateTo());

		long orgId = 0;
		long loanId = params.getLoanId();
		long individualBorrowerId = params.getIndividualBorrowerId();
		long groupBorrowerId = params.getGroupBorrowerId();
		long institutionBorrowerId = params.getInstitutionBorrowerId();
		long membershipAccountId = params.getMembershipAccountId();

		if (params.getLoanId() > 0) {
			MLoanApplication app = loanApplicationRepository.findById(params.getLoanId()).orElse(null);
			if (app != null) {
				individualBorrowerId = app.getIndividualBorrower() != null
						? app.getIndividualBorrower().getIndividualBorrowerId()
						: 0;
				groupBorrowerId = app.getGroupBorrower() != null ? app.getGroupBorrower().getGroupBorrowerId() : 0;
				institutionBorrowerId = app.getInstitutionBorrower() != null
						? app.getInstitutionBorrower().getInstitutionBorrowerId()
						: 0;
			}
			jasperReportName = "LoanStatementReportByLoanId.jasper";
		} else if (params.getIndividualBorrowerId() > 0 || params.getGroupBorrowerId() > 0
				|| params.getInstitutionBorrowerId() > 0) {
			jasperReportName = "LoanStatementReportByBorrowerId.jasper";
		} else {
			jasperReportName = "SummaryLoanStatement.jasper";
		}

		if (params.getAd_Org_ID() > 0) {
			orgId = params.getAd_Org_ID();
		} else {
			orgId = utils.getAD_Org_ID();
		}
		System.out.println("Date From: " + dateFrom + ", Date  To: " + dateTo + ", AD_ORG_ID: " + orgId
				+ ", INDIVIDUAL_DEBTOR_ID: " + individualBorrowerId + ", GROUP_DEBTOR_ID: " + groupBorrowerId
				+ ", INSTITUTION_DEBTOR_ID: " + institutionBorrowerId + ", LOAN_ID:" + loanId);

		if (params.getReportType() == null) {
			params.setReportType(ReportType.PDF);
		}

		if (params.getReportType().equals(ReportType.CSV)) {
			fileOutPut = returnOutPutStream("csv",orgId);
		} else if (params.getReportType().equals(ReportType.EXCEL)) {
			fileOutPut = returnOutPutStream("excel",orgId);
		} else {
			fileOutPut = returnOutPutStream("pdf",orgId);
		}

		FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();

		// Use the new method that accepts all parameters
		switch (params.getReportType()) {
		case CSV:
			generateCSVLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		case EXCEL:
			generateExcelLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		default:
			generatePDFLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
		}

		FileOutPutResponse response = new FileOutPutResponse();
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());

		return response;
	}

	public ResponseEntity<FileOutPutResponse> printCardex(ReportParams params) {
		FileOutPut fileOutPut = null;
		String reportName = "Cardex Report";
		String jasperReportName = "CardexReport.jasper";

		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);

		// If dateFrom is null → set to Jan 1st of current year
		if (params.getDateFrom() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			params.setDateFrom(cal.getTime());
		}

		// If dateTo is null → set to Dec 31st of current year
		if (params.getDateTo() == null) {
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, Calendar.DECEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			params.setDateTo(cal.getTime());
		}
		Date dateFrom = utils.getStartOfDay(params.getDateFrom());
		Date dateTo = utils.getEndOfDay(params.getDateTo());

		long orgId = 0;
		long loanId = params.getLoanId();
		long individualBorrowerId = params.getIndividualBorrowerId();
		long groupBorrowerId = params.getGroupBorrowerId();
		long institutionBorrowerId = params.getInstitutionBorrowerId();
		long membershipAccountId = params.getMembershipAccountId();

		if (params.getLoanId() > 0) {
			MLoanApplication app = loanApplicationRepository.findById(params.getLoanId()).orElse(null);
			if (app != null) {
				individualBorrowerId = app.getIndividualBorrower() != null
						? app.getIndividualBorrower().getIndividualBorrowerId()
						: 0;
				groupBorrowerId = app.getGroupBorrower() != null ? app.getGroupBorrower().getGroupBorrowerId() : 0;
				institutionBorrowerId = app.getInstitutionBorrower() != null
						? app.getInstitutionBorrower().getInstitutionBorrowerId()
						: 0;
			}
			jasperReportName = "CardexReport.jasper";
		} else if (params.getIndividualBorrowerId() > 0 || params.getGroupBorrowerId() > 0
				|| params.getInstitutionBorrowerId() > 0) {
			jasperReportName = "CardexReport.jasper";
		} else {
			jasperReportName = "MembershipAccountCardexReport.jasper";
		}

		if (params.getAd_Org_ID() > 0) {
			orgId = params.getAd_Org_ID();
		} else {
			orgId = utils.getAD_Org_ID();
		}
		System.out.println("Date From: " + dateFrom + ", Date  To: " + dateTo + ", AD_ORG_ID: " + orgId
				+ ", INDIVIDUAL_DEBTOR_ID: " + individualBorrowerId + ", GROUP_DEBTOR_ID: " + groupBorrowerId
				+ ", INSTITUTION_DEBTOR_ID: " + institutionBorrowerId + ", LOAN_ID:" + loanId
				+ ", MEMBERSHIP_ACCOUNT_ID: " + membershipAccountId);

		if (params.getReportType() == null) {
			params.setReportType(ReportType.PDF);
		}

		if (params.getReportType().equals(ReportType.CSV)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("csv", "cardex",orgId);
		} else if (params.getReportType().equals(ReportType.EXCEL)) {
			fileOutPut = returnOutPutStreamDynamicFileNames("excel", "cardex",orgId);
		} else {
			fileOutPut = returnOutPutStreamDynamicFileNames("pdf", "cardex",orgId);
		}

		FileOutputStream fileOutputStream = fileOutPut.getFileOutputStream();

		// Use the new method that accepts all parameters
		switch (params.getReportType()) {
		case CSV:
			generateCSVLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		case EXCEL:
			generateExcelLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
			break;
		default:
			generatePDFLoanStatementReport(fileOutputStream, reportName, jasperReportName, dateFrom, dateTo, orgId,
					loanId, individualBorrowerId, groupBorrowerId, institutionBorrowerId, membershipAccountId);
		}

		FileOutPutResponse response = new FileOutPutResponse();
		response.setFilePath(fileOutPut.getFilePath());
		response.setFileName(fileOutPut.getFileName());
		response.setFileOutputUrl(fileOutPut.getFileOutputUrl());

		return new ResponseEntity<FileOutPutResponse>("Debtor Feedback Report (Cardex) Generated Successfully", 200,
				response);
	}

	public FileOutPut returnOutPutStream(String reportType,long orgId) {
		FileOutPut fileOutPut = new FileOutPut();
		String extension;

		switch (reportType.toLowerCase()) {
		case "excel":
		case "xlsx":
		case "xls":
			extension = ".xlsx";
			break;
		case "pdf":
			extension = ".pdf";
			break;
		case "png":
			extension = ".png";
			break;
		case "csv":
			extension = ".csv";
			break;
		default:
			extension = ".txt"; // fallback
			break;
		}

		boolean isRender = System.getenv("RENDER") != null || System.getenv("RENDER_EXTERNAL_URL") != null;
		String baseDir = null;
		if (isRender) {
			baseDir = "/tmp";
		} else {
			MADSysConfig config = utils
					.getOrganizationSystemConfiguratinsByDynamicOrganisation(SettingCategoriesEnum.GENERAL_SETTINGS, orgId);
			if (config != null) {
				baseDir = config.getDownloadPath();
			}

		}
		if (baseDir == null || baseDir.isEmpty()) {
			baseDir = System.getProperty("user.home") + "/invoices";
		}
		String filename = "loan_statement_report_" + System.currentTimeMillis() + extension;
		String filePath = baseDir + File.separator + filename;
		FileOutputUrl outputUrl = fileOutputUrlRepository.findTop1ByIsActive(true);

		try {
			File file = new File(filePath);
			file.getParentFile().mkdirs(); // Ensure directory exists
			FileOutputStream fos = new FileOutputStream(file);
			String fileUrl = null;
			if (outputUrl != null) {
				fileUrl = isRender ? System.getenv("RENDER_EXTERNAL_URL") + "/file/" + filename
						: outputUrl.getFileUrl() + filename;
			} else {
				fileUrl = isRender ? System.getenv("RENDER_EXTERNAL_URL") + "/file/" + filename : baseDir + filename;
			}

			fileOutPut.setFileOutputStream(fos);
			fileOutPut.setFilePath(filePath);
			fileOutPut.setFileName(filename);
			fileOutPut.setFileOutputUrl(fileUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileOutPut;
	}

	public FileOutPut returnOutPutStreamDynamicFileNames(String reportType, String fileName,long orgId) {
		FileOutPut fileOutPut = new FileOutPut();
		String extension;

		switch (reportType.toLowerCase()) {
		case "excel":
		case "xlsx":
		case "xls":
			extension = ".xlsx";
			break;
		case "pdf":
			extension = ".pdf";
			break;
		case "png":
			extension = ".png";
			break;
		case "csv":
			extension = ".csv";
			break;
		default:
			extension = ".txt"; // fallback
			break;
		}

		boolean isRender = System.getenv("RENDER") != null || System.getenv("RENDER_EXTERNAL_URL") != null;
		String baseDir = null;
		if (isRender) {
			baseDir = "/tmp";
		} else {
			MADSysConfig config = utils
					.getOrganizationSystemConfiguratinsByDynamicOrganisation(SettingCategoriesEnum.GENERAL_SETTINGS, orgId);
			if (config != null) {
				baseDir = config.getDownloadPath();
			}

		}
		if (baseDir == null || baseDir.isEmpty()) {
			baseDir = System.getProperty("user.home") + "/invoices";
		}

		String filename = fileName + System.currentTimeMillis() + extension;
		String filePath = baseDir + File.separator + filename;
		FileOutputUrl outputUrl = fileOutputUrlRepository.findTop1ByIsActive(true);

		try {
			File file = new File(filePath);
			file.getParentFile().mkdirs(); // Ensure directory exists
			FileOutputStream fos = new FileOutputStream(file);
			String fileUrl = null;
			if (outputUrl != null) {
				fileUrl = isRender ? System.getenv("RENDER_EXTERNAL_URL") + "/file/" + filename
						: outputUrl.getFileUrl() + filename;
			} else {
				fileUrl = isRender ? System.getenv("RENDER_EXTERNAL_URL") + "/file/" + filename : baseDir + filename;
			}

			fileOutPut.setFileOutputStream(fos);
			fileOutPut.setFilePath(filePath);
			fileOutPut.setFileName(filename);
			fileOutPut.setFileOutputUrl(fileUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileOutPut;
	}

	// New method that accepts all parameters
	private void generatePDFLoanStatementReport(FileOutputStream fileOutputStream, String reportName,
			String jasperReportName, Date dateFrom, Date dateTo, long orgId, long loanId, long individualBorrowerId,
			long groupBorrowerId, long institutionBorrowerId, long membershipAccountId) {

		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();

			if (!reportFilePath.endsWith("/") && !reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			String mainReportFile = reportFilePath + jasperReportName;

			// Dynamically choose input stream based on path type
			InputStream jasperStream = mainReportFile.startsWith("http") ? new URL(mainReportFile).openStream()
					: new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> reportParams = new HashMap<>();
			reportParams.put("AD_ORG_ID", orgId);
			reportParams.put("DATE_FROM", dateFrom);
			reportParams.put("DATE_TO", dateTo);
			reportParams.put("AD_Loan_Application_ID", loanId);
			reportParams.put("MEMBERSHIP_ACCOUNT_ID", membershipAccountId);
			reportParams.put("INDIVIDUAL_BORROWER_ID", individualBorrowerId);
			reportParams.put("GROUP_BORROWER_ID", groupBorrowerId);
			reportParams.put("INSTITUTION_BORROWER_ID", institutionBorrowerId);
			reportParams.put("SUBREPORT_DIR", reportFilePath);
			reportParams.put("DISPLAY_DATE_FROM", utils.formaatDateForDisplayString(dateFrom));
			reportParams.put("DISPLAY_DATE_TO", utils.formaatDateForDisplayString(dateTo));

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages: " + totalPages);
			reportParams.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			try (FileOutputStream fileOut = fileOutputStream) {
				JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateExcelLoanStatementReport(FileOutputStream fileOutputStream, String reportName,
			String jasperReportName, Date dateFrom, Date dateTo, long orgId, long loanId, long individualBorrowerId,
			long groupBorrowerId, long institutionBorrowerId, long membershipAccountId) {

		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();

			if (!reportFilePath.endsWith("/") && !reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			String mainReportFile = reportFilePath + jasperReportName;

			// Dynamically choose input stream based on path type
			InputStream jasperStream = mainReportFile.startsWith("http") ? new URL(mainReportFile).openStream()
					: new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> reportParams = new HashMap<>();
			reportParams.put("AD_ORG_ID", orgId);
			reportParams.put("DATE_FROM", dateFrom);
			reportParams.put("DATE_TO", dateTo);
			reportParams.put("AD_Loan_Application_ID", loanId);
			reportParams.put("MEMBERSHIP_ACCOUNT_ID", membershipAccountId);
			reportParams.put("INDIVIDUAL_BORROWER_ID", individualBorrowerId);
			reportParams.put("GROUP_BORROWER_ID", groupBorrowerId);
			reportParams.put("INSTITUTION_BORROWER_ID", institutionBorrowerId);
			reportParams.put("SUBREPORT_DIR", reportFilePath);
			reportParams.put("DISPLAY_DATE_FROM", utils.formaatDateForDisplayString(dateFrom));
			reportParams.put("DISPLAY_DATE_TO", utils.formaatDateForDisplayString(dateTo));

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages: " + totalPages);
			reportParams.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			printExcelDocument(fileOutputStream, jasperPrint);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateCSVLoanStatementReport(FileOutputStream fileOutputStream, String reportName,
			String jasperReportName, Date dateFrom, Date dateTo, long orgId, long loanId, long individualBorrowerId,
			long groupBorrowerId, long institutionBorrowerId, long membershipAccountId) {

		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();

			if (!reportFilePath.endsWith("/") && !reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			String mainReportFile = reportFilePath + jasperReportName;

			// Dynamically choose input stream based on path type
			InputStream jasperStream = mainReportFile.startsWith("http") ? new URL(mainReportFile).openStream()
					: new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> reportParams = new HashMap<>();
			reportParams.put("AD_ORG_ID", orgId);
			reportParams.put("DATE_FROM", dateFrom);
			reportParams.put("DATE_TO", dateTo);
			reportParams.put("AD_Loan_Application_ID", loanId);
			reportParams.put("MEMBERSHIP_ACCOUNT_ID", membershipAccountId);
			reportParams.put("INDIVIDUAL_BORROWER_ID", individualBorrowerId);
			reportParams.put("GROUP_BORROWER_ID", groupBorrowerId);
			reportParams.put("INSTITUTION_BORROWER_ID", institutionBorrowerId);
			reportParams.put("SUBREPORT_DIR", reportFilePath);
			reportParams.put("DISPLAY_DATE_FROM", utils.formaatDateForDisplayString(dateFrom));
			reportParams.put("DISPLAY_DATE_TO", utils.formaatDateForDisplayString(dateTo));

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages: " + totalPages);
			reportParams.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, connection);
			printCsvDocument(fileOutputStream, jasperPrint);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Remove the old generic ID methods since we're now using the comprehensive
	// method

	public void printExcelDocument(FileOutputStream fileOutputStream, JasperPrint jasperPrint) {
		try (FileOutputStream fileOut = fileOutputStream) {
			JRXlsxExporter exporter = new JRXlsxExporter();
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(fileOut));

			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setOnePagePerSheet(false);
			configuration.setRemoveEmptySpaceBetweenRows(false); // Ensure empty space isn't removed
			configuration.setDetectCellType(true);
			configuration.setWhitePageBackground(false);
			configuration.setIgnoreCellBorder(false);
			configuration.setCollapseRowSpan(false);
			configuration.setIgnoreGraphics(false);
			configuration.setForcePageBreaks(false);
			configuration.setShowGridLines(true);
			configuration.setWrapText(true);

			// **Ensure Summary Band (Totals) Is Always Printed**
			configuration.setIgnorePageMargins(false);
			configuration.setIgnoreCellBackground(false);

			exporter.setConfiguration(configuration);
			exporter.exportReport();
		} catch (JRException | IOException e) {
			e.printStackTrace();
		}
	}

	public void printCsvDocument(FileOutputStream fileOutputStream, JasperPrint jasperPrint) {
		try (FileOutputStream fileOut = fileOutputStream) {
			JRCsvExporter exporter = new JRCsvExporter();

			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

			exporter.setExporterOutput(new SimpleWriterExporterOutput(fileOut));

			SimpleCsvExporterConfiguration configuration = new SimpleCsvExporterConfiguration();
			configuration.setFieldDelimiter(",");
			configuration.setRecordDelimiter("\n");
			configuration.setWriteBOM(false);

			exporter.setConfiguration(configuration);

			exporter.exportReport();

			System.out.println("CSV report exported successfully.");

		} catch (JRException | IOException e) {
			e.printStackTrace();
		}
	}

	public void printPDFDocument(FileOutputStream fileOutputStream, JasperPrint jasperPrint) {
		try (FileOutputStream fileOut = fileOutputStream) {
			JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (JRException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 */
	private void generatePDFDateRangeReportsRaw(Date dateFrom, Date dateTo, FileOutputStream fileOutputStream,
			String reportName, String jasperReportName, long orgId) {
		// TODO Auto-generated method stub
		dateFrom = utils.getStartOfDay(dateFrom);
		dateTo = utils.getEndOfDay(dateTo);
		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();
			params.put("DateFrom", dateFrom);
			params.put("DateTo", dateTo);
			params.put("AD_ORG_ID", orgId);
			params.put("SUBREPORT_DIR", reportFilePath);

			String formattedDateFrom = utils.formaatDateForDisplayString(dateFrom);
			String formattedDateTo = utils.formaatDateForDisplayString(dateTo);
			params.put("DISPLAY_DATE_FROM", formattedDateFrom);
			params.put("DISPLAY_DATE_TO", formattedDateTo);
			params.put("SUBREPORT_DIR", reportFilePath);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			try (FileOutputStream fileOut = fileOutputStream) {
				// JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);
				JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 */
	private void generatePDFDateRangeReportsByGenericIdRaw(Date dateFrom, Date dateTo,
			FileOutputStream fileOutputStream, String reportName, String jasperReportName, long orgId, long id) {
		// TODO Auto-generated method stub
		dateFrom = utils.getStartOfDay(dateFrom);
		dateTo = utils.getEndOfDay(dateTo);
		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();
			params.put("DateFrom", dateFrom);
			params.put("DateTo", dateTo);
			params.put("AD_ORG_ID", orgId);
			params.put("QUERY_ID", id);
			params.put("SUBREPORT_DIR", reportFilePath);

			String formattedDateFrom = utils.formaatDateForDisplayString(dateFrom);
			String formattedDateTo = utils.formaatDateForDisplayString(dateTo);
			params.put("DISPLAY_DATE_FROM", formattedDateFrom);
			params.put("DISPLAY_DATE_TO", formattedDateTo);
			params.put("SUBREPORT_DIR", reportFilePath);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			try (FileOutputStream fileOut = fileOutputStream) {
				// JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);
				JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 */
	private void generatePDFByGenericIdRaw(FileOutputStream fileOutputStream, String reportName,
			String jasperReportName, long orgId, long id) {
		// TODO Auto-generated method stub

		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();

			params.put("AD_ORG_ID", orgId);
			params.put("QUERY_ID", id);
			params.put("SUBREPORT_DIR", reportFilePath);

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			try (FileOutputStream fileOut = fileOutputStream) {
				// JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);
				JasperExportManager.exportReportToPdfStream(jasperPrint, fileOut);

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 */
	private void generateExcelDateRangeReportsRaw(Date dateFrom, Date dateTo, FileOutputStream fileOutputStream,
			String reportName, String jasperReportName, long orgId) {
		// TODO Auto-generated method stub
		dateFrom = utils.getStartOfDay(dateFrom);
		dateTo = utils.getEndOfDay(dateTo);
		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();
			params.put("DateFrom", dateFrom);
			params.put("DateTo", dateTo);
			params.put("AD_ORG_ID", orgId);
			params.put("SUBREPORT_DIR", reportFilePath);

			String formattedDateFrom = utils.formaatDateForDisplayString(dateFrom);
			String formattedDateTo = utils.formaatDateForDisplayString(dateTo);
			params.put("DISPLAY_DATE_FROM", formattedDateFrom);
			params.put("DISPLAY_DATE_TO", formattedDateTo);
			params.put("SUBREPORT_DIR", reportFilePath);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			printExcelDocument(fileOutputStream, jasperPrint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 */
	private void generateCsvDateRangeReportsRaw(Date dateFrom, Date dateTo, FileOutputStream fileOutputStream,
			String reportName, String jasperReportName, long orgId) {
		// TODO Auto-generated method stub
		dateFrom = utils.getStartOfDay(dateFrom);
		dateTo = utils.getEndOfDay(dateTo);
		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();
			params.put("DateFrom", dateFrom);
			params.put("DateTo", dateTo);
			params.put("AD_ORG_ID", orgId);
			params.put("SUBREPORT_DIR", reportFilePath);

			String formattedDateFrom = utils.formaatDateForDisplayString(dateFrom);
			String formattedDateTo = utils.formaatDateForDisplayString(dateTo);
			params.put("DISPLAY_DATE_FROM", formattedDateFrom);
			params.put("DISPLAY_DATE_TO", formattedDateTo);
			params.put("SUBREPORT_DIR", reportFilePath);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			printCsvDocument(fileOutputStream, jasperPrint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 * @param id
	 */
	private void generateExcelDateRangeReportsByGenericIdRaw(Date dateFrom, Date dateTo,
			FileOutputStream fileOutputStream, String reportName, String jasperReportName, long orgId, long id) {
		// TODO Auto-generated method stub
		dateFrom = utils.getStartOfDay(dateFrom);
		dateTo = utils.getEndOfDay(dateTo);
		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();
			params.put("DateFrom", dateFrom);
			params.put("DateTo", dateTo);
			params.put("AD_ORG_ID", orgId);
			params.put("QUERY_ID", id);
			params.put("SUBREPORT_DIR", reportFilePath);

			String formattedDateFrom = utils.formaatDateForDisplayString(dateFrom);
			String formattedDateTo = utils.formaatDateForDisplayString(dateTo);
			params.put("DISPLAY_DATE_FROM", formattedDateFrom);
			params.put("DISPLAY_DATE_TO", formattedDateTo);
			params.put("SUBREPORT_DIR", reportFilePath);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			printExcelDocument(fileOutputStream, jasperPrint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param dateFrom
	 * @param dateTo
	 * @param fileOutputStream
	 * @param orgId
	 * @param id
	 */
	private void generateExcelReportsByGenericIdRaw(FileOutputStream fileOutputStream, String reportName,
			String jasperReportName, long orgId, long id) {
		// TODO Auto-generated method stub

		try (Connection connection = dataSource.getConnection()) {
			ReportTemplates reportTemp = reportTemplatesRepository.findTop1ByIsActiveAndReportName(true, reportName);
			String reportFilePath = reportTemp.getFilePath();
			// Ensure the file path ends with a separator
			if (!reportFilePath.endsWith(File.separator)) {
				reportFilePath += File.separator;
			}

			// Load the Jasper report using FileInputStream
			String mainReportFile = reportFilePath + jasperReportName;
			InputStream jasperStream = new FileInputStream(mainReportFile);

			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

			Map<String, Object> params = new HashMap<>();

			params.put("AD_ORG_ID", orgId);
			params.put("QUERY_ID", id);
			params.put("SUBREPORT_DIR", reportFilePath);

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			// Get the total number of pages
			int totalPages = jasperPrint.getPages().size();
			System.out.println("No of Pages.-----------------------------" + totalPages);
			params.put("TotalPages", totalPages);

			jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection);
			printExcelDocument(fileOutputStream, jasperPrint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}