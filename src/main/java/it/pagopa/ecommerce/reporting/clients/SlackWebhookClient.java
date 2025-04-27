package it.pagopa.ecommerce.reporting.clients;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class SlackWebhookClient {
    private final String webhookEndpoint;
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private final HttpClientFactory httpClientFactory;

    public interface HttpClientFactory {
        CloseableHttpClient createHttpClient();
    }

    public SlackWebhookClient(String webhookEndpoint) {
        this(
                webhookEndpoint,
                new ObjectMapper(),
                Logger.getLogger(SlackWebhookClient.class.getName()),
                HttpClients::createDefault
        );
    }

    SlackWebhookClient(
            String webhookEndpoint,
            ObjectMapper objectMapper,
            Logger logger,
            HttpClientFactory httpClientFactory
    ) {
        this.webhookEndpoint = webhookEndpoint;
        this.objectMapper = objectMapper;
        this.logger = logger;
        this.httpClientFactory = httpClientFactory;
    }

    public void postRawJsonToWebhook(String jsonPayload) {
        logger.info("Posting message to Slack webhook");

        // Check if webhook endpoint is configured
        if (webhookEndpoint == null || webhookEndpoint.isEmpty()) {
            logger.severe("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT environment variable is not set!");
            return;
        }

        // Validate JSON before sending
        if (!validateJsonPayload(jsonPayload)) {
            logger.severe("Invalid JSON payload provided to postRawJsonToWebhook");
            return;
        }

        try (CloseableHttpClient httpClient = httpClientFactory.createHttpClient()) {
            HttpPost httpPost = new HttpPost(webhookEndpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("Received response with status code: " + statusCode);

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // Check for Slack error responses
                if (statusCode != 200) {
                    logger.info("Error response from Slack: " + responseBody + ", " + jsonPayload);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error posting to Slack webhook", e);
        }
    }

    private boolean validateJsonPayload(String jsonPayload) {
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            return false;
        }

        try {
            objectMapper.readTree(jsonPayload);
            return true;
        } catch (JsonProcessingException e) {
            logger.severe("JSON validation failed: " + e.getMessage() + "\nPayload: " + jsonPayload);
            return false;
        }
    }

    public void postMessageToWebhook(String message) {
        postRawJsonToWebhook(message);
    }
}
