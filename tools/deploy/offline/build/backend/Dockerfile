# ============================================================
# 后端运行时镜像（离线部署专用）
# 产物 jar 在构建机（开发机/CI）打好后 COPY 进来，镜像内不编译
# 配置不打包进镜像，运行期全部由环境变量注入
# ============================================================
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8

WORKDIR /app
COPY dev-platform.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
