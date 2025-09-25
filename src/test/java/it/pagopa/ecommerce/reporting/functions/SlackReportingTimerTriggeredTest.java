package it.pagopa.ecommerce.reporting.functions;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.SlackDateRangeReportMessageUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    @SetEnvironmentVariable(key = "ECOMMERCE_CLIENTS_LIST", value = "[\"clientA\"]")
    @Test
    void shouldSendSlackMessage() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

        LocalDate fixedToday = LocalDate.of(2025, 9, 23);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        List<AggregatedStatusGroup> mockAggregatedStatuses = List.of(
                new AggregatedStatusGroup(
                        "2025-09-16",
                        "clientA",
                        "pspX",
                        "CP",
                        List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
                )
        );

        when(mockAggregationService.aggregateStatusCountByClientAndPaymentType(any(), any(), any()))
                .thenReturn(mockAggregatedStatuses);

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createInitialBlock(
                            any(LocalDate.class),
                            any(LocalDate.class),
                            any(Logger.class)
                    )
            ).thenReturn(
                    new String[] {
                            "Initial block message"
                    }
            );
            mockedUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedTableWeeklyReport(
                            anyList(),
                            any(LocalDate.class),
                            any(LocalDate.class),
                            any(Logger.class),
                            any()
                    )
            ).thenReturn(
                    new String[] {
                            "Test message"
                    }
            );

            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                doAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    task.run(); // execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                        mockEndpoint,
                        fixedToday,
                        mockAggregationService,
                        mockSlackWebhookClient
                );

                function.run("timerInfo", mockContext);

                verify(mockSlackWebhookClient, atLeastOnce()).postMessageToWebhook("Test message");
            }
        }
    }

    @SetEnvironmentVariable(key = "ECOMMERCE_CLIENTS_LIST", value = "[\"clientA\"]")
    @Test
    void shouldLogAppropriateMessages() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

        LocalDate fixedToday = LocalDate.of(2025, 9, 23);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        List<AggregatedStatusGroup> mockAggregatedStatuses = List.of(
                new AggregatedStatusGroup(
                        "2025-09-16",
                        "clientA",
                        "pspX",
                        "CP",
                        List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
                )
        );

        when(mockAggregationService.aggregateStatusCountByClientAndPaymentType(any(), any(), any()))
                .thenReturn(mockAggregatedStatuses);

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createInitialBlock(
                            any(LocalDate.class),
                            any(LocalDate.class),
                            any(Logger.class)
                    )
            ).thenReturn(
                    new String[] {
                            "Initial block message"
                    }
            );

            mockedUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createAggregatedTableWeeklyReport(
                            anyList(),
                            any(LocalDate.class),
                            any(LocalDate.class),
                            any(Logger.class),
                            any()
                    )
            ).thenReturn(
                    new String[] {
                            "Test message"
                    }
            );

            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                doAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    task.run(); // execute immediately
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                        mockEndpoint,
                        fixedToday,
                        mockAggregationService,
                        mockSlackWebhookClient
                );

                function.run("timerInfo", mockContext);

                verify(mockLogger).info(matches("Java Timer trigger SlackReportingTimerTriggered executed at: .*"));
                verify(mockLogger).info(contains("results:"));
                verify(mockLogger).info(contains("Sending 1 table-based messages to Slack"));
            }
        }
    }

    @Test
    void shouldHandleExceptionFromAggregationService() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

        LocalDate fixedToday = LocalDate.of(2025, 9, 23);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                mockEndpoint,
                fixedToday,
                mockAggregationService,
                mockSlackWebhookClient
        );

        try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
            ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
            executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

            assertThrows(RuntimeException.class, () -> function.run("timerInfo", mockContext));

            verify(mockSlackWebhookClient, never()).postMessageToWebhook(anyString());
            verify(mockScheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        }
    }

    @Test
    void shouldHandleMissingEndpointEnvironmentVariable() {
        LocalDate fixedToday = LocalDate.of(2025, 9, 23);

        TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                null,
                fixedToday,
                mockAggregationService,
                null
        );

        assertThrows(NullPointerException.class, () -> function.run("timerInfo", mockContext));
    }

    @SetEnvironmentVariable(key = "ECOMMERCE_CLIENTS_LIST", value = "[\"CLIENT_1\",\"CLIENT2\"]")
    @Test
    void shouldScheduleTasksWithCorrectDelays() throws Exception {
        when(mockContext.getLogger()).thenReturn(mockLogger);

        LocalDate fixedToday = LocalDate.of(2025, 9, 23);
        String mockEndpoint = "https://hooks.slack-mock.com/services/test/webhook";

        when(mockAggregationService.aggregateStatusCountByClientAndPaymentType(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        try (MockedStatic<SlackDateRangeReportMessageUtils> mockedUtils = Mockito
                .mockStatic(SlackDateRangeReportMessageUtils.class)) {
            mockedUtils
                    .when(
                            () -> SlackDateRangeReportMessageUtils
                                    .createAggregatedTableWeeklyReport(anyList(), any(), any(), any(), any())
                    )
                    .thenReturn(
                            new String[] {
                                    "First",
                                    "Second",
                                    "Third"
                            }
                    );
            mockedUtils.when(
                    () -> SlackDateRangeReportMessageUtils.createInitialBlock(
                            any(LocalDate.class),
                            any(LocalDate.class),
                            any(Logger.class)
                    )
            ).thenReturn(
                    new String[] {
                            "Initial block message"
                    }
            );

            try (MockedStatic<Executors> executorsMock = Mockito.mockStatic(Executors.class)) {
                ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
                executorsMock.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockScheduler);

                ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);

                doAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    task.run();
                    return null;
                }).when(mockScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

                TestableSlackReportingTimerTriggered function = new TestableSlackReportingTimerTriggered(
                        mockEndpoint,
                        fixedToday,
                        mockAggregationService,
                        mockSlackWebhookClient
                );

                function.run("timerInfo", mockContext);

                verify(mockScheduler, times(7))
                        .schedule(any(Runnable.class), delayCaptor.capture(), eq(TimeUnit.SECONDS));

                List<Long> capturedDelays = delayCaptor.getAllValues();
                assertEquals(1L, capturedDelays.get(0));
                assertEquals(2L, capturedDelays.get(1));
                assertEquals(3L, capturedDelays.get(2));
            }
        }
    }
}
