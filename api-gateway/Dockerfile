FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml

COPY event-contracts/pom.xml ./event-contracts/pom.xml

COPY user-service/pom.xml ./user-service/pom.xml

COPY notification-service/pom.xml ./notification-service/pom.xml

COPY discovery-server/pom.xml ./discovery-server/pom.xml

COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src

COPY config-server/pom.xml ./config-server/pom.xml

RUN mvn clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/api-gateway/target/api-gateway-1.0.0.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]