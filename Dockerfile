FROM gradle:jdk17-alpine AS build
#COPY aegis.jar aegis.jar
#CMD ["java", "-jar", "aegis.jar"]
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble

FROM openjdk:19-jdk-slim
#EXPOSE 443
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/aegis.jar
ENTRYPOINT ["java", "-jar", "/app/aegis.jar"]


