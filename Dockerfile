FROM eclipse-temurin:21-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN ./gradlew build -x test -x karateTest

FROM dhi.io/eclipse-temurin:21-alpine3.22
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILE=local
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILE}","-jar", "app.jar"]
