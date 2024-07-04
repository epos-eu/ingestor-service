FROM amazoncorretto:17-alpine-jdk

ADD target/*.jar app.jar
ADD target/libs libs

ENTRYPOINT ["java","-Dlog4j.configurationFile=/etc/log4j2/log4j2.properties", "-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]

