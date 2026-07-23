FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /data/incoming /data/processed /data/error \
    && chown -R appuser:appgroup /data /app

COPY target/transfer-of-balances-*.jar app.jar

USER appuser

EXPOSE 8080

ENV TRANSFER_INPUT_DIR=/data/incoming \
    TRANSFER_PROCESSED_DIR=/data/processed \
    TRANSFER_ERROR_DIR=/data/error

ENTRYPOINT ["java", "-jar", "app.jar"]
