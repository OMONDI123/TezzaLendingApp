package co.ke.tezza.loanapp.controller;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.FileUpload;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.LoanApplicationRequest;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.response.LoanApplicationResponse;
import co.ke.tezza.loanapp.service.JasperReportingServices;
import co.ke.tezza.loanapp.service.LoanApplicationService;
import co.ke.tezza.loanapp.service.LoanApprovalWorkFlowService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.Utils;
import lombok.val;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.QueryParam;

@RestController
@RequestMapping("/loanApplication")
@CrossOrigin
public class LoanApplicationController {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LoanApplicationService loanApplicationService;

	@Autowired
	private LoanStatementService loanStatementService;

	private final GsonBuilder gsonBuilder = new GsonBuilder()
			.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);

	@Autowired
	private JasperReportingServices jasper;
	
	@Autowired private LoanApprovalWorkFlowService loanApprovalWorkFlowService;
	
	
	

	@GetMapping("/downloadGuarantorInstructions")
	// @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationBulkController','downloadGuarantorInstructions')")
	public void downloadGuarantorInstructions(HttpServletResponse response) throws IOException {
		loanApplicationService.downloadGuarantorInstructions(response);
		
	}
	
	@GetMapping("/downloadLoanApplicationCsvTemplate")
	// @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationBulkController','downloadLoanApplicationCsvTemplate')")
	public void downloadLoanApplicationCsvTemplate(HttpServletResponse response) throws IOException {
		loanApplicationService.downloadLoanApplicationTemplate(response);
		
	}

	@PostMapping(value = "/uploadLoansFromCsvFile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','uploadLoansFromCsvFile')")
	public String uploadLoansFromCsvFile(@RequestBody FileUploads fileUpload) {
		MultipartFile file = Utils.convertBase64StringFileToMultipartFile(fileUpload);
		logger.debug("Called LoanApplicationController.uploadLoansFromCsvFile");
		return gsonBuilder.create().toJson(loanApplicationService.processLoanApplicationsCsv(file));

	}

	@PostMapping(value = "/applyForLoan", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','applyForLoan')")
	public String applyForLoan(@RequestBody LoanApplicationRequest request) {
		logger.debug("Called LoanApplicationController.applyForLoan");
		return gsonBuilder.create().toJson(loanApplicationService.applyForLoan(request));
	}
	
	@PostMapping(value = "/reAssignLoan/{loanId}/{newAssigneeId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','reAssignLoan/{loanId}/{newAssigneeId}')")
	public String reAssignLoan(@PathVariable(value = "loanId")long loanId,@PathVariable(value = "newAssigneeId")long newAssigneeId) {
		logger.debug("Called LoanApplicationController.reAssignLoan");
		return gsonBuilder.create().toJson(loanApplicationService.reAssignLoan(loanId, newAssigneeId));
	}
	@GetMapping(value = "/getLoanAssignees", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getLoanAssignees')")
	public String getLoanAssignees(@QueryParam(value = "searchTerm")String searchTerm) {
		return gsonBuilder.create()
				.toJson(loanApplicationService.getLoanAssignees(searchTerm));
	}
	
	
	
	@GetMapping(value = "/getAllApplicationsPendingApprovals", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getAllApplications')")
	public String getAllApplicationsPendingApprovals(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "borrowerType", required = false) String borrowerType) {

		logger.debug("Called LoanApplicationController.getAllApplications");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return gsonBuilder.create()
				.toJson(loanApplicationService.getAllApplicationsPendingApprovals(dateFrom, dateTo, page, size, search));
	}

	@PostMapping(value = "/approveLoan/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','approveLoan/{applicationId}')")
	public String approveLoan(@PathVariable Long applicationId) {
		logger.debug("Called LoanApplicationController.approveLoan");
		return gsonBuilder.create().toJson(loanApprovalWorkFlowService.approveLoan(applicationId));
	}

	@PostMapping(value = "/rejectLoan/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','rejectLoan/{applicationId}')")
	public String rejectLoan(@PathVariable Long applicationId, @RequestParam String reason) {
		logger.debug("Called LoanApplicationController.rejectLoan");
		return gsonBuilder.create().toJson(loanApprovalWorkFlowService.rejectLoan(applicationId, reason));
	}

	@GetMapping(value = "/getRecentApplications", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getRecentApplications')")
	public String getRecentApplications() {
		return gsonBuilder.create().toJson(loanApplicationService.getRecentApplications());
	}

	@GetMapping(value = "/getAllApplications", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getAllApplications')")
	public String getAllApplications(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "borrowerType", required = false) String borrowerType) {

		logger.debug("Called LoanApplicationController.getAllApplications");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return gsonBuilder.create()
				.toJson(loanApplicationService.getAllApplications(dateFrom, dateTo, page, size, search));
	}
	
	
	@GetMapping(value = "/getAllRejectedApplications", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getAllRejectedApplications')")
	public String getAllRejectedApplications(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "borrowerType", required = false) String borrowerType) {

		logger.debug("Called LoanApplicationController.getAllApplications");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return gsonBuilder.create()
				.toJson(loanApplicationService.getAllRejectedApplications(dateFrom, dateTo, page, size, search));
	}
	
	
	@GetMapping(value = "/getAllAmendedApplications", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getAllAmendedApplications')")
	public String getAllAmendedApplications(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "borrowerType", required = false) String borrowerType) {

		logger.debug("Called LoanApplicationController.getAllAmendedApplications");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return gsonBuilder.create()
				.toJson(loanApplicationService.getAllAmendedApplications(dateFrom, dateTo, page, size, search));
	}

	@GetMapping(value = "/getLoansWithBalances", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getLoansWithBalances')")
	public String getLoansWithBalances(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "search", required = false) String search) {

		logger.debug("Called LoanApplicationController.getLoansWithBalances");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return gsonBuilder.create()
				.toJson(loanApplicationService.getLoansWithBalances(dateFrom, dateTo, page, size, search));
	}

	@DeleteMapping(value = "/deleteApplication/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','delete/{id}')")
	public String deleteApplication(@PathVariable Long id) {
		logger.debug("Called LoanApplicationController.deleteApplication");
		return gsonBuilder.create().toJson(loanApplicationService.deleteApplication(id));
	}

	// ===========================================
	// LOAN STATEMENT ENDPOINTS
	// ===========================================

	@GetMapping(value = "/getLoanStatement/{loanId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getLoanStatement/{loanId}')")
	public String getLoanStatement(@PathVariable Long loanId) {
		logger.debug("Called LoanApplicationController.getLoanStatement");
		return gsonBuilder.create().toJson(loanStatementService.getLoanStatement(loanId));
	}

	@GetMapping(value = "/getLoanStatementOnly/{loanId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getLoanStatementOnly/{loanId}')")
	public String getLoanStatementOnly(@PathVariable Long loanId) {
		logger.debug("Called LoanApplicationController.getLoanStatementOnly");
		return gsonBuilder.create().toJson(loanStatementService.getLoanStatementOnly(loanId));
	}

	@GetMapping(value = "/getInstallmentsByLoan/{loanId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getInstallmentsByLoan/{loanId}')")
	public String getInstallmentsByLoan(@PathVariable long loanId) {
		logger.debug("Called LoanApplicationController.getInstallmentsByLoan");
		return gsonBuilder.create().toJson(loanStatementService.getInstallmentsByLoan(loanId));
	}

	@GetMapping(value = "/getAllLoanStatement/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','getAllLoanStatement/{page}/{size}')")
	public String getAllLoanStatement(@PathVariable int page, @PathVariable int size,
			@QueryParam("searchTerm") String searchTerm, @QueryParam("transactionType") String transactionType) {
		logger.debug("Called LoanApplicationController.getInstallmentsByLoan");
		return gsonBuilder.create()
				.toJson(loanStatementService.getAllLoanStatement(size, page, searchTerm, transactionType));
	}

	@PostMapping(value = "/printLoanStatement", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanApplicationController','printLoanStatement')")
	public String printLoanStatement(@Valid @RequestBody ReportParams params) {
		logger.debug("Called LoanApplicationController.printLoanStatement");
		return gsonBuilder.create().toJson(jasper.printLoanStatement(params));
	}
}
