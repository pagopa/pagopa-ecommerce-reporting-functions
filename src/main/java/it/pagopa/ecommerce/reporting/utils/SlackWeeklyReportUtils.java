package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SlackWeeklyReportUtils {

    /**
     * Map of status keys to their Italian translations and emojis
     */
    private static final Map<String, Map.Entry<String, String>> STATUS_TRANSLATIONS = Map.ofEntries(
            Map.entry("ACTIVATED", new AbstractMap.SimpleEntry<>("Attivate", " :white_check_mark:")),
            Map.entry("NOTIFIED_OK", new AbstractMap.SimpleEntry<>("Complete con notifica", ":tada:")),
            Map.entry("EXPIRED", new AbstractMap.SimpleEntry<>("Scadute", ":alarm_clock:")),
            Map.entry("CANCELED", new AbstractMap.SimpleEntry<>("Cancellate", ":no_entry_sign:")),
            Map.entry("CLOSED", new AbstractMap.SimpleEntry<>("Chiuse", ":lock:")),
            Map.entry("UNAUTHORIZED", new AbstractMap.SimpleEntry<>("Non autorizzate", ":x:")),
            Map.entry("REFUNDED", new AbstractMap.SimpleEntry<>("Rimborsate", ":money_with_wings:"))
    );

    /**
     * Map of payment type codes and emojis, if present
     */
    private static final Map<String, Map.Entry<String, String>> PAYMENT_TYPE_CODE = Map.ofEntries(
            Map.entry("PAYPAL", new AbstractMap.SimpleEntry<>("PayPal", ":paypal:")),
            Map.entry("BANK_TRANSFER", new AbstractMap.SimpleEntry<>("Tramite banca", ":bank:")),
            Map.entry("CREDIT_CARD", new AbstractMap.SimpleEntry<>("Carta di credito", ":credit_card:")),
            Map.entry("DEBIT_CARD", new AbstractMap.SimpleEntry<>("Carta di debito", ":credit_card:")),
            // Not a possible key, it's a fallback
            Map.entry("GENERIC", new AbstractMap.SimpleEntry<>("Generico", ":money_bag:"))
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

            // Get translation and emoji if available, otherwise use the key as is with a
            // default emoji
            if (STATUS_TRANSLATIONS.containsKey(statusKey)) {
                Map.Entry<String, String> translation = STATUS_TRANSLATIONS.get(statusKey);
                translatedStatus = translation.getKey();
                emoji = translation.getValue();
            } else {
                translatedStatus = statusKey;
                emoji = ":information_source:";
            }

            statusDetails.append("   ")
                    .append(emoji)
                    .append(" *")
                    .append(translatedStatus)
                    .append("*: ")
                    .append(statusCounts.get(statusKey))
                    .append("\n\n");
        }

        return statusDetails.toString();
    }

    /**
     * Aggregates multiple transaction metrics by clientId, paymentTypeCode, and
     * pspId and creates a formatted Slack message for weekly reporting
     *
     * @return Formatted Slack message in JSON format
     */
    public static String createAggregatedWeeklyReport() throws JsonProcessingException {
        // Calculate date range
        DateRange dateRange = calculateDateRange();

        // Mock data, TODO: use Simo service
        List<AggregatedStatusGroup> aggregatedGroups = createMockData();

        // Sort aggregated groups
        sortAggregatedGroups(aggregatedGroups);

        // Create the Slack message structure
        Map<String, Object> message = new HashMap<>();
        List<Map<String, Object>> blocks = new ArrayList<>();

        // Add header blocks
        blocks.add(getHeaderBlock(dateRange.startDate, dateRange.endDate));
        blocks.add(getImageBlock());
        blocks.add(getHeaderDescriptionBlock(dateRange.startDate, dateRange.endDate));
        blocks.add(getDivider());

        // Add sections for each aggregated group
        for (AggregatedStatusGroup group : aggregatedGroups) {
            blocks.add(getGroupHeaderSection(group));
            blocks.add(getStatusDetailsSection(group));
            blocks.add(getDivider());
        }

        message.put("blocks", blocks);

        // Convert to JSON
        return convertToJson(message);
    }

    // Date range calculation helper
    private static DateRange calculateDateRange() {
        Locale italianLocale = Locale.ITALIAN;
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(7);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", italianLocale);
        String formattedEndDate = endDate.format(dateFormatter);
        String formattedStartDate = startDate.format(dateFormatter);

        return new DateRange(formattedStartDate, formattedEndDate);
    }

    // Sort aggregated groups by ACTIVATED count in descending order
    private static void sortAggregatedGroups(List<AggregatedStatusGroup> aggregatedGroups) {
        aggregatedGroups.sort(
                (
                 a,
                 b
                ) -> {
                    Integer aNotifiedOk = a.getStatusCounts().getOrDefault("ACTIVATED", 0);
                    Integer bNotifiedOk = b.getStatusCounts().getOrDefault("ACTIVATED", 0);
                    return bNotifiedOk.compareTo(aNotifiedOk);
                }
        );
    }

    // Create a Slack message header block
    private static Map<String, Object> getHeaderBlock(
                                                      String startDate,
                                                      String endDate
    ) {
        Map<String, Object> headerBlock = new HashMap<>();
        headerBlock.put("type", "header");

        Map<String, Object> headerText = new HashMap<>();
        headerText.put("type", "plain_text");
        headerText.put("text", ":pagopa: Report Settimanale Transazioni " + startDate + " - " + endDate + " :pagopa:");
        headerText.put("emoji", true);

        headerBlock.put("text", headerText);
        return headerBlock;
    }

    // Create a Slack message header block
    private static Map<String, Object> getHeaderDescriptionBlock(
                                                                 String startDate,
                                                                 String endDate
    ) {
        Map<String, Object> headerBlock = new HashMap<>();
        headerBlock.put("type", "header");

        Map<String, Object> headerText = new HashMap<>();
        headerText.put("type", "plain_text");
        headerText.put(
                "text",
                "Di seguito il report suddiviso per Client, PSP e metodo pagamento di pagamento per l'intervallo di tempo dal "
                        + startDate + " al " + endDate
        );
        headerText.put("emoji", true);

        headerBlock.put("text", headerText);
        return headerBlock;
    }

    // Create a Slack message image block
    private static Map<String, Object> getImageBlock() {
        Map<String, Object> imageBlock = new HashMap<>();
        imageBlock.put("type", "image");
        imageBlock.put(
                "image_url",
                "https://pagopa.portaleamministrazionetrasparente.it/moduli/output_media.php?file=enti_trasparenza/2197912210O__Ologo-pagopa-spa.png"
        );
        imageBlock.put("alt_text", "PagoPA Logo");
        imageBlock.put("image_width", 474);
        imageBlock.put("image_height", 133);
        return imageBlock;
    }

    // Create a Slack message divider
    private static Map<String, Object> getDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        return divider;
    }

    // Create a Slack message section with group header
    private static Map<String, Object> getGroupHeaderSection(AggregatedStatusGroup group) {
        Map<String, Object> groupHeader = new HashMap<>();
        groupHeader.put("type", "section");

        Map<String, Object> headerContent = new HashMap<>();
        headerContent.put("type", "mrkdwn");
        headerContent.put(
                "text",
                ":bar_chart: STATISTICHE" +
                        "\nClient *" + group.getClientId() +
                        "* con PSP *" + group.getPspId() +
                        "* e pagato con " + formatPaymentTypeCode(group.getPaymentTypeCode())
        );

        groupHeader.put("text", headerContent);
        return groupHeader;
    }

    private static String formatPaymentTypeCode(String paymentTypeCode) {

        String emoji;
        String enhancedPaymentTypeCode;

        Map.Entry<String, String> translation;

        StringBuilder enhancedPaymentCodeStringBuilder = new StringBuilder();

        // Get translation and emoji if available, otherwise use the key as is with a
        // default emoji
        if (PAYMENT_TYPE_CODE.containsKey(paymentTypeCode)) {
            translation = PAYMENT_TYPE_CODE.get(paymentTypeCode);
            enhancedPaymentTypeCode = translation.getKey();
        } else {
            translation = PAYMENT_TYPE_CODE.get(paymentTypeCode);
            enhancedPaymentTypeCode = paymentTypeCode;
        }

        emoji = translation.getValue();

        enhancedPaymentCodeStringBuilder.append("   ")
                .append(emoji)
                .append(" *")
                .append(enhancedPaymentTypeCode)
                .append("*");

        return enhancedPaymentCodeStringBuilder.toString();

    }

    // Create a Slack message section with status details
    private static Map<String, Object> getStatusDetailsSection(AggregatedStatusGroup group) {
        Map<String, Object> statusSection = new HashMap<>();
        statusSection.put("type", "section");

        Map<String, Object> statusContent = new HashMap<>();
        statusContent.put("type", "mrkdwn");
        statusContent.put("text", formatStatusDetails(group.getStatusCounts()));

        statusSection.put("text", statusContent);
        return statusSection;
    }

    // Convert message object to JSON string
    private static String convertToJson(Map<String, Object> message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(message);
    }

    // Helper class to store date range information
    private record DateRange(
            String startDate,
            String endDate
    ) {
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
                "ACTIVATED",
                "CLOSED",
                "NOTIFIED_OK",
                "EXPIRED",
                "UNAUTHORIZED",
                "REFUNDED",
                "CANCELED"
        );

        // Create mock data with different NOTIFIED_OK counts
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Group 1
        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                today,
                "Client1",
                "PSP001",
                "CREDIT_CARD",
                statusFields
        );
        group1.incrementStatus("ACTIVATED", 1000);
        group1.incrementStatus("CLOSED", 950);
        group1.incrementStatus("NOTIFIED_OK", 900);
        group1.incrementStatus("EXPIRED", 50);
        group1.incrementStatus("UNAUTHORIZED", 30);
        group1.incrementStatus("REFUNDED", 20);
        mockData.add(group1);

        // Group 2
        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                today,
                "Client2",
                "PSP002",
                "BANK_TRANSFER",
                statusFields
        );
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
        AggregatedStatusGroup group4 = new AggregatedStatusGroup(
                today,
                "Client1",
                "PSP004",
                "DEBIT_CARD",
                statusFields
        );
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
