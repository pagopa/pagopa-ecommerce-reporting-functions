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
    private static final String PAGOPA_LOGO_URL = "https://developer.pagopa.it/gitbook/docs/8phwN5u2QXllSKsqBjQU/.gitbook/assets/logo_asset.png";
    private static final int MAX_BLOCKS_PER_MESSAGE = 49;

    /**
     * Creates an aggregated weekly report message for Slack, split into multiple
     * messages with a maximum blocks size each.
     *
     * @param aggregatedGroups List of aggregated status groups
     * @param startDate        Report start date
     * @param endDate          Report end date
     * @param logger           Logger instance
     * @return Array of formatted Slack messages in JSON format
     * @throws JsonProcessingException If JSON conversion fails
     */
    public static String[] createAggregatedWeeklyReport(
                                                        List<AggregatedStatusGroup> aggregatedGroups,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Logger logger
    ) throws JsonProcessingException {
        List<AggregatedStatusGroup> sortedGroups = sortAggregatedGroups(aggregatedGroups);
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        List<Map<String, Object>> allBlocks = new ArrayList<>();

        // Add header blocks (these will be in the first message)
        allBlocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
        allBlocks.add(createImageBlock());
        allBlocks.add(createHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
        allBlocks.add(createDivider());

        // Add sections for each aggregated group
        for (AggregatedStatusGroup group : sortedGroups) {
            allBlocks.add(createGroupHeaderSection(group));
            allBlocks.add(createStatusDetailsSection(group));
            allBlocks.add(createDivider());
        }

        // Split blocks into messages with max N blocks each
        List<String> messages = new ArrayList<>();
        int totalBlocks = allBlocks.size();
        int messageCount = (int) Math.ceil((double) totalBlocks / MAX_BLOCKS_PER_MESSAGE);

        for (int i = 0; i < messageCount; i++) {
            int startIndex = i * MAX_BLOCKS_PER_MESSAGE;
            int endIndex = Math.min(startIndex + MAX_BLOCKS_PER_MESSAGE, totalBlocks);

            List<Map<String, Object>> messageBlocks = allBlocks.subList(startIndex, endIndex);

            Map<String, Object> message = new HashMap<>();
            message.put("blocks", messageBlocks);

            messages.add(OBJECT_MAPPER.writeValueAsString(message));
        }

        logger.info("Created " + messages.size() + " slack messages with a total of " + totalBlocks + " blocks");
        return messages.toArray(new String[0]);
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
            SlackMessageConstants.TranslationEntry entry = SlackMessageConstants.STATUS_TRANSLATIONS.getOrDefault(
                    statusKey,
                    new SlackMessageConstants.TranslationEntry(statusKey, SlackMessageConstants.DEFAULT_STATUS_EMOJI)
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
        SlackMessageConstants.TranslationEntry entry = SlackMessageConstants.PAYMENT_TYPE_CODE.getOrDefault(
                paymentTypeCode,
                new SlackMessageConstants.TranslationEntry(
                        paymentTypeCode,
                        SlackMessageConstants.PAYMENT_TYPE_CODE.get("GENERIC").emoji()
                )
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
                SlackMessageConstants.PAGOPA_EMOJI + " Report Settimanale Transazioni " + startDate + " - " + endDate
                        + " " + SlackMessageConstants.PAGOPA_EMOJI,
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
        String text = SlackMessageConstants.CHART_EMOJI + " STATISTICHE" +
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
}
