package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.enums.*;
import co.ke.tezza.loanapp.model.ApprovalStepsModel;
import co.ke.tezza.loanapp.model.LoanProductConfig;
import co.ke.tezza.loanapp.response.ApprovalStepsResponse;
import co.ke.tezza.loanapp.response.LoanProductConfigResponse;
import co.ke.tezza.loanapp.service.LoanProductConfigurationsService;
import co.ke.tezza.loanapp.util.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link LoanProductConfigurationsController}.
 *
 * Strategy:
 *  - MockMvc is built directly from the controller (standalone) so we avoid
 *    loading the full Spring context and Spring Security filters.
 *  - Security (@PreAuthorize) is intentionally bypassed at this layer; the
 *    securityControllAccessService bean is a separate integration concern.
 *  - All service interactions are verified via Mockito.
 *
 * IMPORTANT – request body serialisation:
 *  The controller uses Spring MVC's Jackson HttpMessageConverter to deserialise
 *  the incoming JSON into LoanProductConfig. Several enums in LoanProductConfig
 *  have non-default (parameterised) constructors, which means:
 *    - Gson serialises them using toString(), which may include constructor
 *      argument values rather than the plain enum name.
 *    - Jackson then fails to deserialise them → 400 BAD_REQUEST.
 *  Fix: use Jackson's ObjectMapper (with WRITE_ENUMS_USING_TO_STRING disabled)
 *  to build POST request bodies so enums are serialised as their .name() value,
 *  which Jackson can reconstruct correctly on the way back in.
 *
 *  Response bodies are still parsed with Gson (or raw string checks) because
 *  the controller returns a Gson-serialised String, not a Spring-managed body.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanProductConfigurationsController – Unit Tests")
class LoanProductConfigurationsControllerTest {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    @Mock
    private LoanProductConfigurationsService loanProductConfigurationsService;

    @InjectMocks
    private LoanProductConfigurationsController controller;

    private MockMvc mockMvc;

    /**
     * Jackson mapper used ONLY for building request bodies.
     * Enums are written as their .name() string (Jackson default), which the
     * controller's own Jackson deserialiser can reconstruct without errors.
     */
    private ObjectMapper requestMapper;

