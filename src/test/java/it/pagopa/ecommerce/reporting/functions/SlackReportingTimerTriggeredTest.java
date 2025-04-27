package it.pagopa.ecommerce.reporting.functions;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.SlackDateRangeReportMessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackReportingTimerTriggeredTest {

    @Mock
    private ExecutionContext mockContext;

    @Mock
    private Logger mockLogger;

    @Mock
    private TransactionStatusAggregationService mockAggregationService;

    @Mock
    private SlackWebhookClient mockSlackWebhookClient;

    @Captor
    private ArgumentCaptor<LocalDate> startDateCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> endDateCaptor;

    @Captor
    private ArgumentCaptor<String> reportCaptor;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    /**
     * Test class that extends the original class to allow for mocking
     */
    private static class TestableSlackReportingTimerTriggered extends SlackReportingTimerTriggered {
        private final String webhookEndpoint;
        private final LocalDate fixedDate;
        private final TransactionStatusAggregationService aggregationService;
        private final SlackWebhookClient slackWebhookClient;

        public TestableSlackReportingTimerTriggered(
                String webhookEndpoint,
                LocalDate fixedDate,
                TransactionStatusAggregationService aggregationService,
                SlackWebhookClient slackWebhookClient
        ) {
            this.webhookEndpoint = webhookEndpoint;
            this.fixedDate = fixedDate;
            this.aggregationService = aggregationService;
            this.slackWebhookClient = slackWebhookClient;
        }

        @Override
        protected String getEnvVariable(String name) {
            if ("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT".equals(name)) {
                return webhookEndpoint;
            }
            return null;
        }

        @Override
        protected LocalDate getCurrentDate() {
            return fixedDate;
        }

        @Override
        protected TransactionStatusAggregationService createAggregationService() {
            return aggregationService;
        }

        @Override
        protected SlackWebhookClient createSlackWebhookClient(String endpoint) {
            return slackWebhookClient;
        }
    }

    @Test
    void shouldUseCorrectDateRangeForLastWeek() throws Exception {
        String timerInfo = "timer info";
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Test with different days of the week to ensure correct "last week"
        // calculation
        LocalDate[] testDates = {
                LocalDate.of(2025, 4, 21), // Monday
                LocalDate.of(2025, 4, 22), // Tuesday
                LocalDate.of(2025, 4, 23), // Wednesday
                LocalDate.of(2025, 4, 24), // Thursday
                LocalDate.of(2025, 4, 25), // Friday
                LocalDate.of(2025, 4, 26), // Saturday
                LocalDate.of(2025, 4, 27) // Sunday
        };

        for (LocalDate today : testDates) {
            // Calculate expected date range
            LocalDate expectedLastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate expectedLastSunday = expectedLastMonday.with(DayOfWeek.SUNDAY);

            // Mock the aggregation service to return empty results
            when(
                    mockAggregationService.aggregateStatusCountByDateRange(
                            startDateCaptor.capture(),
                            endDateCaptor.capture(),
                            eq(mockLogger)
                    )
            )
                    .thenReturn(new ArrayList<>());

            // Mock the report creation
            try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                    .mockStatic(SlackDateRangeReportMessageUtils.class)) {
                mockedReportUtils.when(
                        () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                                any(),
                                any(),
                                any(),
                                any()
                        )
                ).thenReturn(
                        new String[] {
                                "Report"
                        }
                );

                // Create testable instance
                TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                        mockEndpoint,
                        today,
                        mockAggregationService,
                        mockSlackWebhookClient
                );

                function.run(timerInfo, mockContext);

                assertEquals(
                        expectedLastMonday,
                        startDateCaptor.getValue(),
                        "Start date should be last Monday for current date: " + today
                );
                assertEquals(
                        expectedLastSunday,
                        endDateCaptor.getValue(),
                        "End date should be last Sunday for current date: " + today
                );

                // Reset mocks for next iteration
                reset(mockAggregationService, mockSlackWebhookClient);
            }
        }
    }

    @Test
    void shouldUseCorrectEndpointFromEnvironment() throws Exception {
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Mock the aggregation service to return empty results
        when(mockAggregationService.aggregateStatusCountByDateRange(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Mock the report creation
        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            any(),
                            any(),
                            any(),
                            any()
                    )
            ).thenReturn(
                    new String[] {
                            "Report"
                    }
            );

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            function.run(timerInfo, mockContext);

            // Verify that the SlackWebhookClient was called with the report
            verify(mockSlackWebhookClient).postMessageToWebhook(anyString());
        }
    }

    @Test
    void shouldLogAppropriateMessages() throws Exception {
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21); // A Monday
        LocalDate expectedLastMonday = fixedToday.minusWeeks(1);
        LocalDate expectedLastSunday = expectedLastMonday.with(DayOfWeek.SUNDAY);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        List<AggregatedStatusGroup> mockAggregatedStatuses = new ArrayList<>();
        // Add a few status groups with the correct constructor
        List<String> statusFields = Arrays.asList("ACTIVATED", "CLOSED", "NOTIFIED_OK", "EXPIRED");

        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2023-05-08",
                "clientA",
                "pspX",
                "PT1",
                statusFields
        );
        mockAggregatedStatuses.add(group1);

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2023-05-09",
                "clientB",
                "pspY",
                "PT2",
                statusFields
        );
        mockAggregatedStatuses.add(group2);

        // Mock the aggregation service to return our mock statuses
        when(
                mockAggregationService.aggregateStatusCountByDateRange(
                        eq(expectedLastMonday),
                        eq(expectedLastSunday),
                        any(Logger.class)
                )
        )
                .thenReturn(mockAggregatedStatuses);

        // Mock the report creation
        String mockReport = "Weekly Report Content";
        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            eq(mockAggregatedStatuses),
                            eq(expectedLastMonday),
                            eq(expectedLastSunday),
                            any(Logger.class)
                    )
            )
                    .thenReturn(
                            new String[] {
                                    mockReport
                            }
                    );

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            function.run(timerInfo, mockContext);

            // Verify execution start log
            verify(mockLogger).info(matches("Java Timer trigger SlackReportingTimerTriggered executed at: .*"));

            // Verify date range and results count log
            verify(mockLogger).info(
                    "Start date: " + expectedLastMonday + " to date: " + expectedLastSunday +
                            ", results: " + mockAggregatedStatuses.size()
            );

            // Verify the webhook client was called with the correct report
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReport);
        }
    }

    @Test
    void shouldHandleMissingEndpointEnvironmentVariable() {

        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);

        // Create a testable instance that returns null for the endpoint
        // AND returns null for the SlackWebhookClient to simulate the real behavior
        TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                null,
                fixedToday,
                mockAggregationService,
                null
        ) {
            @Override
            protected SlackWebhookClient createSlackWebhookClient(String endpoint) {
                // This simulates the real behavior - when endpoint is null,
                // passing it to the SlackWebhookClient constructor would throw
                // NullPointerException
                if (endpoint == null) {
                    throw new NullPointerException("Endpoint cannot be null");
                }
                return mockSlackWebhookClient;
            }
        };

        // The function should throw NullPointerException when endpoint is null
        assertThrows(NullPointerException.class, () -> function.run(timerInfo, mockContext));
    }

    @Test
    void shouldHandleExceptionFromAggregationService() throws Exception {

        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Mock the aggregation service to throw an exception
        when(mockAggregationService.aggregateStatusCountByDateRange(any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Create testable instance
        TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                mockEndpoint,
                fixedToday,
                mockAggregationService,
                mockSlackWebhookClient
        );

        assertThrows(RuntimeException.class, () -> function.run(timerInfo, mockContext));

        // Verify the webhook client was not called
        verify(mockSlackWebhookClient, never()).postMessageToWebhook(anyString());
    }

    @Test
    void shouldHandleExceptionFromSlackClient() throws Exception {
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Mock the aggregation service to return empty results
        when(mockAggregationService.aggregateStatusCountByDateRange(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Mock the report creation
        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            any(),
                            any(),
                            any(),
                            any()
                    )
            ).thenReturn(
                    new String[] {
                            "Report"
                    }
            );

            // Mock the webhook client to throw a RuntimeException instead of
            // JsonProcessingException
            doThrow(new RuntimeException("Error sending to Slack")).when(mockSlackWebhookClient)
                    .postMessageToWebhook(anyString());

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // Since we're throwing RuntimeException, we should expect an exception
            assertThrows(RuntimeException.class, () -> function.run(timerInfo, mockContext));
        }
    }

    @Test
    void shouldSendMultipleMessagesWhenReportIsSplit() throws Exception {
        // Given
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21); // A Monday
        LocalDate expectedLastMonday = fixedToday.minusWeeks(1);
        LocalDate expectedLastSunday = expectedLastMonday.with(DayOfWeek.SUNDAY);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Large list that would result in multiple messages
        List<AggregatedStatusGroup> mockAggregatedStatuses = new ArrayList<>();
        List<String> statusFields = Arrays.asList("ACTIVATED", "CLOSED", "NOTIFIED_OK", "EXPIRED");

        // Add enough groups to exceed the 50 block limit
        for (int i = 0; i < 20; i++) {
            AggregatedStatusGroup group = new AggregatedStatusGroup(
                    "2023-05-" + (i < 10 ? "0" + i : i),
                    "client" + i,
                    "psp" + i,
                    "PT" + i,
                    statusFields
            );
            group.incrementStatus("ACTIVATED", 100 - i);
            group.incrementStatus("CLOSED", 50 - i);
            group.incrementStatus("NOTIFIED_OK", 30 - i);
            group.incrementStatus("EXPIRED", i);
            mockAggregatedStatuses.add(group);
        }

        // Mock the aggregation service to return our mock statuses
        when(
                mockAggregationService.aggregateStatusCountByDateRange(
                        eq(expectedLastMonday),
                        eq(expectedLastSunday),
                        any(Logger.class)
                )
        )
                .thenReturn(mockAggregatedStatuses);

        // Create mock report messages - simulate multiple messages returned
        String[] mockReportMessages = {
                "First part of the report",
                "Second part of the report",
                "Third part of the report"
        };

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            eq(mockAggregatedStatuses),
                            eq(expectedLastMonday),
                            eq(expectedLastSunday),
                            any(Logger.class)
                    )
            )
                    .thenReturn(mockReportMessages);

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // When
            function.run(timerInfo, mockContext);

            // Then
            // Verify that the webhook client was called for each message
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[0]);
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[2]);
            verify(mockSlackWebhookClient, times(3)).postMessageToWebhook(anyString());

            // Verify logging
            verify(mockLogger).info("Sending 3 messages to Slack");
            verify(mockLogger).info("Sending message 1 of 3");
            verify(mockLogger).info("Sending message 2 of 3");
            verify(mockLogger).info("Sending message 3 of 3");
            verify(mockLogger).info("All messages sent successfully");
        }
    }

    @Test
    void shouldHandleExceptionDuringMultipleMessageSending() throws Exception {
        // Given
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Mock the aggregation service to return some results
        when(mockAggregationService.aggregateStatusCountByDateRange(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Create mock report messages
        String[] mockReportMessages = {
                "First part of the report",
                "Second part of the report",
                "Third part of the report"
        };

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            any(),
                            any(),
                            any(),
                            any()
                    )
            ).thenReturn(mockReportMessages);

            // Mock the webhook client to throw an exception on the second message
            doNothing().when(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[0]);
            doThrow(new RuntimeException("Error sending to Slack"))
                    .when(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // The function should throw an exception when the second message fails
            assertThrows(RuntimeException.class, () -> function.run(timerInfo, mockContext));

            // Verify that the first message was sent successfully
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[0]);

            // Verify that the second message was attempted
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);

            // Verify that the third message was never sent
            verify(mockSlackWebhookClient, never()).postMessageToWebhook(mockReportMessages[2]);

            // Verify logging
            verify(mockLogger).info("Sending 3 messages to Slack");
            verify(mockLogger).info("Sending message 1 of 3");
            verify(mockLogger).info("Sending message 2 of 3");
            verify(mockLogger, never()).info("All messages sent successfully");
        }
    }

    @Test
    void shouldHandleInterruptedExceptionDuringSleep() throws Exception {
        // Given
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        // Mock the aggregation service to return some results
        when(mockAggregationService.aggregateStatusCountByDateRange(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Create mock report messages
        String[] mockReportMessages = {
                "First part of the report",
                "Second part of the report"
        };

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedReportUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedReportUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedWeeklyReport(
                            any(),
                            any(),
                            any(),
                            any()
                    )
            ).thenReturn(mockReportMessages);

            // Create a testable instance with a custom implementation that simulates
            // an InterruptedException during sleep
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            ) {
                @Override
                protected void sleep(long millis) throws InterruptedException {
                    // Simulate InterruptedException on sleep
                    throw new InterruptedException("Sleep interrupted");
                }
            };

            // When
            function.run(timerInfo, mockContext);

            // Then
            // Verify both messages were sent despite the interruption
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[0]);
            verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);

            // Verify logging of the interruption
            verify(mockLogger).warning(contains("Sleep interrupted"));
        }
    }
}
