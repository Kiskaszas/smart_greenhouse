# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline


COPY src ./src

RUN mvn -q -DskipTests clean package spring-boot:repackage

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SERVER_SSL_ENABLED=false

ENTRYPOINT ["java","-jar","app.jar"]