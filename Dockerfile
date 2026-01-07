FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml                          /build/pom.xml
COPY event-generator/pom.xml          /build/event-generator/pom.xml
COPY event-registry/pom.xml           /build/event-registry/pom.xml

RUN mvn -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies \
    -DdownloadSources=false -DdownloadJavadoc=false

COPY event-generator/src              /build/event-generator/src
COPY event-registry/src               /build/event-registry/src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS generator

WORKDIR /app
COPY --from=builder /build/event-generator/target/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:21-jre-alpine AS registry

WORKDIR /app
COPY --from=builder /build/event-registry/target/*.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]