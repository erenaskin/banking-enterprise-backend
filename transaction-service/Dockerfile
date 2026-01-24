# Stage 1: Build (Derleme Aşaması)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# --- 1. Common Modülünü Derle ---
# Docker context'ten common klasörünü kopyalıyoruz
COPY common/pom.xml common/pom.xml
COPY common/src common/src
# Common modülünü kuruyoruz (böylece .m2 deposuna eklenir)
RUN mvn -f common/pom.xml clean install -DskipTests

# --- 2. Transaction Service Modülünü Derle ---
# Transaction service dosyalarını kopyalıyoruz
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY transaction-service/src transaction-service/src
# Şimdi transaction service'i derliyoruz (artık common'ı bulabilir)
RUN mvn -f transaction-service/pom.xml clean package -DskipTests

# Stage 2: Runtime (Çalıştırma Aşaması)
FROM eclipse-temurin:17-jre
WORKDIR /app
# Derlenen jar dosyasını build aşamasından alıyoruz
COPY --from=build /app/transaction-service/target/*.jar application.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]