# Build stage: Maven wrapper JAR is not tracked in git, so use the official Maven image.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/results-service-*.jar app.jar

USER spring:spring
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
