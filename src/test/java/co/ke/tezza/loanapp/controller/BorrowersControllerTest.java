package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.model.*;
import co.ke.tezza.loanapp.response.*;
import co.ke.tezza.loanapp.service.BorrowersServices;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowersController – Unit Tests")
class BorrowersControllerTest {

    // -----------------------------------------------------------------------
    // Mocks
    // -----------------------------------------------------------------------
    @Mock
    private BorrowersServices borrowersServices;

    @InjectMocks
    private BorrowersController controller;

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
    private IndividualBorrowerResponse buildIndividualResponse(Long id) {
        IndividualBorrowerResponse response = new IndividualBorrowerResponse();
        response.setIndividualBorrowerId(id);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setPhone("0712345678");
        response.setEmail("john.doe@example.com");
        response.setNationalId("12345678");
        response.setActive(true);
        return response;
    }

    private InstitutionBorrowerResponse buildInstitutionResponse(Long id) {
        InstitutionBorrowerResponse response = new InstitutionBorrowerResponse();
        response.setInstitutionBorrowerId(id);
        response.setInstitutionName("Test Institution");
        response.setRegistrationNumber("REG-001");
        response.setContactPerson("John Doe");
        response.setContactPhone("0712345678");
        response.setContactEmail("inst@example.com");
        response.setActive(true);
        return response;
    }

    private GroupBorrowerResponse buildGroupResponse(Long id) {
        GroupBorrowerResponse response = new GroupBorrowerResponse();
        response.setGroupBorrowerId(id);
        response.setGroupName("Test Group");
        response.setRegistrationNumber("GRP-001");
        response.setContactPhone("0712345678");
        response.setContactEmail("group@example.com");
        response.setActive(true);
        return response;
    }

    private IndividualBorrowerModel buildIndividualRequest() {
        IndividualBorrowerModel model = new IndividualBorrowerModel();
        model.setFirstName("John");
        model.setLastName("Doe");
        model.setPhone("0712345678");
        model.setEmail("john.doe@example.com");
        model.setNationalId("12345678");
        model.setWardId(1L);
        model.setCountryId(1L);
        model.setCountyId(1L);
        model.setSubCountyId(1L);
        return model;
    }

    private InstitutionBorrowerModel buildInstitutionRequest() {
        InstitutionBorrowerModel model = new InstitutionBorrowerModel();
        model.setInstitutionName("Test Institution");
        model.setRegistrationNumber("REG-001");
        model.setContactPerson("John Doe");
        model.setContactPhone("0712345678");
        model.setContactEmail("inst@example.com");
        model.setWardId(1L);
        model.setCountryId(1L);
        model.setCountyId(1L);
        model.setSubCountyId(1L);
        return model;
    }

    private GroupBorrowerModel buildGroupRequest() {
        GroupBorrowerModel model = new GroupBorrowerModel();
        model.setGroupName("Test Group");
        model.setRegistrationNumber("GRP-001");
        model.setContactPhone("0712345678");
        model.setContactEmail("group@example.com");
        model.setWardId(1L);
        model.setCountryId(1L);
        model.setCountyId(1L);
        model.setSubCountyId(1L);
        
        // Add members
        Set<GroupMembers> members = new HashSet<>();
        GroupMembers member1 = new GroupMembers();
        member1.setFirstName("Member");
        member1.setLastName("One");
        member1.setPhoneNumber("0712345678");
        member1.setGroupRepresentative(true);
        members.add(member1);
        model.setMembers(members);
        
        return model;
    }

    private String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    // =======================================================================
    // 1. Individual Borrower Tests
    // =======================================================================

    @Nested
    @DisplayName("Individual Borrower Endpoints")
    class IndividualBorrowerTests {

