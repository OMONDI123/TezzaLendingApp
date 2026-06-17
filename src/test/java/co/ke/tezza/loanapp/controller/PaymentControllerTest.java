package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.model.ApprovalStepsModel;
import co.ke.tezza.loanapp.model.PaymentAppprovalConfigModel;
import co.ke.tezza.loanapp.model.PaymentRequest;
import co.ke.tezza.loanapp.response.PaymentApprovalResponse;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.service.PaymentApprovalService;
import co.ke.tezza.loanapp.service.PaymentApprovalWorkflowService;
import co.ke.tezza.loanapp.service.PaymentsService;
import co.ke.tezza.loanapp.service.WaiverWriteOffService;
import co.ke.tezza.loanapp.util.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link PaymentController}.
 *
 * NOTE: {@code getAllPaymentApprovalConfigs} and {@code rejectPayment} bind their
 * {@code searchTerm} / {@code reason} parameters using {@code javax.ws.rs.QueryParam},
 * which Spring MVC does not natively recognise. These tests assume Spring's default
 * argument resolution falls back to binding by parameter name (the project must be
 * compiled with parameter name info, e.g. the {@code -parameters} javac flag, for this
 * to work) — adjust the relevant tests if your build does not preserve parameter names.
 *
 * Several collaborator methods (createUpdatePaymentApprovalConfig,
 * getPaymentApprovalConfiguration, processWaiverWriteOff, approvePayment, rejectPayment)
 * have return types not shown in the provided source, so those are stubbed to return
 * {@code null} and assertions focus on status codes and argument capturing rather than
 * response payload shape.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController – Unit Tests")
class PaymentControllerTest {

	// -----------------------------------------------------------------------
	// Mocks
	// -----------------------------------------------------------------------
	@Mock
	private PaymentsService paymentsService;
	@Mock
	private PaymentApprovalService paymentApprovalService;
	@Mock
	private PaymentApprovalWorkflowService approvalWorkflowService;
	@Mock
	private WaiverWriteOffService writeOffService;

	@InjectMocks
	private PaymentController controller;

	private MockMvc mockMvc;
	private ObjectMapper mapper;

