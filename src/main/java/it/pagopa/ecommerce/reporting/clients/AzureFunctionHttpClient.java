package it.pagopa.ecommerce.reporting.clients;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AzureFunctionHttpClient {
    private final String webhookEndpoint;
    private final Logger logger;
    private final AzureFunctionHttpClient.HttpClientFactory httpClientFactory;

    public interface HttpClientFactory {
        CloseableHttpClient createHttpClient();
    }

    public AzureFunctionHttpClient(
            String webhookEndpoint,
            Logger logger
    ) {
        this(
                webhookEndpoint,
                logger,
                HttpClients::createDefault
        );
    }

    AzureFunctionHttpClient(
            String webhookEndpoint,
            Logger logger,
            AzureFunctionHttpClient.HttpClientFactory httpClientFactory
    ) {
        this.webhookEndpoint = webhookEndpoint;
        this.logger = logger;
        this.httpClientFactory = httpClientFactory;
    }

    public void sendGetStatistics(
                                  String clientId,
                                  String paymentTypeCode,
                                  String pspID
    ) {
        System.out.println("SEND GET to Azure trigger webhook");

        // Check if webhook endpoint is configured
        if (webhookEndpoint == null || webhookEndpoint.isEmpty()) {
            System.out.println("SwebhookEndpoint not defined");
            logger.severe("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT environment variable is not set!");
            return;
        }

        try (CloseableHttpClient httpClient = httpClientFactory.createHttpClient()) {
            System.out.println("HTTPCLIENT " + webhookEndpoint);
            HttpGet httpGet = new HttpGet(webhookEndpoint + "/" + clientId + "/" + paymentTypeCode + "/" + pspID);
            System.out.println(httpGet.getURI().getPath());
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("Received response with status code: " + statusCode);
                System.out.println("Received response with status code: " + statusCode);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // Check for Slack error responses
                if (statusCode != 202) {
                    logger.info("Error response from Azure Function Trigger: " + responseBody);
                    System.out.println("Error response from Azure Function Trigger: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending GET to Azure Function Http Trigger", e);
            System.out.println("Error sending GET to Azure Function Http Trigger" + e.getMessage());
        }
    }

    public void invokeGet(
                          String clientId,
                          String paymentMethodTypeCode,
                          String pspId
    ) {
        sendGetStatistics(clientId, paymentMethodTypeCode, pspId);
    }

}
