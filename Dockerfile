# Java 21 런타임(JRE) 기반 이미지.
FROM eclipse-temurin:21-jre-jammy

# 컨테이너 내부에서 애플리케이션을 실행할 작업 디렉터리 설정
WORKDIR /app

# GitHub Actions(또는 로컬)에서 미리 빌드된 JAR을 컨테이너로 복사합니다.
# build.gradle에서 bootJar 산출물 이름을 app.jar로 고정했기 때문에 경로가 안정적입니다.
COPY build/libs/app.jar /app/app.jar

# 컨테이너 시작 시 Spring Boot 애플리케이션을 실행합니다.
# WORKDIR가 /app 이므로 "app.jar"는 /app/app.jar를 의미합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 9001