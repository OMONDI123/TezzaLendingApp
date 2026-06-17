package co.ke.tezza.loanapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.ke.tezza.loanapp.entity.MMessagingCenter;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.MessageForm;
import co.ke.tezza.loanapp.enums.ReceiverCategory;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.model.BorrowerDetails;
import co.ke.tezza.loanapp.model.LoanCommentRequest;
import co.ke.tezza.loanapp.model.MessageRequest;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.model.TriggerMessage;
import co.ke.tezza.loanapp.response.BorrowerWithMessagesResponse;
import co.ke.tezza.loanapp.response.CardexBorrowers;
import co.ke.tezza.loanapp.response.FileOutPutResponse;
import co.ke.tezza.loanapp.response.LoanCommentResponse;
import co.ke.tezza.loanapp.service.ForceSendSmsService;
import co.ke.tezza.loanapp.service.JasperReportingServices;
import co.ke.tezza.loanapp.service.LoanMessagingService;
import co.ke.tezza.loanapp.util.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanMessagingController – Unit Tests")
class LoanMessagingControllerTest {

    @Mock
    private LoanMessagingService loanMessagingService;
    @Mock
    private ForceSendSmsService forceSendSmsService;
    @Mock
    private JasperReportingServices jasper;

    @InjectMocks
    private LoanMessagingController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.findAndRegisterModules();
        // Force this enum to (de)serialize as a plain JSON string (its name), regardless of
        // any extra constructor args it carries internally. Without this, writeValueAsString()
        // produces an object-shaped value for "messageForm" that Spring's Jackson converter then
        // fails to read back ("wrong number of arguments"), causing a 400 on /sendManualMessage.
        mapper.configOverride(MessageForm.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
    }

    // -----------------------------------------------------------------------
    // Builders
    // -----------------------------------------------------------------------
    private LoanCommentRequest buildLoanCommentRequest() {
        LoanCommentRequest request = new LoanCommentRequest();
        request.setLoanId(1L);
        request.setNotes("Test comment");
        request.setActionDate(LocalDate.now());
        request.setCallDateTime(LocalDateTime.now());
        request.setNextCallDate(LocalDateTime.now().plusDays(1));
        return request;
    }

    private LoanCommentResponse buildLoanCommentResponse() {
        LoanCommentResponse response = new LoanCommentResponse();
        response.setCommentId(1L);
        response.setNotes("Test comment");
        return response;
    }

    private MessageRequest buildMessageRequest() {
        MessageRequest request = new MessageRequest();
        request.setMessage("Test message");
        request.setSubject("Test Subject");
        request.setIndividualBorrowerId(Arrays.asList(1L, 2L));
        request.setReceiverCategory(ReceiverCategory.SPECIFIC_OR_INDIVIDUAL_BORROWER);
        request.setMessageForm(MessageForm.SMS);
        request.setSendAlsoGuarantors(false);
        request.setScheduledMessage(false);
        request.setDateToSendMessage(new Date());
        return request;
    }

    private TriggerMessage buildTriggerMessage() {
        TriggerMessage trigger = new TriggerMessage();
        trigger.setMessageType(SmsTypeEnum.LOAN_APPLICATION_OR_DEBT_REGISTRATION);
        trigger.setLoanReferenceNo(Arrays.asList("LN/20000/112"));
        return trigger;
    }

    private CardexBorrowers buildCardexBorrowers() {
        CardexBorrowers cardex = new CardexBorrowers();
        cardex.setBorrowerName("John Doe");
        cardex.setBorrowerNo("BRW-001");
        cardex.setBorrowerType("INDIVIDUAL");
        cardex.setPhoneNumber("0712345678");
        cardex.setBorrowerId(1L);
        cardex.setTotalActiveLoans(2);
        return cardex;
    }

    private BorrowerWithMessagesResponse buildBorrowerWithMessagesResponse() {
        BorrowerWithMessagesResponse response = new BorrowerWithMessagesResponse();
        response.setBorrowerId(1L);
        response.setBorrowerName("John Doe");
        response.setBorrowerType("INDIVIDUAL");
        response.setMessagesSent(5);
        response.setPhone("0712345678");
        response.setEmail("john@example.com");
        response.setBalance(1000.0);
        return response;
    }

