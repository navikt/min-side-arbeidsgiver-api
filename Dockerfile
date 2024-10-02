FROM gcr.io/distroless/java21-debian12
COPY /target/*.jar app.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["app.jar"]
