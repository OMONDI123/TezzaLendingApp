package co.ke.tezza.loanapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.ke.tezza.loanapp.entity.MSMSConfig;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.repository.SmsConfigRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class AfricasTalkingSmsUtil {

    private static final Logger LOGGER = Logger.getLogger(AfricasTalkingSmsUtil.class.getName());

    @Autowired
    private SmsConfigRepository smsConfigRepository;

    @Autowired
    private SmsRepository smsRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================= SINGLE SMS (LEGACY ENDPOINT) =========================
    /**
     * Sends a single SMS using the legacy endpoint (application/x-www-form-urlencoded).
     * This method is useful for simple use cases where only one recipient is needed.
     *
     * @param sms the MSms entity containing the message, phone number, and organisation details.
     */
    public void sendSingleSms(MSms sms) {
        try {
            MSMSConfig config = getActiveConfig(sms.getAdOrgID());

            // Prepare form data
            String form = "username=" + encode(config.getUsername()) +
                    "&to=" + encode(sms.getPhoneNo()) +
                    "&message=" + encode(sms.getMessage()) +
                    "&from=" + encode(config.getSenderId());

            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getSmsBaseUrl() + "/version1/messaging"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("apiKey", config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Africa's Talking Single SMS Response: " + response.body());

            // Parse and handle response
            handleAfricasTalkingResponse(sms, response.body());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending single SMS", e);
            failSms(sms, "Single SMS failed: " + e.getMessage());
        }
    }

    // ========================= BULK SMS (JSON ENDPOINT) =========================
    /**
     * Sends a bulk SMS to multiple recipients using the JSON endpoint.
     * The phone numbers are expected to be comma-separated in the MSms.phoneNo field.
     *
     * @param sms the MSms entity containing the message, phone numbers (comma-separated), and organisation details.
     */
    public void sendBulkSms(MSms sms) {
        try {
            MSMSConfig config = getActiveConfig(sms.getAdOrgID());

            // Parse comma-separated phone numbers
            String[] numbers = sms.getPhoneNo().split(",");
            ArrayNode phoneArray = objectMapper.createArrayNode();
            for (String num : numbers) {
                phoneArray.add(num.trim());
            }

            // Build JSON payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("username", config.getUsername());
            payload.put("message", sms.getMessage());
            payload.put("senderId", config.getSenderId());
            payload.put("enqueue", true);  // Enable queuing by default
            payload.set("phoneNumbers", phoneArray);

            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getSmsBaseUrl() + "/version1/messaging/bulk"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("apiKey", config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Africa's Talking Bulk SMS Response: " + response.body());

            // Parse and handle response
            handleAfricasTalkingResponse(sms, response.body());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending bulk SMS", e);
            failSms(sms, "Bulk SMS failed: " + e.getMessage());
        }
    }

    // ========================= MASKED SMS (HASHED NUMBERS) =========================
    /**
     * Sends an SMS to a masked (hashed) number, primarily for Safaricom in Kenya.
     * The phoneNumbers field must be an empty list, and maskedNumber and telco are required.
     *
     * @param sms           the MSms entity containing the message and organisation details.
     * @param maskedNumber  the hashed phone number.
     * @param telco         the service provider (e.g., "Safaricom").
     */
    public void sendMaskedSms(MSms sms, String maskedNumber, String telco) {
        try {
            MSMSConfig config = getActiveConfig(sms.getAdOrgID());

            // Build JSON payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("username", config.getUsername());
            payload.put("message", sms.getMessage());
            payload.put("maskedNumber", maskedNumber);
            payload.put("telco", telco);
            payload.put("senderId", config.getSenderId());
            payload.set("phoneNumbers", objectMapper.createArrayNode());  // Empty array

            // Build request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getSmsBaseUrl() + "/version1/messaging/bulk"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("apiKey", config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Africa's Talking Masked SMS Response: " + response.body());

            // Parse and handle response
            handleAfricasTalkingResponse(sms, response.body());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending masked SMS", e);
            failSms(sms, "Masked SMS failed: " + e.getMessage());
        }
    }

    // ========================= CHECK BALANCE =========================
    /**
     * Retrieves the SMS balance for the given organisation.
     *
     * @param orgId the organisation ID.
     * @return the balance as a JSON string, or an error message.
     */
    public String getBalance(long orgId) {
        try {
            MSMSConfig config = getActiveConfig(orgId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getSmsBaseUrl() + "/version1/user"))
                    .header("Accept", "application/json")
                    .header("apiKey", config.getApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Africa's Talking Balance Response: " + response.body());

            return response.body();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking balance", e);
            return "Error: " + e.getMessage();
        }
    }

    // ========================= PRIVATE HELPER METHODS =========================

    /**
     * Retrieves the active SMS configuration for the given organisation.
     *
     * @param orgId the organisation ID.
     * @return the active MSMSConfig.
     * @throws RuntimeException if no active configuration is found.
     */
    private MSMSConfig getActiveConfig(long orgId) {
        MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(orgId, true);
        if (config == null) {
            throw new RuntimeException("No active SMS configuration found for orgId: " + orgId);
        }
        return config;
    }

    /**
     * URL-encodes a string using UTF-8.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Handles the Africa's Talking API response, updates the MSms entity, and saves it.
     * The response is expected to be a JSON object with an SMSMessageData field containing
     * an array of recipients. Each recipient has a statusCode and status.
     *
     * @param sms          the MSms entity to update.
     * @param responseBody the raw response string from the API.
     */
    private void handleAfricasTalkingResponse(MSms sms, String responseBody) {
        try {
            // Validate response is JSON
            if (responseBody == null || responseBody.trim().isEmpty()) {
                failSms(sms, "Empty response from Africa's Talking");
                return;
            }

            JSONObject json;
            try {
                json = new JSONObject(responseBody);
            } catch (JSONException e) {
                LOGGER.warning("Response is not valid JSON: " + responseBody);
                failSms(sms, "Invalid JSON response: " + responseBody);
                return;
            }

            // Check for error structure (some errors are returned as a plain JSON object)
            if (json.has("error") || json.has("errors")) {
                String errorMsg = json.optString("error", json.optString("errors", "Unknown error"));
                failSms(sms, "API error: " + errorMsg);
                return;
            }

            // Extract SMSMessageData
            if (!json.has("SMSMessageData")) {
                failSms(sms, "Missing SMSMessageData in response");
                return;
            }

            JSONObject smsData = json.getJSONObject("SMSMessageData");
            String messageSummary = smsData.optString("Message", "");
            JSONArray recipients = smsData.getJSONArray("Recipients");
            String totalCost = extractTotalCost(messageSummary);
            sms.setTotalCost(new BigDecimal(totalCost)); 

            // Determine overall success/failure
            boolean allSuccess = true;
            StringBuilder reasonBuilder = new StringBuilder();

            for (int i = 0; i < recipients.length(); i++) {
                JSONObject r = recipients.getJSONObject(i);
                int statusCode = r.getInt("statusCode");
                String status = r.getString("status");
                String number = r.getString("number");

                reasonBuilder.append(number)
                        .append(" -> ")
                        .append(status)
                        .append(" (")
                        .append(statusCode)
                        .append(")\n");

                // Consider any code other than 100, 101, 102 as failure
                if (!(statusCode == 100 || statusCode == 101 || statusCode == 102)) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                sms.setDocStatus(DocStatus.APPROVED);
                sms.setApprovalStage(ApprovalStage.APPROVED);
                sms.setMessageStatus(MessageStatus.SENT);
                sms.setResponseCode("200");
            } else {
                sms.setDocStatus(DocStatus.REJECTED);
                sms.setApprovalStage(ApprovalStage.CANCELLED);
                sms.setMessageStatus(MessageStatus.FAILED);
                sms.setResponseCode("500");
            }

            sms.setReason(reasonBuilder.toString());
            smsRepository.save(sms);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing Africa's Talking response", e);
            failSms(sms, "Response parsing error: " + e.getMessage());
        }
    }

    /**
     * Marks the SMS as failed with the given reason.
     *
     * @param sms    the MSms entity to update.
     * @param reason the failure reason.
     */
    private void failSms(MSms sms, String reason) {
        sms.setDocStatus(DocStatus.REJECTED);
        sms.setMessageStatus(MessageStatus.FAILED);
        sms.setApprovalStage(ApprovalStage.CANCELLED);
        sms.setReason(reason);
        sms.setResponseCode("0");
        smsRepository.save(sms);
        LOGGER.warning("SMS failed: " + reason);
    }
    /**
     * Extracts the total cost from the SMSMessageData.Message string.
     * Expected format: "Sent to X/X Total Cost: KES 0.8000"
     * Returns the cost part, e.g., "KES 0.8000", or null if not found.
     */
    private String extractTotalCost(String messageSummary) {
        if (messageSummary == null || messageSummary.isEmpty()) {
            return null;
        }
        // Look for "Total Cost: " and take everything after it
        String prefix = "Total Cost: ";
        int index = messageSummary.indexOf(prefix);
        if (index != -1) {
            return messageSummary.substring(index + prefix.length()).trim();
        }
        return null;
    }
}