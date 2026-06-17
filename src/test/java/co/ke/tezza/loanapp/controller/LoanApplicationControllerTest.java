package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.enums.LoanTransactionType;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.LoanApplicationRequest;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.response.FileOutPutResponse;
import co.ke.tezza.loanapp.response.LoanApplicationResponse;
import co.ke.tezza.loanapp.response.LoanStatementResponse;
import co.ke.tezza.loanapp.service.JasperReportingServices;
import co.ke.tezza.loanapp.service.LoanApplicationService;
import co.ke.tezza.loanapp.service.LoanApprovalWorkFlowService;
import co.ke.tezza.loanapp.service.LoanStatementService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link LoanApplicationController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationController – Unit Tests")
class LoanApplicationControllerTest {

	// -----------------------------------------------------------------------
	// Mocks
	// -----------------------------------------------------------------------
	@Mock
	private LoanApplicationService loanApplicationService;
	@Mock
	private LoanStatementService loanStatementService;
	@Mock
	private JasperReportingServices jasper;
	@Mock
	private LoanApprovalWorkFlowService loanApprovalWorkFlowService;

	@InjectMocks
	private LoanApplicationController controller;

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
	private LoanApplicationResponse buildResponse(Long id) {
		LoanApplicationResponse r = new LoanApplicationResponse();
		r.setLoanApplicationId(id);
		r.setDocumentNo("LN/000001");
		r.setAppliedAmount(new BigDecimal("50000"));
		return r;
	}

	private ResponseEntity<LoanApplicationResponse> ok(String message, LoanApplicationResponse data) {
		return new ResponseEntity<>(message, 200, data);
	}

	private LoanApplicationRequest buildRequest() {
		LoanApplicationRequest r = new LoanApplicationRequest();
		r.setLoanProductId(1L);
		r.setIndividualBorrowerId(10L);
		r.setAppliedAmount(new BigDecimal("50000"));
		r.setTermInDays(180);
		r.setExpectedDisbursementDate(new Date());
		return r;
	}

	private LoanStatementResponse buildLoanStatementResponse() {
		LoanStatementResponse response = new LoanStatementResponse();
		response.setStatementId(1L);
		response.setTransactionRef("TXN-001");
		response.setCreditAmount(new BigDecimal("1000"));
		response.setDebitAmount(new BigDecimal("0"));
		response.setTransactionType(LoanTransactionType.REPAYMENT_RECEIVED);
		response.setTransactionDate(LocalDateTime.now());
		response.setBalance(new BigDecimal("5000"));
		return response;
	}

	private FileOutPutResponse buildFileOutPutResponse() {
		FileOutPutResponse response = new FileOutPutResponse();
		response.setFileOutputUrl("http://localhost:8080/reports/statement-123.pdf");
		response.setFileName("loan-statement-123.pdf");
		response.setFilePath("/reports/loan-statement-123.pdf");
		return response;
	}

	private String toJson(Object obj) throws Exception {
		return mapper.writeValueAsString(obj);
	}

	// =======================================================================
	// 1. GET /loanApplication/downloadGuarantorInstructions
	// =======================================================================
	@Nested
	@DisplayName("GET /downloadGuarantorInstructions")
	class DownloadGuarantorInstructionsTests {

		@Test
		@DisplayName("Should invoke service and return 200")
		void download_success() throws Exception {
			doNothing().when(loanApplicationService).downloadGuarantorInstructions(any());

			mockMvc.perform(get("/loanApplication/downloadGuarantorInstructions")).andExpect(status().isOk());

			verify(loanApplicationService, times(1)).downloadGuarantorInstructions(any());
		}

		@Test
		@DisplayName("Should propagate IOException from service")
		void download_serviceThrows_propagates() {
			try {
				doThrow(new IOException("disk error"))
						.when(loanApplicationService).downloadGuarantorInstructions(any());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			assertThrows(IOException.class, () -> {
				mockMvc.perform(get("/loanApplication/downloadGuarantorInstructions"));
			});
		}
	}

