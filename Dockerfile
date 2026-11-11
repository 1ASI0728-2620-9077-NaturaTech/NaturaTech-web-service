FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
# Unit/integration tests and the 80% coverage gate. CI also runs the real MySQL suite.
RUN ./mvnw -B verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/platform-0.0.1-SNAPSHOT.jar app.jar
COPY scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
# Windows checkouts must remain runnable in the Linux deployment image.
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod 755 /app/docker-entrypoint.sh
ENV SPRING_PROFILES_ACTIVE=render
USER 10001:10001
EXPOSE 10000
ENTRYPOINT ["/app/docker-entrypoint.sh"]
