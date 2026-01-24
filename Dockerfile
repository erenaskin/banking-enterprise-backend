# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the project files
COPY . .

# Build all modules
# skipping tests to speed up the build in this example, but in production you might want to run them
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# The service name must be passed as a build argument (e.g., api-gateway, identity-service)
ARG SERVICE_NAME

# Copy the specific service jar from the build stage
# We assume the jar is in the target directory of the respective module
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

# Expose ports (Optional: can be overridden or ignored, but good for documentation)
# Since this is generic, we don't expose a specific port here, or we could expose 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
