FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /data \
    && chown -R appuser:appgroup /data /app

COPY target/transfer.war app.war

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
