FROM frolvlad/alpine-java:jdk8-slim

VOLUME /temp
ENTRYPOINT ["java", "-Xmx256m", "-jar","/app.jar"]
EXPOSE 104
ADD build/libs/storescp-0.0.1-SNAPSHOT.jar app.jar
RUN touch /app.jar