    private MMessagingCenter buildMessagingCenter() {
        MMessagingCenter center = new MMessagingCenter();
        center.setMessagingId(1L);
        center.setMessage("Test message");
        center.setPhoneNumber("0712345678");
        return center;
    }

    private MSms buildSms() {
        MSms sms = new MSms();
        sms.setSmsId(1L);
        sms.setMessage("Test SMS");
        sms.setPhoneNo("0712345678");
        return sms;
    }

    private LoanMessagingService.MessageResponse buildMessageResponse() {
        return new LoanMessagingService.MessageResponse("Message has been staged for processing",
                buildMessageRequest());
    }

    private FileOutPutResponse buildFileOutPutResponse() {
        FileOutPutResponse response = new FileOutPutResponse();
        response.setFileOutputUrl("http://localhost:8080/reports/cardex-123.pdf");
        response.setFileName("cardex-123.pdf");
        response.setFilePath("/reports/cardex-123.pdf");
        return response;
    }

    private String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    // =======================================================================
    // 1. Cardex (Loan Comment) Tests
    // =======================================================================

    @Nested
    @DisplayName("Cardex (Loan Comment) Endpoints")
    class CardexTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("POST /saveOrUpdate - Should save cardex")
        void saveOrUpdate_success() throws Exception {
            LoanCommentResponse response = buildLoanCommentResponse();
            ResponseEntity<LoanCommentResponse> serviceResponse = new ResponseEntity<>("Cardex recorded successfully",
                    200, response);

            when(loanMessagingService.saveOrUpdate(any(LoanCommentRequest.class))).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/saveOrUpdate").contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildLoanCommentRequest()))).andExpect(status().isOk()).andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Cardex recorded successfully");
            verify(loanMessagingService).saveOrUpdate(any(LoanCommentRequest.class));
        }

        @Test
        @DisplayName("GET /byLoan - Should get comments by loan")
        void getByLoan_success() throws Exception {
            List<LoanCommentResponse> comments = Arrays.asList(buildLoanCommentResponse());
            when(loanMessagingService.getByLoan(1L)).thenReturn(comments);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/byLoan").param("loanId", "1"))
                    .andExpect(status().isOk()).andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Test comment");
            verify(loanMessagingService).getByLoan(1L);
        }

        @Test
        @DisplayName("GET /byInstallment - Should get comments by installment")
        void getByInstallment_success() throws Exception {
            List<LoanCommentResponse> comments = Arrays.asList(buildLoanCommentResponse());
            when(loanMessagingService.getByInstallment(1L)).thenReturn(comments);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/byInstallment").param("installmentId", "1"))
                    .andExpect(status().isOk()).andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Test comment");
            verify(loanMessagingService).getByInstallment(1L);
        }

        @Test
        @DisplayName("GET /getCardexBorrowers/{page}/{size} - Should return cardex borrowers")
        void getCardexBorrowers_success() throws Exception {
            Page<CardexBorrowers> page = new PageImpl<>(Arrays.asList(buildCardexBorrowers()), PageRequest.of(0, 10),
                    1);

            // searchTerm isn't supplied by this request, so the controller passes null through.
            // anyString() does NOT match null in Mockito, so we use any() here instead.
            when(loanMessagingService.getCardexBorrowers(eq(0), eq(10), any())).thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/getCardexBorrowers/0/10")).andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("John Doe");
            verify(loanMessagingService).getCardexBorrowers(eq(0), eq(10), any());
        }

        @Test
        @DisplayName("GET /getCardexByBorrowerId/{page}/{size} - Should return cardex by borrower")
        void getCardexByBorrowerId_success() throws Exception {
            Page<LoanCommentResponse> page = new PageImpl<>(Arrays.asList(buildLoanCommentResponse()),
                    PageRequest.of(0, 10), 1);

            when(loanMessagingService.getCardexByBorrower(eq("INDIVIDUAL"), eq(1L), any(), any(), eq(0), eq(10)))
                    .thenReturn(page);

            // This endpoint binds page/size via @RequestParam (not @PathVariable), so they must
            // also be supplied as query params in addition to the path segments used for routing.
            mockMvc.perform(get(BASE_URL + "/getCardexByBorrowerId/0/10")
                    .param("page", "0")
                    .param("size", "10")
                    .param("borrowerId", "1")
                    .param("borrowerType", "INDIVIDUAL")).andExpect(status().isOk());

            verify(loanMessagingService).getCardexByBorrower(eq("INDIVIDUAL"), eq(1L), any(), any(), eq(0),
                    eq(10));
        }
    }

    // =======================================================================
    // 2. Messaging Tests
    // =======================================================================

    @Nested
    @DisplayName("Messaging Endpoints")
    class MessagingTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("POST /sendManualMessage - Should send manual message")
        void sendManualMessage_success() throws Exception {
            // Use manual JSON with enums as objects
            String json = "{"
                + "\"message\":\"Test message content\","
                + "\"subject\":\"Test Subject\","
                + "\"messageForm\":{"
                +   "\"value\":\"SMS\","
                +   "\"description\":\"Sms\""
                + "},"
                + "\"receiverCategory\":{"
                +   "\"value\":\"SPECIFIC_OR_INDIVIDUAL_BORROWER\","
                +   "\"description\":\"Individual/Specific Borrower\""
                + "},"
                + "\"individualBorrowerId\":[1,2],"
                + "\"scheduledMessage\":false,"
                + "\"sendAlsoGuarantors\":false"
                + "}";

            MessageRequest request = new MessageRequest();
            request.setMessage("Test message content");
            request.setSubject("Test Subject");
            request.setMessageForm(MessageForm.SMS);
            request.setReceiverCategory(ReceiverCategory.SPECIFIC_OR_INDIVIDUAL_BORROWER);
            request.setIndividualBorrowerId(Arrays.asList(1L, 2L));
            request.setScheduledMessage(false);
            request.setSendAlsoGuarantors(false);

            LoanMessagingService.MessageResponse messageResponse = new LoanMessagingService.MessageResponse(
                "Message has been staged for processing", request);
            org.springframework.http.ResponseEntity<LoanMessagingService.MessageResponse> serviceResponse = 
                    org.springframework.http.ResponseEntity.ok(messageResponse);

            when(loanMessagingService.sendManualMessages(any(MessageRequest.class))).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/sendManualMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Message has been staged for processing");
            verify(loanMessagingService).sendManualMessages(any(MessageRequest.class));
        }

        
        
