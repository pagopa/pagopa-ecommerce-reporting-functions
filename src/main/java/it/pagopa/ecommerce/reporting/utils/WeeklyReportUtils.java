package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.ecommerce.reporting.entity.SlackMessage;
import it.pagopa.ecommerce.reporting.entity.TransactionMetric;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.ecommerce.reporting.clients.SlackWebhookClient.objectMapper;

public class WeeklyReportUtils {

    /**
     * Map of status keys to their Italian translations and emojis
     */
    private static final Map<String, Map.Entry<String, String>> STATUS_TRANSLATIONS = Map.ofEntries(
            Map.entry("ACTIVATED", new AbstractMap.SimpleEntry<>("Attivate", ":white_check_mark:")),
            Map.entry("CLOSED", new AbstractMap.SimpleEntry<>("Chiuse", ":lock:")),
            Map.entry("NOTIFIED_OK", new AbstractMap.SimpleEntry<>("Notificate OK", ":large_green_circle:")),
            Map.entry("EXPIRED", new AbstractMap.SimpleEntry<>("Scadute", ":alarm_clock:")),
            Map.entry("UNAUTHORIZED", new AbstractMap.SimpleEntry<>("Fallite", ":x:")),
            Map.entry("REFUNDED", new AbstractMap.SimpleEntry<>("Rimborsate", ":money_with_wings:")),
            Map.entry("CANCELED", new AbstractMap.SimpleEntry<>("Cancellate", ":no_entry_sign:"))
    );

    /**
     * Formats status details with proper translations and emojis
     *
     * @param statusCounts Map of status counts
     * @return Formatted status details string
     */
    private static String formatStatusDetails(Map<String, Integer> statusCounts) {
        StringBuilder statusDetails = new StringBuilder();

        // Sort status keys alphabetically for consistent display
        List<String> sortedKeys = new ArrayList<>(statusCounts.keySet());
        Collections.sort(sortedKeys);

        for (String statusKey : sortedKeys) {
            String emoji;
            String translatedStatus;

            // Get translation and emoji if available, otherwise use the key as is with a default emoji
            if (STATUS_TRANSLATIONS.containsKey(statusKey)) {
                Map.Entry<String, String> translation = STATUS_TRANSLATIONS.get(statusKey);
                translatedStatus = translation.getKey();
                emoji = translation.getValue();
            } else {
                translatedStatus = statusKey;
                emoji = ":information_source:";
            }

            statusDetails.append("• ")
                    .append(emoji)
                    .append(" *")
                    .append(translatedStatus)
                    .append("*: ")
                    .append(statusCounts.get(statusKey))
                    .append("\n");
        }

        return statusDetails.toString();
    }

    /**
     * Aggregates multiple transaction metrics by clientId, paymentTypeCode, and pspId
     * and creates a formatted Slack message for weekly reporting
     *
     * @param metrics List of transaction metrics to aggregate
     * @return Formatted Slack message in JSON format
     */
    public static String createAggregatedWeeklyReport(List<TransactionMetric> metrics) throws JsonProcessingException {
        // Set Italian locale for date formatting
        Locale italianLocale = Locale.ITALIAN;

        // Calculate the start and end dates (end date is yesterday, start date is 7 days before)
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(7);

        // Format dates in Italian
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", italianLocale);
        String formattedEndDate = endDate.format(dateFormatter);
        String formattedStartDate = startDate.format(dateFormatter);

        // Group metrics by clientId, paymentTypeCode, and pspId
        Map<String, List<TransactionMetric>> groupedMetrics = metrics.stream()
                .collect(Collectors.groupingBy(metric ->
                        metric.getClientId() + "|" + metric.getPaymentTypeCode() + "|" + metric.getPspId()));

        // Process each group
        /*for (Map.Entry<String, List<TransactionMetric>> entry : groupedMetrics.entrySet()) {
            String[] keys = entry.getKey().split("\\|");
            String clientId = keys[0];
            String paymentTypeCode = keys[1];
            String pspId = keys[2];
            Map<String, Integer> aggregatedStatusCounts = aggregateStatusCounts(entry);

            // Calculate total transactions for this group
            int totalTransactions = aggregatedStatusCounts.values().stream()
                    .mapToInt(Integer::intValue).sum();

            // Format status details
            String statusDetails = formatStatusDetails(aggregatedStatusCounts);

            // Add this group's block to the report
            reportBlocks.add(String.format("""
                    {
                        "type": "divider"
                    },
                    {
                        "type": "section",
                        "fields": [
                            {
                                "type": "mrkdwn",
                                "text": "*ID Cliente:*\n%s"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*Tipo Pagamento:*\n%s"
                            }
                        ]
                    },
                    {
                        "type": "section",
                        "fields": [
                            {
                                "type": "mrkdwn",
                                "text": "*ID PSP:*\n%s"
                            },
                            {
                                "type": "mrkdwn",
                                "text": "*Totale Transazioni:*\n%d"
                            }
                        ]
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "*Dettaglio Stato Transazioni:*\n%s"
                        }
                    }
                    """,
                    clientId,
                    paymentTypeCode,
                    pspId,
                    totalTransactions,
                    statusDetails));
        }*/


        // Then use a JSON library to convert to string
        String jsonPayload = """
                	{
                	"blocks": [
                		{
                			"type": "section",
                			"text": {
                				"type": "mrkdwn",
                				"text": "Hello, Assistant to the Regional Manager Dwight! *Michael Scott* wants to know where you'd like to take the Paper Company investors to dinner tonight.\\n\\n *Please select a restaurant:*"
                			}
                		},
                		{
                             "type": "header",
                             "text": {
                                 "type": "plain_text",
                                 "text": ":tada: Report Settimanale Transazioni",
                                 "emoji": true
                             }
                         }
                	]
                }""";
        // Combine all blocks into the final message
        return jsonPayload;
    }

    private static Map<String, Integer> aggregateStatusCounts(Map.Entry<String, List<TransactionMetric>> entry) {
        List<TransactionMetric> groupMetrics = entry.getValue();

        // Aggregate status counts across all metrics in this group
        Map<String, Integer> aggregatedStatusCounts = new HashMap<>();

        for (TransactionMetric metric : groupMetrics) {
            for (Map.Entry<String, Integer> statusEntry : metric.getStatusCounts().entrySet()) {
                String status = statusEntry.getKey();
                Integer count = statusEntry.getValue();
                aggregatedStatusCounts.put(status,
                        aggregatedStatusCounts.getOrDefault(status, 0) + count);
            }
        }
        return aggregatedStatusCounts;
    }
}