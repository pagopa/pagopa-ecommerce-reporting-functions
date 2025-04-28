package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import it.pagopa.ecommerce.reporting.entity.StateMetricEntity;
import it.pagopa.ecommerce.reporting.utils.StatusStorageFields;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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

    public WriteDataService(TableClient tableClient) {
        this.tableClient = tableClient;
    }

    public static WriteDataService getInstance() {
        if (instance == null) {
            instance = new WriteDataService();
        }
        return instance;
    }

    public void writeStateMetricsInTableStorage(
                                                JsonNode jsonNode,
                                                Logger log,
                                                String clientId,
                                                String paymentTypeCode,
                                                String pspId
    ) {
        try {
            Map<String, Integer> statusCounts = new HashMap<>();
            for (String status : StatusStorageFields.values) {
                JsonNode valueNode = jsonNode.get(status);
                if (valueNode != null && valueNode.isInt()) {
                    statusCounts.put(status, valueNode.asInt());
                }
            }

            TableEntity entity = StateMetricEntity.createEntity(
                    LocalDate.now(),
                    clientId,
                    paymentTypeCode,
                    pspId,
                    statusCounts
            );

            tableClient.createEntity(entity);
            log.info("Successfully inserted state metrics for clientId: " + clientId + ", pspId: " + pspId);
        } catch (Exception e) {
            log.warning(
                    "Failed to write state metrics to Azure Table Storage. Error: " + e.getMessage() +
                            " | JSON content: " + jsonNode.toString()
            );
        }
    }

}
