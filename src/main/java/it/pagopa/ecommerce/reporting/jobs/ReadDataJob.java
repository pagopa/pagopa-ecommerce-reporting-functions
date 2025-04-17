package it.pagopa.ecommerce.reporting.jobs;

import it.pagopa.ecommerce.reporting.utils.ClientId;
import it.pagopa.ecommerce.reporting.utils.PSP;
import it.pagopa.ecommerce.reporting.utils.PaymentMethodTypeCode;

import java.util.ArrayList;
import java.util.Arrays;

public class ReadDataJob {

    public void readData() {
        ClientId[] clientIds = {ClientId.IO, ClientId.CHECKOUT, ClientId.CHECKOUT_CART, ClientId.WISP_REDIRECT};
        PaymentMethodTypeCode[] paymentMethodTypeCodes = {PaymentMethodTypeCode.CARDS, PaymentMethodTypeCode.PAYPAL};
        PSP[] psps = {PSP.PSP_1};
        Arrays.stream(clientIds).parallel().forEach(clientId -> {
            Arrays.stream(paymentMethodTypeCodes).parallel().forEach(paymentMethodTypeCode -> {
                Arrays.stream(psps).parallel().forEach(psp -> {

                });
            });
        });
    }

}
