package it.pagopa.ecommerce.reporting.clients;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EcommerceHelpdeskServiceClient {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static EcommerceHelpdeskServiceClient instance = null;
    private final Logger logger;

    private String apiHost() {
        return System.getenv("HELPDESK_SERVICE_URI");
    }

    private String apiKey() {
        return System.getenv("HELPDESK_SERVICE_API_KEY");
    }

    private String apiEndpoint() {
        return System.getenv("HELPDESK_SERVICE_API_ENDPOINT");
    }

    private EcommerceHelpdeskServiceClient(Logger logger) {
        this.logger = logger;
    }

    public JsonNode fetchTransactionMetrics(
                                            String clientId,
                                            String pspId,
                                            String paymentTypeCode,
                                            OffsetDateTime startDate,
                                            OffsetDateTime endDate
    ) {
        if (!isValid(clientId, "Client ID") || !isValid(pspId, "PSP ID")
                || !isValid(paymentTypeCode, "PaymentTypeCode") || !isValid(startDate, "startDate")
                || !isValid(endDate, "endDate") || !isValid(apiKey(), "Subscription Key")) {
            return objectMapper.createObjectNode();
        }
        logger.warning(
                () -> String.format(
                        "Fetching transaction details for clientId: %s paymentTypeCode: %s pspId: %s startDate: %s endDate: %s",
                        clientId,
                        paymentTypeCode,
                        pspId,
                        startDate.format(DateTimeFormatter.ISO_DATE),
                        endDate.format(DateTimeFormatter.ISO_DATE)
                )
        );
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = createHttpPost(clientId, pspId, paymentTypeCode, startDate, endDate);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                logger.warning(() -> String.format("Response status: %d", response.getStatusLine().getStatusCode()));
                logger.warning(() -> String.format(responseBody));
                return objectMapper.readTree(responseBody);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to fetch transaction details", e);
            return objectMapper.createObjectNode();
        }
    }

    private boolean isValid(
                            String value,
                            String fieldName
    ) {
        if (Optional.ofNullable(value).map(String::isEmpty).orElse(true)) {
            logger.warning(() -> String.format("%s is null or empty", fieldName));
            return false;
        }
        return true;
    }

    private boolean isValid(
                            OffsetDateTime value,
                            String fieldName
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
        HttpPost httpPost = new HttpPost(apiHost() + apiEndpoint());
        httpPost.setHeader("ocp-apim-subscription-key", apiKey());
        httpPost.setHeader("Content-Type", "application/json");
        String jsonPayload = String.format(
                "{\"clientId\":\"%s\",\"pspId\":\"%s\",\"paymentTypeCode\":\"%s\",\"timeRange\":{\"startDate\":\"%s\",\"endDate\":\"%s\"}}",
                clientId,
                pspId,
                paymentTypeCode,
                startDate,
                endDate
        );
        logger.info(jsonPayload);
        httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
        return httpPost;
    }

    public static EcommerceHelpdeskServiceClient getInstance(Logger logger) {
        if (instance == null) {
            instance = new EcommerceHelpdeskServiceClient(logger);
        }
        return instance;
    }
}
