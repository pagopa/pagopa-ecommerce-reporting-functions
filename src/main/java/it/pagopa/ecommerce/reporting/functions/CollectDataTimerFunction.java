package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.services.WriteDataService;
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CollectDataTimerFunction {

    private final Set<String> ecommerceClientList = MapParametersUtils
            .parseSetString(System.getenv("ECOMMERCE_CLIENTS_LIST")).fold(exception -> {
                throw exception;
            }, Function.identity());

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
        executeFunction(timerInfo, context, clientId);

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
}