        @Test
        @DisplayName("GET /getAllBorrowers/{page}/{size} - Should return paginated list")
        void getAllBorrowers_success() throws Exception {
            Page<IndividualBorrowerResponse> page = new PageImpl<>(
                    List.of(buildIndividualResponse(1L), buildIndividualResponse(2L)),
                    PageRequest.of(0, 10), 2);
            
            when(borrowersServices.getAllIndividualBorrowers(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get("/borrowers/getAllBorrowers/0/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("John")
                    .contains("Doe");
            verify(borrowersServices).getAllIndividualBorrowers(0, 10, null);
        }

        @Test
        @DisplayName("GET /getAllBorrowers/{page}/{size} - With searchTerm")
        void getAllBorrowers_withSearchTerm() throws Exception {
            Page<IndividualBorrowerResponse> page = new PageImpl<>(
                    List.of(buildIndividualResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(borrowersServices.getAllIndividualBorrowers(0, 5, "John")).thenReturn(page);

            mockMvc.perform(get("/borrowers/getAllBorrowers/0/5")
                    .param("searchTerm", "John"))
                    .andExpect(status().isOk());

            verify(borrowersServices).getAllIndividualBorrowers(0, 5, "John");
        }

        @Test
        @DisplayName("POST /createUpdateBorrower - Should create borrower")
        void createUpdateBorrower_success() throws Exception {
            IndividualBorrowerResponse response = buildIndividualResponse(1L);
            ResponseEntity<IndividualBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Debtor Registered successfully", 200, response);
            
            when(borrowersServices.createUpdateIndividualBorrowers(any(IndividualBorrowerModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post("/borrowers/createUpdateBorrower")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildIndividualRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Debtor Registered successfully");
            verify(borrowersServices).createUpdateIndividualBorrowers(any(IndividualBorrowerModel.class));
        }

        @Test
        @DisplayName("PUT /deleteBorrower/{id} - Should delete borrower")
        void deleteBorrower_success() throws Exception {
            IndividualBorrowerResponse response = buildIndividualResponse(1L);
            response.setActive(false);
            ResponseEntity<IndividualBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Borrower Deleted Successfully", 200, response);
            
            when(borrowersServices.deleteIndividualBorrowerById(1L)).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(put("/borrowers/deleteBorrower/1"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Borrower Deleted Successfully");
            verify(borrowersServices).deleteIndividualBorrowerById(1L);
        }

        @Test
        @DisplayName("Should propagate exception when service throws error")
        void createUpdateBorrower_serviceThrows_propagates() {
            when(borrowersServices.createUpdateIndividualBorrowers(any(IndividualBorrowerModel.class)))
                    .thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions("National ID already exists"));

            assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
                mockMvc.perform(post("/borrowers/createUpdateBorrower")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildIndividualRequest())));
            });
        }
    }

    // =======================================================================
    // 2. Institution Borrower Tests
    // =======================================================================

    @Nested
    @DisplayName("Institution Borrower Endpoints")
    class InstitutionBorrowerTests {

        @Test
        @DisplayName("GET /getAllInstitutionBorrowers/{page}/{size} - Should return paginated list")
        void getAllInstitutionBorrowers_success() throws Exception {
            Page<InstitutionBorrowerResponse> page = new PageImpl<>(
                    List.of(buildInstitutionResponse(1L), buildInstitutionResponse(2L)),
                    PageRequest.of(0, 10), 2);
            
            when(borrowersServices.getAllInstitutionBorrowers(0, 10, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get("/borrowers/getAllInstitutionBorrowers/0/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Test Institution");
            verify(borrowersServices).getAllInstitutionBorrowers(0, 10, null);
        }

        @Test
        @DisplayName("GET /getAllInstitutionBorrowers/{page}/{size} - With searchTerm")
        void getAllInstitutionBorrowers_withSearchTerm() throws Exception {
            Page<InstitutionBorrowerResponse> page = new PageImpl<>(
                    List.of(buildInstitutionResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(borrowersServices.getAllInstitutionBorrowers(0, 5, "Inst")).thenReturn(page);

            mockMvc.perform(get("/borrowers/getAllInstitutionBorrowers/0/5")
                    .param("searchTerm", "Inst"))
                    .andExpect(status().isOk());

            verify(borrowersServices).getAllInstitutionBorrowers(0, 5, "Inst");
        }

        @Test
        @DisplayName("POST /createUpdateInstitutionBorrower - Should create institution borrower")
        void createUpdateInstitutionBorrower_success() throws Exception {
            InstitutionBorrowerResponse response = buildInstitutionResponse(1L);
            ResponseEntity<InstitutionBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Institution Borrower Created Successfully", 200, response);
            
            when(borrowersServices.createUpdateInstitutionBorrower(any(InstitutionBorrowerModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post("/borrowers/createUpdateInstitutionBorrower")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildInstitutionRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Institution Borrower Created Successfully");
            verify(borrowersServices).createUpdateInstitutionBorrower(any(InstitutionBorrowerModel.class));
        }

        @Test
        @DisplayName("PUT /deleteInstitutionBorrower/{id} - Should delete institution borrower")
        void deleteInstitutionBorrower_success() throws Exception {
            InstitutionBorrowerResponse response = buildInstitutionResponse(1L);
            response.setActive(false);
            ResponseEntity<InstitutionBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Institution Borrower Deleted Successfully", 200, response);
            
            when(borrowersServices.deleteInstitutionBorrowerById(1L)).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(put("/borrowers/deleteInstitutionBorrower/1"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Institution Borrower Deleted Successfully");
            verify(borrowersServices).deleteInstitutionBorrowerById(1L);
        }
    }

    // =======================================================================
    // 3. Group Borrower Tests
    // =======================================================================

    @Nested
    @DisplayName("Group Borrower Endpoints")
    class GroupBorrowerTests {

        @Test
        @DisplayName("GET /getAllGroupBorrowers/{page}/{size} - Should return paginated list")
        void getAllGroupBorrowers_success() throws Exception {
            Page<GroupBorrowerResponse> page = new PageImpl<>(
                    List.of(buildGroupResponse(1L), buildGroupResponse(2L)),
                    PageRequest.of(0, 10), 2);
            
            when(borrowersServices.getAllGroupBorrowers(0, 10, null, null, null)).thenReturn(page);

            MvcResult result = mockMvc.perform(get("/borrowers/getAllGroupBorrowers/0/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Test Group");
            verify(borrowersServices).getAllGroupBorrowers(0, 10, null, null, null);
        }

        @Test
        @DisplayName("GET /getAllGroupBorrowers/{page}/{size} - With searchTerm")
        void getAllGroupBorrowers_withSearchTerm() throws Exception {
            Page<GroupBorrowerResponse> page = new PageImpl<>(
                    List.of(buildGroupResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(borrowersServices.getAllGroupBorrowers(0, 5, "Test", null, null)).thenReturn(page);

            mockMvc.perform(get("/borrowers/getAllGroupBorrowers/0/5")
                    .param("searchTerm", "Test"))
                    .andExpect(status().isOk());

            verify(borrowersServices).getAllGroupBorrowers(0, 5, "Test", null, null);
        }

        @Test
        @DisplayName("GET /getAllGroupBorrowers/{page}/{size} - With filters")
        void getAllGroupBorrowers_withFilters() throws Exception {
            Page<GroupBorrowerResponse> page = new PageImpl<>(
                    List.of(buildGroupResponse(1L)), PageRequest.of(0, 5), 1);
            
            when(borrowersServices.getAllGroupBorrowers(0, 5, "Test", "ACTIVE", "COOPERATIVE"))
                    .thenReturn(page);

            mockMvc.perform(get("/borrowers/getAllGroupBorrowers/0/5")
                    .param("searchTerm", "Test")
                    .param("statusFilter", "ACTIVE")
                    .param("typeFilter", "COOPERATIVE"))
                    .andExpect(status().isOk());

            verify(borrowersServices).getAllGroupBorrowers(0, 5, "Test", "ACTIVE", "COOPERATIVE");
        }

        @Test
        @DisplayName("POST /createUpdateGroupBorrower - Should create group borrower")
        void createUpdateGroupBorrower_success() throws Exception {
            GroupBorrowerResponse response = buildGroupResponse(1L);
            ResponseEntity<GroupBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Group Borrower Created Successfully", 200, response);
            
            when(borrowersServices.createUpdateGroupBorrower(any(GroupBorrowerModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post("/borrowers/createUpdateGroupBorrower")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildGroupRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Group Borrower Created Successfully");
            verify(borrowersServices).createUpdateGroupBorrower(any(GroupBorrowerModel.class));
        }

        @Test
        @DisplayName("PUT /deleteGroupBorrower/{id} - Should delete group borrower")
        void deleteGroupBorrower_success() throws Exception {
            GroupBorrowerResponse response = buildGroupResponse(1L);
            response.setActive(false);
            ResponseEntity<GroupBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Group Borrower Deleted Successfully", 200, response);
            
            when(borrowersServices.deleteGroupBorrowerById(1L)).thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(put("/borrowers/deleteGroupBorrower/1"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .contains("Group Borrower Deleted Successfully");
            verify(borrowersServices).deleteGroupBorrowerById(1L);
        }

        @Test
        @DisplayName("Should return 400 when group has no members")
        void createUpdateGroupBorrower_noMembers_throwsException() {
            GroupBorrowerModel request = buildGroupRequest();
            request.setMembers(new HashSet<>());
            
            when(borrowersServices.createUpdateGroupBorrower(any(GroupBorrowerModel.class)))
                    .thenThrow(new co.ke.tezza.loanapp.exceptions.SetUpExceptions(
                            "Group Registration requires atleast two members"));

            assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
                mockMvc.perform(post("/borrowers/createUpdateGroupBorrower")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)));
            });
        }
    }

    // =======================================================================
    // 4. Cross-cutting Tests
    // =======================================================================

    @Nested
    @DisplayName("Cross-cutting: Content-Type and Response")
    class CrossCuttingTests {

        @Test
        @DisplayName("All endpoints should return application/json")
        void allEndpoints_returnApplicationJson() throws Exception {
            // Individual
            Page<IndividualBorrowerResponse> individualPage = new PageImpl<>(Collections.emptyList());
            when(borrowersServices.getAllIndividualBorrowers(anyInt(), anyInt(), any()))
                    .thenReturn(individualPage);
            
            mockMvc.perform(get("/borrowers/getAllBorrowers/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Institution
            Page<InstitutionBorrowerResponse> institutionPage = new PageImpl<>(Collections.emptyList());
            when(borrowersServices.getAllInstitutionBorrowers(anyInt(), anyInt(), any()))
                    .thenReturn(institutionPage);
            
            mockMvc.perform(get("/borrowers/getAllInstitutionBorrowers/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Group
            Page<GroupBorrowerResponse> groupPage = new PageImpl<>(Collections.emptyList());
            when(borrowersServices.getAllGroupBorrowers(anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(groupPage);
            
            mockMvc.perform(get("/borrowers/getAllGroupBorrowers/0/10"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Response should contain message and status code")
        void response_containsMessageAndCode() throws Exception {
            IndividualBorrowerResponse response = buildIndividualResponse(1L);
            ResponseEntity<IndividualBorrowerResponse> serviceResponse = 
                    new ResponseEntity<>("Success", 200, response);
            
            when(borrowersServices.createUpdateIndividualBorrowers(any(IndividualBorrowerModel.class)))
                    .thenReturn(serviceResponse);

            MvcResult result = mockMvc.perform(post("/borrowers/createUpdateBorrower")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(buildIndividualRequest())))
                    .andExpect(status().isOk())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            assertThat(json).contains("message");
            assertThat(json).contains("200");
        }
    }
}