FROM eclipse-temurin:25-jre-alpine
COPY target/template*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]