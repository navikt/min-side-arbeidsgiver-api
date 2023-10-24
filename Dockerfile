FROM gcr.io/distroless/java17-debian12
COPY /target/*.jar app.jar
CMD ["app.jar"]
