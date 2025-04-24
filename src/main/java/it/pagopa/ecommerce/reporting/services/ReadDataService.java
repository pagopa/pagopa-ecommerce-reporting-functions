package it.pagopa.ecommerce.reporting.services;

import com.fasterxml.jackson.databind.JsonNode;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

public class ReadDataService {
    private final Logger logger;
    private static ReadDataService instance = null;
    // private WriteDataService writeDataService = null;

    private final Set<String> paymentTypeCodeList = MapParametersUtils
            .parseSetString(System.getenv("ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST")).fold(exception -> {
                throw exception;
            }, Function.identity());

    private final Map<String, Set<String>> pspList = MapParametersUtils
            .parsePspMap(System.getenv("ECOMMERCE_PAYMENT_METHODS_PSP_LIST"), paymentTypeCodeList)
            .fold(exception -> {
                throw exception;
            },
                    Function.identity()
            );

    private ReadDataService(Logger logger) {
        this.logger = logger;
        // this.writeDataService = WriteDataService.getInstance();
    }

    public static ReadDataService getInstance(Logger logger) {
        if (instance == null) {
            instance = new ReadDataService(logger);
        }
        return instance;
    }

    public void readAndWriteData(
                                 String clientId,
                                 String paymentMethodTypeCode,
                                 String pspId,
                                 OffsetDateTime startDateTime,
                                 OffsetDateTime endDateTime
    ) {
        EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient = getEcommerceHelpdeskServiceClient(this.logger);
        JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                clientId,
                pspId,
                paymentMethodTypeCode,
                startDateTime,
                endDateTime
        );
        logger.info("Node result " + node);
        getWriteDataService()
                .writeStateMetricsInTableStorage(
                        node,
                        logger,
                        clientId,
                        paymentMethodTypeCode,
                        pspId
                );
        logger.info("Storage write completed");
    }

    public void readAndWriteData(String clientId) {
        OffsetDateTime startDateTime = OffsetDateTime.now().minusHours(2).withSecond(0).withMinute(0).withNano(0);
        OffsetDateTime endDateTime = startDateTime.plusHours(1).minusNanos(1);
        EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient = getEcommerceHelpdeskServiceClient(this.logger);
        AtomicInteger index = new AtomicInteger(0);
        logger.info("Start read and write");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        paymentTypeCodeList.forEach(
                paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).forEach(pspId -> {

                    index.getAndIncrement();
                    TimerTask task = new TimerTask() {

                        @Override
                        public void run() {

                            JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                                    clientId,
                                    pspId,
                                    paymentMethodTypeCode,
                                    startDateTime,
                                    endDateTime
                            );
                            logger.info("[LOGGER] Node result " + node);
                            System.out.println("[SYSOUT] Node read " + node);
                            getWriteDataService()
                                    .writeStateMetricsInTableStorage(
                                            node,
                                            logger,
                                            clientId,
                                            paymentMethodTypeCode,
                                            pspId
                                    );
                            System.out.println("[SYSOUT] Node written ");

                        }
                    };
                    scheduledExecutorService.schedule(task, index.get(), TimeUnit.SECONDS);

                }

                )
        );
    }

    protected EcommerceHelpdeskServiceClient getEcommerceHelpdeskServiceClient(Logger logger) {
        return EcommerceHelpdeskServiceClient.getInstance(logger);
    }

    protected WriteDataService getWriteDataService() {
        return WriteDataService.getInstance();
    }
}
