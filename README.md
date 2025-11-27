# Securosys - Fireblocks Custom Server

## Overview

This project implements a **Custom Server for Fireblocks**, based on the example code from
[fireblocks-custom-server](https://github.com/fireblocks/fireblocks-agent/tree/main/examples/server).
The server acts as a middleware between the **Fireblocks Key Link Agent** and the **Securosys TSB (Transaction Security Broker)**.
This enabled you to keep your Fireblocks wallet keys securely in a Primus HSM, instead of using Fireblocks's MPC scheme.

The purpose of this server is to:

- Receive requests from the Fireblocks Key Link Agent (`/v1/messagesToSign`, `/v1/messagesStatus`, `/v1/signRequest/{id}`, etc.).
- Forward requests to the Securosys TSB REST API.
- Return signing results and status updates.

## Documentation

For a full installation and configuration guide, please see the
[Securosys online documentation](https://docs.securosys.com/fireblocks/overview/).

## Building

```sh
# List available tasks
./gradlew tasks

# Run the server directly
./gradlew bootRun

# Build and run the executable JAR in separate steps
./gradlew bootJar
java -jar fireblocks-application/build/libs/fireblocks-application-${VERSION}.jar

# Build the Docker image
./gradlew :fireblocks-application:jibDockerBuild
```

Use the `fromDockerRegistry` and `toDockerRegistry` properties to override the base image and the target registry, for example by adding the `-PfromDockerRegistry=artifactory.company.com/example-docker-registry` and `-PtoDockerRegistry=artifactory.company.com/example-docker-registry` arguments.

## Configuration

The configuration options for the Securosys Custom Server are defined in the `application.yml`.
For a full example, see the [template](etc/config_templates/config-files/template.yml).

## Running the tests

To run the test suite, you need a connection to an HSM partition via the TSB.
Set the following environment variables:

```sh
export TSB_REST_API="https://tsb.example.com"
export TSB_KEY_MANAGEMENT_TOKEN="my-jwt-key-management-token"
export TSB_KEY_OPERATION_TOKEN="my-jwt-key-operation-token"
export TSB_MTLS_CERT="/path/to/tsb-integration-test-client.crt"
export TSB_MTLS_KEY="/path/to/tsb-integration-test-client.key"

# Override the values in the application-test.yml.
# All uppercase, . is replaced with _ (Spring Boot will pick this up).
export TSB_TSBRESTAPI="https://tsb.example.com"
export TSB_TSBACCESSTOKEN="my-jwt-token"
```

To generate an mTLS client key and client certificate, see the
[TSB documentation](https://docs.securosys.com/tsb/Installation/post-installation/authentication/mtls).

Then run the tests:

```sh
./gradlew test

# run a specific test and be more verbose
 ./gradlew test --tests com.securosys.fireblocks.service.ConnectionIntTest --info
```

## References

- <https://docs.securosys.com/fireblocks/overview>
- <https://support.fireblocks.io/hc/en-us/articles/14228517105052-Fireblocks-Key-Link-Overview>
- <https://github.com/fireblocks/fireblocks-agent>

## License

The content of this repository is licensed under the [Apache 2.0 license](LICENSE).

> [!WARNING]
> Before using this project, please carefully read sections
> "7. Disclaimer of Warranty" and "8. Limitation of Liability" of the license!
