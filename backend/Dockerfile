FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar /app/app.jar
EXPOSE 18082
ENTRYPOINT ["java", "-Xms128m", "-Xmx768m", "-jar", "/app/app.jar"]
