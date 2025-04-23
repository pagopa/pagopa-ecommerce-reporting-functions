package it.pagopa.ecommerce.reporting.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackWebhookClientTest {

    private static final String WEBHOOK_URL = "https://hooks.slack-mock.com/services/test/webhook";

    @Mock
    private CloseableHttpClient httpClientMock;

    @Mock
    private CloseableHttpResponse httpResponseMock;

    @Mock
    private StatusLine statusLineMock;

    @Mock
    private Logger loggerMock;

    @Mock
    private SlackWebhookClient.HttpClientFactory httpClientFactoryMock;

    private ObjectMapper objectMapper;
    private SlackWebhookClient slackWebhookClient;

    @BeforeEach
    void setUp() {
        // Create real ObjectMapper
        objectMapper = new ObjectMapper();

        // Create the client with mocked dependencies
        slackWebhookClient = new SlackWebhookClient(
                WEBHOOK_URL,
                objectMapper,
                loggerMock,
                httpClientFactoryMock
        );
    }

    @Test
    void testPostRawJsonToWebhookWithSuccess() throws Exception {
        String validJson = "{\"text\":\"Test message\"}";
        when(httpClientFactoryMock.createHttpClient()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(200);
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("ok", StandardCharsets.UTF_8));

        slackWebhookClient.postRawJsonToWebhook(validJson);

        ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClientMock).execute(httpPostCaptor.capture());

        HttpPost capturedPost = httpPostCaptor.getValue();
        assertEquals(WEBHOOK_URL, capturedPost.getURI().toString());
        assertEquals("application/json", capturedPost.getFirstHeader("Content-Type").getValue());

        // Verify the entity content matches our JSON
        StringEntity entity = (StringEntity) capturedPost.getEntity();
        String entityContent = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(validJson, entityContent);

        // Verify logging
        verify(loggerMock).info("Posting message to Slack webhook");
        verify(loggerMock).info("Received response with status code: 200");
    }

    @Test
    void testPostRawJsonToWebhookWithMissingWebhookEndpoint() {
        // Create a new client with null webhook
        SlackWebhookClient clientWithNullEndpoint = new SlackWebhookClient(
                null,
                objectMapper,
                loggerMock,
                httpClientFactoryMock
        );
        String validJson = "{\"text\":\"Test message\"}";

        clientWithNullEndpoint.postRawJsonToWebhook(validJson);

        // Verify that no HTTP client is created and an error is logged
        verify(httpClientFactoryMock, never()).createHttpClient();
        verify(loggerMock).severe("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT environment variable is not set!");
    }

    @Test
    void testPostRawJsonToWebhookWithEmptyWebhookEndpoint() {
        // Create a new client with empty webhook
        SlackWebhookClient clientWithEmptyEndpoint = new SlackWebhookClient(
                "",
                objectMapper,
                loggerMock,
                httpClientFactoryMock
        );
        String validJson = "{\"text\":\"Test message\"}";

        clientWithEmptyEndpoint.postRawJsonToWebhook(validJson);

        // Verify that no HTTP client is created and an error is logged
        verify(httpClientFactoryMock, never()).createHttpClient();
        verify(loggerMock).severe("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT environment variable is not set!");
    }

    @Test
    void testPostRawJsonToWebhookWithInvalidJson() {
        slackWebhookClient.postRawJsonToWebhook("Invalid JSON");

        // Verify no HTTP client is created and error is logged
        verify(httpClientFactoryMock, never()).createHttpClient();
        verify(loggerMock).severe("Invalid JSON payload provided to postRawJsonToWebhook");
    }

    @Test
    void testPostRawJsonToWebhookWithNullJson() {
        slackWebhookClient.postRawJsonToWebhook(null);

        // Verify no HTTP client is created and error is logged
        verify(httpClientFactoryMock, never()).createHttpClient();
        verify(loggerMock).severe("Invalid JSON payload provided to postRawJsonToWebhook");
    }

    @Test
    void testPostRawJsonToWebhookWithEmptyJson() {
        slackWebhookClient.postRawJsonToWebhook("");

        // Verify no HTTP client is created and an error is logged
        verify(httpClientFactoryMock, never()).createHttpClient();
        verify(loggerMock).severe("Invalid JSON payload provided to postRawJsonToWebhook");
    }

    @Test
    void testPostRawJsonToWebhookWithErrorResponse() throws Exception {
        String validJson = "{\"text\":\"Test message\"}";
        when(httpClientFactoryMock.createHttpClient()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(400);
        when(httpResponseMock.getEntity()).thenReturn(
                new StringEntity("invalid_payload", StandardCharsets.UTF_8)
        );

        slackWebhookClient.postRawJsonToWebhook(validJson);

        verify(httpClientMock).execute(any(HttpPost.class));
        verify(loggerMock).info("Error response from Slack: invalid_payload");
    }

    @Test
    void testPostRawJsonToWebhookWithHttpClientException() throws Exception {
        String validJson = "{\"text\":\"Test message\"}";
        IOException exception = new IOException("Network error");
        when(httpClientFactoryMock.createHttpClient()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenThrow(exception);

        slackWebhookClient.postRawJsonToWebhook(validJson);

        verify(httpClientMock).execute(any(HttpPost.class));
        verify(loggerMock).log(Level.SEVERE, "Error posting to Slack webhook", exception);
    }

    @Test
    void testPostMessageToWebhook() {
        // Spy of the real client
        SlackWebhookClient spyClient = spy(slackWebhookClient);

        spyClient.postMessageToWebhook("Test message");

        // Verify that postRawJsonToWebhook was called with the message
        verify(spyClient).postRawJsonToWebhook("Test message");
    }
}
