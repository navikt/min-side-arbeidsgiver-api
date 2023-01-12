FROM ghcr.io/navikt/baseimages/temurin:17
COPY /target/*.jar app.jar
EXPOSE 8080
