package co.ke.tezza.loanapp.sasapay.config;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.enums.PaymentGateway;
import co.ke.tezza.loanapp.repository.PaymentGatewayConfigRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class SasaPayOAuth2Client {

    @Autowired
    private PaymentGatewayConfigRepository paymentGatewayConfigRepository;

    public String authenticateSasaPay(long ad_org_id) {

        String clientId = "CLIENT_ID";
        String clientSecret = "CLIENT_SECRET";
        String tokenUrl = "https://sandbox.sasapay.app/api/v1/auth/token/?grant_type=client_credentials";

        MPaymentGatewayConfig config = getSasaPayPaymentGatewayConfig(ad_org_id);
        if (config != null) {
            clientId = config.getMpesaApiKey();
            clientSecret = config.getMpesaApiSecrete();

            String baseUrl = config.isMpesaProductionAllowed() ?
                    config.getMpesaProductionBaseUrl() :
                    config.getMpesaTestBaseUrl();

            tokenUrl = baseUrl + "/api/v1/auth/token/?grant_type=client_credentials";
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(tokenUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));

        try {
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder()
                            .encodeToString((clientId + ":" + clientSecret).getBytes()));

            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                System.out.println("SasaPay Token Response: " + responseBody);
                return responseBody;
            } else {
                System.out.println("SasaPay Auth Error (" + statusCode + "): " + responseBody);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public MPaymentGatewayConfig getSasaPayPaymentGatewayConfig(long orgId) {
        return paymentGatewayConfigRepository
                .findTop1ByIsActiveAndAdOrgIDAndPaymentGatwayOrderByCreatedDesc(
                        true, orgId, PaymentGateway.SASA_PAY
                );
    }
}
