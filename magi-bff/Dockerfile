FROM mcr.microsoft.com/openjdk/jdk:21-mariner AS build
WORKDIR /app
RUN tdnf install -y tar
COPY magi-bff/mvnw* /app/
COPY magi-bff/.mvn /app/.mvn
COPY magi-bff/pom.xml /app
COPY magi-bff/src /app/src
RUN ls -la /app
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B -Dproduction package


FROM mcr.microsoft.com/openjdk/jdk:21-mariner

COPY --from=build /app/target/magi-bff-*.jar /usr/src/myapp/application.jar

EXPOSE 8080
CMD ["/usr/bin/java", "-jar", "/usr/src/myapp/application.jar"]
