package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.SlackDateRangeReportMessageUtils;

import java.time.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class SlackReportingTimerTriggered {

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
                .aggregateStatusCountByDateRange(startDate, endDate, logger);

        logger.info(
                "Start date: " + startDate + " to date: " + endDate + ", results: " + aggregatedStatuses.size()
        );

        // Create the report messages
        String[] reportMessages = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(aggregatedStatuses, startDate, endDate, logger);

        // Send each message to Slack
        logger.info("Sending " + reportMessages.length + " messages to Slack");
        for (int i = 0; i < reportMessages.length; i++) {
            logger.info("Sending message " + (i + 1) + " of " + reportMessages.length);
            slackWebhookClient.postMessageToWebhook(reportMessages[i]);

            // Add a small delay between messages to avoid rate limiting
            if (i < reportMessages.length - 1) {
                try {
                    sleep(1000); // 1-second delay
                } catch (InterruptedException e) {
                    logger.warning("Sleep interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }

        logger.info("All messages sent successfully");
    }

    private LocalDate getDateFromString(
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

    protected void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
