FROM maven:3-eclipse-temurin-21 AS build-stage

LABEL org.opencontainers.image.authors="Matthias Geerdsen <matthias.geerdsen@intranda.com>"
LABEL org.opencontainers.image.source="https://github.com/intranda/goobi-viewer-core"
LABEL org.opencontainers.image.description="Goobi viewer"

# you can use --build-arg build=false to skip viewer.war compilation, a viewer.war file needs to be available in target/viewer.war then
ARG build=true

COPY ./ /viewer/
WORKDIR /viewer
RUN echo $build; if [ "$build" = "true" ]; then mvn clean package; elif [ -f "/viewer/goobi-viewer-theme-reference/target/viewer.war" ]; then echo "using existing viewer.war"; else echo "not supposed to build, but no viewer.war found either"; exit 1; fi

# Build actual application container
FROM tomcat:10-jre21 AS assemble-stage

# CATALINA_HOME is set to /usr/local/tomcat in the base image

ENV SOLR_HOST=solr
ENV TOMCAT_SAMESITECOOKIES=strict
ENV CONFIGSOURCE=folder
ENV CONFIG_FOLDER=/viewer-template
ENV CONFIG_TARGET_FOLDER=/opt/digiverso/viewer
ENV STOPWORDS_LANG="de"

RUN sed -i 's|main$|main contrib|' /etc/apt/sources.list
RUN echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | debconf-set-selections
RUN apt-get update && \
	apt-get -y install git \
	  gettext-base \
	  ttf-mscorefonts-installer \
	  libopenjp2-7 \
	  unzip \
      mysql-client && \
	apt-get -y clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
	rm -rf ${CATALINA_HOME}/webapps/*

RUN ["/bin/bash", "-c", "mkdir -p /opt/digiverso/{config/bin,indexer,logs,viewer/{abbyy,cmdi,deleted_mets,hotfolder,media,orig_lido,orig_denkxweb,ccess,ugc,alto,cms_media,error_mets,indexed_lido,mix,pdf,tei,updated_mets,cache,config,fulltext,indexed_mets,oai/token,ptif,themes,wc,bin}}"]
RUN ["/bin/bash", "-c", "mkdir -p /viewer-template/{config,oai}" ]
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost/ && mkdir -p ${CATALINA_HOME}/webapps/viewer

COPY goobi-viewer-core-config/src/main/resources/docker/setenv.sh ${CATALINA_HOME}/bin/setenv.sh
COPY goobi-viewer-core-config/src/main/resources/install/ /viewer-template/config
COPY goobi-viewer-core-config/src/main/resources/docker/stopwords /stopwords
COPY goobi-viewer-core-config/src/main/resources/docker/viewer.xml.template ${CATALINA_HOME}/conf/
COPY goobi-viewer-core-config/src/main/resources/docker/disable_dev_options.patch /viewer-template/
COPY goobi-viewer-core-config/src/main/resources/docker/insert_theme_preresource.patch.template /viewer-template/

RUN --mount=type=bind,source=goobi-viewer-core-config/src/main/resources/docker,target=/tmp/patches,readonly \
    patch --output=${CATALINA_HOME}/conf/server.xml.template ${CATALINA_HOME}/conf/server.xml < /tmp/patches/server.xml.patch && \
    patch --output=${CATALINA_HOME}/conf/context.xml.template ${CATALINA_HOME}/conf/context.xml < /tmp/patches/context.xml.patch

# redirect / to /viewer/
RUN mkdir ${CATALINA_HOME}/webapps/ROOT && \
    echo '<% response.sendRedirect("/viewer/"); %>' > ${CATALINA_HOME}/webapps/ROOT/index.jsp

COPY --from=build-stage  /viewer/goobi-viewer-theme-reference/target/viewer.war /

RUN unzip /viewer.war -d ${CATALINA_HOME}/webapps/viewer && rm /viewer.war

COPY goobi-viewer-core-config/src/main/resources/docker/run.sh /

EXPOSE 8080
EXPOSE 8009

CMD ["/run.sh"]
