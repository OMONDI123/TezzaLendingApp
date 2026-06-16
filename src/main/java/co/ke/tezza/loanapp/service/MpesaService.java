package co.ke.tezza.loanapp.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.enums.PaymentGateway;
import co.ke.tezza.loanapp.repository.PaymentGatewayConfigRepository;
import co.ke.tezza.loanapp.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MpesaService {

    private static final String STK_PUSH_ENDPOINT = "/mpesa/stkpush/v1/processrequest";
    private static final String STK_QUERY_ENDPOINT = "/mpesa/stkpushquery/v1/query";
    private static final String AUTH_ENDPOINT = "/oauth/v1/generate?grant_type=client_credentials";
    private static final String REGISTER_URL_ENDPOINT = "/mpesa/c2b/v1/registerurl";
    private static final String REVERSAL_ENDPOINT = "/safaricom/reversal/v1/request";
    private static final String BALANCE_ENDPOINT = "/safaricom/accountbalance/v1/query";
    private static final String B2C_ENDPOINT = "/mpesa/b2c/v1/paymentrequest";
    
    private static final long TOKEN_EXPIRY_BUFFER = 300000; 
    private static final int MAX_AUTH_RETRIES = 3;
    private static final int REQUEST_TIMEOUT = 30;

    @Autowired
    private PaymentGatewayConfigRepository paymentGatewayConfigRepository;

    @Autowired
    private Utils utils;

    private String cachedAccessToken;
    private long tokenExpiryTime;

    /**
     * Authenticate with M-Pesa API and get access token with caching
     */
    private String authenticatePush(String baseUrl,long orgId) throws IOException {
        if (isTokenValid()) {
            log.info("✅ Using cached access token");
            return cachedAccessToken;
        }

        MPaymentGatewayConfig config = getMpesaConfig(orgId);
        String encodedCredentials = getEncodedCredentials(config);

        log.debug("========= M-PESA AUTH DEBUG INFO =========");
        log.debug("API Base URL: {}{}", baseUrl, AUTH_ENDPOINT);

        for (int attempt = 1; attempt <= MAX_AUTH_RETRIES; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("🔄 Authentication attempt {} of {}", attempt, MAX_AUTH_RETRIES);
                    Thread.sleep(1000L * attempt);
                }

                OkHttpClient client = createHttpClient();
                Request request = createAuthRequest(baseUrl, encodedCredentials);
                Response response = client.newCall(request).execute();

                log.debug("Auth Response Code: {}", response.code());
                String responseBody = response.body().string();
                log.debug("Auth Response: {}", responseBody);

                if (!response.isSuccessful()) {
                    handleAuthFailure(attempt, response.code());
                    continue;
                }

                if (responseBody == null || responseBody.trim().isEmpty()) {
                    handleEmptyResponse(attempt);
                    continue;
                }

                String accessToken = extractAccessToken(responseBody);
                cacheToken(accessToken);

                log.info("✅ Access Token Received successfully");
                return accessToken;

            } catch (JSONException e) {
                handleJsonException(attempt, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Authentication interrupted", e);
            }
        }
        throw new RuntimeException("All authentication attempts failed");
    }

    /**
     * Initiate STK Push payment request
     */
    public Response STKPushSimulation(String businessShortCode, String password, String timestamp,
                                     String transactionType, String amount, String phoneNumber, 
                                     String partyA, String partyB, String callBackURL,
                                     String accountReference, String transactionDesc,long orgId) throws IOException {
        
        String requestJson = createStkPushRequest(businessShortCode, password, timestamp, transactionType,
                amount, phoneNumber, partyA, partyB, callBackURL, accountReference, transactionDesc);
        
        log.info("STK Push Request: {}", requestJson);

        MPaymentGatewayConfig config = getMpesaConfig(orgId);
        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + STK_PUSH_ENDPOINT;

        return executeApiCall(fullUrl, requestJson, apiBaseUrl,orgId);
    }

    /**
     * Query STK Push transaction status
     */
    public Response STKPushTransactionStatus(String shortCode, String password, String timestamp,
                                           String checkoutRequestID, long adorgId) throws IOException {
        
        MPaymentGatewayConfig config = paymentGatewayConfigRepository
                .findTop1ByIsActiveAndAdOrgIDOrderByCreatedDesc(true, adorgId);
        
        log.info("Querying transaction with ShortCode: {}", shortCode);

        String requestJson = createStkQueryRequest(shortCode, password, timestamp, checkoutRequestID);
        log.info("STK Query Request: {}", requestJson);

        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + STK_QUERY_ENDPOINT;

        return executeApiCall(fullUrl, requestJson, apiBaseUrl,adorgId);
    }

    /**
     * Initiate transaction reversal
     */
    public String reversal(String initiator, String securityCredential, String commandID, String transactionID,
                          String amount, String receiverParty, String receiverIdentifierType, String resultURL,
                          String queueTimeOutURL, String remarks, String occasion,long orgId) throws IOException {
        
        String requestJson = createReversalRequest(initiator, securityCredential, commandID, transactionID,
                amount, receiverParty, receiverIdentifierType, resultURL, queueTimeOutURL, remarks, occasion);
        
        log.info("Reversal Request: {}", requestJson);

        MPaymentGatewayConfig config = getMpesaConfig(orgId);
        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + REVERSAL_ENDPOINT;

        Response response = executeApiCall(fullUrl, requestJson, apiBaseUrl,orgId);
        return response.body().string();
    }

    /**
     * Check account balance
     */
    public String balanceInquiry(String initiator, String commandID, String securityCredential, String partyA,
                                String identifierType, String remarks, String queueTimeOutURL, 
                                String resultURL,long orgId) throws IOException {
        
        String requestJson = createBalanceInquiryRequest(initiator, commandID, securityCredential, partyA,
                identifierType, remarks, queueTimeOutURL, resultURL);
        
        log.info("Balance Inquiry Request: {}", requestJson);

        MPaymentGatewayConfig config = getMpesaConfig(orgId);
        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + BALANCE_ENDPOINT;

        Response response = executeApiCall(fullUrl, requestJson, apiBaseUrl,orgId);
        return response.body().string();
    }

    /**
     * Initiate B2C payment
     */
    public String b2cPayment(String initiatorName, String securityCredential, String commandID, String amount,
                            String partyA, String partyB, String remarks, String queueTimeOutURL,
                            String resultURL, String occasion,long orgId) throws IOException {
        
        String requestJson = createB2CRequest(initiatorName, securityCredential, commandID, amount,
                partyA, partyB, remarks, queueTimeOutURL, resultURL, occasion);
        
        log.info("B2C Payment Request: {}", requestJson);

        MPaymentGatewayConfig config = getMpesaConfig(orgId);
        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + B2C_ENDPOINT;

        Response response = executeApiCall(fullUrl, requestJson, apiBaseUrl,orgId);
        return response.body().string();
    }

    /**
     * Currency conversion utility
     */
    public String convertKshToUSDOrUSDToKsh(String targetCurrency, String baseCurrency, String amountStr) {
        try {
            String appId = "97353134f7744d0585a41e93e95f5639";
            String apiUrl = "https://open.er-api.com/v6/latest/" + baseCurrency + "?apikey=" + appId;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            String jsonResponse = readResponse(connection);
            String exchangeRate = extractExchangeRate(jsonResponse, targetCurrency);
            
            double rate = Double.parseDouble(exchangeRate);
            double amount = rate * Double.parseDouble(amountStr);
            int roundedAmount = (int) Math.round(amount);

            log.info("Currency conversion: {} {} = {} {}", amountStr, baseCurrency, roundedAmount, targetCurrency);
            return String.valueOf(roundedAmount);

        } catch (Exception e) {
            log.error("Currency conversion failed", e);
            return amountStr; // Return original amount on failure
        }
    }

    /**
     * Register C2B URLs
     */
    public String registerURL(String shortCode, String responseType, String confirmationURL, 
                             String validationURL, boolean useProductionUrl, 
                             String prodBaseUrl, String testBaseUrl,long orgId) throws IOException {
        
        String requestJson = createRegisterUrlRequest(shortCode, responseType, confirmationURL, validationURL);
        log.info("Register URL Request: {}", requestJson);

        String apiUrl = useProductionUrl ? prodBaseUrl : testBaseUrl;
        String fullUrl = apiUrl + REGISTER_URL_ENDPOINT;

        Response response = executeApiCall(fullUrl, requestJson, apiUrl,orgId);
        return response.body().string();
    }

    // Helper Methods

    private boolean isTokenValid() {
        return cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime;
    }

    private MPaymentGatewayConfig getMpesaConfig(long orgId) {
        MPaymentGatewayConfig config = paymentGatewayConfigRepository
                .findTop1ByIsActiveAndAdOrgIDAndPaymentGatwayOrderByCreatedDesc(true, orgId,PaymentGateway.MPESA);
        if (config == null) {
            throw new RuntimeException("M-Pesa configuration not found");
        }
        return config;
    }

    private String getEncodedCredentials(MPaymentGatewayConfig config) {
        String appKey = config.getMpesaApiKey();
        String appSecret = config.getMpesaApiSecrete();
        
        if (appKey == null || appSecret == null) {
            throw new RuntimeException("M-Pesa API credentials not configured");
        }
        
        String appKeySecret = appKey + ":" + appSecret;
        return Base64.getEncoder().encodeToString(appKeySecret.getBytes());
    }

    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    private Request createAuthRequest(String baseUrl, String encodedCredentials) {
        return new Request.Builder()
                .url(baseUrl + AUTH_ENDPOINT)
                .get()
                .addHeader("authorization", "Basic " + encodedCredentials)
                .addHeader("cache-control", "no-cache")
                .build();
    }

    private String extractAccessToken(String responseBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(responseBody);
        return jsonObject.getString("access_token");
    }

    private void cacheToken(String accessToken) {
        cachedAccessToken = accessToken;
        tokenExpiryTime = System.currentTimeMillis() + (3600 * 1000) - TOKEN_EXPIRY_BUFFER;
    }

    // Request Creation Methods

    private String createStkPushRequest(String businessShortCode, String password, String timestamp,
                                      String transactionType, String amount, String phoneNumber,
                                      String partyA, String partyB, String callBackURL,
                                      String accountReference, String transactionDesc) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("BusinessShortCode", businessShortCode);
        jsonObject.put("Password", password);
        jsonObject.put("Timestamp", timestamp);
        jsonObject.put("TransactionType", transactionType);
        jsonObject.put("Amount", amount);
        jsonObject.put("PartyA", partyA);
        jsonObject.put("PartyB", partyB);
        jsonObject.put("PhoneNumber", phoneNumber);
        jsonObject.put("CallBackURL", callBackURL);
        jsonObject.put("AccountReference", accountReference);
        jsonObject.put("TransactionDesc", transactionDesc);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String createStkQueryRequest(String shortCode, String password, String timestamp, 
                                       String checkoutRequestID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("BusinessShortCode", shortCode);
        jsonObject.put("Password", password);
        jsonObject.put("Timestamp", timestamp);
        jsonObject.put("CheckoutRequestID", checkoutRequestID);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String createReversalRequest(String initiator, String securityCredential, String commandID, 
                                       String transactionID, String amount, String receiverParty,
                                       String receiverIdentifierType, String resultURL, String queueTimeOutURL,
                                       String remarks, String occasion) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Initiator", initiator);
        jsonObject.put("SecurityCredential", securityCredential);
        jsonObject.put("CommandID", commandID);
        jsonObject.put("TransactionID", transactionID);
        jsonObject.put("Amount", amount);
        jsonObject.put("ReceiverParty", receiverParty);
        jsonObject.put("ReceiverIdentifierType", receiverIdentifierType);
        jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
        jsonObject.put("ResultURL", resultURL);
        jsonObject.put("Remarks", remarks);
        jsonObject.put("Occasion", occasion);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String createBalanceInquiryRequest(String initiator, String commandID, String securityCredential,
                                             String partyA, String identifierType, String remarks,
                                             String queueTimeOutURL, String resultURL) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Initiator", initiator);
        jsonObject.put("SecurityCredential", securityCredential);
        jsonObject.put("CommandID", commandID);
        jsonObject.put("PartyA", partyA);
        jsonObject.put("IdentifierType", identifierType);
        jsonObject.put("Remarks", remarks);
        jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
        jsonObject.put("ResultURL", resultURL);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String createB2CRequest(String initiatorName, String securityCredential, String commandID, String amount,
                                  String partyA, String partyB, String remarks, String queueTimeOutURL,
                                  String resultURL, String occasion) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("InitiatorName", initiatorName);
        jsonObject.put("SecurityCredential", securityCredential);
        jsonObject.put("CommandID", commandID);
        jsonObject.put("Amount", amount);
        jsonObject.put("PartyA", partyA);
        jsonObject.put("PartyB", partyB);
        jsonObject.put("Remarks", remarks);
        jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
        jsonObject.put("ResultURL", resultURL);
        jsonObject.put("Occasion", occasion);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String createRegisterUrlRequest(String shortCode, String responseType, 
                                          String confirmationURL, String validationURL) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ShortCode", shortCode);
        jsonObject.put("ResponseType", responseType);
        jsonObject.put("ConfirmationURL", confirmationURL);
        jsonObject.put("ValidationURL", validationURL);

        return new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
    }

    private String getApiBaseUrl(MPaymentGatewayConfig config) {
        return config.isMpesaProductionAllowed() ? 
               config.getMpesaProductionBaseUrl() : 
               config.getMpesaTestBaseUrl();
    }

    private Response executeApiCall(String url, String requestJson, String baseUrl,long orgId) throws IOException {
        OkHttpClient client = createHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, requestJson);
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer " + authenticatePush(baseUrl,orgId))
                .addHeader("cache-control", "no-cache")
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        
        log.info("API Response: {}", responseBody);

        // Create new response with consumed body
        return new Response.Builder()
                .request(response.request())
                .protocol(response.protocol())
                .code(response.code())
                .message(response.message())
                .headers(response.headers())
                .body(ResponseBody.create(mediaType, responseBody))
                .build();
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String extractExchangeRate(String jsonResponse, String targetCurrency) {
        int startIndex = jsonResponse.indexOf("\"" + targetCurrency + "\":") + targetCurrency.length() + 3;
        int endIndex = jsonResponse.indexOf(",", startIndex);
        return jsonResponse.substring(startIndex, endIndex);
    }

    // Error handling methods
    private void handleAuthFailure(int attempt, int responseCode) {
        if (attempt < MAX_AUTH_RETRIES) {
            log.warn("❌ Authentication failed with HTTP {}, retrying...", responseCode);
        } else {
            throw new RuntimeException("Authentication failed with HTTP " + responseCode);
        }
    }

    private void handleEmptyResponse(int attempt) {
        if (attempt < MAX_AUTH_RETRIES) {
            log.warn("❌ Empty authentication response, retrying...");
        } else {
            throw new RuntimeException("Empty authentication response");
        }
    }

    private void handleJsonException(int attempt, JSONException e) {
        if (attempt < MAX_AUTH_RETRIES) {
            log.warn("❌ JSON parsing failed, retrying...");
        } else {
            throw new RuntimeException("Failed to parse authentication response after " + MAX_AUTH_RETRIES + " attempts", e);
        }
    }

    /**
     * Clear token cache (useful for testing)
     */
    public void clearTokenCache() {
        cachedAccessToken = null;
        tokenExpiryTime = 0;
        log.info("🔄 Token cache cleared");
    }
    
    /**
     * Query transaction status with full details using Transaction Status API
     */
    public Response queryTransactionStatusWithDetails(String initiator, String securityCredential, 
                                                     String commandID, String transactionID,
                                                     String partyA, String identifierType,
                                                     String resultURL, String queueTimeOutURL,
                                                     String remarks, String occasion,long adOrgId) throws IOException {
        
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Initiator", initiator);
        jsonObject.put("SecurityCredential", securityCredential);
        jsonObject.put("CommandID", commandID);
        jsonObject.put("TransactionID", transactionID);
        jsonObject.put("PartyA", partyA);
        jsonObject.put("IdentifierType", identifierType);
        jsonObject.put("ResultURL", resultURL);
        jsonObject.put("QueueTimeOutURL", queueTimeOutURL);
        jsonObject.put("Remarks", remarks);
        jsonObject.put("Occasion", occasion);

        String requestJson = new JSONArray().put(jsonObject).toString().replaceAll("[\\[\\]]", "");
        log.info("Transaction Status Request: {}", requestJson);

        MPaymentGatewayConfig config = getMpesaConfig(adOrgId);
        String apiBaseUrl = getApiBaseUrl(config);
        String fullUrl = apiBaseUrl + "/mpesa/transactionstatus/v1/query";

        return executeApiCall(fullUrl, requestJson, apiBaseUrl,adOrgId);
    }
}