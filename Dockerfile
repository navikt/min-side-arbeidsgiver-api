FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
COPY target/min-side-arbeidsgiver-api/app.jar app.jar
COPY target/min-side-arbeidsgiver-api/lib lib

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar","app.jar"]
