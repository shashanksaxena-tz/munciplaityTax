FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Build all modules with retry logic and increased timeout
RUN mvn clean package -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.pool=false \
    -Dhttp.keepAlive=false

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