	// -----------------------------------------------------------------------
	// Setup
	// -----------------------------------------------------------------------
	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		mapper = new ObjectMapper();
		mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		mapper.findAndRegisterModules();
	}

	// -----------------------------------------------------------------------
	// Builders
	// -----------------------------------------------------------------------
	private PaymentRequest buildPaymentRequest() {
		PaymentRequest request = new PaymentRequest();
		request.setPaymentId(0L);
		request.setAmount(new BigDecimal("1000"));
		request.setLoanId(1L);
		request.setBillId(0L);
		request.setPhoneNo("254712345678");
		request.setUseMpesaPrompt(false);
		request.setUseOtherPhone(false);
		request.setPaymentDate("2026-06-17");
		request.setPaymentDateTime(LocalDateTime.now());
		request.setReference("REF-001");
		request.setPaymentMethod("CASH");
		request.setSecurityPayment(false);
		request.setExpectedAllocationDate(LocalDateTime.now().plusDays(30));
		request.setPaymentModeId(1L);
		request.setPaymentReceivedBy(0L);
		return request;
	}

	private PaymentResponse buildPaymentResponse(long id) {
		PaymentResponse response = new PaymentResponse();
		response.setPaymentId(id);
		response.setAmount(new BigDecimal("1000"));
		response.setReference("PAM/LN/2026/000001");
		return response;
	}

	private ResponseEntity<PaymentResponse> ok(String message, PaymentResponse data) {
		return new ResponseEntity<>(message, 200, data);
	}

	private PaymentAppprovalConfigModel buildApprovalConfigModel() {
		PaymentAppprovalConfigModel model = new PaymentAppprovalConfigModel();
		model.setPaymentMethodId(1L);
		model.setRequiredAprrovalSteps(1);
		ApprovalStepsModel step = new ApprovalStepsModel();
		step.setSteps(1);
		step.setApprovalRoleInvolvedId(5L);
		model.setApprovalLevels(List.of(step));
		return model;
	}

	private String toJson(Object obj) throws Exception {
		return mapper.writeValueAsString(obj);
	}

	// =======================================================================
	// 1. POST /payments/createUpdatePaymentApprovalConfig
	// =======================================================================
	@Nested
	@DisplayName("POST /createUpdatePaymentApprovalConfig")
	class CreateUpdatePaymentApprovalConfigTests {

		private static final String URL = "/payments/createUpdatePaymentApprovalConfig";

		@Test
		@DisplayName("Should return 200 when config is created/updated")
		void create_success() throws Exception {
			when(paymentApprovalService.createUpdatePaymentApprovalConfig(any())).thenReturn(null);

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
					.content(toJson(buildApprovalConfigModel()))).andExpect(status().isOk());

			verify(paymentApprovalService, times(1)).createUpdatePaymentApprovalConfig(any());
		}

		@Test
		@DisplayName("Should delegate the request body to the service")
		void create_delegatesRequestToService() throws Exception {
			when(paymentApprovalService.createUpdatePaymentApprovalConfig(any())).thenReturn(null);
			PaymentAppprovalConfigModel model = buildApprovalConfigModel();
			model.setPaymentMethodId(99L);

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(model)))
					.andExpect(status().isOk());

			ArgumentCaptor<PaymentAppprovalConfigModel> captor = ArgumentCaptor
					.forClass(PaymentAppprovalConfigModel.class);
			verify(paymentApprovalService).createUpdatePaymentApprovalConfig(captor.capture());
			assertThat(captor.getValue().getPaymentMethodId()).isEqualTo(99L);
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void create_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service")
		void create_serviceThrows_propagates() {
			when(paymentApprovalService.createUpdatePaymentApprovalConfig(any()))
					.thenThrow(new RuntimeException("Invalid approval configuration"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
							.content(toJson(buildApprovalConfigModel()))));
		}
	}

	// =======================================================================
	// 2. GET /payments/getAllPaymentApprovalConfigs/{page}/{size}
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllPaymentApprovalConfigs/{page}/{size}")
	class GetAllPaymentApprovalConfigsTests {

		private static final String URL = "/payments/getAllPaymentApprovalConfigs/{page}/{size}";

		@Test
		@DisplayName("Should return 200 with configuration data")
		void getAll_success() throws Exception {
			when(paymentApprovalService.getPaymentApprovalConfiguration(anyInt(), anyInt(), any())).thenReturn(null);

			mockMvc.perform(get(URL, 0, 10)).andExpect(status().isOk());

			verify(paymentApprovalService).getPaymentApprovalConfiguration(eq(0), eq(10), any());
		}

		@Test
		@DisplayName("Should forward page and size path variables to the service")
		void getAll_correctPagingForwarded() throws Exception {
			when(paymentApprovalService.getPaymentApprovalConfiguration(anyInt(), anyInt(), any())).thenReturn(null);

			mockMvc.perform(get(URL, 2, 5)).andExpect(status().isOk());

			verify(paymentApprovalService).getPaymentApprovalConfiguration(eq(2), eq(5), any());
		}

		@Test
		@DisplayName("Should return 400 for non-numeric path variables")
		void getAll_invalidPaging_returns400() throws Exception {
			mockMvc.perform(get("/payments/getAllPaymentApprovalConfigs/abc/def"))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service")
		void getAll_serviceThrows_propagates() {
			when(paymentApprovalService.getPaymentApprovalConfiguration(anyInt(), anyInt(), any()))
					.thenThrow(new RuntimeException("Lookup failed"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(get(URL, 0, 10)));
		}
	}

	// =======================================================================
	// 3. POST /payments/pay
	// =======================================================================
	@Nested
	@DisplayName("POST /pay")
	class PayTests {

		private static final String URL = "/payments/pay";

		@Test
		@DisplayName("Should return 200 with payment response")
		void pay_success_returns200() throws Exception {
			ResponseEntity<PaymentResponse> serviceResp = ok(
					"Payment with reference number PAM/LN/2026/000001 has been made successfully.",
					buildPaymentResponse(1L));
			when(paymentsService.processPayment(any())).thenReturn(serviceResp);

			MvcResult result = mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildPaymentRequest())))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("PAM/LN/2026/000001");
		}

		@Test
		@DisplayName("Should delegate request body to service")
		void pay_delegatesRequestToService() throws Exception {
			PaymentRequest request = buildPaymentRequest();
			request.setAmount(new BigDecimal("2500"));
			when(paymentsService.processPayment(any())).thenReturn(ok("ok", buildPaymentResponse(1L)));

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)))
					.andExpect(status().isOk());

			ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
			verify(paymentsService).processPayment(captor.capture());
			assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2500"));
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void pay_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions as NestedServletException")
		void pay_serviceThrows_setUpException_propagates() {
			when(paymentsService.processPayment(any()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions(
							"This loan has already been fully paid."));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildPaymentRequest()))));
		}

		@Test
		@DisplayName("Should propagate RuntimeException as NestedServletException")
		void pay_serviceThrows_runtimeException_propagates() {
			when(paymentsService.processPayment(any())).thenThrow(new RuntimeException("Loan not found"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildPaymentRequest()))));
		}

		@Test
		@DisplayName("Service should be called exactly once")
		void pay_serviceCalledOnce() throws Exception {
			when(paymentsService.processPayment(any())).thenReturn(ok("ok", buildPaymentResponse(1L)));

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildPaymentRequest())))
					.andExpect(status().isOk());

			verify(paymentsService, times(1)).processPayment(any());
		}
	}

	// =======================================================================
	// 4. POST /payments/writeOffOrWaiver
	// =======================================================================
	@Nested
	@DisplayName("POST /writeOffOrWaiver")
	class WriteOffOrWaiverTests {

		private static final String URL = "/payments/writeOffOrWaiver";

		@Test
		@DisplayName("Should return 200 when waiver/write-off is processed")
		void writeOff_success() throws Exception {
			when(writeOffService.processWaiverWriteOff(any())).thenReturn(null);

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
					.content(toJson(buildPaymentRequest()))).andExpect(status().isOk());

			verify(writeOffService, times(1)).processWaiverWriteOff(any());
		}

		@Test
		@DisplayName("Should delegate request body to service")
		void writeOff_delegatesRequestToService() throws Exception {
			when(writeOffService.processWaiverWriteOff(any())).thenReturn(null);
			PaymentRequest request = buildPaymentRequest();
			request.setWriteOffWaiverReason("Client passed away");

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)))
					.andExpect(status().isOk());

			ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
			verify(writeOffService).processWaiverWriteOff(captor.capture());
			assertThat(captor.getValue().getWriteOffWaiverReason()).isEqualTo("Client passed away");
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void writeOff_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service")
		void writeOff_serviceThrows_propagates() {
			when(writeOffService.processWaiverWriteOff(any()))
					.thenThrow(new RuntimeException("Write-off processing failed"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildPaymentRequest()))));
		}
	}

	// =======================================================================
	// 5. POST /payments/approvePayment/{id}
	// =======================================================================
	@Nested
	@DisplayName("POST /approvePayment/{id}")
	class ApprovePaymentTests {

		private static final String URL = "/payments/approvePayment/{id}";

		@Test
		@DisplayName("Should return 200 on successful approval")
		void approve_success() throws Exception {
			when(approvalWorkflowService.approvePayment(4L)).thenReturn(null);

			mockMvc.perform(post(URL, 4L)).andExpect(status().isOk());

			verify(approvalWorkflowService).approvePayment(4L);
		}

		@Test
		@DisplayName("Should forward correct id to the workflow service")
		void approve_correctIdForwarded() throws Exception {
			when(approvalWorkflowService.approvePayment(55L)).thenReturn(null);

			mockMvc.perform(post(URL, 55L)).andExpect(status().isOk());

			verify(approvalWorkflowService).approvePayment(55L);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric id")
		void approve_invalidId_returns400() throws Exception {
			mockMvc.perform(post("/payments/approvePayment/abc")).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate exception when approval fails")
		void approve_serviceThrows_propagates() {
			when(approvalWorkflowService.approvePayment(anyLong()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Payment not in approvable state"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 1L)));
		}
	}

	// =======================================================================
	// 6. POST /payments/rejectPayment/{id}
	// =======================================================================
	@Nested
	@DisplayName("POST /rejectPayment/{id}")
	class RejectPaymentTests {

		private static final String URL = "/payments/rejectPayment/{id}";

		@Test
		@DisplayName("Should return 200 on successful rejection")
		void reject_success() throws Exception {
			when(approvalWorkflowService.rejectPayment(6L, "Insufficient documentation")).thenReturn(null);

			mockMvc.perform(post(URL, 6L).param("reason", "Insufficient documentation"))
					.andExpect(status().isOk());

			verify(approvalWorkflowService).rejectPayment(6L, "Insufficient documentation");
		}

		@Test
		@DisplayName("Should pass rejection reason to the service")
		void reject_reasonPassedToService() throws Exception {
			when(approvalWorkflowService.rejectPayment(anyLong(), any())).thenReturn(null);

			mockMvc.perform(post(URL, 1L).param("reason", "Duplicate reference")).andExpect(status().isOk());

			ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
			verify(approvalWorkflowService).rejectPayment(eq(1L), reasonCaptor.capture());
			assertThat(reasonCaptor.getValue()).isEqualTo("Duplicate reference");
		}

		@Test
		@DisplayName("Should return 400 for non-numeric id")
		void reject_invalidId_returns400() throws Exception {
			mockMvc.perform(post("/payments/rejectPayment/abc").param("reason", "test"))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate exception when rejection fails")
		void reject_serviceThrows_propagates() {
			when(approvalWorkflowService.rejectPayment(anyLong(), any()))
					.thenThrow(new RuntimeException("Payment already approved"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 1L).param("reason", "test")));
		}
	}

	// =======================================================================
	// 7. GET /payments/getAllPendingPaymentsPendingApprovals
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllPendingPaymentsPendingApprovals")
	class PendingPaymentsApprovalsTests {

		private static final String URL = "/payments/getAllPendingPaymentsPendingApprovals";

		@Test
		@DisplayName("Should return 200 with paginated pending payments")
		void getPending_success() throws Exception {
			Page<PaymentApprovalResponse> page = new PageImpl<>(Collections.emptyList());
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(eq(0), eq(10), any(), any(), any(),
					eq(false))).thenReturn(page);

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());
		}

		@Test
		@DisplayName("Should apply default date range when dateFrom/dateTo are not provided")
		void getPending_defaultDatesApplied() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(anyInt(), anyInt(), any(), any(),
					any(), eq(false))).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "5")).andExpect(status().isOk());

			ArgumentCaptor<String> dateFromCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<String> dateToCaptor = ArgumentCaptor.forClass(String.class);
			verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(eq(0), eq(5),
					dateFromCaptor.capture(), dateToCaptor.capture(), isNull(), eq(false));
			assertThat(dateFromCaptor.getValue()).matches("\\d{4}-\\d{2}-\\d{2}");
			assertThat(dateToCaptor.getValue()).matches("\\d{4}-\\d{2}-\\d{2}");
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getPending_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should pass false flag (non waiver/write-off) to the workflow service")
		void getPending_usesNonWaiverFlag() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(anyInt(), anyInt(), any(), any(),
					any(), anyBoolean())).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(eq(0), eq(10), any(), any(), any(),
					eq(false));
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getPending_serviceThrows_returnsErrorPayload() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(anyInt(), anyInt(), any(), any(),
					any(), eq(false))).thenThrow(new RuntimeException("DB connection lost"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			String body = result.getResponse().getContentAsString();
			assertThat(body).contains("\"error\":true");
			assertThat(body).contains("DB connection lost");
		}
	}

	// =======================================================================
	// 8. GET /payments/getAllPendingWaiverOrWriteOffPaymentsPendingApprovals
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllPendingWaiverOrWriteOffPaymentsPendingApprovals")
	class PendingWaiverWriteOffApprovalsTests {

		private static final String URL = "/payments/getAllPendingWaiverOrWriteOffPaymentsPendingApprovals";

		@Test
		@DisplayName("Should return 200 with paginated pending waiver/write-off payments")
		void getPending_success() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(eq(0), eq(10), any(), any(), any(),
					eq(true))).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());
		}

		@Test
		@DisplayName("Should pass true flag (waiver/write-off) to the workflow service")
		void getPending_usesWaiverFlag() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(anyInt(), anyInt(), any(), any(),
					any(), anyBoolean())).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(eq(0), eq(10), any(), any(), any(),
					eq(true));
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getPending_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getPending_serviceThrows_returnsErrorPayload() throws Exception {
			when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(anyInt(), anyInt(), any(), any(),
					any(), eq(true))).thenThrow(new RuntimeException("Query timed out"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("\"error\":true");
		}
	}

	// =======================================================================
	// 9. GET /payments/getAllWriteOffOrWaiverPayments
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllWriteOffOrWaiverPayments")
	class GetAllWriteOffOrWaiverPaymentsTests {

		private static final String URL = "/payments/getAllWriteOffOrWaiverPayments";

		@Test
		@DisplayName("Should return 200 with paginated write-off/waiver payments")
		void getAll_success() throws Exception {
			Page<PaymentResponse> page = new PageImpl<>(List.of(buildPaymentResponse(1L)));
			when(paymentsService.getAllWriteOffOrWaiverPayments(eq(0), eq(10), any(), any(), any(), any(), any()))
					.thenReturn(page);

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("content");
		}

		@Test
		@DisplayName("Should apply default date range when not provided")
		void getAll_defaultDatesApplied() throws Exception {
			when(paymentsService.getAllWriteOffOrWaiverPayments(anyInt(), anyInt(), any(), any(), any(), any(),
					any())).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			ArgumentCaptor<String> dateFromCaptor = ArgumentCaptor.forClass(String.class);
			verify(paymentsService).getAllWriteOffOrWaiverPayments(eq(0), eq(10), dateFromCaptor.capture(), any(),
					isNull(), isNull(), isNull());
			assertThat(dateFromCaptor.getValue()).matches("\\d{4}-\\d{2}-\\d{2}");
		}

		@Test
		@DisplayName("Should pass search, paymentMethod and status to service")
		void getAll_withFilters() throws Exception {
			when(paymentsService.getAllWriteOffOrWaiverPayments(eq(0), eq(10), any(), any(), eq("john"),
					eq("MPESA"), eq("COMPLETED"))).thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10").param("search", "john")
					.param("paymentMethod", "MPESA").param("status", "COMPLETED")).andExpect(status().isOk());

			verify(paymentsService).getAllWriteOffOrWaiverPayments(eq(0), eq(10), any(), any(), eq("john"),
					eq("MPESA"), eq("COMPLETED"));
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getAll_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getAll_serviceThrows_returnsErrorPayload() throws Exception {
			when(paymentsService.getAllWriteOffOrWaiverPayments(anyInt(), anyInt(), any(), any(), any(), any(),
					any())).thenThrow(new RuntimeException("Failed to load payments"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("\"error\":true");
		}
	}

	// =======================================================================
	// 10. GET /payments/getAllWriteOffOrWaiverRejectedPayments
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllWriteOffOrWaiverRejectedPayments")
	class GetAllWriteOffOrWaiverRejectedPaymentsTests {

		private static final String URL = "/payments/getAllWriteOffOrWaiverRejectedPayments";

		@Test
		@DisplayName("Should return 200 with paginated rejected write-off/waiver payments")
		void getRejected_success() throws Exception {
			when(paymentsService.getAllWriteOffOrWaiverRejectedPayments(eq(0), eq(10), any(), any(), any(), any(),
					any())).thenReturn(new PageImpl<>(List.of(buildPaymentResponse(3L))));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("content");
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getRejected_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getRejected_serviceThrows_returnsErrorPayload() throws Exception {
			when(paymentsService.getAllWriteOffOrWaiverRejectedPayments(anyInt(), anyInt(), any(), any(), any(),
					any(), any())).thenThrow(new RuntimeException("Unable to fetch rejected payments"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("\"error\":true");
		}
	}

	// =======================================================================
	// 11. POST /payments/manuallyQueryPaymentStatus/{paymentId}
	// =======================================================================
	@Nested
	@DisplayName("POST /manuallyQueryPaymentStatus/{paymentId}")
	class ManuallyQueryPaymentStatusTests {

		private static final String URL = "/payments/manuallyQueryPaymentStatus/{paymentId}";

		@Test
		@DisplayName("Should return 200 with status query result")
		void query_success() throws Exception {
			ResponseEntity<PaymentResponse> serviceResp = ok(
					"Status query completed. Current status: COMPLETED", buildPaymentResponse(7L));
			when(paymentsService.manuallyQueryPaymentStatus(7L)).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 7L)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("Current status");
			verify(paymentsService).manuallyQueryPaymentStatus(7L);
		}

		@Test
		@DisplayName("Should forward correct paymentId to service")
		void query_correctIdForwarded() throws Exception {
			when(paymentsService.manuallyQueryPaymentStatus(42L)).thenReturn(ok("ok", buildPaymentResponse(42L)));

			mockMvc.perform(post(URL, 42L)).andExpect(status().isOk());

			verify(paymentsService).manuallyQueryPaymentStatus(42L);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric paymentId")
		void query_invalidId_returns400() throws Exception {
			mockMvc.perform(post("/payments/manuallyQueryPaymentStatus/abc")).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate exception thrown directly by the service")
		void query_serviceThrows_propagates() {
			when(paymentsService.manuallyQueryPaymentStatus(anyLong()))
					.thenThrow(new RuntimeException("Payment not found"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 999L)));
		}
	}

	// =======================================================================
	// 12. GET /payments/getAllPayments
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllPayments")
	class GetAllPaymentsTests {

		private static final String URL = "/payments/getAllPayments";

		@Test
		@DisplayName("Should return 200 with paginated payments")
		void getAll_success() throws Exception {
			Page<PaymentResponse> page = new PageImpl<>(List.of(buildPaymentResponse(1L), buildPaymentResponse(2L)));
			when(paymentsService.getAllPayments(eq(0), eq(10), any(), any(), any(), any(), any())).thenReturn(page);

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("content");
		}

		@Test
		@DisplayName("Should apply default date range when not provided")
		void getAll_defaultDatesApplied() throws Exception {
			when(paymentsService.getAllPayments(anyInt(), anyInt(), any(), any(), any(), any(), any()))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			ArgumentCaptor<String> dateToCaptor = ArgumentCaptor.forClass(String.class);
			verify(paymentsService).getAllPayments(eq(0), eq(10), any(), dateToCaptor.capture(), isNull(), isNull(),
					isNull());
			assertThat(dateToCaptor.getValue()).matches("\\d{4}-\\d{2}-\\d{2}");
		}

		@Test
		@DisplayName("Should pass search param to service")
		void getAll_withSearch() throws Exception {
			when(paymentsService.getAllPayments(eq(0), eq(10), any(), any(), eq("Smith"), any(), any()))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10").param("search", "Smith"))
					.andExpect(status().isOk());

			verify(paymentsService).getAllPayments(eq(0), eq(10), any(), any(), eq("Smith"), any(), any());
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getAll_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getAll_serviceThrows_returnsErrorPayload() throws Exception {
			when(paymentsService.getAllPayments(anyInt(), anyInt(), any(), any(), any(), any(), any()))
					.thenThrow(new RuntimeException("Database unavailable"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			String body = result.getResponse().getContentAsString();
			assertThat(body).contains("\"error\":true");
			assertThat(body).contains("Database unavailable");
		}
	}

	// =======================================================================
	// 13. GET /payments/getAllRejectedPayments
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllRejectedPayments")
	class GetAllRejectedPaymentsTests {

		private static final String URL = "/payments/getAllRejectedPayments";

		@Test
		@DisplayName("Should return 200 with paginated rejected payments")
		void getRejected_success() throws Exception {
			when(paymentsService.getAllRejectedPayments(eq(0), eq(10), any(), any(), any(), any(), any()))
					.thenReturn(new PageImpl<>(List.of(buildPaymentResponse(9L))));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("content");
		}

		@Test
		@DisplayName("Should return 400 when required page/size params are missing")
		void getRejected_missingPaging_returns400() throws Exception {
			mockMvc.perform(get(URL)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void getRejected_serviceThrows_returnsErrorPayload() throws Exception {
			when(paymentsService.getAllRejectedPayments(anyInt(), anyInt(), any(), any(), any(), any(), any()))
					.thenThrow(new RuntimeException("Unable to fetch rejected payments"));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("\"error\":true");
		}
	}

	// =======================================================================
	// 14. GET /payments/paymentStatistics
	// =======================================================================
	@Nested
	@DisplayName("GET /paymentStatistics")
	class PaymentStatisticsTests {

		private static final String URL = "/payments/paymentStatistics";

		@Test
		@DisplayName("Should return 200 with statistics payload")
		void statistics_success() throws Exception {
			java.util.Map<String, Object> stats = new java.util.HashMap<>();
			stats.put("totalPayments", 10L);
			stats.put("totalAmount", new BigDecimal("100000"));
			when(paymentsService.getPaymentStatistics(any(), any())).thenReturn(stats);

			MvcResult result = mockMvc
					.perform(get(URL).param("dateFrom", "2026-01-01").param("dateTo", "2026-06-17"))
					.andExpect(status().isOk()).andReturn();

			String body = result.getResponse().getContentAsString();
			assertThat(body).contains("totalPayments");
			assertThat(body).contains("Statistics retrieved successfully");
		}

		@Test
		@DisplayName("Should pass dateFrom and dateTo to service")
		void statistics_passesDateRange() throws Exception {
			when(paymentsService.getPaymentStatistics(any(), any())).thenReturn(new java.util.HashMap<>());

			mockMvc.perform(get(URL).param("dateFrom", "2026-01-01").param("dateTo", "2026-06-17"))
					.andExpect(status().isOk());

			verify(paymentsService).getPaymentStatistics("2026-01-01", "2026-06-17");
		}

		@Test
		@DisplayName("Should work without dateFrom/dateTo params (both optional)")
		void statistics_withoutDateRange() throws Exception {
			when(paymentsService.getPaymentStatistics(isNull(), isNull())).thenReturn(new java.util.HashMap<>());

			mockMvc.perform(get(URL)).andExpect(status().isOk());

			verify(paymentsService).getPaymentStatistics(isNull(), isNull());
		}

		@Test
		@DisplayName("Should catch service exceptions and return a 200 response with an error payload")
		void statistics_serviceThrows_returnsErrorPayload() throws Exception {
			when(paymentsService.getPaymentStatistics(any(), any()))
					.thenThrow(new RuntimeException("Stats computation failed"));

			MvcResult result = mockMvc
					.perform(get(URL).param("dateFrom", "2026-01-01").param("dateTo", "2026-06-17"))
					.andExpect(status().isOk()).andReturn();

			String body = result.getResponse().getContentAsString();
			assertThat(body).contains("\"error\":true");
			assertThat(body).contains("Stats computation failed");
		}
	}

	// =======================================================================
	// 15. Cross-cutting: Content-Type
	// =======================================================================
	@Nested
	@DisplayName("Cross-cutting: Content-Type")
	class ContentTypeTests {

		@Test
		@DisplayName("All endpoints should return application/json")
		void endpoints_returnApplicationJson() throws Exception {
			// pay
			when(paymentsService.processPayment(any())).thenReturn(ok("ok", buildPaymentResponse(1L)));
			mockMvc.perform(post("/payments/pay").contentType(MediaType.APPLICATION_JSON)
					.content(toJson(buildPaymentRequest())))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

			// getAllPayments
			when(paymentsService.getAllPayments(anyInt(), anyInt(), any(), any(), any(), any(), any()))
					.thenReturn(new PageImpl<>(Collections.emptyList()));
			mockMvc.perform(get("/payments/getAllPayments").param("page", "0").param("size", "10"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

			// manuallyQueryPaymentStatus
			when(paymentsService.manuallyQueryPaymentStatus(anyLong()))
					.thenReturn(ok("ok", buildPaymentResponse(1L)));
			mockMvc.perform(post("/payments/manuallyQueryPaymentStatus/1"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

			// paymentStatistics
			when(paymentsService.getPaymentStatistics(any(), any())).thenReturn(new java.util.HashMap<>());
			mockMvc.perform(get("/payments/paymentStatistics"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		}
	}
}