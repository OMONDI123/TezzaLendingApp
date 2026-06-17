package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private PaymentResponse buildPaymentResponse(Long id) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(id);
        response.setAmount(new BigDecimal("1000"));
        response.setReference("PAY-001");
        response.setPaymentDate(LocalDateTime.now().toString());
        response.setPaymentDateTime(LocalDateTime.now());
        response.setActive(true);
        return response;
    }

    private PaymentApprovalResponse buildPaymentApprovalResponse(Long id) {
        PaymentApprovalResponse response = new PaymentApprovalResponse();
        response.setPaymentId(id);
        response.setAmount(new BigDecimal("1000"));
        response.setPaymentDate(LocalDateTime.now().toString());
        response.setPaymentMethod("MPESA");
        response.setApprovalStatus("PENDING");
        response.setRequestedBy("John Doe");
        return response;
    }

    private MPaymentApprovalConfiguration buildMPaymentApprovalConfiguration() {
        MPaymentApprovalConfiguration config = new MPaymentApprovalConfiguration();
        config.setPaymentApprovalConfigId(1L);
        config.setRequiredAprrovalSteps(2);
        return config;
    }

    private PaymentAppprovalConfigModel buildPaymentApprovalConfigRequest() {
        PaymentAppprovalConfigModel model = new PaymentAppprovalConfigModel();
        model.setPaymentMethodId(1L);
        model.setRequiredAprrovalSteps(2);
        
        // Add approval steps
        List<ApprovalStepsModel> steps = new ArrayList<>();
        ApprovalStepsModel step1 = new ApprovalStepsModel();
        step1.setSteps(1);
        step1.setApprovalRoleInvolvedId(10L);
        step1.setNextApprovalRoleId(11L);
        step1.setResponsiblePersonIds(Arrays.asList(1L, 2L));
        steps.add(step1);
        
        ApprovalStepsModel step2 = new ApprovalStepsModel();
        step2.setSteps(2);
        step2.setApprovalRoleInvolvedId(11L);
        step2.setNextApprovalRoleId(12L);
        step2.setResponsiblePersonIds(Arrays.asList(3L, 4L));
        steps.add(step2);
        
        model.setApprovalLevels(steps);
        return model;
    }

    private PaymentRequest buildPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setLoanId(1L);
        request.setAmount(new BigDecimal("1000"));
        request.setPaymentMethod("MPESA");
        request.setReference("TXN-001");
        request.setPhoneNo("0712345678");
        request.setPaymentDateTime(LocalDateTime.now());
        request.setPaymentDate(LocalDateTime.now().toString());
        request.setPaymentModeId(1L);
        request.setPaymentReceivedBy(1L);
        return request;
    }

    private String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    // =======================================================================
    // 1. Payment Approval Configuration Tests
    // =======================================================================

    @Nested
    @DisplayName("Payment Approval Configuration Endpoints")
    class PaymentApprovalConfigTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("POST /createUpdatePaymentApprovalConfig - Should create config")
        void createUpdatePaymentApprovalConfig_success() throws Exception {
            MPaymentApprovalConfiguration config = buildMPaymentApprovalConfiguration();
            ResponseEntity<MPaymentApprovalConfiguration> serviceResponse = 
                    new ResponseEntity<>("Payment Approval Configuration Created Successfully.", 200, config);
            
            when(paymentApprovalService.createUpdatePaymentApprovalConfig(any(PaymentAppprovalConfigModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/createUpdatePaymentApprovalConfig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildPaymentApprovalConfigRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Payment Approval Configuration Created Successfully");
            verify(paymentApprovalService).createUpdatePaymentApprovalConfig(any(PaymentAppprovalConfigModel.class));
        }

        @Test
        @DisplayName("POST /createUpdatePaymentApprovalConfig - Should update existing config")
        void createUpdatePaymentApprovalConfig_update() throws Exception {
            PaymentAppprovalConfigModel request = buildPaymentApprovalConfigRequest();
            request.setPaymentApprovalConfigId(5L);
            
            MPaymentApprovalConfiguration config = buildMPaymentApprovalConfiguration();
            config.setPaymentApprovalConfigId(5L);
            ResponseEntity<MPaymentApprovalConfiguration> serviceResponse = 
                    new ResponseEntity<>("Payment Approval Configuration Updated Successfully.", 200, config);
            
            when(paymentApprovalService.createUpdatePaymentApprovalConfig(any(PaymentAppprovalConfigModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/createUpdatePaymentApprovalConfig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Payment Approval Configuration Updated Successfully");
            verify(paymentApprovalService).createUpdatePaymentApprovalConfig(any(PaymentAppprovalConfigModel.class));
        }

        @Test
        @DisplayName("GET /getAllPaymentApprovalConfigs/{page}/{size} - Should return paginated list")
        void getAllPaymentApprovalConfigs_success() throws Exception {
            Page<MPaymentApprovalConfiguration> page = new PageImpl<>(
                    Arrays.asList(buildMPaymentApprovalConfiguration(), buildMPaymentApprovalConfiguration()), 
                    PageRequest.of(0, 10), 2);
            
            when(paymentApprovalService.getPaymentApprovalConfiguration(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllPaymentApprovalConfigs/0/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).isNotBlank();
            verify(paymentApprovalService).getPaymentApprovalConfiguration(0, 10, null);
        }

        @Test
        @DisplayName("GET /getAllPaymentApprovalConfigs/{page}/{size} - With searchTerm")
        void getAllPaymentApprovalConfigs_withSearchTerm() throws Exception {
            Page<MPaymentApprovalConfiguration> page = new PageImpl<>(
                    Arrays.asList(buildMPaymentApprovalConfiguration()), PageRequest.of(0, 5), 1);
            
            when(paymentApprovalService.getPaymentApprovalConfiguration(0, 5, "MPESA")).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getAllPaymentApprovalConfigs/0/5")
                    .param("searchTerm", "MPESA"))
                    .andExpect(status().isOk());

            verify(paymentApprovalService).getPaymentApprovalConfiguration(0, 5, "MPESA");
        }

        @Test
        @DisplayName("GET /getAllPaymentApprovalConfigs/{page}/{size} - With empty results")
        void getAllPaymentApprovalConfigs_emptyResults() throws Exception {
            Page<MPaymentApprovalConfiguration> page = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
            
            when(paymentApprovalService.getPaymentApprovalConfiguration(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllPaymentApprovalConfigs/0/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("\"content\":[]");
            verify(paymentApprovalService).getPaymentApprovalConfiguration(0, 10, null);
        }
    }

    // =======================================================================
    // 2. Payment Processing Tests
    // =======================================================================

    @Nested
    @DisplayName("Payment Processing Endpoints")
    class PaymentProcessingTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("POST /pay - Should process payment")
        void pay_success() throws Exception {
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Payment processed successfully", 200, response);
            
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildPaymentRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Payment processed successfully");
            verify(paymentsService).processPayment(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("POST /pay - Should handle interest-only payment")
        void pay_interestOnly() throws Exception {
            PaymentRequest request = buildPaymentRequest();
            request.setInterestOnly(true);
            
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Interest-only payment processed", 200, response);
            
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Interest-only payment processed");
            verify(paymentsService).processPayment(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("POST /pay - Should handle penalties-only payment")
        void pay_penaltiesOnly() throws Exception {
            PaymentRequest request = buildPaymentRequest();
            request.setPenaltiesOnly(true);
            
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Penalties-only payment processed", 200, response);
            
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Penalties-only payment processed");
            verify(paymentsService).processPayment(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("POST /writeOffOrWaiver - Should process waiver/write-off")
        void writeOffOrWaiver_success() throws Exception {
            PaymentRequest request = buildPaymentRequest();
            request.setWriteOffWaiverReason("Financial hardship");
            
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Waiver/Write-off processed successfully", 200, response);
            
            when(writeOffService.processWaiverWriteOff(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/writeOffOrWaiver")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Waiver/Write-off processed successfully");
            verify(writeOffService).processWaiverWriteOff(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("POST /writeOffOrWaiver - Should handle wallet deposit")
        void writeOffOrWaiver_walletDeposit() throws Exception {
            PaymentRequest request = buildPaymentRequest();
            request.setWalletDeposit(true);
            request.setIndividualBorrowerId(1L);
            
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Wallet deposit processed", 200, response);
            
            when(writeOffService.processWaiverWriteOff(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/writeOffOrWaiver")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Wallet deposit processed");
            verify(writeOffService).processWaiverWriteOff(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("POST /manuallyQueryPaymentStatus/{paymentId} - Should query payment status")
        void manuallyQueryPaymentStatus_success() throws Exception {
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Status queried successfully", 200, response);
            
            when(paymentsService.manuallyQueryPaymentStatus(1L))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/manuallyQueryPaymentStatus/1"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Status queried successfully");
            verify(paymentsService).manuallyQueryPaymentStatus(1L);
        }

        @Test
        @DisplayName("POST /manuallyQueryPaymentStatus/{paymentId} - Should handle non-existent payment")
        void manuallyQueryPaymentStatus_notFound() throws Exception {
            when(paymentsService.manuallyQueryPaymentStatus(999L))
                    .thenThrow(new RuntimeException("Payment not found"));

            assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
                mockMvc.perform(post(BASE_URL + "/manuallyQueryPaymentStatus/999"));
            });
        }
    }

    // =======================================================================
    // 3. Payment Approval Workflow Tests
    // =======================================================================

    @Nested
    @DisplayName("Payment Approval Workflow Endpoints")
    class PaymentApprovalWorkflowTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("POST /approvePayment/{id} - Should approve payment")
        void approvePayment_success() throws Exception {
            PaymentApprovalResponse response = buildPaymentApprovalResponse(1L);
            ResponseEntity<PaymentApprovalResponse> serviceResponse = 
                    new ResponseEntity<>("Payment approved successfully", 200, response);
            
            when(approvalWorkflowService.approvePayment(1L))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/approvePayment/1"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Payment approved successfully");
            verify(approvalWorkflowService).approvePayment(1L);
        }

        @Test
        @DisplayName("POST /rejectPayment/{id} - Should reject payment with reason")
        void rejectPayment_success() throws Exception {
            PaymentApprovalResponse response = buildPaymentApprovalResponse(1L);
            ResponseEntity<PaymentApprovalResponse> serviceResponse = 
                    new ResponseEntity<>("Payment rejected successfully", 200, response);
            
            when(approvalWorkflowService.rejectPayment(eq(1L), eq("Invalid amount")))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/rejectPayment/1")
                    .param("reason", "Invalid amount"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Payment rejected successfully");
            verify(approvalWorkflowService).rejectPayment(1L, "Invalid amount");
        }

        @Test
        @DisplayName("POST /rejectPayment/{id} - Should handle missing reason")
        void rejectPayment_missingReason() throws Exception {
            PaymentApprovalResponse response = buildPaymentApprovalResponse(1L);
            ResponseEntity<PaymentApprovalResponse> serviceResponse = 
                    new ResponseEntity<>("Payment rejected", 200, response);
            
            when(approvalWorkflowService.rejectPayment(eq(1L), isNull()))
                    .thenReturn(serviceResponse);

            mockMvc.perform(post(BASE_URL + "/rejectPayment/1"))
                    .andExpect(status().isOk());

            verify(approvalWorkflowService).rejectPayment(eq(1L), isNull());
        }
    }

    // =======================================================================
    // 4. Payment Retrieval Tests
    // =======================================================================

    @Nested
    @DisplayName("Payment Retrieval Endpoints")
    class PaymentRetrievalTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("GET /getAllPendingPaymentsPendingApprovals - Should return pending approvals")
        void getAllPendingPaymentsPendingApprovals_success() throws Exception {
            Page<PaymentApprovalResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentApprovalResponse(1L), buildPaymentApprovalResponse(2L)),
                    PageRequest.of(0, 10), 2);
            
            when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(10), anyString(), anyString(), anyString(), eq(false)))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllPendingPaymentsPendingApprovals")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements")
                    .contains("totalPages");
            verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(10), anyString(), anyString(), anyString(), eq(false));
        }

        @Test
        @DisplayName("GET /getAllPendingPaymentsPendingApprovals - With all filters")
        void getAllPendingPaymentsPendingApprovals_withFilters() throws Exception {
            Page<PaymentApprovalResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentApprovalResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(5), anyString(), anyString(), eq("John"), eq(false)))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getAllPendingPaymentsPendingApprovals")
                    .param("page", "0")
                    .param("size", "5")
                    .param("search", "John")
                    .param("paymentMethod", "MPESA")
                    .param("dateFrom", "2024-01-01")
                    .param("dateTo", "2024-12-31"))
                    .andExpect(status().isOk());

            verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(5), anyString(), anyString(), eq("John"), eq(false));
        }

        @Test
        @DisplayName("GET /getAllPendingWaiverOrWriteOffPaymentsPendingApprovals - Should return pending waiver/write-off approvals")
        void getAllPendingWaiverOrWriteOffPaymentsPendingApprovals_success() throws Exception {
            Page<PaymentApprovalResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentApprovalResponse(1L)), PageRequest.of(0, 10), 1);
            
            when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(10), anyString(), anyString(), anyString(), eq(true)))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllPendingWaiverOrWriteOffPaymentsPendingApprovals")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements");
            verify(approvalWorkflowService).getAllPendingPaymentsPendingApprovals(
                    eq(0), eq(10), anyString(), anyString(), anyString(), eq(true));
        }

        @Test
        @DisplayName("GET /getAllPayments - Should return all payments")
        void getAllPayments_success() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentResponse(1L), buildPaymentResponse(2L)),
                    PageRequest.of(0, 10), 2);
            
            when(paymentsService.getAllPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllPayments")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements")
                    .contains("totalPages");
            verify(paymentsService).getAllPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("GET /getAllPayments - With all filters")
        void getAllPayments_withFilters() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(paymentsService.getAllPayments(eq(0), eq(5), anyString(), anyString(), eq("John"), eq("MPESA"), eq("COMPLETED")))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getAllPayments")
                    .param("page", "0")
                    .param("size", "5")
                    .param("search", "John")
                    .param("paymentMethod", "MPESA")
                    .param("status", "COMPLETED")
                    .param("dateFrom", "2024-01-01")
                    .param("dateTo", "2024-12-31"))
                    .andExpect(status().isOk());

            verify(paymentsService).getAllPayments(eq(0), eq(5), anyString(), anyString(), eq("John"), eq("MPESA"), eq("COMPLETED"));
        }

        @Test
        @DisplayName("GET /getAllRejectedPayments - Should return rejected payments")
        void getAllRejectedPayments_success() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentResponse(1L)), PageRequest.of(0, 10), 1);
            
            when(paymentsService.getAllRejectedPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllRejectedPayments")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements");
            verify(paymentsService).getAllRejectedPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("GET /getAllWriteOffOrWaiverPayments - Should return write-off/waiver payments")
        void getAllWriteOffOrWaiverPayments_success() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentResponse(1L)), PageRequest.of(0, 10), 1);
            
            when(paymentsService.getAllWriteOffOrWaiverPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllWriteOffOrWaiverPayments")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements");
            verify(paymentsService).getAllWriteOffOrWaiverPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("GET /getAllWriteOffOrWaiverRejectedPayments - Should return rejected write-off/waiver payments")
        void getAllWriteOffOrWaiverRejectedPayments_success() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    Arrays.asList(buildPaymentResponse(1L)), PageRequest.of(0, 10), 1);
            
            when(paymentsService.getAllWriteOffOrWaiverRejectedPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getAllWriteOffOrWaiverRejectedPayments")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("content")
                    .contains("totalElements");
            verify(paymentsService).getAllWriteOffOrWaiverRejectedPayments(eq(0), eq(10), anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    // =======================================================================
    // 5. Payment Statistics Tests
    // =======================================================================

    @Nested
    @DisplayName("Payment Statistics Endpoints")
    class PaymentStatisticsTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("GET /paymentStatistics - Should return statistics")
        void getPaymentStatistics_success() throws Exception {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPayments", 100);
            statistics.put("totalAmount", new BigDecimal("100000"));
            statistics.put("completedPayments", 80);
            statistics.put("pendingPayments", 20);
            statistics.put("rejectedPayments", 0);
            
            when(paymentsService.getPaymentStatistics(anyString(), anyString()))
                    .thenReturn(statistics);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/paymentStatistics"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("statistics")
                    .contains("totalPayments")
                    .contains("totalAmount")
                    .contains("statusCode")
                    .contains("200");
            verify(paymentsService).getPaymentStatistics(anyString(), anyString());
        }

        @Test
        @DisplayName("GET /paymentStatistics - With date filters")
        void getPaymentStatistics_withDateFilters() throws Exception {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPayments", 50);
            statistics.put("totalAmount", new BigDecimal("50000"));
            
            when(paymentsService.getPaymentStatistics(eq("2024-01-01"), eq("2024-12-31")))
                    .thenReturn(statistics);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/paymentStatistics")
                    .param("dateFrom", "2024-01-01")
                    .param("dateTo", "2024-12-31"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("statistics")
                    .contains("totalPayments");
            verify(paymentsService).getPaymentStatistics("2024-01-01", "2024-12-31");
        }

        @Test
        @DisplayName("GET /paymentStatistics - Should handle service error")
        void getPaymentStatistics_serviceError() throws Exception {
            when(paymentsService.getPaymentStatistics(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            MvcResult result = mockMvc.perform(get(BASE_URL + "/paymentStatistics"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Failed to retrieve payment statistics")
                    .contains("500")
                    .contains("error");
        }
    }

    // =======================================================================
    // 6. Exception Handling Tests
    // =======================================================================

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("Should propagate RuntimeException from service")
        void serviceThrowsException_propagates() {
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenThrow(new RuntimeException("Payment processing failed"));

            assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
                mockMvc.perform(post(BASE_URL + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildPaymentRequest())));
            });
        }

        @Test
        @DisplayName("Should handle invalid request body")
        void invalidRequestBody_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing path variables")
        void missingPathVariable_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/approvePayment/"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should handle non-numeric ID in path")
        void nonNumericId_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/approvePayment/abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // 7. Cross-cutting Tests
    // =======================================================================

    @Nested
    @DisplayName("Cross-cutting: Content-Type and Response")
    class CrossCuttingTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("All endpoints should return application/json")
        void allEndpoints_returnApplicationJson() throws Exception {
            // Payment Approval Config
            when(paymentApprovalService.getPaymentApprovalConfiguration(anyInt(), anyInt(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getAllPaymentApprovalConfigs/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Payments
            when(paymentsService.getAllPayments(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getAllPayments")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Pending Approvals
            when(approvalWorkflowService.getAllPendingPaymentsPendingApprovals(
                    anyInt(), anyInt(), anyString(), anyString(), anyString(), anyBoolean()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getAllPendingPaymentsPendingApprovals")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Statistics
            when(paymentsService.getPaymentStatistics(anyString(), anyString()))
                    .thenReturn(new HashMap<>());
            mockMvc.perform(get(BASE_URL + "/paymentStatistics"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Response should contain statusCode and message")
        void response_containsStatusCodeAndMessage() throws Exception {
            PaymentResponse response = buildPaymentResponse(1L);
            ResponseEntity<PaymentResponse> serviceResponse = 
                    new ResponseEntity<>("Success", 200, response);
            
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildPaymentRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(json).contains("message");
            assertThat(json).contains("200");
        }

        @Test
        @DisplayName("Error response should contain error flag")
        void errorResponse_containsErrorFlag() throws Exception {
            when(paymentsService.getPaymentStatistics(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Service error"));

            MvcResult result = mockMvc.perform(get(BASE_URL + "/paymentStatistics"))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(json).contains("error");
            assertThat(json).contains("true");
            assertThat(json).contains("statusCode");
            assertThat(json).contains("500");
        }
    }

    // =======================================================================
    // 8. Argument Captor Tests    // =======================================================================

    @Nested
    @DisplayName("Argument Captor Tests")
    class ArgumentCaptorTests {

        private static final String BASE_URL = "/payments";

        @Test
        @DisplayName("Should capture and verify PaymentRequest fields")
        void capturePaymentRequest_fields() throws Exception {
            PaymentRequest request = buildPaymentRequest();
            request.setAmount(new BigDecimal("5000"));
            request.setReference("REF-123");
            
            when(paymentsService.processPayment(any(PaymentRequest.class)))
                    .thenReturn(new ResponseEntity<>("Success", 200, buildPaymentResponse(1L)));

            mockMvc.perform(post(BASE_URL + "/pay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
            verify(paymentsService).processPayment(captor.capture());
            
            PaymentRequest captured = captor.getValue();
            assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(captured.getReference()).isEqualTo("REF-123");
            assertThat(captured.getLoanId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should capture and verify PaymentAppprovalConfigModel fields")
        void capturePaymentApprovalConfigModel_fields() throws Exception {
            PaymentAppprovalConfigModel request = buildPaymentApprovalConfigRequest();
            request.setRequiredAprrovalSteps(3);
            
            MPaymentApprovalConfiguration config = buildMPaymentApprovalConfiguration();
            config.setRequiredAprrovalSteps(3);
            
            when(paymentApprovalService.createUpdatePaymentApprovalConfig(any(PaymentAppprovalConfigModel.class)))
                    .thenReturn(new ResponseEntity<>("Success", 200, config));

            mockMvc.perform(post(BASE_URL + "/createUpdatePaymentApprovalConfig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<PaymentAppprovalConfigModel> captor = 
                    ArgumentCaptor.forClass(PaymentAppprovalConfigModel.class);
            verify(paymentApprovalService).createUpdatePaymentApprovalConfig(captor.capture());
            
            PaymentAppprovalConfigModel captured = captor.getValue();
            assertThat(captured.getRequiredAprrovalSteps()).isEqualTo(3);
            assertThat(captured.getPaymentMethodId()).isEqualTo(1L);
            assertThat(captured.getApprovalLevels()).hasSize(2);
        }
    }
}