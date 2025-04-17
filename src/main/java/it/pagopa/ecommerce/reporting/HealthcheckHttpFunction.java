package it.pagopa.ecommerce.reporting;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import it.pagopa.ecommerce.reporting.utils.AppInfo;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This is the function that will be invoked when a Http Trigger occurs
 * It will return the version of the application
 */
public class HealthcheckHttpFunction {

	/**
	 * This function will be invoked when a Http Trigger occurs
	 * @return
	 */
	@FunctionName("Liveness")
	public HttpResponseMessage run (
			@HttpTrigger(name = "LivenessTrigger",
			methods = {HttpMethod.GET},
			route = "liveness",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		return request.createResponseBuilder(HttpStatus.OK)
				.header("Content-Type", "application/json")
				.body(getInfo(context.getLogger(), "/META-INF/maven/it.gov.pagopa.bizeventsdatastore/biz-events-datastore-function/pom.properties"))
				.build();
	}

	public synchronized AppInfo getInfo(Logger logger, String path) {
		String version = null;
		String name = null;
		try {
			Properties properties = new Properties();
			InputStream inputStream = getClass().getResourceAsStream(path);
			if (inputStream != null) {
				properties.load(inputStream);
				version = properties.getProperty("version", null);
				name = properties.getProperty("artifactId", null);
			}
		} catch (Exception e) {
			logger.severe("Impossible to retrieve information from pom.properties file.");
		}
		return AppInfo.builder().version(version).environment("azure-fn").name(name).build();
	}

}