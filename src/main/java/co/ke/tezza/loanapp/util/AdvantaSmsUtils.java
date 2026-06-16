package co.ke.tezza.loanapp.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.ke.tezza.loanapp.entity.MSMSConfig;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.repository.SmsConfigRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;

@Component
public class AdvantaSmsUtils {
	@Autowired
	private SmsConfigRepository smsConfigRepository;

	@Autowired
	private SmsRepository smsRepository;

//	================ADVANTA SMS INTEGRATIONS==========================
	// --- Single SMS (GET) ---
	public void sendSmsAdvanta(MSms sms) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(sms.getAdOrgID(),
					true);

			if (config == null) {
				System.err.println("❌ No active SMS config found for orgId: " + sms.getAdOrgID());

				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				sms.setReason("No SMS configuration found");
				sms.setResponseCode(String.valueOf(0));
				sms.setApprovalStage(ApprovalStage.CANCELLED);
				smsRepository.save(sms);
				return;
			}

			// Prepare request parameters
			String apiKey = config.getApiKey().trim();
			String partnerId = config.getPartnerId().trim();
			String senderId = config.getSenderId().trim();
			String baseUrl = config.getSmsBaseUrl().trim();
			String phoneNo = sms.getPhoneNo().trim();
			String message = sms.getMessage().trim();

			String params = String.format("apikey=%s&partnerID=%s&message=%s&shortcode=%s&mobile=%s",
					URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
					URLEncoder.encode(partnerId, StandardCharsets.UTF_8),
					URLEncoder.encode(message, StandardCharsets.UTF_8),
					URLEncoder.encode(senderId, StandardCharsets.UTF_8),
					URLEncoder.encode(phoneNo, StandardCharsets.UTF_8));

			String fullUrl = baseUrl + "/api/services/sendsms?" + params;

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String responseBody = response.body();

			System.out.println("📨 Bulk SMS Response ====== " + responseBody);

			// --- Parse JSON ---
			JSONObject json = new JSONObject(responseBody);
			JSONArray responses = json.getJSONArray("responses");
			JSONObject first = responses.getJSONObject(0);

			int code = first.getInt("response-code");
			String desc = first.getString("response-description");

			if (code == 200) {
				sms.setDocStatus(DocStatus.APPROVED);
				sms.setApprovalStage(ApprovalStage.APPROVED);
				sms.setMessageStatus(MessageStatus.SENT);
				sms.setReason(desc);
				sms.setResponseCode(String.valueOf(code));
			} else {
				// --- FAILURE ---
				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				sms.setReason(desc);
				sms.setResponseCode(String.valueOf(code));
				sms.setApprovalStage(ApprovalStage.CANCELLED);
			}

