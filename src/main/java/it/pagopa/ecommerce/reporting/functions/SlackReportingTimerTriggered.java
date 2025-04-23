package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.entity.TransactionMetric;
import it.pagopa.ecommerce.reporting.utils.WeeklyReportUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class SlackReportingTimerTriggered {

    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("SlackReportingTimerTriggered")
    public void run(
            // TEST ONLY
            @HttpTrigger(name = "SlackTrigger",
            route = "slack",
            methods = {HttpMethod.GET},
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            // @TimerTrigger(name = "timerInfo", schedule = "0 * * * * *") String timerInfo,
            final ExecutionContext context
    ) throws JsonProcessingException {
        context.getLogger().info("Java Timer trigger function executed at: " + LocalDateTime.now());
        /// Example with various status types, including custom ones
        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("ACTIVATED", 15);
        statusCounts.put("CLOSED", 8);
        statusCounts.put("NOTIFIED_OK", 6);
        statusCounts.put("EXPIRED", 3);
        statusCounts.put("FAILED", 2);
        statusCounts.put("CUSTOM_STATUS", 4);  // Custom status not in the predefined map

        TransactionMetric metric1 = new TransactionMetric(
                LocalDate.now(),
                "CHECKOUT",
                "PAYPAL-1",
                "PPAL",
                statusCounts
        );
        TransactionMetric metric2 = new TransactionMetric(
                LocalDate.now(),
                "IO",
                "PAYPAL-2",
                "PPAL",
                statusCounts
        );
        TransactionMetric metric3 = new TransactionMetric(
                LocalDate.now(),
                "IO",
                "TYPE3",
                "CPL",
                statusCounts
        );

        List<TransactionMetric> metrics = List.of();

        // Post the weekly report to Slack
        String report = WeeklyReportUtils.createAggregatedWeeklyReport(metrics);
        SlackWebhookClient.postMessageToWebhook(report);
    }
}
