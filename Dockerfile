FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/notification-pipeline-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]