			smsRepository.save(sms);

		} catch (Exception e) {
			e.printStackTrace();

			sms.setDocStatus(DocStatus.REJECTED);
			sms.setMessageStatus(MessageStatus.FAILED);
			sms.setReason("Exception: " + e.getMessage());
			smsRepository.save(sms);
		}
	}

	// --- Bulk SMS with scheduling (POST) ---
	// --- Bulk SMS (POST) ---
	public void sendBulkSmsAdvanta(MSms sms) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(sms.getAdOrgID(),
					true);

			if (config == null || sms.getPhoneNo() == null || sms.getMessage() == null) {
				System.err.println("❌ No active SMS configuration found for orgId: " + sms.getAdOrgID());

				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				if (config == null) {
					sms.setReason("No SMS configuration found");
				}
				if (sms.getPhoneNo() == null) {
					sms.setReason("Phone number should never be null or empty");
				}
				if (sms.getMessage() == null) {
					sms.setReason("Message should never be null or empty");
				}

				sms.setResponseCode(String.valueOf(0));
				sms.setApprovalStage(ApprovalStage.CANCELLED);
				smsRepository.save(sms);
				return;
			}

			// Prepare payload
			String apiKey = config.getApiKey().trim();
			String partnerId = config.getPartnerId().trim();
			String senderId = config.getSenderId().trim();
			String baseUrl = config.getSmsBaseUrl().trim();
			String phoneNo = sms.getPhoneNo().trim();
			String message = sms.getMessage().trim();

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode payload = mapper.createObjectNode();
			payload.put("apikey", apiKey);
			payload.put("partnerID", partnerId);
			payload.put("message", message);
			payload.put("shortcode", senderId);
			payload.put("mobile", phoneNo);

			if (sms.getTimeToSend() != null && !sms.getTimeToSend().isBlank()) {
				payload.put("timeToSend", sms.getTimeToSend().trim());
			}

			String url = baseUrl + "/api/services/sendsms";

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			String responseBody = response.body();
			System.out.println("📨 Bulk SMS Response ====== " + responseBody);

			// --- Parse JSON response ---
			JSONObject json = new JSONObject(responseBody);
			JSONArray responses = json.getJSONArray("responses");
			JSONObject first = responses.getJSONObject(0);

			int code = first.getInt("response-code");
			String desc = first.getString("response-description");

			// --- SUCCESS LOGIC ---
			if (code == 200) {
				sms.setDocStatus(DocStatus.APPROVED);
				sms.setApprovalStage(ApprovalStage.APPROVED);
				sms.setMessageStatus(MessageStatus.SENT);
				sms.setReason(desc);
				sms.setResponseCode(String.valueOf(code));
			} else {
				// --- FAILURE LOGIC ---
				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				sms.setReason(desc);
				sms.setResponseCode(String.valueOf(code));
				sms.setApprovalStage(ApprovalStage.CANCELLED);
			}

			smsRepository.save(sms);

		} catch (Exception e) {
			e.printStackTrace();
			sms.setDocStatus(DocStatus.REJECTED);
			sms.setMessageStatus(MessageStatus.FAILED);
			sms.setReason("Exception: " + e.getMessage());
			smsRepository.save(sms);
		}
	}

	// --- Check SMS Balance (GET) ---
	public String getSmsBalanceAdvanta(long orgId) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(orgId, true);

			if (config == null) {
				return "No active SMS configuration found for orgId: " + orgId;
			}

			// Trim all config values
			String apiKey = config.getApiKey().trim();
			String partnerId = config.getPartnerId().trim();
			String baseUrl = config.getSmsBaseUrl().trim();

			String params = String.format("apikey=%s&partnerID=%s", URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
					URLEncoder.encode(partnerId, StandardCharsets.UTF_8));

			String fullUrl = baseUrl + "/api/services/getbalance?" + params;

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();

		} catch (Exception e) {
			e.printStackTrace();
			return "Error checking SMS balance: " + e.getMessage();
		}
	}

//		============================AFRICAS TALKING SMS INTEGRATIONS==========================
	// ============================ AFRICAS TALKING SMS ==========================

	// --- Send SMS ---
	public void sendSmsAfricasTalking(MSms sms) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(sms.getAdOrgID(),
					true);

			if (config == null) {
				System.err.println("❌ No active SMS config found for orgId: " + sms.getAdOrgID());

				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				sms.setReason("No SMS configuration found");
				sms.setResponseCode("0");
				sms.setApprovalStage(ApprovalStage.CANCELLED);
				smsRepository.save(sms);
				return;
			}

			String apiKey = config.getApiKey().trim();
			String senderId = config.getSenderId().trim();
			String baseUrl = config.getSmsBaseUrl().trim(); // e.g https://api.africastalking.com
			String phoneNo = sms.getPhoneNo().trim();
			String message = sms.getMessage().trim();

			// Form params
			String form = "username=" + URLEncoder.encode("sandbox", StandardCharsets.UTF_8) + "&to="
					+ URLEncoder.encode(phoneNo, StandardCharsets.UTF_8) + "&message="
					+ URLEncoder.encode(message, StandardCharsets.UTF_8) + "&from="
					+ URLEncoder.encode(senderId, StandardCharsets.UTF_8);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/version1/messaging"))
					.header("Content-Type", "application/x-www-form-urlencoded").header("apiKey", apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(form)).build();

			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			String responseBody = response.body();
			System.out.println("📨 AT Response ====== " + responseBody);

			JSONObject json = new JSONObject(responseBody);
			JSONObject smsData = json.getJSONObject("SMSMessageData");
			JSONArray recipients = smsData.getJSONArray("Recipients");
			JSONObject first = recipients.getJSONObject(0);

			String status = first.getString("status");

			if (status.toLowerCase().contains("success")) {
				sms.setDocStatus(DocStatus.APPROVED);
				sms.setApprovalStage(ApprovalStage.APPROVED);
				sms.setMessageStatus(MessageStatus.SENT);
				sms.setReason(status);
				sms.setResponseCode("200");
			} else {
				sms.setDocStatus(DocStatus.REJECTED);
				sms.setMessageStatus(MessageStatus.FAILED);
				sms.setReason(status);
				sms.setResponseCode("500");
				sms.setApprovalStage(ApprovalStage.CANCELLED);
			}

			smsRepository.save(sms);

		} catch (Exception e) {
			e.printStackTrace();
			sms.setDocStatus(DocStatus.REJECTED);
			sms.setMessageStatus(MessageStatus.FAILED);
			sms.setReason("Exception: " + e.getMessage());
			smsRepository.save(sms);
		}
	}

}
