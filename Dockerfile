FROM maven:3-eclipse-temurin-21 AS build-stage

LABEL org.opencontainers.image.authors="Matthias Geerdsen <matthias.geerdsen@intranda.com>"
LABEL org.opencontainers.image.source="https://github.com/intranda/goobi-viewer-core"
LABEL org.opencontainers.image.description="Goobi viewer"

# you can use --build-arg build=false to skip viewer.war compilation, a viewer.war file needs to be available in target/viewer.war then
ARG build=true

COPY ./ /viewer/
WORKDIR /viewer
RUN echo $build; if [ "$build" = "true" ]; then mvn clean package; elif [ -f "/viewer/goobi-viewer-theme-reference/target/viewer.war" ]; then echo "using existing viewer.war"; else echo "not supposed to build, but no viewer.war found either"; exit 1; fi

RUN mkdir -p /viewer-exploded && cd /viewer-exploded && jar -xf /viewer/goobi-viewer-theme-reference/target/viewer.war

# Build actual application container
FROM tomcat:10-jre21 AS assemble-stage

# ENV CATALINA_HOME is set to /usr/local/tomcat in the base image

RUN echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | debconf-set-selections && \
	apt-get update && \
	apt-get -y install --no-install-recommends git \
      openssh-client \
	  gettext-base \
	  ttf-mscorefonts-installer \
	  libopenjp2-7 \
      mariadb-client-core \
      gosu \
      curl \
      whois && \
	apt-get -y clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
	rm -rf ${CATALINA_HOME}/webapps/*

RUN ["/bin/bash", "-c", "mkdir -p /opt/digiverso/{logs,viewer/{abbyy,cmdi,deleted_mets,hotfolder,media,orig_lido,orig_denkxweb,ccess,ugc,alto,cms_media,error_mets,mix,pdf,tei,mei,updated_mets,cache,config,fulltext,indexed_lido,indexed_mets,indexed_ead,indexed_statistics,oai/token,ptif,themes,wc,bin}}"]
RUN ["/bin/bash", "-c", "mkdir -p /viewer-template/{config,oai}" ]
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost/ && mkdir -p ${CATALINA_HOME}/webapps/viewer

COPY goobi-viewer-config/install/docker/setenv.sh ${CATALINA_HOME}/bin/setenv.sh
COPY goobi-viewer-config/install/config/ /viewer-template/config
COPY goobi-viewer-config/install/docker/stopwords /stopwords
COPY goobi-viewer-config/install/docker/viewer.xml.template ${CATALINA_HOME}/conf/
COPY goobi-viewer-config/install/docker/enable_dev_options.patch /viewer-template/
COPY goobi-viewer-config/install/docker/insert_theme_preresource.patch.template /viewer-template/

# Install the shipped Tomcat configuration from install/etc/tomcat10 instead of the
# stock files of the base image. The only container-specific changes are binding
# the connector to all interfaces (0.0.0.0) instead of loopback (127.0.0.1) and
# turning the fixed sameSiteCookies value into a placeholder that run.sh fills in
# from ${TOMCAT_SAMESITECOOKIES} at container start.
RUN --mount=type=bind,source=goobi-viewer-config/install/etc/tomcat10,target=/tmp/tomcat-conf,readonly \
    sed 's/address="127.0.0.1"/address="0.0.0.0"/' /tmp/tomcat-conf/server.xml > ${CATALINA_HOME}/conf/server.xml && \
    sed 's/sameSiteCookies="strict"/sameSiteCookies="${TOMCAT_SAMESITECOOKIES}"/' /tmp/tomcat-conf/context.xml > ${CATALINA_HOME}/conf/context.xml.template

RUN grep -qxF 'org.omnifaces.cdi.push.SocketEndpoint.level = OFF' ${CATALINA_HOME}/conf/logging.properties || echo 'org.omnifaces.cdi.push.SocketEndpoint.level = OFF' >> ${CATALINA_HOME}/conf/logging.properties && \
    grep -qxF 'org.apache.tomcat.util.net.NioEndpoint.level = OFF' ${CATALINA_HOME}/conf/logging.properties || echo 'org.apache.tomcat.util.net.NioEndpoint.level = OFF' >> ${CATALINA_HOME}/conf/logging.properties && \
    grep -qxF 'org.apache.tomcat.websocket.level = OFF' ${CATALINA_HOME}/conf/logging.properties || echo 'org.apache.tomcat.websocket.level = OFF' >> ${CATALINA_HOME}/conf/logging.properties && \
    grep -qxF 'org.glassfish.jersey.server.level = OFF' ${CATALINA_HOME}/conf/logging.properties || echo 'org.glassfish.jersey.server.level = OFF' >> ${CATALINA_HOME}/conf/logging.properties

COPY --from=build-stage /viewer-exploded/ ${CATALINA_HOME}/webapps/viewer/

COPY goobi-viewer-config/install/docker/run.sh /
COPY goobi-viewer-config/install/docker/configure-config-urls.sh /
COPY goobi-viewer-config/install/docker/healthcheck.sh /

EXPOSE 8080

# Ubuntu 24.04 ships a default 'ubuntu' user/group at 1000;
# remove it, then create our unprivileged 'user' at uid/gid 1000.
RUN userdel -r ubuntu 2>/dev/null || true; groupdel ubuntu 2>/dev/null || true; \
    groupadd -g 1000 user && useradd -u 1000 -g user -M -s /usr/sbin/nologin user

# timeout must exceed healthcheck.sh's curl --max-time. start-period covers the
# first-run schema initialisation, during which the app is legitimately absent.
HEALTHCHECK --interval=30s --timeout=15s --retries=4 --start-period=120s CMD ["/healthcheck.sh"]

CMD ["/run.sh"]
