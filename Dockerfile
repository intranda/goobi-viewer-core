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
      whois && \
	apt-get -y clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
	rm -rf ${CATALINA_HOME}/webapps/*

RUN ["/bin/bash", "-c", "mkdir -p /opt/digiverso/{config/bin,indexer,logs,viewer/{abbyy,cmdi,deleted_mets,hotfolder,media,orig_lido,orig_denkxweb,ccess,ugc,alto,cms_media,error_mets,mix,pdf,tei,mei,updated_mets,cache,config,fulltext,indexed_lido,indexed_mets,indexed_ead,indexed_statistics,oai/token,ptif,themes,wc,bin}}"]
RUN ["/bin/bash", "-c", "mkdir -p /viewer-template/{config,oai}" ]
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost/ && mkdir -p ${CATALINA_HOME}/webapps/viewer

COPY goobi-viewer-config/docker/setenv.sh ${CATALINA_HOME}/bin/setenv.sh
COPY goobi-viewer-config/install/ /viewer-template/config
COPY goobi-viewer-config/docker/stopwords /stopwords
COPY goobi-viewer-config/docker/viewer.xml.template ${CATALINA_HOME}/conf/
COPY goobi-viewer-config/docker/disable_dev_options.patch /viewer-template/
COPY goobi-viewer-config/docker/insert_theme_preresource.patch.template /viewer-template/

RUN --mount=type=bind,source=goobi-viewer-config/docker,target=/tmp/patches,readonly \
    patch --output=${CATALINA_HOME}/conf/server.xml.template ${CATALINA_HOME}/conf/server.xml < /tmp/patches/server.xml.patch && \
    patch --output=${CATALINA_HOME}/conf/context.xml.template ${CATALINA_HOME}/conf/context.xml < /tmp/patches/context.xml.patch

COPY --from=build-stage /viewer-exploded/ ${CATALINA_HOME}/webapps/viewer/

COPY goobi-viewer-config/docker/run.sh /
COPY goobi-viewer-config/docker/healthcheck.sh /

EXPOSE 8080
EXPOSE 8009

# Ubuntu 24.04 ships a default 'ubuntu' user/group at 1000;
# remove it, then create our unprivileged 'user' at uid/gid 1000.
RUN userdel -r ubuntu 2>/dev/null || true; groupdel ubuntu 2>/dev/null || true; \
    groupadd -g 1000 user && useradd -u 1000 -g user -M -s /usr/sbin/nologin user

HEALTHCHECK --interval=30s --timeout=5s --retries=4 CMD ["/healthcheck.sh"]

CMD ["/run.sh"]
