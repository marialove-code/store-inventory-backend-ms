FROM maven:3.9.9-eclipse-temurin-17
LABEL service="ai-service"
WORKDIR /app
COPY ai-service/target/ai-service-3.6.0.jar /app/app.jar
EXPOSE 8084
ENV SPRING_PROFILES_ACTIVE=dev,docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
