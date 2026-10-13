# GitHub Actions(또는 로컬)에서 ./gradlew bootJar로 만든 jar를 이미지에 담습니다.
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
