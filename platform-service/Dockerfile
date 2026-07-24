FROM maven:3.9.9-eclipse-temurin-17
LABEL service="platform-service"
WORKDIR /app
COPY platform-service/target/platform-service-3.6.0.jar /app/app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=dev,docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
