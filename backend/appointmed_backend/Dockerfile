# ===========
# Build stage
# ===========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom + source
COPY pom.xml .
COPY src ./src

# Build the jar
RUN mvn -q -DskipTests clean package

# ============
# Runtime stage
# ============
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Render sets PORT; default to 8080 locally
ENV PORT=8080
EXPOSE 8080

# Use PORT when starting Spring Boot
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]