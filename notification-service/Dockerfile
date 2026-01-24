# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Common Modülünü Derle
COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml clean install -DskipTests

# 2. Notification Service Modülünü Derle
COPY notification-service/pom.xml notification-service/pom.xml
COPY notification-service/src notification-service/src
RUN mvn -f notification-service/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar application.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]