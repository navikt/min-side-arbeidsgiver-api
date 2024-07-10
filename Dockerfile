FROM gcr.io/distroless/java21-debian12
COPY /target/*.jar app.jar
CMD ["app.jar"]
