#FROM gradle:8.5-jdk21 AS build
#COPY . /home/gradle/src
#WORKDIR /home/gradle/src

#RUN gradle fatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
RUN mkdir /app

#COPY --from=build /home/gradle/src/build/libs/server-1.0-all.jar /app/g-cal-server.jar
COPY build/libs/server-1.0-all.jar /app/g-cal-server.jar
ENTRYPOINT ["java", "-jar", "/app/g-cal-server.jar"]