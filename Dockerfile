FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
LABEL authors="gabriel-vinicius"
WORKDIR /app
COPY target/1_Imp_rinha2025-0.0.1-SNAPSHOT.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dserver.port=8080", "-jar","backend.jar"]