package it.pagopa.ecommerce.reporting.clients;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.ecommerce.reporting.utils.WeeklyReportUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class SlackWebhookClient {
    // This value contains the whole webhook endpoint, client id/secret included
    private static final String WEBHOOK_ENDPOINT = System.getenv("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT");

    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(SlackWebhookClient.class.getName());

    /**
     * Posts a raw JSON message to the Slack webhook
     *
     * @param jsonPayload The JSON payload to send
     * @return Response from Slack as JsonNode, or an empty JsonNode if an error occurred
     */
    public static JsonNode postRawJsonToWebhook(String jsonPayload) {
        logger.info("Posting message to Slack webhook");

        // Check if webhook endpoint is configured
        if (WEBHOOK_ENDPOINT == null || WEBHOOK_ENDPOINT.isEmpty()) {
            logger.severe("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT environment variable is not set!");
            return objectMapper.createObjectNode();
        }

        // Validate JSON before sending
        if (!validateJsonPayload(jsonPayload)) {
            logger.severe("Invalid JSON payload provided to postRawJsonToWebhook");
            return objectMapper.createObjectNode();
        }

        // Log the payload for debugging (consider removing in production)
        logger.info("Sending payload to Slack: " + jsonPayload);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(WEBHOOK_ENDPOINT);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("Received response with status code: " + statusCode);

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // Check for Slack error responses
                if (statusCode != 200) {
                    logger.info("Error response from Slack: " + responseBody);
                    // Don't try to parse as JSON if it's an error message
                    if (responseBody.startsWith("invalid_payload")) {
                        logger.info("Slack reported invalid payload. Original payload: " + jsonPayload);
                        return objectMapper.createObjectNode();
                    }
                }

                // Only try to parse as JSON if we have actual JSON content
                if (responseBody != null && !responseBody.isEmpty() &&
                        (responseBody.startsWith("{") || responseBody.startsWith("["))) {
                    return objectMapper.readTree(responseBody);
                } else {
                    // Return empty object for empty or non-JSON responses
                    return objectMapper.createObjectNode();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error posting to Slack webhook", e);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Validates that a string is valid JSON
     *
     * @param jsonPayload The JSON string to validate
     * @return true if valid JSON, false otherwise
     */
    private static boolean validateJsonPayload(String jsonPayload) {
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

    /**
     * Posts a simple text message to the Slack webhook
     *
     * @param message The text message to post
     */
    public static void postMessageToWebhook(String message) {
        postRawJsonToWebhook(message);
    }
}