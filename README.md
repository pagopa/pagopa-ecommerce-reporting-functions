# pagopa-ecommerce-reporting-functions

## What is this?

This is a PagoPA Azure functions that handles ecommerce transactions reporting.

### Environment variables

These are all environment variables needed by the application:

| Variable name                                 |     | Description                                                             | type   | default |
|-----------------------------------------------|-----|-------------------------------------------------------------------------|--------|---------|
| ECOMMERCE_HELPDESK_SERVICE_URI                |     | eCommerce Helpdesk service connection URI                               | string |         |
| ECOMMERCE_HELPDESK_SERVICE_READ_TIMEOUT       |     | Timeout for requests towards eCommerce Helpdesk service                 | number |         |
| ECOMMERCE_HELPDESK_SERVICE_CONNECTION_TIMEOUT |     | Timeout for establishing connections towards eCommerce Helpdesk service | number |         |
| ECOMMERCE_HELPDESK_SERVICE_API_KEY            |     | Helpdesk methods API key                                                | string |         |

An example configuration of these environment variables is in the `.env.example` file.

## Installation

### Prerequisites

Before you begin, make sure you have the following tools installed:

- **[Java Development Kit (JDK) 11+](https://adoptopenjdk.net/)** (required to run Azure Functions with Java)
- **[Maven](https://maven.apache.org/)** (for building the project)
- **[Azure Functions Core Tools](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local)** (for local development and testing)
- **[Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)** (for Azure deployment)
- **[Visual Studio Code](https://code.visualstudio.com/)** or your preferred IDE
- **[Azure Functions for Java](https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-java)** (Java runtime for Azure Functions)

### Install Dependencies
The project uses Maven for dependency management. Run the following command to download the required dependencies:

### Install Azure Functions core tools
```bash
brew install azure-functions-core-tools@4
brew tap azure/functions 
```

```bash
mvn clean install
Configure Azure Functions
Create the necessary Azure Function configurations (e.g., connection strings, API keys) in local.settings.json for local development. For example:
```

```json
{
  "IsEncrypted": false,
  "Values": {
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "AzureWebJobsStorage": "<your-storage-connection-string>",
    "YourApiKey": "<your-api-key>"
  }
}
```

### Run the Function Locally
You can test the function locally using the Azure Functions Core Tools. Run the following command to start the function:

```bash
mvn azure-functions:run
This will start the Azure Function locally, and you can test it with HTTP requests or other configured triggers.
```

## Run Azurite
Ref.: https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=docker-hub%2Cblob-storage

## Run the application with `Docker`

Create your environment typing :

```sh
cp .env.example .env
```

Then from current project directory run :

```sh
docker-compose up
```

if all right you'll see something like that :

```sh
# -- snip --
```

## Code formatting

Code formatting checks are automatically performed during build phase.
If the code is not well formatted an error is raised blocking the maven build.

Helpful commands:

```sh
mvn spotless:check # --> used to perform format checks
mvn spotless:apply # --> used to format all misformatted files
```

## CI

Repo has Github workflow and actions that trigger Azure devops deploy pipeline once a PR is merged on main branch.

In order to properly set version bump parameters for call Azure devops deploy pipelines will be check for the following
tags presence during PR analysis:

| Tag                | Semantic versioning scope | Meaning                                                           |
|--------------------|---------------------------|-------------------------------------------------------------------|
| patch              | Application version       | Patch-bump application version into pom.xml and Chart app version |
| minor              | Application version       | Minor-bump application version into pom.xml and Chart app version |
| major              | Application version       | Major-bump application version into pom.xml and Chart app version |
| ignore-for-release | Application version       | Ignore application version bump                                   |
| chart-patch        | Chart version             | Patch-bump Chart version                                          |
| chart-minor        | Chart version             | Minor-bump Chart version                                          |
| chart-major        | Chart version             | Major-bump Chart version                                          |
| skip-release       | Any                       | The release will be skipped altogether                            |

For the check to be successfully passed only one of the `Application version` labels and only ones of
the `Chart version` labels must be contemporary present for a given PR or the `skip-release` for skipping release step
