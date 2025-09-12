FROM mcr.microsoft.com/openjdk/jdk:21-mariner AS build
WORKDIR /app
RUN tdnf install -y tar
COPY magi-core/mvnw* /app/
COPY magi-core/.mvn /app/.mvn
COPY magi-core/pom.xml /app
COPY magi-core/src /app/src
RUN ls -la /app
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B -Dproduction package


FROM mcr.microsoft.com/openjdk/jdk:21-mariner

COPY --from=build /app/target/magi-core-*.jar /usr/src/myapp/application.jar

EXPOSE 8080
CMD ["/usr/bin/java", "-jar", "/usr/src/myapp/application.jar"]
