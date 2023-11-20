FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/order-server.jar .

CMD ["java", "-jar", "order-server.jar"]
