FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/smart-learning-agent-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 18082
ENTRYPOINT ["java", "-Xms128m", "-Xmx768m", "-jar", "/app/app.jar"]
