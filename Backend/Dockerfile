FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /
COPY --from=build /app/target/ScribbleIoClone.jar backend.jar
RUN mkdir -p /uploads
ENTRYPOINT ["java", "-jar", "/backend.jar"]