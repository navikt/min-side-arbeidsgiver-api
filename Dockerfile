FROM navikt/java:17
COPY /target/*.jar app.jar
EXPOSE 8080
