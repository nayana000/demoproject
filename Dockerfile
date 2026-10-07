FROM maven:3.9.12-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


FROM tomcat:10.1-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build \
    /app/target/demoproject.war \
    /usr/local/tomcat/webapps/demoproject.war

EXPOSE 8080

CMD ["catalina.sh", "run"]

