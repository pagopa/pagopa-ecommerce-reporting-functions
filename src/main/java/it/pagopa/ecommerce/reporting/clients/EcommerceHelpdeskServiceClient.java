package it.pagopa.ecommerce.reporting.clients;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EcommerceHelpdeskServiceClient {

    private static final String API_HOST = System.getenv("HELPDESK_SERVICE_URI");
    private static final String API_KEY = System.getenv("HELPDESK_SERVICE_API_KEY");
    private static final String API_ENDPOINT = System.getenv("HELPDESK_SERVICE_API_ENDPOINT");
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static EcommerceHelpdeskServiceClient instance = null;

    public JsonNode fetchTransactionMetrics(
                                            String clientId,
                                            String pspId,
                                            String paymentTypeCode,
                                            OffsetDateTime startDate,
                                            OffsetDateTime endDate,
                                            Logger logger
    ) {
        if (!isValid(clientId, "Client ID", logger) || !isValid(pspId, "PSP ID", logger)
                || !isValid(paymentTypeCode, "PaymentTypeCode", logger) || !isValid(startDate, "startDate", logger)
                || !isValid(endDate, "endDate", logger) || !isValid(API_KEY, "Subscription Key", logger)) {
            return objectMapper.createObjectNode();
        }
        logger.info(
                () -> String.format(
                        "Fetching transaction details for clientId: %s paymentTypeCode: %s pspId: %s startDate: %s endDate: %s",
                        clientId,
                        paymentTypeCode,
                        pspId,
                        startDate.toString(),
                        endDate.toString()
                )
        );
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = createHttpPost(clientId, pspId, paymentTypeCode, startDate, endDate);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                logger.info(() -> String.format("Response status: %d", response.getStatusLine().getStatusCode()));
                return objectMapper.readTree(responseBody);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to fetch transaction details", e);
            return objectMapper.createObjectNode();
        }
    }

    private boolean isValid(
                            String value,
                            String fieldName,
                            Logger logger
    ) {
        if (Optional.ofNullable(value).map(String::isEmpty).orElse(true)) {
            logger.warning(() -> String.format("%s is null or empty", fieldName));
            return false;
        }
        return true;
    }

    private boolean isValid(
                            OffsetDateTime value,
                            String fieldName,
                            Logger logger
    ) {
        if (value == null) {
            logger.warning(() -> String.format("%s is null or empty", fieldName));
            return false;
        }
        return true;
    }

    private HttpPost createHttpPost(
                                    String clientId,
                                    String pspId,
                                    String paymentTypeCode,
                                    OffsetDateTime startDate,
                                    OffsetDateTime endDate
    ) {
        HttpPost httpPost = new HttpPost(API_HOST + API_ENDPOINT);
        httpPost.setHeader("ocp-apim-subscription-key", API_KEY);
        httpPost.setHeader("Content-Type", "application/json");
        String jsonPayload = String.format(
                "{\"clientId\":\"%s\",\"pspId\":\"%s\",\"paymentTypeCode\":\"%s\",\"timeRage\":{\"startDate\":\"%s\",\"endDate\":\"%s\",}}",
                clientId,
                pspId,
                paymentTypeCode,
                startDate,
                endDate
        );
        httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
        return httpPost;
    }

    public static EcommerceHelpdeskServiceClient getInstance() {
        if (instance == null) {
            instance = new EcommerceHelpdeskServiceClient();
        }
        return instance;
    }
}
