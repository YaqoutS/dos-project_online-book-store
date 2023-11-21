FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/order-server.jar .
COPY orderserver.db .

CMD ["java", "-jar", "order-server.jar"]
