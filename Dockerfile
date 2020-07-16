FROM adoptopenjdk/openjdk11:jre-11.0.7_10-alpine
RUN mkdir /opt/application
COPY build/install/opentracing-specialagent /opt/application
COPY agent/opentracing-specialagent-1.7.4-SNAPSHOT.jar /opt/application/agent/opentracing-specialagent-1.7.4-SNAPSHOT.jar

ENV JAVA_OPTS="--add-modules java.sql -javaagent:/opt/application/agent/opentracing-specialagent-1.7.4-SNAPSHOT.jar -Dsa.exporter=jaeger -Dsa.config=/opt/application/agent/agent.props"
WORKDIR /opt/application

CMD ["/opt/application/bin/opentracing-specialagent"]