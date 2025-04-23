package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.ecommerce.reporting.clients.SlackWebhookClient;
import it.pagopa.ecommerce.reporting.entity.TransactionMetric;
import it.pagopa.ecommerce.reporting.utils.SlackWeeklyReportUtils;

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
        context.getLogger().info("Java Timer trigger function executed at: " + LocalDateTime.now());
        /// Example with various status types, including custom ones
        // Post the weekly report to Slack
        String report = SlackWeeklyReportUtils.createAggregatedWeeklyReport();
        SlackWebhookClient.postMessageToWebhook(report);
    }
}
