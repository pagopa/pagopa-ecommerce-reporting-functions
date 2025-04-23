package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableAsyncClient;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableTransactionAction;
import com.azure.data.tables.models.TableTransactionActionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.ecommerce.reporting.entity.StateMetricEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteDataService {

    private static WriteDataService instance = null;
    private final String storageConnectionString = System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING");
    private final String tableName = System.getenv("ECOMMERCE_REPORTING_TABLE");
    private final TableClient tableClient;

    private WriteDataService() {
        tableClient = new TableClientBuilder()
                .connectionString(storageConnectionString)
                .tableName(tableName)
                .buildClient();
    }

    public static WriteDataService getInstance() {
        if (instance == null) {
            instance = new WriteDataService();
        }
        return instance;
    }

    public void writeData(List<JsonNode> metricsResponseDtos) {
        // try {
        // get the table

        List<TableTransactionAction> transactionActions = new ArrayList<>();

        metricsResponseDtos.forEach(jsonNode -> {
            Map<String, Integer> value = new HashMap<>();
            value.put("ACTIVATED", 1);
            value.put("NOTIFIED_OK", 2);
            transactionActions.add(
                    new TableTransactionAction(
                            TableTransactionActionType.CREATE,
                            StateMetricEntity.createEntity(
                                    LocalDate.now(),
                                    "clientId",
                                    "paymentTypeCode",
                                    "pspId",
                                    value
                            )
                    )
            );
        });

        tableClient.submitTransaction(transactionActions).getTransactionActionResponses().forEach(
                tableTransactionActionResponse -> System.out
                        .printf("%n%d", tableTransactionActionResponse.getStatusCode())
        );

        /*
         * } catch (URISyntaxException | InvalidKeyException |) { //exception }
         */

    }
}
