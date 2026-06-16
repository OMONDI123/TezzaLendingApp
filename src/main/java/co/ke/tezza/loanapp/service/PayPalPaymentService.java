package co.ke.tezza.loanapp.service;
//package co.ke.tezza.loanapp.service;
//
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.ArrayList;
//import java.util.Base64;
//import java.util.List;
//
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//
//import com.paypal.api.payments.Amount;
//import com.paypal.api.payments.Payer;
//import com.paypal.api.payments.Payment;
//import com.paypal.api.payments.PaymentExecution;
//import com.paypal.api.payments.RedirectUrls;
//import com.paypal.api.payments.Transaction;
//
//import com.paypal.base.rest.APIContext;
//import com.paypal.base.rest.PayPalRESTException;
//
//import co.ke.tezza.loanapp.paypal.config.PaypalPaymentIntent;
//import co.ke.tezza.loanapp.paypal.config.PaypalPaymentMethod;
//
//@Service
//public class PayPalPaymentService {
//
//    @Autowired
//    private APIContext apiContext;
//
//
//    // ==================================
//    //   1️⃣ OLD PAYPAL SDK CHECKOUT
//    // ==================================
//
//    public Payment createPayment(
//            Double total,
//            String currency,
//            PaypalPaymentMethod method,
//            PaypalPaymentIntent intent,
//            String description,
//            String cancelUrl,
//            String successUrl) throws PayPalRESTException {
//
//        Amount amount = new Amount();
//        amount.setCurrency(currency);
//        total = new BigDecimal(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
//        amount.setTotal(String.format("%.2f", total));
//
//        Transaction transaction = new Transaction();
//        transaction.setDescription(description);
//        transaction.setAmount(amount);
//
//        List<Transaction> list = new ArrayList<>();
//        list.add(transaction);
//
//        Payer payer = new Payer();
//        payer.setPaymentMethod(method.toString());
//
//        Payment payment = new Payment();
//        payment.setIntent(intent.toString());
//        payment.setPayer(payer);
//        payment.setTransactions(list);
//
//        RedirectUrls redirectUrls = new RedirectUrls();
//        redirectUrls.setCancelUrl(cancelUrl);
//        redirectUrls.setReturnUrl(successUrl);
//        payment.setRedirectUrls(redirectUrls);
//
//        return payment.create(apiContext);
//    }
//
//
//    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
//        Payment payment = new Payment();
//        payment.setId(paymentId);
//
//        PaymentExecution paymentExecute = new PaymentExecution();
//        paymentExecute.setPayerId(payerId);
//
//        return payment.execute(apiContext, paymentExecute);
//    }
//
//
//
//    // =======================================================
//    //   2️⃣ NEW PAYPAL V2 REST API (ADVANCED CARD PROCESSING)
//    // =======================================================
//
//    // 🔐 Use the SAME keys from PaypalConfig
//    @Value("${paypal.client.app}")
//    private String clientId;
//
//    @Value("${paypal.client.secret}")
//    private String clientSecret;
//
//
//    private final String PAYPAL_OAUTH_URL = "https://api-m.paypal.com/v1/oauth2/token";
//    private final String PAYPAL_ORDER_URL = "https://api-m.paypal.com/v2/checkout/orders";
//
//
//    // STEP 1: Get access token
//    private String getAccessToken() throws Exception {
//
//        CloseableHttpClient client = HttpClients.createDefault();
//        HttpPost post = new HttpPost(PAYPAL_OAUTH_URL);
//
//        post.setHeader("Accept", "application/json");
//        post.setHeader("Accept-Language", "en_US");
//
//        String credentials = clientId + ":" + clientSecret;
//        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
//
//        post.setHeader("Authorization", "Basic " + encoded);
//        post.setEntity(new StringEntity("grant_type=client_credentials"));
//
//        CloseableHttpResponse response = client.execute(post);
//        String body = EntityUtils.toString(response.getEntity());
//
//        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
//
//        return json.get("access_token").getAsString();
//    }
//
//
//    // STEP 2: Create order (for capturing card payment)
//    public String createOrderV2(String amount) throws Exception {
//
//        String token = getAccessToken();
//        CloseableHttpClient client = HttpClients.createDefault();
//
//        HttpPost post = new HttpPost(PAYPAL_ORDER_URL);
//
//        post.setHeader("Content-Type", "application/json");
//        post.setHeader("Authorization", "Bearer " + token);
//
//        String json = "{"
//                + "\"intent\":\"CAPTURE\","
//                + "\"purchase_units\":[{"
//                + "\"amount\":{"
//                + "\"currency_code\":\"USD\","
//                + "\"value\":\"" + amount + "\""
//                + "}"
//                + "}]"
//                + "}";
//
//        post.setEntity(new StringEntity(json));
//
//        CloseableHttpResponse response = client.execute(post);
//        String result = EntityUtils.toString(response.getEntity());
//
//        JsonObject responseJson = JsonParser.parseString(result).getAsJsonObject();
//
//        return responseJson.get("id").getAsString();
//    }
//
//
//    // STEP 3: Capture order
//    public JsonObject captureOrderV2(String orderId) throws Exception {
//
//        String token = getAccessToken();
//        String captureUrl = PAYPAL_ORDER_URL + "/" + orderId + "/capture";
//
//        CloseableHttpClient client = HttpClients.createDefault();
//        HttpPost post = new HttpPost(captureUrl);
//
//        post.setHeader("Content-Type", "application/json");
//        post.setHeader("Authorization", "Bearer " + token);
//
//        CloseableHttpResponse response = client.execute(post);
//        String body = EntityUtils.toString(response.getEntity());
//
//        return JsonParser.parseString(body).getAsJsonObject();
//    }
//
//}
