# 使用官方的 OpenJDK 17 镜像作为基础镜像
FROM openjdk:17

VOLUME /data/tools
# 设置工作目录
WORKDIR /data/tools

EXPOSE 8088
# 复制源代码
COPY ./target/tools-ms-1.0-SNAPSHOT.jar  /data/tools/app.jar

ENTRYPOINT ["java","-jar","app.jar"]
