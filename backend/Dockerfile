# ============================================================
# 后端镜像（两阶段构建；配置不打包进镜像，全部环境变量注入）
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/dev-platform.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
