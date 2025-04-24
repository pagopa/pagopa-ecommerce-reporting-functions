package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.SlackDateRangeReportMessageUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        SlackWebhookClient slackWebhookClient = createSlackWebhookClient(endpoint);
        LocalDate today = getCurrentDate();
        LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.with(DayOfWeek.SUNDAY);

        TransactionStatusAggregationService transactionStatusAggregationService = createAggregationService();
        List<AggregatedStatusGroup> aggregatedStatuses = transactionStatusAggregationService
                .aggregateStatusCountByDateRange(lastMonday, lastSunday, logger);

        logger.info(
                "Start date: " + lastMonday + " to date: " + lastSunday + ", results: " + aggregatedStatuses.size()
        );

        // Create the report message
        String report = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(aggregatedStatuses, lastMonday, lastSunday, logger);
        // Send it to Slack
        slackWebhookClient.postMessageToWebhook(report);
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