	// =======================================================================
	// 2. GET /loanApplication/downloadLoanApplicationCsvTemplate
	// =======================================================================
	@Nested
	@DisplayName("GET /downloadLoanApplicationCsvTemplate")
	class DownloadCsvTemplateTests {

		@Test
		@DisplayName("Should invoke service and return 200")
		void download_success() throws Exception {
			doNothing().when(loanApplicationService).downloadLoanApplicationTemplate(any());

			mockMvc.perform(get("/loanApplication/downloadLoanApplicationCsvTemplate")).andExpect(status().isOk());

			verify(loanApplicationService, times(1)).downloadLoanApplicationTemplate(any());
		}

		@Test
		@DisplayName("Should propagate IOException from service")
		void download_serviceThrows_propagates() {
			try {
				doThrow(new IOException("Template generation failed"))
						.when(loanApplicationService).downloadLoanApplicationTemplate(any());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			assertThrows(IOException.class, () -> {
				mockMvc.perform(get("/loanApplication/downloadLoanApplicationCsvTemplate"));
			});
		}
	}

	// =======================================================================
	// 3. POST /loanApplication/uploadLoansFromCsvFile
	// =======================================================================
	@Nested
	@DisplayName("POST /uploadLoansFromCsvFile")
	class UploadCsvTests {

		private static final String URL = "/loanApplication/uploadLoansFromCsvFile";

