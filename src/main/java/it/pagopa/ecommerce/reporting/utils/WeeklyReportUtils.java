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

        // Mock data, TODO: use Simo service
        List<AggregatedStatusGroup> aggregatedGroups = createMockData();

        // Sort by ACTIVATED in descending order
        aggregatedGroups.sort((a, b) -> {
            Integer aNotifiedOk = a.getStatusCounts().getOrDefault("ACTIVATED", 0);
            Integer bNotifiedOk = b.getStatusCounts().getOrDefault("ACTIVATED", 0);
            return bNotifiedOk.compareTo(aNotifiedOk);
        });

        // Build the blocks for the Slack message
        StringBuilder blocksJson = new StringBuilder();
        blocksJson.append("{\n");
        blocksJson.append("  \"blocks\": [\n");

        // Header block
        blocksJson.append("    {\n");
        blocksJson.append("      \"type\": \"header\",\n");
        blocksJson.append("      \"text\": {\n");
        blocksJson.append("        \"type\": \"plain_text\",\n");
        blocksJson.append("        \"text\": \":tada: Report Settimanale Transazioni\",\n");
        blocksJson.append("        \"emoji\": true\n");
        blocksJson.append("      }\n");
        blocksJson.append("    },\n");

        // Image PagoPA
        blocksJson.append("    {\n");
        blocksJson.append("      \"type\": \"image\",\n");
        blocksJson.append("      \"image_url\": \"https://developer.pagopa.it/gitbook/docs/8phwN5u2QXllSKsqBjQU/.gitbook/assets/logo_asset.png\",\n");
        blocksJson.append("      \"alt_text\": \"PagoPA Logo\",\n");
        blocksJson.append("      \"image_width\": 250,\n");
        blocksJson.append("      \"image_height\": 100\n");
        blocksJson.append("    },\n");

        // Date range section
        blocksJson.append("    {\n");
        blocksJson.append("      \"type\": \"section\",\n");
        blocksJson.append("      \"text\": {\n");
        blocksJson.append("        \"type\": \"mrkdwn\",\n");
        blocksJson.append("        \"text\": \"*Periodo:* " + formattedStartDate + " - " + formattedEndDate + "\"\n");
        blocksJson.append("      }\n");
        blocksJson.append("    },\n");

        // Divider
        blocksJson.append("    {\n");
        blocksJson.append("      \"type\": \"divider\"\n");
        blocksJson.append("    },\n");

        // Add sections for each aggregated group
        for (int i = 0; i < aggregatedGroups.size(); i++) {
            AggregatedStatusGroup group = aggregatedGroups.get(i);

            // Group header
            blocksJson.append("    {\n");
            blocksJson.append("      \"type\": \"section\",\n");
            blocksJson.append("      \"text\": {\n");
            blocksJson.append("        \"type\": \"mrkdwn\",\n");
            blocksJson.append("        \"text\": \"*Client:* " + group.getClientId() + " | *PSP:* " + group.getPspId() + " | *Payment Type:* " + group.getPaymentTypeCode() + "\"\n");
            blocksJson.append("      }\n");
            blocksJson.append("    },\n");

            // Status details
            blocksJson.append("    {\n");
            blocksJson.append("      \"type\": \"section\",\n");
            blocksJson.append("      \"text\": {\n");
            blocksJson.append("        \"type\": \"mrkdwn\",\n");
            blocksJson.append("        \"text\": \"" + formatStatusDetails(group.getStatusCounts()).replace("\n", "\\n") + "\"\n");
            blocksJson.append("      }\n");
            blocksJson.append("    }");

            // Add comma if not the last item
            if (i < aggregatedGroups.size() - 1) {
                blocksJson.append(",\n");
            } else {
                blocksJson.append("\n");
            }
        }

        blocksJson.append("  ]\n");
        blocksJson.append("}");

        return blocksJson.toString();
    }

    /**
     * Creates mock data for testing the report
     *
     * @return List of mock AggregatedStatusGroup objects
     */
    private static List<AggregatedStatusGroup> createMockData() {
        List<AggregatedStatusGroup> mockData = new ArrayList<>();

        // Define some status fields
        List<String> statusFields = Arrays.asList(
                "ACTIVATED", "CLOSED", "NOTIFIED_OK", "EXPIRED", "UNAUTHORIZED",
                "REFUNDED", "CANCELED"
        );

        // Create mock data with different NOTIFIED_OK counts
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Group 1
        AggregatedStatusGroup group1 = new AggregatedStatusGroup(today, "Client1", "PSP001", "CREDIT_CARD", statusFields);
        group1.incrementStatus("ACTIVATED", 1000);
        group1.incrementStatus("CLOSED", 950);
        group1.incrementStatus("NOTIFIED_OK", 900);
        group1.incrementStatus("EXPIRED", 50);
        group1.incrementStatus("UNAUTHORIZED", 30);
        group1.incrementStatus("REFUNDED", 20);
        mockData.add(group1);

        // Group 2
        AggregatedStatusGroup group2 = new AggregatedStatusGroup(today, "Client2", "PSP002", "BANK_TRANSFER", statusFields);
        group2.incrementStatus("ACTIVATED", 800);
        group2.incrementStatus("CLOSED", 780);
        group2.incrementStatus("NOTIFIED_OK", 750);
        group2.incrementStatus("EXPIRED", 20);
        group2.incrementStatus("UNAUTHORIZED", 15);
        group2.incrementStatus("REFUNDED", 10);
        group2.incrementStatus("CANCELED", 5);
        mockData.add(group2);

        // Group 3
        AggregatedStatusGroup group3 = new AggregatedStatusGroup(today, "Client3", "PSP003", "PAYPAL", statusFields);
        group3.incrementStatus("ACTIVATED", 1200);
        group3.incrementStatus("CLOSED", 1150);
        group3.incrementStatus("NOTIFIED_OK", 1100);
        group3.incrementStatus("EXPIRED", 50);
        group3.incrementStatus("UNAUTHORIZED", 25);
        group3.incrementStatus("REFUNDED", 15);
        group3.incrementStatus("CANCELED", 10);
        mockData.add(group3);

        // Group 4
        AggregatedStatusGroup group4 = new AggregatedStatusGroup(today, "Client1", "PSP004", "DEBIT_CARD", statusFields);
        group4.incrementStatus("ACTIVATED", 500);
        group4.incrementStatus("CLOSED", 480);
        group4.incrementStatus("NOTIFIED_OK", 450);
        group4.incrementStatus("EXPIRED", 20);
        group4.incrementStatus("UNAUTHORIZED", 10);
        group4.incrementStatus("REFUNDED", 5);
        mockData.add(group4);

        return mockData;
    }
}