FROM alpine:3.18.4

ARG USER=spring
ARG USER_ID=1000
ARG GROUP_ID=1000

LABEL maintainer="Gentian Licenji <glicenji@leanindustries.com>"
LABEL description="Spring Boot TicketDispatcherServer"
LABEL version="2.0"

RUN apk add --no-cache openjdk8-jre-base \
 && addgroup -g ${GROUP_ID} ${USER} \
 && adduser -D -u ${USER_ID} -G ${USER} -s /bin/sh ${USER} \
 && mkdir -p /app/logs /app/tmp /embedded \
 && chown -R ${USER_ID}:${GROUP_ID} /app /embedded

COPY --chown=${USER_ID}:${GROUP_ID} target/TicketDispatcherServer.jar /embedded/

WORKDIR /app
EXPOSE 8080

USER ${USER}

ENTRYPOINT ["java $JAVA_OPTS", \
  "-Dserver.port=8080", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", \
  "-Xmx512m", \
  "-Xms256m", \
  "-XX:+UseG1GC", \
  "-XX:+UseContainerSupport", \
  "-jar", \
  "/embedded/TicketDispatcherServer.jar"]