# ================================
# 1단계: 빌드(Stage: builder)
# ================================
FROM gradle:8.10-jdk21 AS builder
WORKDIR /home/gradle/project

# Gradle 캐시를 최대한 활용하기 위해 설정 파일 먼저 복사
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성만 먼저 내려받아 캐시층 생성 (선택 사항이지만 빌드 속도 최적화에 도움)
RUN ./gradlew dependencies --no-daemon || true

# 나머지 소스 전체 복사
COPY . .

# 테스트는 일단 제외하고 JAR 빌드
RUN ./gradlew bootJar -x test --no-daemon

# ================================
# 2단계: 실행(Stage: runtime)
# ================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Spring 프로필 (필요시 docker run -e SPRING_PROFILES_ACTIVE=xxx 로 덮어쓸 수 있음)
ENV SPRING_PROFILES_ACTIVE=prod

# 빌드 단계에서 만들어진 JAR 파일을 런타임 이미지로 복사
COPY --from=builder /home/gradle/project/build/libs/book-collecting-worker-*.jar app.jar

# 애플리케이션이 사용하는 포트 (현재 8080이라고 가정)
EXPOSE 8080

# 컨테이너 시작 시 실행할 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
