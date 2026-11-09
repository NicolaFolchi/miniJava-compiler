FROM maven:3.9.11-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY WebContent ./WebContent
RUN mvn --batch-mode clean package

FROM tomcat:9.0-jdk8-temurin-jammy

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/miniJava-compiler.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
