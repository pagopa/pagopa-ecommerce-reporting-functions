package it.pagopa.ecommerce.reporting.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import it.pagopa.ecommerce.reporting.clients.AzureFunctionHttpClient;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CollectDataTimerFunction {

    private final Set<String> ecommerceClientList = MapParametersUtils
            .parseSetString(System.getenv("ECOMMERCE_CLIENTS_LIST")).fold(exception -> {
                throw exception;
            }, Function.identity());

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

    public CollectDataTimerFunction() {
    }

    @FunctionName("readAndWriteData_CHECKOUT")
    public void readAndWriteDataCheckout(
                                         @TimerTrigger(
                                                 name = "readAndWriteDataTrigger", schedule = "%NCRON_SCHEDULE_CHECKOUT%"
                                         ) String timerInfo,
                                         ExecutionContext context
    ) {

        String clientId = "CHECKOUT";
        // executeFunction(timerInfo, context, clientId);
        proxyToHTTP(context, clientId);
    }

    @FunctionName("readAndWriteData_IO")
    public void readAndWriteDataIO(
                                   @TimerTrigger(
                                           name = "readAndWriteDataTrigger", schedule = "%NCRON_SCHEDULE_IO%"
                                   ) String timerInfo,
                                   ExecutionContext context
    ) {
        String clientId = "IO";
        executeFunction(timerInfo, context, clientId);
    }

    @FunctionName("readAndWriteData_CHECKOUT_CART")
    public void readAndWriteDataCheckoutCart(
                                             @TimerTrigger(
                                                     name = "readAndWriteDataTrigger", schedule = "%NCRON_SCHEDULE_CHECKOUT_CART%"
                                             ) String timerInfo,
                                             ExecutionContext context
    ) {
        String clientId = "CHECKOUT_CART";
        executeFunction(timerInfo, context, clientId);
    }

    @FunctionName("readAndWriteData_WISP_REDIRECT")
    public void readAndWriteDataWispRedirect(
                                             @TimerTrigger(
                                                     name = "readAndWriteDataTrigger", schedule = "%NCRON_SCHEDULE_WISP_REDIRECT%"
                                             ) String timerInfo,
                                             ExecutionContext context
    ) {
        String clientId = "WISP_REDIRECT";
        executeFunction(timerInfo, context, clientId);
    }

    @FunctionName("TriggerStringRoute")
    public HttpResponseMessage run(
                                   @HttpTrigger(
                                           name = "req", methods = {
                                                   HttpMethod.GET
                                           }, authLevel = AuthorizationLevel.ANONYMOUS, route = "readAndWrite/{clientId}/{paymentTypeCode}/{pspId}"
                                   ) // name is optional and defaults to EMPTY
                                   HttpRequestMessage<Optional<String>> request,
                                   @BindingName("clientId") String clientId,
                                   @BindingName("paymentTypeCode") String paymentTypeCode,
                                   @BindingName("pspId") String pspId,
                                   final ExecutionContext context
    ) {

        // Item list
        context.getLogger().info("Route parameters are: " + clientId + " " + paymentTypeCode + " " + pspId);
        // Convert and display
        if (clientId == null || paymentTypeCode == null || pspId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .build();
        } else {
            // run execution
            executeSingleQuery(context, clientId, paymentTypeCode, pspId);
            return request.createResponseBuilder(HttpStatus.ACCEPTED)
                    .header("Content-Type", "application/json")
                    .build();
        }
    }

    private void executeSingleQuery(
                                    ExecutionContext context,
                                    String clientId,
                                    String paymentTypeCode,
                                    String pspId
    ) {
        Logger logger = context.getLogger();
        if (!ecommerceClientList.contains(clientId)) {
            logger.log(
                    Level.WARNING,
                    () -> "[CollectDataTimerFunction][client=" + clientId
                            + "isn't present in the client list. No run will be performed for this client. Please update the list if needed]"
            );
        } else if (!paymentTypeCodeList.contains(paymentTypeCode)) {
            logger.log(
                    Level.WARNING,
                    () -> "[CollectDataTimerFunction][paymentTypeCode=" + paymentTypeCode
                            + "isn't present in the paymentTypeCode list. No run will be performed for this paymentTypeCode. Please update the list if needed]"
            );

        } else if (!pspList.get(paymentTypeCode).contains(pspId)) {
            logger.log(
                    Level.WARNING,
                    () -> "[CollectDataTimerFunction][pspId=" + pspId + " for paymentTypeCode=" + paymentTypeCode
                            + "isn't present in the paymentTypeCode_pspId map. No run will be performed for this pspId. Please update the map if needed]"
            );
        } else {
            logger.log(
                    Level.CONFIG,
                    () -> "[CollectDataTimerFunction][client=" + clientId + " paymentTypeCode=" + paymentTypeCode
                            + "pspId=" + pspId
                            + "] new httpTrigger "
            );
            ReadDataService readDataService = this.getReadDataServiceInstance(context.getLogger());
            OffsetDateTime startDateTime = OffsetDateTime.now().minusHours(2).withSecond(0).withMinute(0).withNano(0);
            OffsetDateTime endDateTime = startDateTime.plusHours(1).minusNanos(1);
            readDataService.readAndWriteData(clientId, paymentTypeCode, pspId, startDateTime, endDateTime);
        }

    }

    private void proxyToHTTP(
                             ExecutionContext context,
                             String clientId
    ) {
        Logger logger = context.getLogger();
        if (!ecommerceClientList.contains(clientId)) {
            logger.log(
                    Level.WARNING,
                    () -> "[CollectDataTimerFunction][client=" + clientId
                            + "isn't present in the client list. No run will be performed for this client. Please update the list or delete this function]"
            );
        } else {
            logger.log(
                    Level.CONFIG,
                    () -> "[CollectDataTimerFunction][client=" + clientId + "+id=" + context.getInvocationId()
                            + "] new http trigger "
            );
            String httpTriggerEndPoint = System.getenv("HTTP_TRIGGER_ENDPOINT");
            AzureFunctionHttpClient azureFunctionHttpClient = createAzureFunctionHttpClient(
                    httpTriggerEndPoint,
                    context.getLogger()
            );
            AtomicInteger index = new AtomicInteger(0);
            logger.info("Start read and write");
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            paymentTypeCodeList.forEach(
                    paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).forEach(pspId -> {

                        index.getAndIncrement();
                        TimerTask task = new TimerTask() {

                            @Override
                            public void run() {
                                System.out.println(
                                        "Prepare http GET " + clientId + " " + paymentMethodTypeCode + " " + pspId
                                );
                                azureFunctionHttpClient.invokeGet(clientId, paymentMethodTypeCode, pspId);
                                System.out.println("Sent http GET");

                            }
                        };
                        scheduledExecutorService.schedule(task, index.get(), TimeUnit.SECONDS);
                    }

                    )
            );
        }

    }

    private void executeFunction(
                                 String timerInfo,
                                 ExecutionContext context,
                                 String clientId
    ) {
        Logger logger = context.getLogger();
        if (!ecommerceClientList.contains(clientId)) {
            logger.log(
                    Level.WARNING,
                    () -> "[CollectDataTimerFunction][client=" + clientId
                            + "isn't present in the client list. No run will be performed for this client. Please update the list or delete this function]"
            );
        } else {
            logger.log(
                    Level.CONFIG,
                    () -> "[CollectDataTimerFunction][client=" + clientId + "+id=" + context.getInvocationId()
                            + "] new timer " + timerInfo
            );
            ReadDataService readDataService = this.getReadDataServiceInstance(context.getLogger());
            readDataService.readAndWriteData(clientId);
        }

    }

    protected ReadDataService getReadDataServiceInstance(Logger logger) {
        return ReadDataService.getInstance(logger);
    }

    protected AzureFunctionHttpClient createAzureFunctionHttpClient(
                                                                    String endpoint,
                                                                    Logger logger
    ) {
        return new AzureFunctionHttpClient(endpoint, logger);
    }
}
