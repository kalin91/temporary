FROM eclipse-temurin:21-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN ./gradlew build -x test -x karateTest

# Generate the JSA file by running the application briefly to identify the loaded classes.
RUN ls .

FROM dhi.io/eclipse-temurin:21-alpine3.22
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN ["java", "-XX:ArchiveClassesAtExit=application.jsa", "-Dspring.profiles.active=cds", "-jar", "app.jar"]
# COPY --from=builder /app/application.jsa application.jsa
EXPOSE 8080

# 2. Configure the Entrypoint to use the generated JSA file for faster startup. 
# The SPRING_PROFILE environment variable allows us to specify the active Spring profile at runtime.
ENV SPRING_PROFILE=local
ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-Dspring.profiles.active=${SPRING_PROFILE}", "-jar", "app.jar"]
