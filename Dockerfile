FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/frontend.jar .

CMD ["java", "-jar", "frontend.jar"]
