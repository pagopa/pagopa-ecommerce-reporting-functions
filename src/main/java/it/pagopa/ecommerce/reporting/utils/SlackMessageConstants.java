package it.pagopa.ecommerce.reporting.utils;

import java.util.Map;

/**
 * Contains emoji and label constants for use in Slack messages.
 */
public class SlackMessageConstants {
    // Status emojis
    public static final String DEFAULT_STATUS_EMOJI = ":black_circle:";
    public static final String ACTIVATED_EMOJI = ":white_check_mark:";
    public static final String NOTIFIED_OK_EMOJI = ":tada:";
    public static final String EXPIRED_EMOJI = ":alarm_clock:";
    public static final String CANCELED_EMOJI = ":no_entry_sign:";
    public static final String CLOSED_EMOJI = ":lock:";
    public static final String UNAUTHORIZED_EMOJI = ":x:";
    public static final String REFUNDED_EMOJI = ":money_with_wings:";
    public static final String AUTHORIZATION_COMPLETED_EMOJI = ":large_green_circle:";
    public static final String AUTHORIZATION_REQUESTED_EMOJI = ":large_purple_circle:";
    public static final String CANCELLATION_EXPIRED_EMOJI = ":large_orange_circle:";
    public static final String CANCELLATION_REQUESTED_EMOJI = ":large_yellow_circle:";
    public static final String CLOSURE_ERROR_EMOJI = ":red_circle:";
    public static final String CLOSURE_REQUESTED_EMOJI = ":white_circle:";
    public static final String EXPIRED_NOT_AUTHORIZED_EMOJI = ":large_orange_circle:";
    public static final String REFUND_ERROR_EMOJI = ":red_circle:";
    public static final String REFUND_REQUESTED_EMOJI = ":large_orange_circle:";

    // Payment type emojis
    public static final String PAYPAL_EMOJI = ":paypal:";
    public static final String CREDIT_CARD_EMOJI = ":credit_card:";
    public static final String BANK_EMOJI = ":bank:";
    public static final String SATISPAY_EMOJI = ":satispay:";
    public static final String APPLE_EMOJI = ":apple:";
    public static final String GOOGLE_PAY_EMOJI = ":google-pay:";
    public static final String GENERIC_PAYMENT_EMOJI = ":moneybag:";

    // Other emojis
    public static final String PAGOPA_EMOJI = ":pagopa:";
    public static final String CHART_EMOJI = ":bar_chart:";

    // Maps for lookup
    public static final Map<String, TranslationEntry> STATUS_TRANSLATIONS = createStatusTranslations();
    public static final Map<String, TranslationEntry> PAYMENT_TYPE_CODE = createPaymentTypeCodes();

    private static Map<String, TranslationEntry> createStatusTranslations() {
        return Map.ofEntries(
                entry("ACTIVATED", "Attivate", ACTIVATED_EMOJI),
                entry("NOTIFIED_OK", "Complete con notifica", NOTIFIED_OK_EMOJI),
                entry("EXPIRED", "Scadute", EXPIRED_EMOJI),
                entry("CANCELED", "Cancellate", CANCELED_EMOJI),
                entry("CLOSED", "Chiuse", CLOSED_EMOJI),
                entry("UNAUTHORIZED", "Non autorizzate", UNAUTHORIZED_EMOJI),
                entry("REFUNDED", "Rimborsate", REFUNDED_EMOJI),
                entry("AUTHORIZATION_COMPLETED", "Autorizzazione completata", AUTHORIZATION_COMPLETED_EMOJI),
                entry("AUTHORIZATION_REQUESTED", "Autorizzazione richiesta", AUTHORIZATION_REQUESTED_EMOJI),
                entry("CANCELLATION_EXPIRED", "Cancellazione scaduta", CANCELLATION_EXPIRED_EMOJI),
                entry("CANCELLATION_REQUESTED", "Cancellazione richiesta", CANCELLATION_REQUESTED_EMOJI),
                entry("CLOSURE_ERROR", "Closure in errore", CLOSURE_ERROR_EMOJI),
                entry("CLOSURE_REQUESTED", "Closure richiesta", CLOSURE_REQUESTED_EMOJI),
                entry("EXPIRED_NOT_AUTHORIZED", "Scadute - non autorizzate", EXPIRED_NOT_AUTHORIZED_EMOJI),
                entry("REFUND_ERROR", "Errore rimborso", REFUND_ERROR_EMOJI),
                entry("REFUND_REQUESTED", "Rimborso richiesto", REFUND_REQUESTED_EMOJI)
        );
    }

    private static Map<String, TranslationEntry> createPaymentTypeCodes() {
        return Map.ofEntries(
                entry("PPAL", "PayPal", PAYPAL_EMOJI),
                entry("CP", "Carte", CREDIT_CARD_EMOJI),
                entry("BPAY", "Bancomat Pay", BANK_EMOJI),
                entry("RPIC", "Conto Intesa", BANK_EMOJI),
                entry("RBPS", "SCRIGNO Internet Banking", BANK_EMOJI),
                entry("RBPP", "PostePAY", BANK_EMOJI),
                entry("RBPR", "Poste addebito in conto Retail", BANK_EMOJI),
                entry("MYBK", "MyBank", BANK_EMOJI),
                entry("SATY", "Satispay", SATISPAY_EMOJI),
                entry("APPL", "Apple", APPLE_EMOJI),
                entry("RICO", "Redirect IConto", BANK_EMOJI),
                entry("GOOG", "Google Pay", GOOGLE_PAY_EMOJI),
                // Fallback entry
                entry("GENERIC", "<not_used>", GENERIC_PAYMENT_EMOJI)
        );
    }

    private static Map.Entry<String, TranslationEntry> entry(
                                                             String key,
                                                             String translation,
                                                             String emoji
    ) {
        return Map.entry(key, new TranslationEntry(translation, emoji));
    }

    public record TranslationEntry(
            String translation,
            String emoji
    ) {
    }
}
