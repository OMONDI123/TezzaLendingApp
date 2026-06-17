package co.ke.tezza.loanapp.controller;

import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.enums.PaymentType;
import co.ke.tezza.loanapp.model.PaymentMethodModel;
import co.ke.tezza.loanapp.service.CommonService;
import co.ke.tezza.loanapp.service.MOrgService;
import co.ke.tezza.loanapp.util.DatabaseBackupUtil;
import co.ke.tezza.loanapp.util.DatabaseCleanUpUtil;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CommonController}.
 *
 * Scope (for now): Payment Method Configuration endpoints only
 * (createUpdatePaymentMethod, getPaymentMethodsList, getPaymentMethodsPaginated).
 * SMS config/setup and Reminder config sections will be added once the
 * corresponding request/response/enum classes are available.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommonController – Unit Tests")
class CommonControllerTest {

	// -----------------------------------------------------------------------
	// Mocks
	// -----------------------------------------------------------------------
	@Mock
	private CommonService commonService;
	@Mock
	private MOrgService orgService;
	@Mock
	private DatabaseCleanUpUtil databaseCleanUpUtil;
	@Mock
	private DatabaseBackupUtil databaseBackupUtil;

	@InjectMocks
	private CommonController controller;

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
	private PaymentMethodModel buildPaymentMethodModel() {
		PaymentMethodModel model = new PaymentMethodModel();
		model.setPaymentMethodId(0L);
		model.setName("M-Pesa");
		model.setDescription("Mobile money payments via M-Pesa");
		model.setPaymentType(PaymentType.MPESA);
		return model;
	}

	private MPaymentMethod buildPaymentMethod(long id, String name) {
		MPaymentMethod method = new MPaymentMethod();
		method.setPaymentMethodId(id);
		method.setName(name);
		method.setDescription("Mobile money payments via M-Pesa");
		method.setPaymentType(PaymentType.MPESA);
		return method;
	}

	private ResponseEntity<MPaymentMethod> ok(String message, MPaymentMethod data) {
		return new ResponseEntity<>(message, 200, data);
	}

	private String toJson(Object obj) throws Exception {
		return mapper.writeValueAsString(obj);
	}

	// =======================================================================
	// 1. POST /common/createUpdatePaymentMethod
	// =======================================================================
	@Nested
	@DisplayName("POST /createUpdatePaymentMethod")
	class CreateUpdatePaymentMethodTests {

		private static final String URL = "/common/createUpdatePaymentMethod";

		@Test
		@DisplayName("Should return 200 with the created payment method")
		void create_success() throws Exception {
			ResponseEntity<MPaymentMethod> serviceResp = ok("Payment Method Added Successfully.",
					buildPaymentMethod(1L, "M-Pesa"));
			when(commonService.createUpdatePaymentMethod(any())).thenReturn(serviceResp);

			MvcResult result = mockMvc
					.perform(post(URL).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
							.content(toJson(buildPaymentMethodModel())))
					.andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("M-Pesa")
					.contains("Payment Method Added Successfully.");
		}

		@Test
		@DisplayName("Should delegate the request body to the service")
		void create_delegatesRequestToService() throws Exception {
			when(commonService.createUpdatePaymentMethod(any()))
					.thenReturn(ok("ok", buildPaymentMethod(1L, "Bank Transfer")));
			PaymentMethodModel model = buildPaymentMethodModel();
			model.setName("Bank Transfer");

			mockMvc.perform(post(URL).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
					.content(toJson(model))).andExpect(status().isOk());

			ArgumentCaptor<PaymentMethodModel> captor = ArgumentCaptor.forClass(PaymentMethodModel.class);
			verify(commonService).createUpdatePaymentMethod(captor.capture());
			assertThat(captor.getValue().getName()).isEqualTo("Bank Transfer");
		}

		@Test
		@DisplayName("Should return 400 when request body is missing")
		void create_missingBody_returns400() throws Exception {
			mockMvc.perform(post(URL).contentType(org.springframework.http.MediaType.APPLICATION_JSON))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate SetUpExceptions when payment method name already exists")
		void create_duplicateName_propagates() {
			when(commonService.createUpdatePaymentMethod(any())).thenThrow(
					new co.ke.tezza.loanapp.exceptions.SetUpExceptions("Payment Method M-Pesa already exists."));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(post(URL).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
							.content(toJson(buildPaymentMethodModel()))));
		}

