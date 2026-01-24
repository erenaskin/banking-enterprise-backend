# Stage 1: Build the application
# Alpine yerine standart sürümü kullanıyoruz (ARM64 destekler)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean install -DskipTests

# Stage 2: Create the final image
# "alpine" yerine standart "jre" etiketini kullanıyoruz.
# Bu imaj Ubuntu/Debian tabanlıdır ve Apple Silicon (arm64) desteği tamdır.
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} application.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]