# syntax=docker/dockerfile:1

# ========== 构建阶段：官方 Maven + JDK 17 镜像编译可执行包 ==========
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先单独拷贝 pom 预热依赖缓存，提高重复构建速度
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 拷贝源码编译；默认执行全部自动化测试，测试不过则镜像构建失败
COPY src ./src
RUN mvn -B clean package

# ========== 运行阶段：瘦身 JRE 17 镜像，只加载可执行包 ==========
FROM eclipse-temurin:17-jre
WORKDIR /app

# 非 root 运行
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /build/target/distillation-shortcut-service.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
