# Multi-stage build for production
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven files first for better caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B \
    && mkdir -p target/dependency \
    && (cd target/dependency; jar -xf ../*.jar)

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk add --no-cache curl ca-certificates

# Create non-root user
RUN addgroup -S messenger -g 1000 && \
    adduser -S messenger -G messenger -u 1000

WORKDIR /app

# Copy the built artifact
COPY --from=builder /build/target/dependency/BOOT-INF/lib ./lib
COPY --from=builder /build/target/dependency/META-INF ./META-INF
COPY --from=builder /build/target/dependency/BOOT-INF/classes ./

# Set permissions
RUN chown -R messenger:messenger /app

USER messenger

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+OptimizeStringConcat \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true \
               -Dspring.jmx.enabled=false"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp .:lib/* com.messenger.SecureMessengerApplication"]
