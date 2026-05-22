FROM gradle:8.11.1-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle gradle gradle
COPY --chown=gradle:gradle gradlew gradlew.bat gradle.properties build.gradle.kts settings.gradle.kts ./
COPY --chown=gradle:gradle protocol protocol
COPY --chown=gradle:gradle signalingServer signalingServer
RUN sed -i '/include(":composeApp")/d' settings.gradle.kts \
 && gradle :signalingServer:buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Xmx200m -XX:+UseSerialGC -XX:MaxRAMPercentage=75"
COPY --from=build /home/gradle/src/signalingServer/build/libs/trovatacast-signaling.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
