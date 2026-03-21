# Web Animo Backend

The Web Animo Backend is the server-side component of the ANIMO (Analysis of Networks with Interactive Modeling and Observations) web application, developed as part of a master's thesis. It provides RESTful APIs for modeling, simulation, and analysis of biological networks using Quarkus, a supersonic subatomic Java framework.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [Building and Packaging](#building-and-packaging)
- [Creating a Native Executable](#creating-a-native-executable)
- [Docker](#docker)

## Prerequisites

Before running this application, ensure you have the following installed:

- **Java 17** or later
- **Maven 3.9.0** or later

You can verify your installations with:

```bash
java -version
mvn -version
```

## Installation

Navigate to the project directory:

```bash
cd web-animo-backend-v2/web
```

Install dependencies:

```bash
./mvnw clean install
```

## Running the Application

### Development Mode

To run the application in development mode with live coding:

```bash
./mvnw quarkus:dev
```

The application will start on `http://localhost:8080`. The Quarkus Dev UI is available at `http://localhost:8080/q/dev/`.

### Production Mode

Package and run the application:

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## Building and Packaging

### Standard JAR

Build a standard JAR:

```bash
./mvnw package
```

This creates `target/quarkus-app/quarkus-run.jar` with dependencies in `target/quarkus-app/lib/`.

### Uber JAR

Build an uber JAR (self-contained):

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

Run with:

```bash
java -jar target/*-runner.jar
```

## Creating a Native Executable

For optimal performance, build a native executable using GraalVM:

```bash
./mvnw package -Dnative
```

Or, build in a container if GraalVM is not installed:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Execute the native binary:

```bash
./target/web-1.0.2-SNAPSHOT-runner
```

For more details, see the [Quarkus native executable guide](https://quarkus.io/guides/maven-tooling).

## Docker

To build and run the application using Docker (requires UPPAAL_KEY for UPPAAL integration):

1. Build the Docker image:

```bash
UPPAAL_KEY=$KEY docker buildx build --secret id=UPPAAL_KEY --tag your-registry/web-animo-backend:latest -f src/main/docker/Dockerfile.jvm .
```

2. Run the container:

```bash
docker run -p 8080:8080 your-registry/web-animo-backend:latest
```

For native builds, use `Dockerfile.native` instead.
