package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.services.TransactionStatusAggregationService;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.SlackWeeklyReportMessageUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
                    // TEST ONLY
                    @HttpTrigger(
                            name = "SlackTrigger", route = "slack", methods = {
                                    HttpMethod.GET
                            }, authLevel = AuthorizationLevel.ANONYMOUS
                    ) HttpRequestMessage<Optional<String>> request,
                    // @TimerTrigger(name = "timerInfo", schedule = "0 * * * * *") String timerInfo,
                    final ExecutionContext context
    ) throws JsonProcessingException {
        Logger logger = context.getLogger();
        logger.info("Java Timer trigger function executed at: " + LocalDateTime.now());
        // Post the weekly report to Slack
        // Mock data, TODO: use Simo service
        // List<AggregatedStatusGroup> aggregatedStatuses =
        // SlackWeeklyReportMessageUtils.createMockData();

        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.with(DayOfWeek.SUNDAY);

        TransactionStatusAggregationService transactionStatusAggregationService = new TransactionStatusAggregationService();
        List<AggregatedStatusGroup> aggregatedStatuses = transactionStatusAggregationService
                .aggregateStatusCountByDateRange(lastMonday, lastSunday, logger);

        logger.info(
                "Start date: " + lastMonday + " to date: " + lastSunday + ", results: " + aggregatedStatuses.size()
        );

        String report = SlackWeeklyReportMessageUtils.createAggregatedWeeklyReport(aggregatedStatuses);
        SlackWebhookClient.postMessageToWebhook(report);
    }
}
