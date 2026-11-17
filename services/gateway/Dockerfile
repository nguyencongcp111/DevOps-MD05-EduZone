# syntax=docker/dockerfile:1

# ---- Giai đoạn 1: build bằng Maven ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- Giai đoạn 2: image chạy, chỉ có JRE ----
FROM eclipse-temurin:21-jre-alpine
ENV LANG=C.UTF-8
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /src/target/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