//        @Test
//        @DisplayName("POST /sendManualMessage - Should handle error response")
//        void sendManualMessage_error() throws Exception {
//            // Use manual JSON with enums as objects - include receiverCategory (UNCOMMENTED)
//            String json = "{"
//                + "\"message\":\"Test message content\","
//                + "\"subject\":\"Test Subject\","
//                + "\"messageForm\":{"
//                +   "\"value\":\"SMS\","
//                +   "\"description\":\"Sms\""
//                + "},"
////                + "\"receiverCategory\":{"
////                +   "\"value\":\"SPECIFIC_OR_INDIVIDUAL_BORROWER\","
////                +   "\"description\":\"Individual/Specific Borrower\""
////                + "},"
//                + "\"individualBorrowerId\":[1,2],"
//                + "\"scheduledMessage\":false,"
//                + "\"sendAlsoGuarantors\":false"
//                + "}";
//
//            LoanMessagingService.MessageResponse errorResponse = new LoanMessagingService.MessageResponse(
//                "Failed to process message: Error", null);
//            org.springframework.http.ResponseEntity<LoanMessagingService.MessageResponse> serviceResponse = 
//                    org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//
//            when(loanMessagingService.sendManualMessages(any(MessageRequest.class))).thenReturn(serviceResponse);
//
//            MvcResult result = mockMvc.perform(post(BASE_URL + "/sendManualMessage")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(json))
//                    .andExpect(status().isInternalServerError())
//                    .andReturn();
//
//            assertThat(result.getResponse().getContentAsString()).contains("Failed to process message");
//            verify(loanMessagingService).sendManualMessages(any(MessageRequest.class));
//        }

        @Test
        @DisplayName("POST /resendAlreadySentLoanReminder - Should resend reminder")
        void resendAlreadySentLoanReminder_success() throws Exception {
            // Stub message and assertion now agree (previously stubbed "Cardex recorded successfully"
            // but asserted on "Reminder resent successfully", which could never pass).
            ResponseEntity<TriggerMessage> messageResponse = new ResponseEntity<>("Reminder resent successfully",
                    200, buildTriggerMessage());
            when(forceSendSmsService.forceSms(any(TriggerMessage.class)))
                    .thenReturn(messageResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/resendAlreadySentLoanReminder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildTriggerMessage())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).contains("Reminder resent successfully");
            verify(forceSendSmsService).forceSms(any(TriggerMessage.class));
        }
    }

    // =======================================================================
    // 3. Borrower Details Tests
    // =======================================================================

    @Nested
    @DisplayName("Borrower Details Endpoints")
    class BorrowerDetailsTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("GET /getBorroweDetails/{page}/{size} - Should return borrower details")
        void getBorroweDetails_success() throws Exception {
            Page<BorrowerDetails> page = new PageImpl<>(Arrays.asList(new BorrowerDetails()), PageRequest.of(0, 10), 1);

            when(loanMessagingService.getBorrowers(eq(0), eq(10), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getBorroweDetails/0/10")).andExpect(status().isOk());

            verify(loanMessagingService).getBorrowers(eq(0), eq(10), any());
        }

        @Test
        @DisplayName("GET /getBorrowersWithMessages/{page}/{size} - Should return borrowers with messages")
        void getBorrowersWithMessages_success() throws Exception {
            Page<BorrowerWithMessagesResponse> page = new PageImpl<>(Arrays.asList(buildBorrowerWithMessagesResponse()),
                    PageRequest.of(0, 10), 1);

            when(loanMessagingService.getBorrowersWithMessages(eq(0), eq(10), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getBorrowersWithMessages/0/10")).andExpect(status().isOk());

            verify(loanMessagingService).getBorrowersWithMessages(eq(0), eq(10), any(), any(), any());
        }
    }

    // =======================================================================
    // 4. Message Center Tests
    // =======================================================================

    @Nested
    @DisplayName("Message Center Endpoints")
    class MessageCenterTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("GET /getMessagesSent/{page}/{size} - Should return messages sent")
        void getMessagesSent_success() throws Exception {
            Page<MMessagingCenter> page = new PageImpl<>(Arrays.asList(buildMessagingCenter()), PageRequest.of(0, 10), 1);

            when(loanMessagingService.getAllMessageCenter(eq(0), eq(10), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getMessagesSent/0/10")).andExpect(status().isOk());

            verify(loanMessagingService).getAllMessageCenter(eq(0), eq(10), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /getMessagesSentByBorrower/{page}/{size} - Should return messages by borrower")
        void getMessagesSentByBorrower_success() throws Exception {
            Page<MMessagingCenter> page = new PageImpl<>(Arrays.asList(buildMessagingCenter()), PageRequest.of(0, 10), 1);

            when(loanMessagingService.getMessagesSentByBorrower(eq(0), eq(10), any(), any(), any(), any(),
                    eq(1L), eq("INDIVIDUAL"))).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getMessagesSentByBorrower/0/10")
                    .param("borrowerId", "1")
                    .param("borrowerType", "INDIVIDUAL"))
                    .andExpect(status().isOk());

            verify(loanMessagingService).getMessagesSentByBorrower(eq(0), eq(10), any(), any(), any(),
                    any(), eq(1L), eq("INDIVIDUAL"));
        }
    }

    // =======================================================================
    // 5. SMS Reminders Tests
    // =======================================================================

    @Nested
    @DisplayName("SMS Reminders Endpoints")
    class SmsRemindersTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("GET /getSmsRemindersSent/{page}/{size} - Should return SMS reminders sent")
        void getSmsRemindersSent_success() throws Exception {
            Page<MSms> page = new PageImpl<>(Arrays.asList(buildSms()), PageRequest.of(0, 10), 1);

            when(loanMessagingService.getSmsRemindersSent(eq(0), eq(10), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getSmsRemindersSent/0/10")).andExpect(status().isOk());

            verify(loanMessagingService).getSmsRemindersSent(eq(0), eq(10), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /getBorrowersWithSmsReminders/{page}/{size} - Should return borrowers with SMS reminders")
        void getBorrowersWithSmsReminders_success() throws Exception {
            Page<BorrowerWithMessagesResponse> page = new PageImpl<>(Arrays.asList(buildBorrowerWithMessagesResponse()),
                    PageRequest.of(0, 10), 1);

            when(loanMessagingService.getBorrowersWithSmsReminders(eq(0), eq(10), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getBorrowersWithSmsReminders/0/10")).andExpect(status().isOk());

            verify(loanMessagingService).getBorrowersWithSmsReminders(eq(0), eq(10), any(), any(), any());
        }

        @Test
        @DisplayName("GET /getSmsRemindersSentByBorrowerId/{page}/{size} - Should return SMS reminders by borrower")
        void getSmsRemindersSentByBorrowerId_success() throws Exception {
            Page<MSms> page = new PageImpl<>(Arrays.asList(buildSms()), PageRequest.of(0, 10), 1);

            when(loanMessagingService.getSmsRemindersSentByBorrowerId(eq(0), eq(10), any(), any(), any(),
                    any(), eq(1L), eq("INDIVIDUAL"))).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/getSmsRemindersSentByBorrowerId/0/10")
                    .param("borrowerId", "1")
                    .param("borrowerType", "INDIVIDUAL"))
                    .andExpect(status().isOk());

            verify(loanMessagingService).getSmsRemindersSentByBorrowerId(eq(0), eq(10), any(), any(), any(),
                    any(), eq(1L), eq("INDIVIDUAL"));
        }
    }

    // =======================================================================
    // 6. Print Cardex Tests
    // =======================================================================

    @Nested
    @DisplayName("Print Cardex Endpoints")
    class PrintCardexTests {

        private static final String URL = "/loanMessages/printCardex";

        @Test
        @DisplayName("POST /printCardex - Should print cardex")
        void printCardex_success() throws Exception {
            FileOutPutResponse fileOutPutResponse = buildFileOutPutResponse();
            ResponseEntity<FileOutPutResponse> serviceResponse = new ResponseEntity<>(
                    "Report generated successfully", 200, fileOutPutResponse);
            when(jasper.printCardex(any())).thenReturn(serviceResponse);

            ReportParams params = new ReportParams();
            params.setLoanId(1L);

            MvcResult result = mockMvc
                    .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(toJson(params)))
                    .andExpect(status().isOk()).andReturn();

            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("fileOutputUrl");
            assertThat(responseContent).contains("fileName");
            assertThat(responseContent).contains("filePath");
            verify(jasper).printCardex(any());
        }
    }

    // =======================================================================
    // 7. Exception Handling Tests
    // =======================================================================

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("Should propagate RuntimeException from service")
        void serviceThrowsException_propagates() {
            when(loanMessagingService.saveOrUpdate(any(LoanCommentRequest.class)))
                    .thenThrow(new RuntimeException("Service error"));

            assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
                mockMvc.perform(post(BASE_URL + "/saveOrUpdate").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildLoanCommentRequest())));
            });
        }

        @Test
        @DisplayName("Should handle invalid request body")
        void invalidRequestBody_returns400() throws Exception {
            mockMvc.perform(
                    post(BASE_URL + "/saveOrUpdate").contentType(MediaType.APPLICATION_JSON).content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =======================================================================
    // 8. Cross-cutting Tests
    // =======================================================================

    @Nested
    @DisplayName("Cross-cutting: Content-Type and Response")
    class CrossCuttingTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("All endpoints should return application/json")
        void allEndpoints_returnApplicationJson() throws Exception {
            // Cardex
            when(loanMessagingService.getCardexBorrowers(anyInt(), anyInt(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getCardexBorrowers/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Messages
            when(loanMessagingService.getAllMessageCenter(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getMessagesSent/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // SMS Reminders
            when(loanMessagingService.getSmsRemindersSent(anyInt(), anyInt(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getSmsRemindersSent/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Borrowers
            when(loanMessagingService.getBorrowers(anyInt(), anyInt(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getBorroweDetails/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Borrowers with Messages
            when(loanMessagingService.getBorrowersWithMessages(anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get(BASE_URL + "/getBorrowersWithMessages/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Response should contain success message")
        void response_containsSuccessMessage() throws Exception {
            LoanCommentResponse response = buildLoanCommentResponse();
            ResponseEntity<LoanCommentResponse> serviceResponse = new ResponseEntity<>("Success", 200, response);

            when(loanMessagingService.saveOrUpdate(any(LoanCommentRequest.class))).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/saveOrUpdate").contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildLoanCommentRequest()))).andExpect(status().isOk()).andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(json).contains("message");
            assertThat(json).contains("200");
        }
    }

    // =======================================================================
    // 9. Argument Captor Tests
    // =======================================================================

    @Nested
    @DisplayName("Argument Captor Tests")
    class ArgumentCaptorTests {

        private static final String BASE_URL = "/loanMessages";

        @Test
        @DisplayName("Should capture and verify LoanCommentRequest fields")
        void captureLoanCommentRequest_fields() throws Exception {
            LoanCommentRequest request = buildLoanCommentRequest();
            request.setNotes("Important note");

            when(loanMessagingService.saveOrUpdate(any(LoanCommentRequest.class)))
                    .thenReturn(new ResponseEntity<>("Success", 200, buildLoanCommentResponse()));

            mockMvc.perform(
                    post(BASE_URL + "/saveOrUpdate").contentType(MediaType.APPLICATION_JSON).content(toJson(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<LoanCommentRequest> captor = ArgumentCaptor.forClass(LoanCommentRequest.class);
            verify(loanMessagingService).saveOrUpdate(captor.capture());

            LoanCommentRequest captured = captor.getValue();
            assertThat(captured.getNotes()).isEqualTo("Important note");
            assertThat(captured.getLoanId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should capture and verify MessageRequest fields")
        void captureMessageRequest_fields() throws Exception {
            // Manually construct JSON with enums as objects {value, description}
            String json = "{"
                + "\"message\":\"Test message content\","
                + "\"subject\":\"Test Subject\","
                + "\"messageForm\":{"
                +   "\"value\":\"SMS\","
                +   "\"description\":\"Sms\""
                + "},"
                + "\"receiverCategory\":{"
                +   "\"value\":\"SPECIFIC_OR_INDIVIDUAL_BORROWER\","
                +   "\"description\":\"Individual/Specific Borrower\""
                + "},"
                + "\"individualBorrowerId\":[1,2],"
                + "\"scheduledMessage\":false,"
                + "\"sendAlsoGuarantors\":false"
                + "}";
            
            System.out.println("JSON being sent: " + json);

            MessageRequest request = buildMessageRequest();
            request.setMessage("Test message content");
            request.setSubject("Test Subject");
            request.setMessageForm(MessageForm.SMS);
            request.setReceiverCategory(ReceiverCategory.SPECIFIC_OR_INDIVIDUAL_BORROWER);

            LoanMessagingService.MessageResponse messageResponse = new LoanMessagingService.MessageResponse(
                "Message has been staged for processing", request);
            org.springframework.http.ResponseEntity<LoanMessagingService.MessageResponse> serviceResponse = 
                    org.springframework.http.ResponseEntity.ok(messageResponse);

            when(loanMessagingService.sendManualMessages(any(MessageRequest.class))).thenReturn(serviceResponse);

            mockMvc.perform(post(BASE_URL + "/sendManualMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<MessageRequest> captor = ArgumentCaptor.forClass(MessageRequest.class);
            verify(loanMessagingService).sendManualMessages(captor.capture());

            MessageRequest captured = captor.getValue();
            assertThat(captured.getMessage()).isEqualTo("Test message content");
            assertThat(captured.getSubject()).isEqualTo("Test Subject");
            assertThat(captured.getIndividualBorrowerId()).hasSize(2);
            assertThat(captured.getReceiverCategory()).isEqualTo(ReceiverCategory.SPECIFIC_OR_INDIVIDUAL_BORROWER);
            assertThat(captured.getMessageForm()).isEqualTo(MessageForm.SMS);
        }
    }
}