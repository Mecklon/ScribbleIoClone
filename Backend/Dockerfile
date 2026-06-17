FROM eclipse-temurin:21-jre
COPY target/ScribbleIoClone.jar backend.jar
RUN mkdir -p /uploads
ENTRYPOINT ["java", "-jar", "/backend.jar"]