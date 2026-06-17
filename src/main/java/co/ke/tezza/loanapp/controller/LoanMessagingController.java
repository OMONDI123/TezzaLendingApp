package co.ke.tezza.loanapp.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.LoanCommentRequest;
import co.ke.tezza.loanapp.model.MessageRequest;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.model.TriggerMessage;
import co.ke.tezza.loanapp.response.LoanCommentResponse;
import co.ke.tezza.loanapp.service.ForceSendSmsService;
import co.ke.tezza.loanapp.service.JasperReportingServices;
import co.ke.tezza.loanapp.service.LoanMessagingService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;

@RestController
@RequestMapping("/loanMessages")
public class LoanMessagingController {

	@Autowired
	private LoanMessagingService loanMessagingService;

	@Autowired
	private ForceSendSmsService forceSendSmsService;

	@Autowired
	private JasperReportingServices jasper;

	

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final GsonBuilder gsonBuilder = new GsonBuilder()
			.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);

	/**
	 * Save or update a loan cardex comment entry.
	 */
	@PostMapping(value = "/saveOrUpdate", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','saveOrUpdate')")
	public String saveOrUpdate(@RequestBody LoanCommentRequest request) {
		logger.debug("Called LoanMessagingController.saveOrUpdate with request: {}", request);
		return gsonBuilder.create().toJson(loanMessagingService.saveOrUpdate(request));
	}

	@PostMapping(value = "/printCardex", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','printCardex')")
	public String printCardex(@Valid @RequestBody ReportParams params) {
		logger.debug("Called LoanApplicationController.printCardex");
		return gsonBuilder.create().toJson(jasper.printCardex(params));
	}

	@PostMapping(value = "/sendManualMessage", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','sendManualMessage')")
	public String sendManualMessage(@RequestBody @Valid MessageRequest request) {
		logger.debug("Called LoanMessagingController.sendManualMessage with request: {}", request);
		return gsonBuilder.create().toJson(loanMessagingService.sendManualMessages(request));
	}

	@PostMapping(value = "/resendAlreadySentLoanReminder", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','resendAlreadySentLoanReminder')")
	public String resendAlreadySentLoanReminder(@RequestBody TriggerMessage request) {
		logger.debug("Called LoanMessagingController.resendAlreadySentLoanReminder with request: {}", request);
		return gsonBuilder.create().toJson(forceSendSmsService.forceSms(request));
	}

	// ==================== CARDEX ENDPOINTS ====================

	@GetMapping(value = "/getCardexBorrowers/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getCardexBorrowers/{page}/{size}')")
	public String getCardexBorrowers(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size,
			@QueryParam("dateFrom") Date dateFrom, @QueryParam("dateTo") Date dateTo,
			@QueryParam("searchTerm") String searchTerm) {
		logger.debug("Called LoanMessagingController.getCardexBorrowers");
		return gsonBuilder.create().toJson(loanMessagingService.getCardexBorrowers(page, size, searchTerm));
	}

	@GetMapping(value = "/getCardexByBorrowerId/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getCardexByBorrowerId/{page}/{size}')")
	public String getCardexByBorrowerId(@PathVariable("page") int page, @PathVariable("size") int size,
			@RequestParam(value = "borrowerId", required = true) Long borrowerId,
			@RequestParam(value = "borrowerType", required = true) String borrowerType,
			@QueryParam("addedById") Long addedById, @QueryParam("dateFrom") Date dateFrom,
			@QueryParam("dateTo") Date dateTo, @QueryParam("searchTerm") String searchTerm) {
		return gsonBuilder.create().toJson(
				loanMessagingService.getCardexByBorrower(borrowerType, borrowerId, searchTerm, addedById, page, size));
	}

	

	// ==================== BORROWER DETAILS ENDPOINTS ====================

	@GetMapping(value = "/getBorroweDetails/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getBorroweDetails/{page}/{size}')")
	public String getBorroweDetails(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size,
			@QueryParam("searchTerm") String searchTerm) {
		logger.debug("Called LoanMessagingController.getBorroweDetails");
		return gsonBuilder.create().toJson(loanMessagingService.getBorrowers(page, size, searchTerm));
	}

	@GetMapping(value = "/getBorrowersWithMessages/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getBorrowersWithMessages/{page}/{size}')")
	public String getBorrowersWithMessages(@PathVariable(value = "page") int page,
			@PathVariable(value = "size") int size, @QueryParam("dateFrom") String dateFrom,
			@QueryParam("dateTo") String dateTo, @QueryParam("searchTerm") String searchTerm) {
		logger.debug("Called LoanMessagingController.getBorrowersWithMessages");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create()
				.toJson(loanMessagingService.getBorrowersWithMessages(page, size, searchTerm, fromDate, toDate));
	}

	

	

	// ==================== MESSAGE CENTER ENDPOINTS ====================

	@GetMapping(value = "/getMessagesSent/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getMessagesSent/{page}/{size}')")
	public String getMessagesSent(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size,
			@QueryParam("searchTerm") String searchTerm, @QueryParam("messageStatus") String messageStatus,
			@QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
		logger.debug("Called LoanMessagingController.getMessagesSent");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create().toJson(
				loanMessagingService.getAllMessageCenter(page, size, searchTerm, messageStatus, fromDate, toDate));
	}

	@GetMapping(value = "/getMessagesSentByBorrower/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getMessagesSentByBorrower/{page}/{size}')")
	public String getMessagesSentByBorrower(@PathVariable(value = "page") int page,
			@PathVariable(value = "size") int size, @QueryParam("borrowerId") long borrowerId,
			@QueryParam("borrowerType") String borrowerType, @QueryParam("searchTerm") String searchTerm,
			@QueryParam("messageStatus") String messageStatus, @QueryParam("dateFrom") String dateFrom,
			@QueryParam("dateTo") String dateTo) {
		logger.debug("Called LoanMessagingController.getMessagesSentByBorrower");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create().toJson(loanMessagingService.getMessagesSentByBorrower(page, size, searchTerm,
				messageStatus, fromDate, toDate, borrowerId, borrowerType));
	}
	
	

	

	// ==================== SMS REMINDERS ENDPOINTS ====================

	@GetMapping(value = "/getSmsRemindersSent/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getSmsRemindersSent/{page}/{size}')")
	public String getSmsRemindersSent(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size,
			@QueryParam("searchTerm") String searchTerm, @QueryParam("messageStatus") String messageStatus,
			@QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
		logger.debug("Called LoanMessagingController.getSmsRemindersSent");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create().toJson(
				loanMessagingService.getSmsRemindersSent(page, size, searchTerm, messageStatus, fromDate, toDate));
	}

	@GetMapping(value = "/getBorrowersWithSmsReminders/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getBorrowersWithSmsReminders/{page}/{size}')")
	public String getBorrowersWithSmsReminders(@PathVariable(value = "page") int page,
			@PathVariable(value = "size") int size, @QueryParam("dateFrom") String dateFrom,
			@QueryParam("dateTo") String dateTo, @QueryParam("searchTerm") String searchTerm) {
		logger.debug("Called LoanMessagingController.getBorrowersWithSmsReminders");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create()
				.toJson(loanMessagingService.getBorrowersWithSmsReminders(page, size, searchTerm, fromDate, toDate));
	}

	@GetMapping(value = "/getSmsRemindersSentByBorrowerId/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','getSmsRemindersSentByBorrowerId/{page}/{size}')")
	public String getSmsRemindersSentByBorrowerId(@PathVariable(value = "page") int page,
			@PathVariable(value = "size") int size, @QueryParam("borrowerId") long borrowerId,
			@QueryParam("borrowerType") String borrowerType, @QueryParam("searchTerm") String searchTerm,
			@QueryParam("messageStatus") String messageStatus, @QueryParam("dateFrom") String dateFrom,
			@QueryParam("dateTo") String dateTo) {
		logger.debug("Called LoanMessagingController.getSmsRemindersSentByBorrowerId");

		Date fromDate = null;
		Date toDate = null;

		try {
			if (dateFrom != null && !dateFrom.isEmpty()) {
				fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
			}
			if (dateTo != null && !dateTo.isEmpty()) {
				toDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
			}
		} catch (ParseException e) {
			logger.error("Error parsing dates", e);
		}

		return gsonBuilder.create().toJson(loanMessagingService.getSmsRemindersSentByBorrowerId(page, size, searchTerm,
				messageStatus, fromDate, toDate, borrowerId, borrowerType));
	}

	

	// ==================== LEGACY ENDPOINTS ====================

	/**
	 * Get all comments for a specific loan.
	 */
	@GetMapping(value = "/byLoan", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','byLoan')")
	public String getByLoan(@RequestParam("loanId") Long loanId) {
		logger.debug("Called LoanMessagingController.getByLoan with loanId: {}", loanId);
		List<LoanCommentResponse> comments = loanMessagingService.getByLoan(loanId);
		return gsonBuilder.create().toJson(comments);
	}

	/**
	 * Get all comments for a specific installment.
	 */
	@GetMapping(value = "/byInstallment", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanMessagingController','byInstallment')")
	public String getByInstallment(@RequestParam("installmentId") Long installmentId) {
		logger.debug("Called LoanMessagingController.getByInstallment with installmentId: {}", installmentId);
		List<LoanCommentResponse> comments = loanMessagingService.getByInstallment(installmentId);
		return gsonBuilder.create().toJson(comments);
	}

	
}