# ---- Stage 1: build ----
# Full JDK + tools, used only to compile and package. Thrown away after.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper and build config FIRST (these change rarely).
# Doing this before copying source lets Docker cache the dependency
# download layer, so later rebuilds skip re-downloading everything.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies as their own cached step.
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# Now copy the actual source (this changes every commit).
COPY src ./src

# Build the jar, skipping tests (CI runs tests separately; the image
# build shouldn't need a database).
RUN ./gradlew bootJar --no-daemon -x test

# ---- Stage 2: runtime ----
# Slim JRE only — no JDK, no Gradle, no source. Much smaller image.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Create a non-root user to run the app (security best practice —
# a compromised app shouldn't have root inside the container).
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

# Copy ONLY the finished jar out of the build stage.
COPY --from=build /app/build/libs/*.jar app.jar

# Document the port the app listens on (Spring's default 8080).
EXPOSE 8080

# How to start the app.
ENTRYPOINT ["java", "-jar", "app.jar"]