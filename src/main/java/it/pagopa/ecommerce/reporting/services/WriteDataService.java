package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableAsyncClient;
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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WriteDataService {

    private static WriteDataService instance = null;
    private final String storageConnectionString = System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING");
    private final String tableName = System.getenv("ECOMMERCE_REPORTING_TABLE");

    public static WriteDataService getInstance() {
        if (instance == null) {
            instance = new WriteDataService();
        }
        return instance;
    }

    public void writeData(List<JsonNode> metricsResponseDtos) {
        // try {
        // get the table
        TableAsyncClient tableAsyncClient = new TableClientBuilder()
                .connectionString(storageConnectionString)
                .tableName(tableName)
                .buildAsyncClient();
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

        tableAsyncClient.submitTransaction(transactionActions)
                // .contextWrite(Context.of("key1", "value1", "key2", "value2"))
                .subscribe(tableTransactionResult -> {
                    System.out.print("Submitted transaction. The ordered response status codes for the actions are:");

                    tableTransactionResult.getTransactionActionResponses().forEach(
                            tableTransactionActionResponse -> System.out
                                    .printf("%n%d", tableTransactionActionResponse.getStatusCode())
                    );
                });

        /*
         * } catch (URISyntaxException | InvalidKeyException |) { //exception }
         */

    }
}
