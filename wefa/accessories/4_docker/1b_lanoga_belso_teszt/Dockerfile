FROM postgres:10
ENV POSTGRES_PASSWORD=f7312aae

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update -y
RUN apt-get install -y openjdk-8-jdk

COPY wefa.jar wefa.jar
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2 -Dspring.profiles.active=dev -Dspring.datasource.password=f7312aae -Duser.home=/data -XshowSettings:vm -Dtools.redirect-uri-host-backend=http://89.107.251.208:1092/wefa -Dtools.redirect-uri-host-frontend=http://89.107.251.208:1092/wefa"

CMD docker-entrypoint.sh postgres & java $JAVA_OPTS -jar /wefa.jar