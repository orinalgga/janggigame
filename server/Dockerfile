# 저장소 루트를 빌드 컨텍스트로 쓴다: docker build -f server/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY janggi-core janggi-core
COPY server server
RUN ./gradlew :server:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/libs/server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
