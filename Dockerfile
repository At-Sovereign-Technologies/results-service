FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source
COPY src ./src

# Build the app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xms128m", "-Xmx2G", "-jar", "app.jar"]