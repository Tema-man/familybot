FROM gradle AS build
ENV APP_HOME=/app
WORKDIR $APP_HOME
COPY build.gradle.kts settings.gradle.kts $APP_HOME
COPY gradle $APP_HOME/gradle
COPY --chown=gradle:gradle . /home/gradle/src
USER root
RUN chown -R gradle /home/gradle/src
COPY ./src ./src
RUN gradle clean build --no-daemon || return 0

FROM openjdk:17-jdk-slim
RUN apt update && apt install -y curl python3
ADD "https://api.github.com/repos/yt-dlp/yt-dlp/releases?per_page=1" latest_release
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
ENV SPRING_PROFILES_ACTIVE production

COPY --from=build /app/build/libs/familybot-2.0.0.jar /usr/local/lib/familybot.jar
ENTRYPOINT ["java","-Xmx128m","-jar","/usr/local/lib/familybot.jar"]
