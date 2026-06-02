FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

ARG GITLAB_TOKEN

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies -PgitLabPrivateToken=${GITLAB_TOKEN} --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar -x test -PgitLabPrivateToken=${GITLAB_TOKEN} --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]