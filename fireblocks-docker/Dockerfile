FROM eclipse-temurin:21-jdk

ENV spring.config.additional-location "/etc/app/config/"
ENV logging.config "/etc/app/config/logback.xml"
ENV JAVA_OPTS="-Dcom.securosys.primus.jce.logoutCloseReaderGracetimeMillis=0"

# Embedded tomcat working directory
# External app configuration
VOLUME ["/tmp",  "/etc/app/config" ]

# Multi-layer setup according to Spring recommendations
ARG DEPENDENCY=install

COPY ${DEPENDENCY}/BOOT-INF/lib /opt/app/lib
COPY ${DEPENDENCY}/META-INF /opt/app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /opt/app

EXPOSE 8080

ENTRYPOINT ["java","-cp","/opt/app:/opt/app/lib/*","-Djava.security.egd=file:/dev/./urandom", "--add-opens", "java.base/sun.security.pkcs10=ALL-UNNAMED", "--add-opens", "java.base/sun.security.x509=ALL-UNNAMED", "--add-opens", "java.base/sun.security.pkcs=ALL-UNNAMED", "--add-opens", "java.base/sun.security.util=ALL-UNNAMED", "-Dcom.securosys.primus.jce.logoutCloseReaderGracetimeMillis=0", "-Dcom.securosys.primus.jce.cutLmsSignaturePrefix=false", "-Dcom.securosys.primus.jce.fallBackToSignatureLessLogFetching=true","com.securosys.fireblocks.FireblocksApplication"]