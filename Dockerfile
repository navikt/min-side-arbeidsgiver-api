FROM gcr.io/distroless/java21-debian12
COPY target/min-side-arbeidsgiver-api/app.jar app.jar
COPY target/min-side-arbeidsgiver-api/lib lib

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["app.jar"]
