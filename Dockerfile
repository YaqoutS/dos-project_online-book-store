FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/catalog-server.jar .

CMD ["java", "-jar", "catalog-server.jar"]