		@Test
		@DisplayName("Service should be called exactly once")
		void create_serviceCalledOnce() throws Exception {
			when(commonService.createUpdatePaymentMethod(any()))
					.thenReturn(ok("ok", buildPaymentMethod(1L, "M-Pesa")));

			mockMvc.perform(post(URL).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
					.content(toJson(buildPaymentMethodModel()))).andExpect(status().isOk());

			verify(commonService, times(1)).createUpdatePaymentMethod(any());
		}
	}

	// =======================================================================
	// 2. GET /common/getPaymentMethodsList
	// =======================================================================
	@Nested
	@DisplayName("GET /getPaymentMethodsList")
	class GetPaymentMethodsListTests {

		private static final String URL = "/common/getPaymentMethodsList";

		@Test
		@DisplayName("Should return list of payment methods")
		void getList_success() throws Exception {
			when(commonService.getPaymentMethodList())
					.thenReturn(List.of(buildPaymentMethod(1L, "M-Pesa"), buildPaymentMethod(2L, "Cash")));

			MvcResult result = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("M-Pesa").contains("Cash");
		}

		@Test
		@DisplayName("Should return empty list when no payment methods configured")
		void getList_emptyList() throws Exception {
			when(commonService.getPaymentMethodList()).thenReturn(Collections.emptyList());

			MvcResult result = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("[]");
		}

		@Test
		@DisplayName("Should return 200 with application/json content type")
		void getList_returnsJsonContentType() throws Exception {
			when(commonService.getPaymentMethodList()).thenReturn(Collections.emptyList());

			mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content()
					.contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON));
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service")
		void getList_serviceThrows_propagates() {
			when(commonService.getPaymentMethodList()).thenThrow(new RuntimeException("DB error"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(get(URL)));
		}
	}

	// =======================================================================
	// 3. GET /common/getPaymentMethodsPaginated/{page}/{size}
	// =======================================================================
	@Nested
	@DisplayName("GET /getPaymentMethodsPaginated/{page}/{size}")
	class GetPaymentMethodsPaginatedTests {

		private static final String URL = "/common/getPaymentMethodsPaginated/{page}/{size}";

		@Test
		@DisplayName("Should return paginated payment methods")
		void getPaginated_success() throws Exception {
			Page<MPaymentMethod> page = new PageImpl<>(List.of(buildPaymentMethod(1L, "M-Pesa")),
					PageRequest.of(0, 10), 1);
			when(commonService.getPaymentMethodsPaginated(0, 10)).thenReturn(page);

			MvcResult result = mockMvc.perform(get(URL, 0, 10)).andExpect(status().isOk()).andReturn();

			assertThat(result.getResponse().getContentAsString()).contains("M-Pesa");
		}

		@Test
		@DisplayName("Should forward page and size to the service")
		void getPaginated_correctPagingForwarded() throws Exception {
			when(commonService.getPaymentMethodsPaginated(2, 5))
					.thenReturn(new PageImpl<>(Collections.emptyList()));

			mockMvc.perform(get(URL, 2, 5)).andExpect(status().isOk());

			verify(commonService).getPaymentMethodsPaginated(2, 5);
		}

		@Test
		@DisplayName("Should return 400 for non-numeric path variables")
		void getPaginated_invalidPaging_returns400() throws Exception {
			mockMvc.perform(get("/common/getPaymentMethodsPaginated/abc/def"))
					.andExpect(status().is4xxClientError());
		}

		@Test
		@DisplayName("Should propagate RuntimeException from service")
		void getPaginated_serviceThrows_propagates() {
			when(commonService.getPaymentMethodsPaginated(anyInt(), anyInt()))
					.thenThrow(new RuntimeException("Lookup failed"));

			assertThrows(org.springframework.web.util.NestedServletException.class,
					() -> mockMvc.perform(get(URL, 0, 10)));
		}
	}
}