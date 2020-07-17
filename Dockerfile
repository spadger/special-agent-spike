FROM adoptopenjdk/openjdk11:jre-11.0.7_10-alpine
RUN mkdir /opt/application
COPY build/install/opentracing-specialagent /opt/application

WORKDIR /opt/application

CMD ["/opt/application/bin/opentracing-specialagent"]