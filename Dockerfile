# Stage 1: Build the application using Maven + JDK 21
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run with a lightweight JRE-only image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/url-shortener-0.0.1-SNAPSHOT.jar app.jar

ENV SERVER_PORT=${PORT:-8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]