package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;
import it.pagopa.ecommerce.reporting.utils.SlackDateRangeReportMessageUtils;

import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Queue trigger.
 */
public abstract class SlackReportingTimerTriggered {

    /**
     * This function will be invoked periodically according to the specified
     * schedule.
     */
    @FunctionName("SlackReportingTimerTriggered")
    public void run(
                    @TimerTrigger(
                            name = "slackMessageTimerInfo", schedule = "%NCRON_SCHEDULE_SLACK_REPORTING%"
                    ) String timerInfo,
                    final ExecutionContext context
    ) throws JsonProcessingException {

        Logger logger = context.getLogger();
        logger.info("Java Timer trigger SlackReportingTimerTriggered executed at: " + LocalDateTime.now());

        String endpoint = getEnvVariable("ECOMMERCE_SLACK_REPORTING_WEBHOOK_ENDPOINT");
        String reportStartDate = getEnvVariable("REPORT_START_DATE");
        String reportEndDate = getEnvVariable("REPORT_END_DATE");

        Set<String> ecommerceClientList = MapParametersUtils
                .parseSetString(System.getenv("ECOMMERCE_CLIENTS_LIST")).fold(exception -> {
                    throw exception;
                }, Function.identity());

        SlackWebhookClient slackWebhookClient = createSlackWebhookClient(endpoint);
        LocalDate today = getCurrentDate();

        LocalDate startDate = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate endDate = startDate.with(DayOfWeek.SUNDAY);

        if (reportStartDate != null && reportEndDate != null) {
            startDate = getDateFromString(reportStartDate, startDate);
            endDate = getDateFromString(reportEndDate, endDate);
        }

        TransactionStatusAggregationService transactionStatusAggregationService = createAggregationService();
        List<AggregatedStatusGroup> aggregatedStatuses = transactionStatusAggregationService
                .aggregateStatusCountByClientAndPaymentType(startDate, endDate, logger);

        logger.info(
                "Start date: " + startDate + " to date: " + endDate +
                        ", results: " + aggregatedStatuses.size()
        );

        // Create the report messages
        List<String> reportMessages = new java.util.ArrayList<>(List.of(""));

        for (String client : ecommerceClientList) {
            String[] reportMessage = SlackDateRangeReportMessageUtils
                    .createAggregatedTableWeeklyReport(aggregatedStatuses, startDate, endDate, logger, client);
            reportMessages.addAll(Arrays.asList(reportMessage));
        }
        reportMessages.removeIf(String::isBlank);

        logger.info("Sending " + reportMessages.size() + " table-based messages to Slack");

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger index = new AtomicInteger(0);
        String[] initialBlock = SlackDateRangeReportMessageUtils.createInitialBlock(startDate, endDate, logger);
        for (String block : initialBlock) {
            index.getAndIncrement();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    logger.info("Sending message " + block);
                    slackWebhookClient.postMessageToWebhook(block);
                }
            };
            scheduledExecutorService.schedule(task, index.get(), TimeUnit.SECONDS);
        }
        for (String report : reportMessages) {
            int currentIndex = index.incrementAndGet();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    logger.info("Sending message " + report);
                    System.out.println("Sending message system " + report);
                    logger.info(
                            "Sending table message " + currentIndex + " of " +
                                    reportMessages.size()
                    );
                    slackWebhookClient.postMessageToWebhook(report);
                }
            };
            scheduledExecutorService.schedule(task, index.get(), TimeUnit.SECONDS);
        }

        logger.info("All messages sent successfully");
    }

    protected LocalDate getDateFromString(
                                          String dateFormat,
                                          LocalDate defaultDate
    ) {
        try {
            String[] dateComponents = dateFormat.split("-");
            if (dateComponents.length == 3) {
                int day = Integer.parseInt(dateComponents[0]);
                int month = Integer.parseInt(dateComponents[1]);
                int year = Integer.parseInt(dateComponents[2]);
                return LocalDate.now().withYear(year).withMonth(month).withDayOfMonth(day);
            }
        } catch (NumberFormatException e) {
            return defaultDate;
        }
        return defaultDate;
    }

    /**
     * Gets an environment variable value
     *
     * @param name The name of the environment variable
     * @return The value of the environment variable
     */
    protected String getEnvVariable(String name) {
        return System.getenv(name);
    }

    /**
     * Gets the current date
     *
     * @return The current date
     */
    protected LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * Creates a new TransactionStatusAggregationService
     *
     * @return A new TransactionStatusAggregationService instance
     */
    protected TransactionStatusAggregationService createAggregationService() {
        return new TransactionStatusAggregationService();
    }

    /**
     * Creates a new SlackWebhookClient
     *
     * @param endpoint The webhook endpoint URL
     * @return A new SlackWebhookClient instance
     */
    protected SlackWebhookClient createSlackWebhookClient(String endpoint) {
        return new SlackWebhookClient(endpoint);
    }
}
