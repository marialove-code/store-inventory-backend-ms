FROM maven:3.9.9-eclipse-temurin-17
LABEL service="inventory-service"
WORKDIR /app
COPY inventory-service/target/inventory-service-3.6.0.jar /app/app.jar
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=dev,docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
