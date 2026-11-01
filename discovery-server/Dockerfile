FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml

COPY event-contracts/pom.xml ./event-contracts/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY discovery-server/pom.xml ./discovery-server/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

COPY discovery-server/src ./discovery-server/src

COPY config-server/pom.xml ./config-server/pom.xml

RUN mvn clean package -pl discovery-server -am -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/discovery-server/target/discovery-server-1.0.0.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]