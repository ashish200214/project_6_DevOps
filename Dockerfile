FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY target/demo-v1.jar app.jar
EXPOSE 8080
ENTRYPOINT [ "java","-jar", "app.jar" ]