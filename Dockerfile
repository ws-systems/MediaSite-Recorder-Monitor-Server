FROM tomcat:8.5
MAINTAINER Tom Paulus <tom@whitestar.systems>

RUN rm -rf /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.war
COPY ms-monitor.war /usr/local/tomcat/webapps/ROOT.war

VOLUME /usr/local/tomcat/logs
EXPOSE 8080

CMD ["catalina.sh", "run"]