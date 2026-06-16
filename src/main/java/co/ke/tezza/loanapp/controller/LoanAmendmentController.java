package co.ke.tezza.loanapp.controller;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.LoanAmendmentRequest;
import co.ke.tezza.loanapp.service.LoanAmendmentApprovalService;
import co.ke.tezza.loanapp.service.LoanAmendmentService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;

import java.time.LocalDate;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loanAmendment")
public class LoanAmendmentController {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LoanAmendmentService loanAmendmentService;

	@Autowired
	private LoanAmendmentApprovalService loanAmendmentApprovalService;

	@PostMapping(value = "/requestLoanAmendment", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanAmendmentController','requestLoanAmendment')")
	public String requestLoanAmendment(@RequestBody LoanAmendmentRequest model) {
		logger.debug("Called LoanAmendmentController.requestLoanAmendment");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(loanAmendmentService.requestLoanAmendment(model));
	}
	
	@PostMapping(value = "/approveAmendmentDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanAmendmentController','approveAmendmentDetail/{id}')")
	public String approveAmendmentDetail(@PathVariable("id") Long id) {
		logger.debug("Called LoanAmendmentController.approveAmendmentDetail");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(loanAmendmentApprovalService.approveAmendmentDetail(id));
	}
	@PostMapping(value = "/rejectAmendmentDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanAmendmentController','rejectAmendmentDetail/{id}')")
	public String rejectAmendmentDetail(@PathVariable("id") Long id,@RequestParam(value = "reason", required = true) String reason) {
		logger.debug("Called LoanAmendmentController.rejectAmendmentDetail");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(loanAmendmentApprovalService.rejectAmendmentDetail(id,reason));
	}
	

	@GetMapping(value = "/getLoanAmendmentRequests", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanAmendmentController','getLoanAmendmentRequests')")
	public String getLoanAmendmentRequests(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "amendmentRequestsSearchTerm", required = false) String amendmentRequestsSearchTerm,
			@RequestParam(value = "amendmentRequestsStatus", required = false) String amendmentRequestsStatus) {

		logger.debug("Called LoanAmendmentController.getLoanAmendmentRequests");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(loanAmendmentService.getAmendmentRequests(amendmentRequestsStatus, amendmentRequestsSearchTerm,
						dateFrom, dateTo, page, size));
	}

	@GetMapping(value = "/getAllAmendmentsReadyForApproval", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanAmendmentController','getAllAmendmentsReadyForApproval')")
	public String getAllAmendmentsReadyForApproval(
			@RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFrom,
			@RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateTo,
			@RequestParam("page") int page, @RequestParam("size") int size,
			@RequestParam(value = "searchTerm", required = false) String searchTerm) {

		logger.debug("Called LoanAmendmentController.getAllAmendmentsReadyForApproval");

		// Apply default current month range if dates are null
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100));
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100));
		}

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(loanAmendmentApprovalService.getAllAmendmentsReadyForApproval(searchTerm, dateFrom, dateTo,
						page, size));
	}
}
