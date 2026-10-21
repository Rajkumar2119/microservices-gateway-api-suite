# One Dockerfile for every service: docker build --build-arg MODULE=order-service .
ARG MODULE=api-gateway

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY order-service/pom.xml order-service/
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline
COPY api-gateway/src api-gateway/src
COPY auth-service/src auth-service/src
COPY order-service/src order-service/src
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests \
 && cp ${MODULE}/target/${MODULE}-*.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/app.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
