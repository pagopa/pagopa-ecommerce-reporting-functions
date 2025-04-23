package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class will build a message using Slack blocks elements.
 * Ref: <a href="https://api.slack.com/reference/block-kit/block-element">Slack block elements reference</a>.
 * The data is passed as argument, as well as start and end date for the report.
 */
public class SlackDateRangeReportMessageUtils {

    /**
     * Map of transaction status keys to their Italian translations and emojis
     */
    private static final Map<String, Map.Entry<String, String>> STATUS_TRANSLATIONS = Map.ofEntries(
            Map.entry("ACTIVATED", new AbstractMap.SimpleEntry<>("Attivate", " :white_check_mark:")),
            Map.entry("NOTIFIED_OK", new AbstractMap.SimpleEntry<>("Complete con notifica", ":tada:")),
            Map.entry("EXPIRED", new AbstractMap.SimpleEntry<>("Scadute", ":alarm_clock:")),
            Map.entry("CANCELED", new AbstractMap.SimpleEntry<>("Cancellate", ":no_entry_sign:")),
            Map.entry("CLOSED", new AbstractMap.SimpleEntry<>("Chiuse", ":lock:")),
            Map.entry("UNAUTHORIZED", new AbstractMap.SimpleEntry<>("Non autorizzate", ":x:")),
            Map.entry("REFUNDED", new AbstractMap.SimpleEntry<>("Rimborsate", ":money_with_wings:")),
            Map.entry(
                    "AUTHORIZATION_COMPLETED",
                    new AbstractMap.SimpleEntry<>("Autorizzazione completata", ":large_green_circle:")
            ),
            Map.entry(
                    "AUTHORIZATION_REQUESTED",
                    new AbstractMap.SimpleEntry<>("Autorizzazione richiesta", ":large_purple_circle:")
            ),
            Map.entry(
                    "CANCELLATION_EXPIRED",
                    new AbstractMap.SimpleEntry<>("Cancellazione scaduta", ":large_orange_circle:")
            ),
            Map.entry(
                    "CANCELLATION_REQUESTED",
                    new AbstractMap.SimpleEntry<>("Cancellazione richiesta", ":large_yellow_circle:")
            ),
            Map.entry("CLOSURE_ERROR", new AbstractMap.SimpleEntry<>("Closure in errore", ":red_circle:")),
            Map.entry("CLOSURE_REQUESTED", new AbstractMap.SimpleEntry<>("Closure richiesta", ":white_circle:")),
            Map.entry(
                    "EXPIRED_NOT_AUTHORIZED",
                    new AbstractMap.SimpleEntry<>("Scadute - non autorizzate", ":large_orange_circle:")
            ),
            Map.entry("REFUND_ERROR", new AbstractMap.SimpleEntry<>("Errore rimborso", ":red_circle:")),
            Map.entry("REFUND_REQUESTED", new AbstractMap.SimpleEntry<>("Rimborso richiesto", ":large_orange_circle:"))

    );

    /**
     * Map of payment type codes and emojis, if present
     */
    private static final Map<String, Map.Entry<String, String>> PAYMENT_TYPE_CODE = Map.ofEntries(
            Map.entry("PAYPAL", new AbstractMap.SimpleEntry<>("PayPal", ":paypal:")),
            Map.entry("BANK_TRANSFER", new AbstractMap.SimpleEntry<>("Tramite banca", ":bank:")),
            Map.entry("CREDIT_CARD", new AbstractMap.SimpleEntry<>("Carta di credito", ":credit_card:")),
            Map.entry("CREDIT", new AbstractMap.SimpleEntry<>("Carta di credito", ":credit_card:")),
            Map.entry("DEBIT_CARD", new AbstractMap.SimpleEntry<>("Carta di debito", ":credit_card:")),
            Map.entry("DEBIT", new AbstractMap.SimpleEntry<>("Carta di debito", ":credit_card:")),
            // Not a possible key, it's a fallback
            Map.entry("GENERIC", new AbstractMap.SimpleEntry<>("<not_used>", ":moneybag:"))
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

            // Get translation and emoji if available
            if (STATUS_TRANSLATIONS.containsKey(statusKey)) {
                Map.Entry<String, String> translation = STATUS_TRANSLATIONS.get(statusKey);
                translatedStatus = translation.getKey();
                emoji = translation.getValue();
            // otherwise use the key as is with a default emoji
            } else {
                translatedStatus = statusKey;
                emoji = ":black_circle:";
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
    public static String createAggregatedWeeklyReport(List<AggregatedStatusGroup> aggregatedGroups, LocalDate startDate, LocalDate endDate, Logger logger)
            throws JsonProcessingException {

        // Sort aggregated groups
        sortAggregatedGroups(aggregatedGroups);

        // Create the Slack message structure
        Map<String, Object> message = new HashMap<>();
        List<Map<String, Object>> blocks = new ArrayList<>();

        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        // Add header blocks
        blocks.add(getHeaderBlock(formattedStartDate, formattedEndDate));
        blocks.add(getImageBlock());
        blocks.add(getHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
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
    private static String formatDate(LocalDate date) {
        Locale italianLocale = Locale.ITALIAN;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", italianLocale);
        return date.format(dateFormatter);
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
                        "\n\t\t| Client *" + group.getClientId() +
                        "* con PSP *" + group.getPspId() +
                        "* e pagato con " + formatPaymentTypeCode(group.getPaymentTypeCode())
        );

        groupHeader.put("text", headerContent);
        return groupHeader;
    }

    /**
     * Format the payment type code, with an italian description and related
     * emoji, if present. Otherwise, return the raw type code and a generic emoji.
     * @param paymentTypeCode payment type code
     * @return the formatted representation of the type code
     */
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
            translation = PAYMENT_TYPE_CODE.get("GENERIC");
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
}
