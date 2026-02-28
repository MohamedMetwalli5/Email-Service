# ---- Stage 1: Building the app ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests

# ---- Stage 2: Run the app ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar backendemailservice.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "backendemailservice.jar"]