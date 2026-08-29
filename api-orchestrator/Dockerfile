# syntax=docker/dockerfile:1

# ---- build ------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the wrapper and POM first so dependency resolution is cached separately
# from the source: editing a Java file does not re-download the world.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
RUN mvn -B -DskipTests package

# ---- runtime ----------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Run unprivileged: nothing in this image needs root.
RUN addgroup -S app && adduser -S -G app app

COPY --from=build /build/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Spring Boot's actuator is not on the classpath, so probe the OpenAPI document,
# which is served by springdoc and requires the context to be fully up.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
