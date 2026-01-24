# Stage 1: Build (Derleme Aşaması)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# --- 1. Common Modülünü Derle ---
# Eğer projen common modülüne bağımlıysa bu satırlar hayat kurtarır
COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN mvn -f common/pom.xml clean install -DskipTests

# --- 2. Identity Service Modülünü Derle ---
COPY identity-service/pom.xml identity-service/pom.xml
COPY identity-service/src identity-service/src
RUN mvn -f identity-service/pom.xml clean package -DskipTests

# Stage 2: Runtime (Çalıştırma Aşaması)
FROM eclipse-temurin:17-jre
WORKDIR /app
# Derlenen jar dosyasını al
COPY --from=build /app/identity-service/target/*.jar application.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]