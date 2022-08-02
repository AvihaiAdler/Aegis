FROM gradle:jdk18-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble

FROM openjdk:18-jdk-alpine
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/aegis.jar
EXPOSE 443
CMD ["java", "-jar", "/app/aegis.jar"]


