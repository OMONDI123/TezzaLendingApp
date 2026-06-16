package co.ke.tezza.loanapp.controller;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.PaymentAppprovalConfigModel;
import co.ke.tezza.loanapp.model.PaymentRequest;
import co.ke.tezza.loanapp.response.PaymentApprovalResponse;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.service.PaymentApprovalService;
import co.ke.tezza.loanapp.service.PaymentApprovalWorkflowService;
import co.ke.tezza.loanapp.service.PaymentsService;
import co.ke.tezza.loanapp.service.WaiverWriteOffService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	@Autowired
	private PaymentsService paymentsService;
	@Autowired
	private PaymentApprovalService paymentApprovalService;
	@Autowired
	private PaymentApprovalWorkflowService approvalWorkflowService;
	@Autowired private WaiverWriteOffService writeOffService;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final GsonBuilder gsonBuilder = new GsonBuilder()
			.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);

	@PostMapping(value = "/createUpdatePaymentApprovalConfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','createUpdatePaymentApprovalConfig')")
	public String createUpdatePaymentApprovalConfig(@RequestBody PaymentAppprovalConfigModel model) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(paymentApprovalService.createUpdatePaymentApprovalConfig(model));
	}

	@GetMapping(value = "/getAllPaymentApprovalConfigs/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllPaymentApprovalConfigs/{page}/{size}')")
	public String getAllPaymentApprovalConfigs(@PathVariable(value = "page") int page,
			@PathVariable(value = "size") int size, @QueryParam(value = "searchTerm") String searchTerm) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(paymentApprovalService.getPaymentApprovalConfiguration(page, size, searchTerm));
	}

	@PostMapping(value = "/pay", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','pay')")
	public String pay(@RequestBody PaymentRequest request) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(paymentsService.processPayment(request));
	}
	
	@PostMapping(value = "/writeOffOrWaiver", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','writeOffOrWaiver')")
	public String writeOffOrWaiver(@RequestBody PaymentRequest request) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(writeOffService.processWaiverWriteOff(request));
	}

	@PostMapping(value = "/approvePayment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','approvePayment/{id}')")
	public String approvePayment(@PathVariable(value = "id") long id) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(approvalWorkflowService.approvePayment(id));
	}

	@PostMapping(value = "/rejectPayment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','rejectPayment/{id}')")
	public String rejectPayment(@PathVariable(value = "id") long id, @QueryParam(value = "reason") String reason) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(approvalWorkflowService.rejectPayment(id, reason));
	}

	@GetMapping(value = "/getAllPendingPaymentsPendingApprovals", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllPendingPaymentsPendingApprovals')")
	public String getAllPendingPaymentsPendingApprovals(
			@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam("page") int page, @RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPendingPaymentsPendingApprovals with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentApprovalResponse> paymentPage = approvalWorkflowService
					.getAllPendingPaymentsPendingApprovals(page, size, dateFrom, dateTo, search,false);
			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}
	
	@GetMapping(value = "/getAllPendingWaiverOrWriteOffPaymentsPendingApprovals", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllPendingWaiverOrWriteOffPaymentsPendingApprovals')")
	public String getAllPendingWaiverOrWriteOffPaymentsPendingApprovals(
			@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam("page") int page, @RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPendingPaymentsPendingApprovals with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentApprovalResponse> paymentPage = approvalWorkflowService
					.getAllPendingPaymentsPendingApprovals(page, size, dateFrom, dateTo, search,true);
			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}
	
	@GetMapping(value = "/getAllWriteOffOrWaiverPayments", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllWriteOffOrWaiverPayments')")
	public String getAllWriteOffOrWaiverPayments(@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam(value = "status", required = false) String status, @RequestParam("page") int page,
			@RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPayments with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, status, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentResponse> paymentPage = paymentsService.getAllWriteOffOrWaiverPayments(page, size, dateFrom, dateTo, search,
					paymentMethod, status);

			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}
	
	@GetMapping(value = "/getAllWriteOffOrWaiverRejectedPayments", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllWriteOffOrWaiverRejectedPayments')")
	public String getAllWriteOffOrWaiverRejectedPayments(@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam(value = "status", required = false) String status, @RequestParam("page") int page,
			@RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPayments with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, status, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentResponse> paymentPage = paymentsService.getAllWriteOffOrWaiverRejectedPayments(page, size, dateFrom, dateTo, search,
					paymentMethod, status);

			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}

	@PostMapping(value = "/manuallyQueryPaymentStatus/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','manuallyQueryPaymentStatus/{paymentId}')")
	public String manuallyQueryPaymentStatus(@PathVariable(value = "paymentId") long paymentId) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(paymentsService.manuallyQueryPaymentStatus(paymentId));
	}

	@GetMapping(value = "/getAllPayments", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllPayments')")
	public String getAllPayments(@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam(value = "status", required = false) String status, @RequestParam("page") int page,
			@RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPayments with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, status, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentResponse> paymentPage = paymentsService.getAllPayments(page, size, dateFrom, dateTo, search,
					paymentMethod, status);

			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}
	
	@GetMapping(value = "/getAllRejectedPayments", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','getAllRejectedPayments')")
	public String getAllRejectedPayments(@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod,
			@RequestParam(value = "status", required = false) String status, @RequestParam("page") int page,
			@RequestParam("size") int size) {

		logger.debug(
				"Called PaymentController.getAllPayments with params - dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}, page: {}, size: {}",
				dateFrom, dateTo, search, paymentMethod, status, page, size);
		LocalDate now = LocalDate.now();

		if (dateFrom == null) {
			dateFrom = java.sql.Date.valueOf(now.minusYears(100)).toString();
		}

		if (dateTo == null) {
			dateTo = java.sql.Date.valueOf(now.plusYears(100)).toString();
		}

		try {
			Page<PaymentResponse> paymentPage = paymentsService.getAllRejectedPayments(page, size, dateFrom, dateTo, search,
					paymentMethod, status);

			// Create response wrapper similar to your existing pattern
			Map<String, Object> response = new HashMap<>();
			response.put("content", paymentPage.getContent());
			response.put("totalElements", paymentPage.getTotalElements());
			response.put("totalPages", paymentPage.getTotalPages());
			response.put("size", paymentPage.getSize());
			response.put("number", paymentPage.getNumber());
			response.put("numberOfElements", paymentPage.getNumberOfElements());
			response.put("first", paymentPage.isFirst());
			response.put("last", paymentPage.isLast());
			response.put("empty", paymentPage.isEmpty());

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getAllPayments: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payments: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}

	@GetMapping(value = "/paymentStatistics", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('PaymentController','paymentStatistics')")
	public String getPaymentStatistics(@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo) {

		logger.debug("Called PaymentController.getPaymentStatistics with params - dateFrom: {}, dateTo: {}", dateFrom,
				dateTo);

		try {
			Map<String, Object> statistics = paymentsService.getPaymentStatistics(dateFrom, dateTo);

			Map<String, Object> response = new HashMap<>();
			response.put("statistics", statistics);
			response.put("statusCode", 200);
			response.put("message", "Statistics retrieved successfully");

			return gsonBuilder.create().toJson(response);

		} catch (Exception e) {
			logger.error("Error in getPaymentStatistics: {}", e.getMessage(), e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("message", "Failed to retrieve payment statistics: " + e.getMessage());
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", true);

			return gsonBuilder.create().toJson(errorResponse);
		}
	}

}
