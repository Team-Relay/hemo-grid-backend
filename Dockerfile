# Reproducible Java 21 build for Render or any OCI-compatible host.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Cache Maven metadata before copying frequently changing application sources.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

# The runtime image contains only the JRE and the built application artifact.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN useradd --system --uid 10001 --create-home hemogrid
COPY --from=build --chown=hemogrid:hemogrid /workspace/target/hemo-grid-*.jar app.jar
USER hemogrid
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
