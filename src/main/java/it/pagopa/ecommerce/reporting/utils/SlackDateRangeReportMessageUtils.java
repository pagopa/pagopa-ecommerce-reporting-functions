package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Builds Slack messages using Block Kit elements for date range reports.
 *
 * @see <a href="https://api.slack.com/reference/block-kit/block-element">Slack
 *      block elements reference</a>
 */
public class SlackDateRangeReportMessageUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN);

    // Translation constants
    private static final Map<String, TranslationEntry> STATUS_TRANSLATIONS = Map.ofEntries(
            entry("ACTIVATED", "Attivate", ":white_check_mark:"),
            entry("NOTIFIED_OK", "Complete con notifica", ":tada:"),
            entry("EXPIRED", "Scadute", ":alarm_clock:"),
            entry("CANCELED", "Cancellate", ":no_entry_sign:"),
            entry("CLOSED", "Chiuse", ":lock:"),
            entry("UNAUTHORIZED", "Non autorizzate", ":x:"),
            entry("REFUNDED", "Rimborsate", ":money_with_wings:"),
            entry("AUTHORIZATION_COMPLETED", "Autorizzazione completata", ":large_green_circle:"),
            entry("AUTHORIZATION_REQUESTED", "Autorizzazione richiesta", ":large_purple_circle:"),
            entry("CANCELLATION_EXPIRED", "Cancellazione scaduta", ":large_orange_circle:"),
            entry("CANCELLATION_REQUESTED", "Cancellazione richiesta", ":large_yellow_circle:"),
            entry("CLOSURE_ERROR", "Closure in errore", ":red_circle:"),
            entry("CLOSURE_REQUESTED", "Closure richiesta", ":white_circle:"),
            entry("EXPIRED_NOT_AUTHORIZED", "Scadute - non autorizzate", ":large_orange_circle:"),
            entry("REFUND_ERROR", "Errore rimborso", ":red_circle:"),
            entry("REFUND_REQUESTED", "Rimborso richiesto", ":large_orange_circle:")
    );

    private static final Map<String, TranslationEntry> PAYMENT_TYPE_CODE = Map.ofEntries(
            entry("PAYPAL", "PayPal", ":paypal:"),
            entry("BANK_TRANSFER", "Tramite banca", ":bank:"),
            entry("CREDIT_CARD", "Carta di credito", ":credit_card:"),
            entry("CREDIT", "Carta di credito", ":credit_card:"),
            entry("DEBIT_CARD", "Carta di debito", ":credit_card:"),
            entry("DEBIT", "Carta di debito", ":credit_card:"),
            // Fallback entry
            entry("GENERIC", "<not_used>", ":moneybag:")
    );

    private static final String DEFAULT_STATUS_EMOJI = ":black_circle:";
    private static final String PAGOPA_LOGO_URL = "https://pagopa.portaleamministrazionetrasparente.it/moduli/output_media.php?file=enti_trasparenza/2197912210O__Ologo-pagopa-spa.png";

    /**
     * Creates an aggregated weekly report message for Slack.
     *
     * @param aggregatedGroups List of aggregated status groups
     * @param startDate        Report start date
     * @param endDate          Report end date
     * @param logger           Logger instance
     * @return Formatted Slack message in JSON format
     * @throws JsonProcessingException If JSON conversion fails
     */
    public static String createAggregatedWeeklyReport(
                                                      List<AggregatedStatusGroup> aggregatedGroups,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Logger logger
    ) throws JsonProcessingException {

        List<AggregatedStatusGroup> sortedGroups = sortAggregatedGroups(aggregatedGroups);
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        Map<String, Object> message = new HashMap<>();
        List<Map<String, Object>> blocks = new ArrayList<>();

        // Add header blocks
        blocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
        blocks.add(createImageBlock());
        blocks.add(createHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
        blocks.add(createDivider());

        // Add sections for each aggregated group
        for (AggregatedStatusGroup group : sortedGroups) {
            blocks.add(createGroupHeaderSection(group));
            blocks.add(createStatusDetailsSection(group));
            blocks.add(createDivider());
        }

        message.put("blocks", blocks);
        logger.info("slack message blocks created successfully");
        return OBJECT_MAPPER.writeValueAsString(message);
    }

    /**
     * Sorts aggregated groups by ACTIVATED count in descending order.
     *
     * @param aggregatedGroups Groups to sort
     * @return Sorted list of groups
     */
    static List<AggregatedStatusGroup> sortAggregatedGroups(List<AggregatedStatusGroup> aggregatedGroups) {
        List<AggregatedStatusGroup> sortedGroups = new ArrayList<>(aggregatedGroups);
        sortedGroups.sort(
                Comparator.comparing(
                        group -> group.getStatusCounts().getOrDefault("ACTIVATED", 0),
                        Comparator.reverseOrder()
                )
        );
        return sortedGroups;
    }

    /**
     * Formats a date.
     *
     * @param date Date to format
     * @return Formatted date string
     */
    static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Formats status details with proper translations and emojis.
     *
     * @param statusCounts Map of status counts
     * @return Formatted status details string
     */
    static String formatStatusDetails(Map<String, Integer> statusCounts) {
        StringBuilder statusDetails = new StringBuilder();
        List<String> sortedKeys = new ArrayList<>(statusCounts.keySet());
        Collections.sort(sortedKeys);

        for (String statusKey : sortedKeys) {
            TranslationEntry entry = STATUS_TRANSLATIONS.getOrDefault(
                    statusKey,
                    new TranslationEntry(statusKey, DEFAULT_STATUS_EMOJI)
            );

            statusDetails.append("   ")
                    .append(entry.emoji())
                    .append(" *")
                    .append(entry.translation())
                    .append("*: ")
                    .append(statusCounts.get(statusKey))
                    .append("\n\n");
        }

        return statusDetails.toString();
    }

    /**
     * Formats payment type code with description and emoji.
     *
     * @param paymentTypeCode Payment type code
     * @return Formatted payment type string
     */
    static String formatPaymentTypeCode(String paymentTypeCode) {
        TranslationEntry entry = PAYMENT_TYPE_CODE.getOrDefault(
                paymentTypeCode,
                new TranslationEntry(paymentTypeCode, PAYMENT_TYPE_CODE.get("GENERIC").emoji())
        );

        return "   " + entry.emoji() + " *" + entry.translation() + "*";
    }

    // Block creation methods
    static Map<String, Object> createHeaderBlock(
                                                 String startDate,
                                                 String endDate
    ) {
        return createTextBlock(
                "header",
                "plain_text",
                ":pagopa: Report Settimanale Transazioni " + startDate + " - " + endDate + " :pagopa:",
                true
        );
    }

    static Map<String, Object> createHeaderDescriptionBlock(
                                                            String startDate,
                                                            String endDate
    ) {
        return createTextBlock(
                "header",
                "plain_text",
                "Di seguito il report suddiviso per Client, PSP e metodo pagamento di pagamento per l'intervallo di tempo dal "
                        + startDate + " al " + endDate,
                true
        );
    }

    static Map<String, Object> createImageBlock() {
        Map<String, Object> imageBlock = new HashMap<>();
        imageBlock.put("type", "image");
        imageBlock.put("image_url", PAGOPA_LOGO_URL);
        imageBlock.put("alt_text", "PagoPA Logo");
        imageBlock.put("image_width", 474);
        imageBlock.put("image_height", 133);
        return imageBlock;
    }

    static Map<String, Object> createDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        return divider;
    }

    static Map<String, Object> createGroupHeaderSection(AggregatedStatusGroup group) {
        String text = ":bar_chart: STATISTICHE" +
                "\n\t\t| Client *" + group.getClientId() +
                "* con PSP *" + group.getPspId() +
                "* e pagato con " + formatPaymentTypeCode(group.getPaymentTypeCode());

        return createTextBlock("section", "mrkdwn", text, false);
    }

    static Map<String, Object> createStatusDetailsSection(AggregatedStatusGroup group) {
        return createTextBlock(
                "section",
                "mrkdwn",
                formatStatusDetails(group.getStatusCounts()),
                false
        );
    }

    // Helper method to create text blocks
    static Map<String, Object> createTextBlock(
                                               String blockType,
                                               String textType,
                                               String content,
                                               boolean emoji
    ) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", blockType);

        Map<String, Object> text = new HashMap<>();
        text.put("type", textType);
        text.put("text", content);

        if (emoji) {
            text.put("emoji", true);
        }

        block.put("text", text);
        return block;
    }

    // Helper method to create map entries
    private static Map.Entry<String, TranslationEntry> entry(
                                                             String key,
                                                             String translation,
                                                             String emoji
    ) {
        return Map.entry(key, new TranslationEntry(translation, emoji));
    }

    /**
     * Record to store translation and emoji information.
     */
    record TranslationEntry(
            String translation,
            String emoji
    ) {
    }
}