    /** Gson is used to inspect response bodies (the controller writes Gson JSON). */
    private Gson responseGson;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // Standalone MockMvc – no security filter chain, no full context.
        // No ControllerAdvice is registered intentionally: Spring MVC's own
        // DefaultHandlerExceptionResolver handles 4xx (bad body, bad path var),
        // and unhandled application exceptions surface as NestedServletException
        // which we assert in the relevant tests via assertThrows.
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        requestMapper = new ObjectMapper();
        requestMapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        responseGson = new GsonBuilder().create();
    }



    // -----------------------------------------------------------------------
    // Helpers / Builders
    // -----------------------------------------------------------------------

    /**
     * Builds a fully-populated LoanProductConfig using ONLY enum constants that
     * are confirmed to exist from the test-run logs (COMPOUND, APPROVED, etc.).
     * Adjust these to the real enum values in your codebase if they differ.
     */
    private LoanProductConfig buildValidLoanProductConfig() {
        LoanProductConfig config = new LoanProductConfig();
        config.setLoanProductConfigId(0L);
        config.setName("Personal Loan");
        config.setDescription("Standard personal loan product");
        config.setIsDebtProduct(false);

        // Tenure – use FLEXIBLE so min/max days apply
        config.setTenureType(TenureTypeEnum.FLEXIBLE);
        config.setMinTenureDays(30);
        config.setMaxTenureDays(365);
        config.setTenureUnit(TenureUnitEnum.DAYS);

        // Principal
        config.setMinPrincipal(new BigDecimal("5000"));
        config.setMaxPrincipal(new BigDecimal("500000"));

        // Fees
        config.setEnableServiceFee(false);
        config.setEnableDailyFee(false);

        // Interest – COMPOUND confirmed present in passing log
        config.setInterestCalculationMethod(InterestCalculationMethodEnum.COMPOUND);
        config.setInterestFrequency(InterestFrequencyEnum.MONTHLY);
        config.setMonthlyInterestRate(new BigDecimal("2.5"));

        // Repayment
        config.setRepaymentScheduleType(RepaymentScheduleTypeEnum.INSTALLMENTS);
        config.setInstallmentFrequency(InstallmentFrequencyEnum.MONTHLY);

        // Operational
        config.setRequiredApprovalSteps(1);
        config.setAllowEarlyRepayment(true);
        config.setAllowTopUpLoans(false);
        config.setIsDefaultLoanProductConfig(false);
        config.setAutoCloseOnFullPayment(true);
        config.setAllowReinstatement(false);

        // Approval step – APPROVED confirmed present in passing log
        ApprovalStepsModel step = new ApprovalStepsModel();
        step.setSteps(1);
        step.setApprovalRoleInvolvedId(10L);
        step.setTrigureStatus(DocStatus.IN_PROGRESS);
        step.setApprovalStage(ApprovalStage.APPROVED);
        step.setRejectiontrigeredStatus(DocStatus.REJECTED);
        step.setResponsiblePersonIds(List.of(1L));
        config.setApprovalLevels(List.of(step));

        return config;
    }

    private LoanProductConfigResponse buildResponse(Long id, String name) {
        LoanProductConfigResponse r = new LoanProductConfigResponse();
        r.setLoanProductConfigId(id);
        r.setName(name);
        r.setActive(true);
        r.setDocumentNo("LNT/TYP/2025/" + id);
        r.setAD_LoanProductConfiguration_UU(UUID.randomUUID().toString());
        return r;
    }

    private ResponseEntity<LoanProductConfigResponse> ok(String message, LoanProductConfigResponse data) {
        return new ResponseEntity<>(message, 200, data);
    }

    /** Serialize a request object to JSON using Jackson (safe for enum deserialization). */
    private String toRequestJson(Object obj) throws Exception {
        return requestMapper.writeValueAsString(obj);
    }

    // =======================================================================
    // 1. POST /loanProductConfig/createOrUpdateLoanProductConfiguration
    // =======================================================================

    @Nested
    @DisplayName("POST /createOrUpdateLoanProductConfiguration")
    class CreateOrUpdateTests {

        private static final String URL = "/loanProductConfig/createOrUpdateLoanProductConfiguration";

        @Test
        @DisplayName("Should create a new loan product configuration and return HTTP 200")
        void create_success_returns200() throws Exception {
            LoanProductConfigResponse data = buildResponse(1L, "Personal Loan");
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Loan/Debt Product Configuration has been created successfully.", data));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return creation success message in response body")
        void create_success_containsMessage() throws Exception {
            LoanProductConfigResponse data = buildResponse(1L, "Personal Loan");
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Loan/Debt Product Configuration has been created successfully.", data));

            MvcResult result = mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("created successfully")
                    .contains("Personal Loan");
        }

        @Test
        @DisplayName("Should return update success message when loanProductConfigId > 0")
        void update_success_containsUpdateMessage() throws Exception {
            LoanProductConfig request = buildValidLoanProductConfig();
            request.setLoanProductConfigId(5L);
            LoanProductConfigResponse data = buildResponse(5L, "Personal Loan");
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Loan/Debt Product Configuration has been updated successfully.", data));

            MvcResult result = mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("updated successfully");
        }

        @Test
        @DisplayName("Should delegate request body to service and forward correct name")
        void create_delegatesNameToService() throws Exception {
            LoanProductConfig request = buildValidLoanProductConfig();
            request.setName("SME Loan");
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Created.", buildResponse(2L, "SME Loan")));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<LoanProductConfig> captor = ArgumentCaptor.forClass(LoanProductConfig.class);
            verify(loanProductConfigurationsService).createUpdateLoanProductConfig(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("SME Loan");
        }

        @Test
        @DisplayName("Should succeed when approval levels list is empty")
        void create_emptyApprovalLevels_succeeds() throws Exception {
            LoanProductConfig request = buildValidLoanProductConfig();
            request.setRequiredApprovalSteps(0);
            request.setApprovalLevels(Collections.emptyList());
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Created.", buildResponse(3L, "No-Approval Loan")));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should include approvalLevels field in response when steps are present")
        void create_responseContainsApprovalLevels() throws Exception {
            LoanProductConfigResponse data = buildResponse(1L, "Personal Loan");
            ApprovalStepsResponse stepResp = new ApprovalStepsResponse();
            stepResp.setId(1L);
            stepResp.setSteps(1);
            data.setApprovalLevels(Set.of(stepResp));
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Created.", data));

            MvcResult result = mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("approvalLevels");
        }

        @Test
        @DisplayName("Should propagate RuntimeException from service as NestedServletException (no ControllerAdvice in standalone)")
        void create_serviceThrows_propagatesException() {
            // Standalone MockMvc has no @ControllerAdvice to convert unhandled exceptions
            // to a 500 response — they surface as NestedServletException wrapping the cause.
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenThrow(new RuntimeException("Unexpected DB error"));

            org.springframework.web.util.NestedServletException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            org.springframework.web.util.NestedServletException.class,
                            () -> mockMvc.perform(post(URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toRequestJson(buildValidLoanProductConfig()))));

            assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("Unexpected DB error");
        }

        @Test
        @DisplayName("Should return HTTP 400 when request body is absent")
        void create_missingBody_returns400() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Service should be called exactly once per request")
        void create_serviceCalledExactlyOnce() throws Exception {
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Created.", buildResponse(1L, "X")));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(status().isOk());

            verify(loanProductConfigurationsService, times(1)).createUpdateLoanProductConfig(any());
        }
    }

    // =======================================================================
    // 2. GET /loanProductConfig/getAllLoanProductConfiguration/{page}/{size}
    // =======================================================================

    @Nested
    @DisplayName("GET /getAllLoanProductConfiguration/{page}/{size}")
    class GetAllTests {

        private static final String URL = "/loanProductConfig/getAllLoanProductConfiguration/{page}/{size}";

        @Test
        @DisplayName("Should return HTTP 200 with paginated list")
        void getAll_noSearchTerm_returns200() throws Exception {
            Page<LoanProductConfigResponse> page = new PageImpl<>(
                    List.of(buildResponse(1L, "Personal Loan"), buildResponse(2L, "Business Loan")),
                    PageRequest.of(0, 10), 2);
            when(loanProductConfigurationsService.getAllLoanProductConfigs(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get(URL, 0, 10))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Personal Loan")
                    .contains("Business Loan");
            verify(loanProductConfigurationsService).getAllLoanProductConfigs(0, 10, null);
        }

        @Test
        @DisplayName("Should pass searchTerm query param through to the service")
        void getAll_withSearchTerm_passedToService() throws Exception {
            Page<LoanProductConfigResponse> page = new PageImpl<>(
                    List.of(buildResponse(1L, "SME Loan")), PageRequest.of(0, 5), 1);
            when(loanProductConfigurationsService.getAllLoanProductConfigs(0, 5, "SME")).thenReturn(page);

            mockMvc.perform(get(URL, 0, 5).param("searchTerm", "SME"))
                    .andExpect(status().isOk());

            verify(loanProductConfigurationsService).getAllLoanProductConfigs(0, 5, "SME");
        }

        @Test
        @DisplayName("Should return empty content array when no records exist")
        void getAll_emptyResult_containsEmptyContent() throws Exception {
            when(loanProductConfigurationsService.getAllLoanProductConfigs(0, 10, null))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0));

            MvcResult result = mockMvc.perform(get(URL, 0, 10))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("\"content\":[]");
        }

        @Test
        @DisplayName("Should forward page=2, size=25 path variables to the service correctly")
        void getAll_pageAndSizeForwarded() throws Exception {
            when(loanProductConfigurationsService.getAllLoanProductConfigs(2, 25, null))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 25), 0));

            mockMvc.perform(get(URL, 2, 25)).andExpect(status().isOk());

            verify(loanProductConfigurationsService).getAllLoanProductConfigs(2, 25, null);
        }

        @Test
        @DisplayName("Should handle large page numbers without error")
        void getAll_largePageNumber_returns200() throws Exception {
            when(loanProductConfigurationsService.getAllLoanProductConfigs(999, 10, null))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(999, 10), 0));

            mockMvc.perform(get(URL, 999, 10)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should propagate RuntimeException from service as NestedServletException (no ControllerAdvice in standalone)")
        void getAll_serviceThrows_propagatesException() {
            when(loanProductConfigurationsService.getAllLoanProductConfigs(anyInt(), anyInt(), any()))
                    .thenThrow(new RuntimeException("DB error"));

            org.springframework.web.util.NestedServletException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            org.springframework.web.util.NestedServletException.class,
                            () -> mockMvc.perform(get(URL, 0, 10)));

            assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("DB error");
        }

        @Test
        @DisplayName("Response JSON should contain Spring Page metadata fields")
        void getAll_responseContainsPageMetadata() throws Exception {
            Page<LoanProductConfigResponse> page = new PageImpl<>(
                    List.of(buildResponse(1L, "Loan X")), PageRequest.of(0, 10), 1);
            when(loanProductConfigurationsService.getAllLoanProductConfigs(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get(URL, 0, 10))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            // Gson serialises Spring's Page as: {"total":N,"content":[...],"pageable":{...}}
            assertThat(json).contains("\"total\"").contains("\"content\"").contains("\"pageable\"");
        }
    }

    // =======================================================================
    // 3. DELETE /loanProductConfig/deleteLoanProductConfige/{id}
    // =======================================================================

    @Nested
    @DisplayName("DELETE /deleteLoanProductConfige/{id}")
    class DeleteTests {

        private static final String URL = "/loanProductConfig/deleteLoanProductConfige/{id}";

        @Test
        @DisplayName("Should return HTTP 200 with deletion success message")
        void delete_success_returns200WithMessage() throws Exception {
            LoanProductConfigResponse data = buildResponse(1L, "Loan A");
            data.setActive(false);
            when(loanProductConfigurationsService.deleteLoanProductConfig(1L))
                    .thenReturn(ok("Loan product configuration has been deleted successfully.", data));

            MvcResult result = mockMvc.perform(delete(URL, 1L))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("deleted successfully");
            verify(loanProductConfigurationsService, times(1)).deleteLoanProductConfig(1L);
        }

        @Test
        @DisplayName("Should forward the correct numeric ID to the service")
        void delete_correctIdForwardedToService() throws Exception {
            LoanProductConfigResponse data = buildResponse(42L, "Loan B");
            data.setActive(false);
            when(loanProductConfigurationsService.deleteLoanProductConfig(42L))
                    .thenReturn(ok("Deleted.", data));

            mockMvc.perform(delete(URL, 42L)).andExpect(status().isOk());

            verify(loanProductConfigurationsService).deleteLoanProductConfig(42L);
        }

        @Test
        @DisplayName("Should propagate SetUpExceptions as NestedServletException when config not found")
        void delete_notFound_propagatesException() {
            when(loanProductConfigurationsService.deleteLoanProductConfig(99L))
                    .thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions(
                            "Loan product configuration not found"));

            org.springframework.web.util.NestedServletException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            org.springframework.web.util.NestedServletException.class,
                            () -> mockMvc.perform(delete(URL, 99L)));

            assertThat(ex.getCause())
                    .isInstanceOf(co.ke.tezza.loanapp.exceptions.SetUpExceptions.class);
            assertThat(ex.getCause().getMessage())
                    .isEqualTo("Loan product configuration not found");
        }

        @Test
        @DisplayName("Response body should show active:false after soft-delete")
        void delete_responseShowsInactiveFlag() throws Exception {
            LoanProductConfigResponse data = buildResponse(7L, "Loan C");
            data.setActive(false);
            when(loanProductConfigurationsService.deleteLoanProductConfig(7L))
                    .thenReturn(ok("Deleted.", data));

            MvcResult result = mockMvc.perform(delete(URL, 7L))
                    .andExpect(status().isOk())
                    .andReturn();

            // The response entity uses field name 'isActive' (Lombok @Data on boolean isActive)
            assertThat(result.getResponse().getContentAsString()).contains("\"isActive\":false");
        }

        @Test
        @DisplayName("Should return HTTP 400 when id path variable is not a valid long")
        void delete_nonNumericId_returns400() throws Exception {
            mockMvc.perform(delete("/loanProductConfig/deleteLoanProductConfige/not-a-number"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Service should be called exactly once per delete request")
        void delete_serviceCalledExactlyOnce() throws Exception {
            LoanProductConfigResponse data = buildResponse(5L, "X");
            data.setActive(false);
            when(loanProductConfigurationsService.deleteLoanProductConfig(5L))
                    .thenReturn(ok("Deleted.", data));

            mockMvc.perform(delete(URL, 5L)).andExpect(status().isOk());

            verify(loanProductConfigurationsService, times(1)).deleteLoanProductConfig(5L);
        }
    }

    // =======================================================================
    // 4. Cross-cutting / serialisation
    // =======================================================================

    @Nested
    @DisplayName("Cross-cutting: content type and response wrapper")
    class SerializationTests {

        @Test
        @DisplayName("All three endpoints should return Content-Type: application/json")
        void allEndpoints_returnApplicationJson() throws Exception {
            LoanProductConfigResponse data = buildResponse(1L, "X");

            // POST
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("ok", data));
            mockMvc.perform(post("/loanProductConfig/createOrUpdateLoanProductConfiguration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // GET
            when(loanProductConfigurationsService.getAllLoanProductConfigs(anyInt(), anyInt(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/loanProductConfig/getAllLoanProductConfiguration/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // DELETE
            data.setActive(false);
            when(loanProductConfigurationsService.deleteLoanProductConfig(anyLong()))
                    .thenReturn(ok("ok", data));
            mockMvc.perform(delete("/loanProductConfig/deleteLoanProductConfige/1"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("ResponseEntity wrapper should include 'message' and status code '200'")
        void create_responseWrapperContainsMessageAndCode() throws Exception {
            when(loanProductConfigurationsService.createUpdateLoanProductConfig(any()))
                    .thenReturn(ok("Loan/Debt Product Configuration has been created successfully.",
                            buildResponse(1L, "Personal Loan")));

            MvcResult result = mockMvc.perform(
                            post("/loanProductConfig/createOrUpdateLoanProductConfiguration")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toRequestJson(buildValidLoanProductConfig())))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(json).contains("message");
            assertThat(json).contains("200");
        }
    }
}