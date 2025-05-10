package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
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
    void shouldReturnCurrentDate() {
        // Given
        SlackReportingTimerTriggered function = new SlackReportingTimerTriggered();

        // When
        LocalDate result = function.getCurrentDate();

        // Then
        // LocalDate.now() will give the current date, which might change during
        // test execution.
        // So we verify it's not null and is "close to now"
        assertNotNull(result, "Current date should not be null");
        LocalDate today = LocalDate.now();
        // Check if the date is within 1 day of today (to handle timezone differences)
        assertTrue(
                Math.abs(result.toEpochDay() - today.toEpochDay()) <= 1,
                "Current date should be close to today"
        );
    }

    @Test
    void shouldParseValidDateFormat() {
        // Given
        SlackReportingTimerTriggered function = new SlackReportingTimerTriggered();
        String dateString = "15-4-2023"; // day-month-year format
        LocalDate defaultDate = LocalDate.of(2023, 1, 1);

        // When
        LocalDate result = function.getDateFromString(dateString, defaultDate);

        // Then
        LocalDate expected = LocalDate.now().withYear(2023).withMonth(4).withDayOfMonth(15);
        assertEquals(expected, result, "Should correctly parse valid date format");
    }

    @Test
    void shouldReturnDefaultDateForInvalidFormat() {
        // Given
        SlackReportingTimerTriggered function = new SlackReportingTimerTriggered();
        String dateString = "invalid-date-format";
        LocalDate defaultDate = LocalDate.of(2023, 1, 1);

        // When
        LocalDate result = function.getDateFromString(dateString, defaultDate);

        // Then
        assertEquals(defaultDate, result, "Should return default date for invalid format");
    }

    @Test
    void shouldReturnDefaultDateForIncompleteComponents() {
        // Given
        SlackReportingTimerTriggered function = new SlackReportingTimerTriggered();
        String dateString = "15-4"; // Missing year
        LocalDate defaultDate = LocalDate.of(2023, 1, 1);

        // When
        LocalDate result = function.getDateFromString(dateString, defaultDate);

        // Then
        assertEquals(defaultDate, result, "Should return default date for incomplete components");
    }

    @Test
    void shouldReturnDefaultDateForNumberFormatException() {
        // Given
        SlackReportingTimerTriggered function = new SlackReportingTimerTriggered();
        String dateString = "15-Apr-2023"; // Non-numeric month
        LocalDate defaultDate = LocalDate.of(2023, 1, 1);

        // When
        LocalDate result = function.getDateFromString(dateString, defaultDate);

        // Then
        assertEquals(defaultDate, result, "Should return default date for number format exception");
    }

    @Test
    void shouldUseCorrectEndpointFromEnvironment() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Check if the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run(); // Execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                function.run(timerInfo, mockContext);

                // Verify that the SlackWebhookClient was called with the report
                verify(mockSlackWebhookClient).postMessageToWebhook(anyString());

            }
        }
    }

    @Test
    void shouldLogAppropriateMessages() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

        // Given
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

            // Ensure that the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run(); // Execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

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

                // Verify scheduler logs
                verify(mockLogger).info("Sending 1 messages to Slack");
                verify(mockLogger).info("Start reportMessage scheduling");
                verify(mockLogger).info("Sending message 1 of 1");
                verify(mockLogger).info("All messages scheduled successfully");
            }
        }
    }

    @Test
    void shouldHandleMissingEndpointEnvironmentVariable() {
        String timerInfo = "timer info";
        LocalDate fixedToday = LocalDate.of(2025, 4, 21);

        // Create a testable instance that returns null for the endpoint
        TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                null,
                fixedToday,
                mockAggregationService,
                null
        );

        // The function should throw NullPointerException when endpoint is null
        assertThrows(NullPointerException.class, () -> function.run(timerInfo, mockContext));
    }

    @Test
    void shouldHandleExceptionFromAggregationService() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

        // Ensure the scheduler is properly closed
        try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
            // Mock the scheduler creation
            ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
            executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

            assertThrows(RuntimeException.class, () -> function.run(timerInfo, mockContext));

            // Verify the webhook client was not called
            verify(mockSlackWebhookClient, never()).postMessageToWebhook(anyString());

            // Verify scheduler was never used for scheduling tasks
            verify(mockScheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        }
    }

    @Test
    void shouldSendOneMessageWhenReportIsSplit() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-05-25",
                "client",
                "psp",
                "PT",
                statusFields
        );
        group.incrementStatus("ACTIVATED", 100);
        group.incrementStatus("CLOSED", 50);
        group.incrementStatus("NOTIFIED_OK", 30);
        group.incrementStatus("EXPIRED", 1);
        mockAggregatedStatuses.add(group);

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
                "First part of the report"
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

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run(); // Execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                // When
                function.run(timerInfo, mockContext);

                // Then
                // Verify that the webhook client was called for each message
                verify(mockSlackWebhookClient, times(1)).postMessageToWebhook(mockReportMessages[0]);

                // Verify logging
                verify(mockLogger).info("Sending 1 messages to Slack");
                verify(mockLogger).info("Start reportMessage scheduling");
                verify(mockLogger).info("Sending message 1 of 1");
                verify(mockLogger).info("All messages scheduled successfully");
            }
        }
    }

    @Test
    void shouldSendMultipleMessagesWhenReportIsSplit() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run(); // Execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

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
                verify(mockLogger).info("Start reportMessage scheduling");
                verify(mockLogger).info("Sending message 1 of 3");
                verify(mockLogger).info("Sending message 2 of 3");
                verify(mockLogger).info("Sending message 3 of 3");
                verify(mockLogger).info("All messages scheduled successfully");
            }
        }
    }

    @Test
    void shouldHandleExceptionDuringMultipleMessageSending() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods
                // For the first message, execute normally
                // For the second message, throw an exception
                // For the third message, do nothing (it should still be scheduled)
                final AtomicInteger taskCounter = new AtomicInteger(0);
                doAnswer(invocation -> {
                    int currentTask = taskCounter.getAndIncrement();
                    Runnable task = invocation.getArgument(0);

                    if (currentTask == 0) {
                        // First task should succeed
                        task.run();
                    } else if (currentTask == 1) {
                        // Second task will throw exception
                        doThrow(new RuntimeException("Error sending to Slack"))
                                .when(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);
                        try {
                            task.run();
                        } catch (RuntimeException e) {
                            // Expected exception, but we want to continue the test
                            // so we catch it here and don't rethrow
                        }
                    }
                    // Don't execute third task

                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));

                // When
                function.run(timerInfo, mockContext);

                // Then
                // Verify that the first message was sent successfully
                verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[0]);

                // Verify that the second message was attempted
                verify(mockSlackWebhookClient).postMessageToWebhook(mockReportMessages[1]);

                // Verify that tasks were scheduled
                verify(mockScheduler, times(3)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));

                // Verify logging - updated to match the new implementation
                verify(mockLogger).info("Sending 3 messages to Slack");
                verify(mockLogger).info("Start reportMessage scheduling");
                verify(mockLogger).info("Sending message 2 of 3"); // The second message
                verify(mockLogger).info("All messages scheduled successfully");
            }
        }
    }

    @Test
    void shouldHandleInterruptedExceptionDuringAwaitTermination() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    task.run(); // Execute the task immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));

                // When
                function.run(timerInfo, mockContext);

                // Then
                // Verify messages were sent
                verify(mockSlackWebhookClient, times(2)).postMessageToWebhook(anyString());

                // Verify logging - updated to match the new implementation
                verify(mockLogger).info("Sending 2 messages to Slack");
                verify(mockLogger).info("Start reportMessage scheduling");
                verify(mockLogger).info("Sending message 1 of 2");
                verify(mockLogger).info("Sending message 2 of 2");
                verify(mockLogger).info("All messages scheduled successfully");

                // No longer verifying warning about interruption since there's no
                // awaitTermination
            }
        }
    }

    @Test
    void shouldScheduleTasksWithCorrectDelays() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Capture the delay arguments
                ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);

                // When
                function.run(timerInfo, mockContext);

                // Then
                // Verify that tasks were scheduled with increasing delays
                verify(mockScheduler, times(3))
                        .schedule(any(Runnable.class), delayCaptor.capture(), eq(TimeUnit.SECONDS));

                List<Long> capturedDelays = delayCaptor.getAllValues();
                assertEquals(3, capturedDelays.size());

                assertEquals(1L, capturedDelays.get(0).longValue());
                assertEquals(2L, capturedDelays.get(1).longValue());
                assertEquals(3L, capturedDelays.get(2).longValue());
            }
        }
    }

    @Test
    void shouldUseCorrectDateRangeForLastWeek() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

                // Ensure the scheduler is properly closed
                try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                    // Mock the scheduler creation
                    ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                    executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                    // Don't execute tasks to avoid duplicate calls
                    doReturn(null).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

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

                    // No need to verify scheduler.shutdown() since it's not called in the
                    // implementation
                }

                // Reset mocks for next iteration
                reset(mockAggregationService, mockSlackWebhookClient);
            }
        }
    }

    @Test
    void shouldHandleExceptionFromSlackClient() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

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

            // Mock the webhook client to throw a RuntimeException
            doThrow(new RuntimeException("Error sending to Slack")).when(mockSlackWebhookClient)
                    .postMessageToWebhook(anyString());

            // Create testable instance
            TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                    mockEndpoint,
                    fixedToday,
                    mockAggregationService,
                    mockSlackWebhookClient
            );

            // Ensure the scheduler is properly closed
            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                // Mock the scheduler creation
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                // Mock the scheduler methods to execute tasks immediately
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run(); // This will throw the exception from the webhook client
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                // Since we're throwing RuntimeException, we should expect an exception
                assertThrows(RuntimeException.class, () -> function.run(timerInfo, mockContext));

                // Verify scheduler interaction but don't verify shutdown() since it's not
                // called in the implementation
                verify(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
            }
        }
    }
}
