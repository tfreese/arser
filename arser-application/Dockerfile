# docker build -t arser:latest .
# docker build -t arser:latest -f service/Dockerfile .
# docker run [-d] --name arser --memory 64m --publish 7999:7999 --network host arser:latest
# docker start/stop arser;

# --network host: für Internetzugang

#FROM localhost:5083/openjdk:21
FROM openjdk:latest
 
MAINTAINER Thomas Freese <commercial@freese-home.de>
 
ARG JAR_FILE=app/*-spring.jar
ENV VM_PARAMS="-Xms32m -Xmx64m -Darser.port=7999"

COPY ${JAR_FILE} /app/app.jar

RUN echo "Europe/Berlin" > /etc/timezone

WORKDIR /
VOLUME /tmp
EXPOSE 7999

ENTRYPOINT exec java \
 "$VM_PARAMS" \
 -XX:TieredStopAtLevel=1 \
 -Djava.security.egd=file:/dev/./urandom \
 -jar \
 /app/app.jar \
 "$0" $@
 
#ENTRYPOINT ["java",\
# "-Xms256m",\
# "-Xmx256m",\
# "-XX:TieredStopAtLevel=1",\
# "-Djava.security.egd=file:/dev/./urandom",\
# "-Dspring.profiles.active=default",\
# "-jar",\
# "/app/app.jar"] 

#CMD /usr/bin/java -Xms64m -Xmx256m -XX:TieredStopAtLevel=1 -noverify -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=default -jar /app/app.jar
#RUN [“java”, “...”]
