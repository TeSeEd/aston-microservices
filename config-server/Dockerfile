FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml

COPY event-contracts/pom.xml ./event-contracts/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY discovery-server/pom.xml ./discovery-server/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY config-server/pom.xml ./config-server/pom.xml

COPY config-server/src ./config-server/src

RUN mvn clean package -pl config-server -am -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/config-server/target/config-server-1.0.0.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]