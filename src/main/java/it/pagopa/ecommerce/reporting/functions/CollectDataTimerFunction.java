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

public class CollectDataTimerFunction {

    private ReadDataService readDataService;
    private WriteDataService writeDataService;

    public CollectDataTimerFunction(
            ReadDataService readDataService,
            WriteDataService writeDataService
    ) {
        Set<String> ecommerceClientList = MapParametersUtils.parseSetString(System.getenv("ECOMMERCE_CLIENTS_LIST"))
                .fold(exception -> {
                    throw exception;
                },
                        Function.identity()
                );
        Set<String> paymentTypeCodeList = MapParametersUtils
                .parseSetString(System.getenv("ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST")).fold(exception -> {
                    throw exception;
                },
                        Function.identity()
                );

        Map<String, Set<String>> paymentTypeCodePspIdList = MapParametersUtils
                .parsePspMap(System.getenv("ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST"), paymentTypeCodeList)
                .fold(exception -> {
                    throw exception;
                },
                        Function.identity()
                );
        this.readDataService = ReadDataService.builder().ecommerceClientList(ecommerceClientList)
                .paymentTypeCodeList(paymentTypeCodeList).pspList(paymentTypeCodePspIdList).build();
    }

    @FunctionName("readAndWriteData")
    public void readAndWriteData(
                                 @TimerTrigger(
                                         name = "readAndWriteDataTrigger", schedule = "0 */5 * * * *"
                                 ) String timerInfo,
                                 ExecutionContext context
    ) {
        List<JsonNode> transactionMetricsResponseDtoList = readDataService.readData();
        writeDataService.writeData(transactionMetricsResponseDtoList);
    }

}