		@Test
		@DisplayName("Should return 200 with processed count message")
		void upload_success() throws Exception {
			List<LoanApplicationResponse> responses = List.of(buildResponse(1L), buildResponse(2L));
			ResponseEntity<List<LoanApplicationResponse>> serviceResp = new ResponseEntity<>(
					"2 Applications Uploaded Successfully.", 200, responses);
			when(loanApplicationService.processLoanApplicationsCsv(any())).thenReturn(serviceResp);

			FileUploads fileUpload = new FileUploads();
			fileUpload.setFileName("loans.csv");
			fileUpload.setBase64File("dGVzdA==");
			fileUpload.setMimeType("text/csv");

			MvcResult result = mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(fileUpload)))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("Uploaded Successfully");
			verify(loanApplicationService, times(1)).processLoanApplicationsCsv(any());
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void upload_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service as NestedServletException")
		void upload_serviceThrows_propagates() {
			when(loanApplicationService.processLoanApplicationsCsv(any()))
					.thenThrow(new RuntimeException("CSV processing failed"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
				FileUploads fileUpload = new FileUploads();
				fileUpload.setFileName("loans.csv");
				fileUpload.setBase64File("dGVzdA==");
				fileUpload.setMimeType("text/csv");
				mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(fileUpload)));
			});
		}
	}

	// =======================================================================
	// 4. POST /loanApplication/applyForLoan
	// =======================================================================
	@Nested
	@DisplayName("POST /applyForLoan")
	class ApplyForLoanTests {

		private static final String URL = "/loanApplication/applyForLoan";

		@Test
		@DisplayName("Should return 200 with loan application response")
		void apply_success_returns200() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Loan application successful.",
					buildResponse(1L));
			when(loanApplicationService.applyForLoan(any())).thenReturn(serviceResp);

			MvcResult result = mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildRequest())))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("LN/000001");
		}

		@Test
		@DisplayName("Should delegate request body to service")
		void apply_delegatesRequestToService() throws Exception {
			LoanApplicationRequest request = buildRequest();
			request.setAppliedAmount(new BigDecimal("75000"));
			when(loanApplicationService.applyForLoan(any())).thenReturn(ok("ok", buildResponse(1L)));

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(request)))
					.andExpect(status().isOk());

			ArgumentCaptor<LoanApplicationRequest> captor = ArgumentCaptor.forClass(LoanApplicationRequest.class);
			verify(loanApplicationService).applyForLoan(captor.capture());
			assertThat(captor.getValue().getAppliedAmount()).isEqualByComparingTo(new BigDecimal("75000"));
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void apply_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions as NestedServletException")
		void apply_serviceThrows_propagates() {
			when(loanApplicationService.applyForLoan(any()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Borrower not found"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildRequest()))));
		}

		@Test
		@DisplayName("Service should be called exactly once")
		void apply_serviceCalledOnce() throws Exception {
			when(loanApplicationService.applyForLoan(any())).thenReturn(ok("ok", buildResponse(1L)));

			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(buildRequest())))
					.andExpect(status().isOk());

			verify(loanApplicationService, times(1)).applyForLoan(any());
		}
	}

	// =======================================================================
	// 5. POST /loanApplication/requestAmendment/{applicationId}
	// =======================================================================
	@Nested
	@DisplayName("POST /requestAmendment/{applicationId}")
	class RequestAmendmentTests {

		private static final String URL = "/loanApplication/requestAmendment/{applicationId}";

		@Test
		@DisplayName("Should return 200 with amendment confirmation message")
		void amendment_success() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Amendment Request submitted successfully.",
					buildResponse(5L));
			when(loanApplicationService.requestAmendment(5L)).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 5L)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("Amendment");
			verify(loanApplicationService).requestAmendment(5L);
		}

		@Test
		@DisplayName("Should forward correct applicationId to service")
		void amendment_correctIdForwarded() throws Exception {
			when(loanApplicationService.requestAmendment(99L)).thenReturn(ok("ok", buildResponse(99L)));

			mockMvc.perform(post(URL, 99L)).andExpect(status().isOk());

			verify(loanApplicationService).requestAmendment(99L);
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions when application not found")
		void amendment_notFound_propagates() {
			when(loanApplicationService.requestAmendment(anyLong()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions(
							"The application selected does not exists."));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 999L)));
		}

		@Test
		@DisplayName("Should return 400 for non-numeric applicationId")
		void amendment_invalidId_returns400() throws Exception {
			mockMvc.perform(post("/loanApplication/requestAmendment/abc")).andExpect(status().is4xxClientError());
		}
	}

	// =======================================================================
	// 6. POST /loanApplication/cancelLoan/{loanId}
	// =======================================================================
	@Nested
	@DisplayName("POST /cancelLoan/{loanId}")
	class CancelLoanTests {

		private static final String URL = "/loanApplication/cancelLoan/{loanId}";

		@Test
		@DisplayName("Should return 200 with cancellation message")
		void cancel_success() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Loan has been successfully cancelled.",
					buildResponse(3L));
			when(loanApplicationService.cancelLoan(3L, "No longer needed")).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 3L).param("cancellationReason", "No longer needed"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("cancelled");
			verify(loanApplicationService).cancelLoan(3L, "No longer needed");
		}

		@Test
		@DisplayName("Should pass cancellation reason to service")
		void cancel_reasonPassedToService() throws Exception {
			when(loanApplicationService.cancelLoan(anyLong(), anyString())).thenReturn(ok("ok", buildResponse(1L)));

			mockMvc.perform(post(URL, 1L).param("cancellationReason", "Client request")).andExpect(status().isOk());

			ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
			verify(loanApplicationService).cancelLoan(eq(1L), reasonCaptor.capture());
			assertThat(reasonCaptor.getValue()).isEqualTo("Client request");
		}

		@Test
		@DisplayName("Should return 400 when cancellationReason param is missing")
		void cancel_missingReason_returns400() throws Exception {
			mockMvc.perform(post(URL, 1L)).andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions when loan not found")
		void cancel_notFound_propagates() {
			when(loanApplicationService.cancelLoan(anyLong(), anyString()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Loan not found"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 999L).param("cancellationReason", "Test")));
		}
	}

	// =======================================================================
	// 7. POST /loanApplication/reAssignLoan/{loanId}/{newAssigneeId}
	// =======================================================================
	@Nested
	@DisplayName("POST /reAssignLoan/{loanId}/{newAssigneeId}")
	class ReAssignLoanTests {

		private static final String URL = "/loanApplication/reAssignLoan/{loanId}/{newAssigneeId}";

		@Test
		@DisplayName("Should return 200 with reassignment confirmation")
		void reassign_success() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Loan has been re-assigned.",
					buildResponse(10L));
			when(loanApplicationService.reAssignLoan(10L, 20L)).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 10L, 20L)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("re-assigned");
			verify(loanApplicationService).reAssignLoan(10L, 20L);
		}

		@Test
		@DisplayName("Should forward both loanId and newAssigneeId correctly")
		void reassign_correctIdsForwarded() throws Exception {
			when(loanApplicationService.reAssignLoan(7L, 15L)).thenReturn(ok("ok", buildResponse(7L)));

			mockMvc.perform(post(URL, 7L, 15L)).andExpect(status().isOk());

			verify(loanApplicationService).reAssignLoan(7L, 15L);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric path variables")
		void reassign_invalidIds_returns400() throws Exception {
			mockMvc.perform(post("/loanApplication/reAssignLoan/abc/def")).andExpect(status().is4xxClientError());
		}
	}

	// =======================================================================
	// 8. GET /loanApplication/getLoanAssignees
	// =======================================================================
	@Nested
	@DisplayName("GET /getLoanAssignees")
	class GetLoanAssigneesTests {

		@Test
		@DisplayName("Should return list of assignees")
		void getAssignees_success() throws Exception {
			co.ke.tezza.loanapp.response.User user = new co.ke.tezza.loanapp.response.User();
			user.setUserId(1L);
			user.setFirstName("Jane");
			when(loanApplicationService.getLoanAssignees(null)).thenReturn(List.of(user));

			MvcResult result = mockMvc.perform(get("/loanApplication/getLoanAssignees")).andExpect(status().isOk())
					.andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("Jane");
		}

		@Test
		@DisplayName("Should pass searchTerm to service")
		void getAssignees_withSearchTerm() throws Exception {
			when(loanApplicationService.getLoanAssignees("Jane")).thenReturn(Collections.emptyList());

			mockMvc.perform(get("/loanApplication/getLoanAssignees").param("searchTerm", "Jane"))
					.andExpect(status().isOk());

			verify(loanApplicationService).getLoanAssignees("Jane");
		}
	}

	// =======================================================================
	// 9. GET /loanApplication/getAllApplicationsPendingApprovals
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllApplicationsPendingApprovals")
	class PendingApprovalsTests {

		private static final String URL = "/loanApplication/getAllApplicationsPendingApprovals";

		@Test
		@DisplayName("Should return paginated pending approvals")
		void getPending_success() throws Exception {
			Page<LoanApplicationResponse> page = new PageImpl<>(List.of(buildResponse(1L)), PageRequest.of(0, 10),
					1);
			when(loanApplicationService.getAllApplicationsPendingApprovals(any(), any(), eq(0), eq(10), any()))
					.thenReturn(page);

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("LN/000001");
		}

		@Test
		@DisplayName("Should apply default date range when dateFrom and dateTo are null")
		void getPending_defaultDates_appliedByController() throws Exception {
			Page<LoanApplicationResponse> page = new PageImpl<>(Collections.emptyList());
			when(loanApplicationService.getAllApplicationsPendingApprovals(any(), any(), anyInt(), anyInt(), any()))
					.thenReturn(page);

			mockMvc.perform(get(URL).param("page", "0").param("size", "5")).andExpect(status().isOk());

			verify(loanApplicationService).getAllApplicationsPendingApprovals(notNull(), notNull(), eq(0), eq(5),
					isNull());
		}

		@Test
		@DisplayName("Should pass search param to service")
		void getPending_withSearch() throws Exception {
			when(loanApplicationService.getAllApplicationsPendingApprovals(any(), any(), eq(0), eq(10), eq("John")))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10").param("search", "John"))
					.andExpect(status().isOk());

			verify(loanApplicationService).getAllApplicationsPendingApprovals(any(), any(), eq(0), eq(10),
					eq("John"));
		}
	}

	// =======================================================================
	// 10. POST /loanApplication/approveLoan/{applicationId}
	// =======================================================================
	@Nested
	@DisplayName("POST /approveLoan/{applicationId}")
	class ApproveLoanTests {

		private static final String URL = "/loanApplication/approveLoan/{applicationId}";

		@Test
		@DisplayName("Should return 200 with approval response")
		void approve_success() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Loan approved.", buildResponse(4L));
			when(loanApprovalWorkFlowService.approveLoan(4L)).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 4L)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("approved");
			verify(loanApprovalWorkFlowService).approveLoan(4L);
		}

		@Test
		@DisplayName("Should forward applicationId to approval workflow service")
		void approve_correctIdForwarded() throws Exception {
			when(loanApprovalWorkFlowService.approveLoan(55L)).thenReturn(ok("ok", buildResponse(55L)));

			mockMvc.perform(post(URL, 55L)).andExpect(status().isOk());

			verify(loanApprovalWorkFlowService).approveLoan(55L);
		}

		@Test
		@DisplayName("Should propagate exception when approval fails")
		void approve_serviceThrows_propagates() {
			when(loanApprovalWorkFlowService.approveLoan(anyLong()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Not in approvable state"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL, 1L)));
		}
	}

	// =======================================================================
	// 11. POST /loanApplication/rejectLoan/{applicationId}
	// =======================================================================
	@Nested
	@DisplayName("POST /rejectLoan/{applicationId}")
	class RejectLoanTests {

		private static final String URL = "/loanApplication/rejectLoan/{applicationId}";

		@Test
		@DisplayName("Should return 200 with rejection response")
		void reject_success() throws Exception {
			ResponseEntity<LoanApplicationResponse> serviceResp = ok("Loan rejected.", buildResponse(6L));
			when(loanApprovalWorkFlowService.rejectLoan(6L, "Insufficient income")).thenReturn(serviceResp);

			MvcResult result = mockMvc.perform(post(URL, 6L).param("reason", "Insufficient income"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("rejected");
			verify(loanApprovalWorkFlowService).rejectLoan(6L, "Insufficient income");
		}

		@Test
		@DisplayName("Should pass rejection reason to service")
		void reject_reasonPassedToService() throws Exception {
			when(loanApprovalWorkFlowService.rejectLoan(anyLong(), anyString()))
					.thenReturn(ok("ok", buildResponse(1L)));

			mockMvc.perform(post(URL, 1L).param("reason", "Poor credit history")).andExpect(status().isOk());

			ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
			verify(loanApprovalWorkFlowService).rejectLoan(eq(1L), reasonCaptor.capture());
			assertThat(reasonCaptor.getValue()).isEqualTo("Poor credit history");
		}

		@Test
		@DisplayName("Should return 400 when reason param is missing")
		void reject_missingReason_returns400() throws Exception {
			mockMvc.perform(post(URL, 1L)).andExpect(status().is4xxClientError());
		}
	}

	// =======================================================================
	// 12. GET /loanApplication/getRecentApplications
	// =======================================================================
	@Nested
	@DisplayName("GET /getRecentApplications")
	class RecentApplicationsTests {

		@Test
		@DisplayName("Should return list of recent applications")
		void getRecent_success() throws Exception {
			when(loanApplicationService.getRecentApplications())
					.thenReturn(List.of(buildResponse(1L), buildResponse(2L)));

			MvcResult result = mockMvc.perform(get("/loanApplication/getRecentApplications"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("LN/000001");
		}

		@Test
		@DisplayName("Should return empty list when no recent applications")
		void getRecent_emptyList() throws Exception {
			when(loanApplicationService.getRecentApplications()).thenReturn(Collections.emptyList());

			MvcResult result = mockMvc.perform(get("/loanApplication/getRecentApplications"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("[]");
		}
	}

	// =======================================================================
	// 13. GET /loanApplication/getAllApplications
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllApplications")
	class GetAllApplicationsTests {

		private static final String URL = "/loanApplication/getAllApplications";

		@Test
		@DisplayName("Should return paginated applications")
		void getAll_success() throws Exception {
			Page<LoanApplicationResponse> page = new PageImpl<>(List.of(buildResponse(1L), buildResponse(2L)),
					PageRequest.of(0, 10), 2);
			when(loanApplicationService.getAllApplications(any(), any(), eq(0), eq(10), isNull())).thenReturn(page);

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("content");
		}

		@Test
		@DisplayName("Should apply default date range when not provided")
		void getAll_defaultDatesApplied() throws Exception {
			when(loanApplicationService.getAllApplications(any(), any(), anyInt(), anyInt(), any()))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			verify(loanApplicationService).getAllApplications(notNull(), notNull(), eq(0), eq(10), isNull());
		}

		@Test
		@DisplayName("Should pass search param to service")
		void getAll_withSearch() throws Exception {
			when(loanApplicationService.getAllApplications(any(), any(), eq(0), eq(10), eq("Smith")))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10").param("search", "Smith"))
					.andExpect(status().isOk());

			verify(loanApplicationService).getAllApplications(any(), any(), eq(0), eq(10), eq("Smith"));
		}
	}

	// =======================================================================
	// 14. GET /loanApplication/getAllRejectedApplications
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllRejectedApplications")
	class GetRejectedApplicationsTests {

		private static final String URL = "/loanApplication/getAllRejectedApplications";

		@Test
		@DisplayName("Should return paginated rejected applications")
		void getRejected_success() throws Exception {
			when(loanApplicationService.getAllRejectedApplications(any(), any(), eq(0), eq(10), isNull()))
					.thenReturn(new PageImpl<>(List.of(buildResponse(3L)), PageRequest.of(0, 10), 1));

			MvcResult result = mockMvc.perform(get(URL).param("page", "0").param("size", "10"))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("LN/000001");
		}
	}

	// =======================================================================
	// 15. GET /loanApplication/getAllAmendedApplications
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllAmendedApplications")
	class GetAmendedApplicationsTests {

		private static final String URL = "/loanApplication/getAllAmendedApplications";

		@Test
		@DisplayName("Should return paginated amended applications")
		void getAmended_success() throws Exception {
			when(loanApplicationService.getAllAmendedApplications(any(), any(), eq(0), eq(10), isNull()))
					.thenReturn(new PageImpl<>(List.of(buildResponse(4L)), PageRequest.of(0, 10), 1));

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			verify(loanApplicationService).getAllAmendedApplications(any(), any(), eq(0), eq(10), isNull());
		}
	}

	// =======================================================================
	// 16. GET /loanApplication/getLoansWithBalances
	// =======================================================================
	@Nested
	@DisplayName("GET /getLoansWithBalances")
	class GetLoansWithBalancesTests {

		private static final String URL = "/loanApplication/getLoansWithBalances";

		@Test
		@DisplayName("Should return paginated loans with balances")
		void getWithBalances_success() throws Exception {
			Page<LoanApplicationResponse> page = new PageImpl<>(List.of(buildResponse(5L)), PageRequest.of(0, 10),
					1);
			when(loanApplicationService.getLoansWithBalances(any(), any(), eq(0), eq(10), isNull()))
					.thenReturn(page);

			mockMvc.perform(get(URL).param("page", "0").param("size", "10")).andExpect(status().isOk());

			verify(loanApplicationService).getLoansWithBalances(any(), any(), eq(0), eq(10), isNull());
		}
	}

	// =======================================================================
	// 17. DELETE /loanApplication/deleteApplication/{id}
	// =======================================================================
	@Nested
	@DisplayName("DELETE /deleteApplication/{id}")
	class DeleteApplicationTests {

		private static final String URL = "/loanApplication/deleteApplication/{id}";

		@Test
		@DisplayName("Should return 200 with soft-deleted response")
		void delete_success() throws Exception {
			LoanApplicationResponse deleted = buildResponse(8L);
			when(loanApplicationService.deleteApplication(8L)).thenReturn(deleted);

			MvcResult result = mockMvc.perform(delete(URL, 8L)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("LN/000001");
			verify(loanApplicationService).deleteApplication(8L);
		}

		@Test
		@DisplayName("Should forward correct id to service")
		void delete_correctIdForwarded() throws Exception {
			when(loanApplicationService.deleteApplication(42L)).thenReturn(buildResponse(42L));

			mockMvc.perform(delete(URL, 42L)).andExpect(status().isOk());

			verify(loanApplicationService).deleteApplication(42L);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric id")
		void delete_invalidId_returns400() throws Exception {
			mockMvc.perform(delete("/loanApplication/deleteApplication/not-a-number"))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions when not found")
		void delete_notFound_propagates() {
			when(loanApplicationService.deleteApplication(anyLong()))
					.thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Loan Application not found"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(delete(URL, 99L)));
		}
	}

	// =======================================================================
	// 18. GET /loanApplication/getLoanStatement/{loanId}
	// =======================================================================
	@Nested
	@DisplayName("GET /getLoanStatement/{loanId}")
	class GetLoanStatementTests {

		@Test
		@DisplayName("Should return statement for a given loanId")
		void getStatement_success() throws Exception {
			List<LoanStatementResponse> statements = Arrays.asList(buildLoanStatementResponse(),
					buildLoanStatementResponse());

			when(loanStatementService.getLoanStatement(10L)).thenReturn(statements);

			MvcResult result = mockMvc.perform(get("/loanApplication/getLoanStatement/{loanId}", 10L))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).isNotBlank();
			verify(loanStatementService).getLoanStatement(10L);
		}

		@Test
		@DisplayName("Should return empty list when no statements found")
		void getStatement_emptyList() throws Exception {
			when(loanStatementService.getLoanStatement(10L)).thenReturn(Collections.emptyList());

			MvcResult result = mockMvc.perform(get("/loanApplication/getLoanStatement/{loanId}", 10L))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("[]");
			verify(loanStatementService).getLoanStatement(10L);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric loanId")
		void getStatement_invalidId_returns400() throws Exception {
			mockMvc.perform(get("/loanApplication/getLoanStatement/abc")).andExpect(status().is4xxClientError());
		}
	}

	// =======================================================================
	// 19. GET /loanApplication/getLoanStatementOnly/{loanId}
	// =======================================================================
	@Nested
	@DisplayName("GET /getLoanStatementOnly/{loanId}")
	class GetLoanStatementOnlyTests {

		@Test
		@DisplayName("Should return statement-only data")
		void getStatementOnly_success() throws Exception {
			List<LoanStatementResponse> statements = Arrays.asList(buildLoanStatementResponse());

			when(loanStatementService.getLoanStatementOnly(11L)).thenReturn(statements);

			mockMvc.perform(get("/loanApplication/getLoanStatementOnly/{loanId}", 11L)).andExpect(status().isOk());

			verify(loanStatementService).getLoanStatementOnly(11L);
		}

		@Test
		@DisplayName("Should return empty list when no data")
		void getStatementOnly_emptyList() throws Exception {
			when(loanStatementService.getLoanStatementOnly(11L)).thenReturn(Collections.emptyList());

			MvcResult result = mockMvc.perform(get("/loanApplication/getLoanStatementOnly/{loanId}", 11L))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("[]");
			verify(loanStatementService).getLoanStatementOnly(11L);
		}
	}

	// =======================================================================
	// 20. GET /loanApplication/getInstallmentsByLoan/{loanId}
	// =======================================================================
	@Nested
	@DisplayName("GET /getInstallmentsByLoan/{loanId}")
	class GetInstallmentsTests {

		@Test
		@DisplayName("Should return installments for loanId")
		void getInstallments_success() throws Exception {
			when(loanStatementService.getInstallmentsByLoan(12L)).thenReturn(Collections.emptyList());

			mockMvc.perform(get("/loanApplication/getInstallmentsByLoan/{loanId}", 12L)).andExpect(status().isOk());

			verify(loanStatementService).getInstallmentsByLoan(12L);
		}
	}

	// =======================================================================
	// 21. GET /loanApplication/getAllLoanStatement/{page}/{size}
	// =======================================================================
	@Nested
	@DisplayName("GET /getAllLoanStatement/{page}/{size}")
	class GetAllLoanStatementTests {

		private static final String URL = "/loanApplication/getAllLoanStatement/{page}/{size}";

		@Test
		@DisplayName("Should return paginated loan statements")
		void getAll_success() throws Exception {
			Page<LoanStatementResponse> page = new PageImpl<>(Collections.emptyList());
			when(loanStatementService.getAllLoanStatement(10, 0, null, null)).thenReturn(page);

			mockMvc.perform(get(URL, 0, 10)).andExpect(status().isOk());

			verify(loanStatementService).getAllLoanStatement(10, 0, null, null);
		}

		@Test
		@DisplayName("Should pass searchTerm and transactionType query params")
		void getAll_withQueryParams() throws Exception {
			Page<LoanStatementResponse> page = new PageImpl<>(Collections.emptyList());
			when(loanStatementService.getAllLoanStatement(10, 0, "ref123", "REPAYMENT")).thenReturn(page);

			mockMvc.perform(get(URL, 0, 10).param("searchTerm", "ref123").param("transactionType", "REPAYMENT"))
					.andExpect(status().isOk());

			verify(loanStatementService).getAllLoanStatement(10, 0, "ref123", "REPAYMENT");
		}
	}

	// =======================================================================
	// 22. POST /loanApplication/printLoanStatement
	// =======================================================================
	@Nested
	@DisplayName("POST /printLoanStatement")
	class PrintLoanStatementTests {

		private static final String URL = "/loanApplication/printLoanStatement";

		@Test
		@DisplayName("Should return 200 with report data")
		void print_success() throws Exception {
			FileOutPutResponse fileOutPutResponse = buildFileOutPutResponse();
			ResponseEntity<FileOutPutResponse> serviceResponse = new ResponseEntity<>(
					"Report generated successfully", 200, fileOutPutResponse);
			when(jasper.printLoanStatement(any())).thenReturn(serviceResponse);

			ReportParams params = new ReportParams();
			params.setLoanId(1L);

			MvcResult result = mockMvc
					.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(params)))
					.andExpect(status().isOk()).andReturn();

			String responseContent = result.getResponse().getContentAsString();
			assertThat(responseContent).contains("fileOutputUrl");
			assertThat(responseContent).contains("fileName");
			assertThat(responseContent).contains("filePath");
			verify(jasper).printLoanStatement(any());
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void print_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate exception from Jasper service")
		void print_serviceThrows_propagates() {
			when(jasper.printLoanStatement(any())).thenThrow(new RuntimeException("Report generation failed"));

			assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
				ReportParams params = new ReportParams();
				params.setLoanId(1L);
				mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(params)));
			});
		}
	}

	// =======================================================================
	// 23. Cross-cutting: Content-Type
	// =======================================================================
	@Nested
	@DisplayName("Cross-cutting: Content-Type")
	class ContentTypeTests {

		@Test
		@DisplayName("All JSON endpoints should return application/json")
		void jsonEndpoints_returnApplicationJson() throws Exception {
			// applyForLoan
			when(loanApplicationService.applyForLoan(any())).thenReturn(ok("ok", buildResponse(1L)));
			mockMvc.perform(post("/loanApplication/applyForLoan").contentType(MediaType.APPLICATION_JSON)
					.content(toJson(buildRequest())))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

			// getRecentApplications
			when(loanApplicationService.getRecentApplications()).thenReturn(Collections.emptyList());
			mockMvc.perform(get("/loanApplication/getRecentApplications"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

			// deleteApplication
			when(loanApplicationService.deleteApplication(anyLong())).thenReturn(buildResponse(1L));
			mockMvc.perform(delete("/loanApplication/deleteApplication/1"))
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		}
	}
}