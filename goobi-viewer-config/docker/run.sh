#!/bin/bash
set -e

[ -z "$CONFIGSOURCE" ] && CONFIGSOURCE="default"
[ -z "$USE_SSL" ] && USE_SSL="false"
[ -z "$DEV" ] && DEV="false"
[ -z "$VIEWER_DOMAIN" ] && VIEWER_DOMAIN="localhost:8080"
[ -z "$THEME_NAME" ] && THEME_NAME="reference"
[ -z "$STOPWORDS_LANG" ] && STOPWORDS_LANG="de"
[ -z "${VIEWER_BASE_PATH+x}" ] && VIEWER_BASE_PATH="/viewer"

# /path/to/application -> path#to#application  (Tomcat nested-context convention)
# / (root)             -> ROOT
if [[ "$VIEWER_BASE_PATH" == "/" ]]; then
  WEBAPP_NAME="ROOT"
  VIEWER_BASE_PATH=""
elif ! [[ "$VIEWER_BASE_PATH" =~ ^(/[a-zA-Z0-9_-]+)+$ ]]; then
  echo "VIEWER_BASE_PATH invalid: '$VIEWER_BASE_PATH'"
  exit 1
else
  WEBAPP_NAME="${VIEWER_BASE_PATH#/}"
  WEBAPP_NAME="${WEBAPP_NAME//\//#}"
  VIEWER_BASE_PATH="${VIEWER_BASE_PATH#/}"
  # redirect to application path
  rm -rf "${CATALINA_HOME}/webapps/ROOT" && mkdir "${CATALINA_HOME}/webapps/ROOT" && echo "<% response.sendRedirect(\"/${VIEWER_BASE_PATH}/\"); %>" > "${CATALINA_HOME}/webapps/ROOT/index.jsp"
fi
WEBAPP_DIR="${CATALINA_HOME}/webapps/${WEBAPP_NAME}"

if [[ "$WEBAPP_NAME" != "viewer" && -d "${CATALINA_HOME}/webapps/viewer" && ! -d "$WEBAPP_DIR" ]]; then
    mv "${CATALINA_HOME}/webapps/viewer" "$WEBAPP_DIR"
fi

if [[ -z "$API_TOKEN" ]]; then
  API_TOKEN="$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32)"
  echo "No API Token specified, using random token."
  sed -i "s|<token>[^<]*</token>|<token>${API_TOKEN}</token>|" "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
fi

set -u

# Enable / Disable developer option (hot reloading for theme developers)
if ! [[ "$DEV" == "true" ]]; then
  patch ${CATALINA_HOME}/conf/context.xml.template < /viewer-template/disable_dev_options.patch
else
  echo "[WARN] Developer options enabled. Don't use in production ('DEV'=false)"
fi

if [[ -n "${THEME_PATH-}" ]]; then
  echo "Configuring theme as tomcat preresource..."
  envsubst "\$THEME_PATH" </viewer-template/insert_theme_preresource.patch.template > /viewer-template/insert_theme_preresource.patch
  patch ${CATALINA_HOME}/conf/context.xml.template < /viewer-template/insert_theme_preresource.patch
fi

# Generate directory structure in case the viewer directory is bind mounted
mkdir -p /opt/digiverso/{config/bin,indexer,logs,viewer/{abbyy,cmdi,deleted_mets,hotfolder,media,orig_lido,orig_denkxweb,success,ugc,alto,cms_media,error_mets,indexed_lido,mix,pdf,tei,updated_mets,cache,config,fulltext,indexed_mets,oai/token,ptif,themes,wc,bin}}

echo "Setting database configuration from environment..."
envsubst "\$DB_HOST \$DB_PORT \$DB_NAME \$DB_USER \$DB_PASSWORD" <"${CATALINA_HOME}/conf/viewer.xml.template" > "${CATALINA_HOME}/conf/Catalina/localhost/${WEBAPP_NAME}.xml"
envsubst "\$VIEWER_DOMAIN" <"${CATALINA_HOME}/conf/server.xml.template" >"${CATALINA_HOME}/conf/server.xml"
envsubst "\$TOMCAT_SAMESITECOOKIES" <"${CATALINA_HOME}/conf/context.xml.template" >"${CATALINA_HOME}/conf/context.xml"

if ! [[ -v SOLR_URL ]]; then
  export SOLR_URL="http://${SOLR_HOST}:8983/solr/collection1"
fi

echo "Setting SOLR URL from environment..."
sed -i "s#http://localhost:8983/solr/collection1#${SOLR_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
sed -i "s#http://localhost:8983/solr/collection1#${SOLR_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_oai.xml"

# Create stopwords file from env var
echo "Generating stopwords file"
IFS=',' read -r -a parts <<< "$STOPWORDS_LANG"
shopt -s nullglob
OUTPUT_FILE="/opt/digiverso/viewer/config/stopwords.txt"
echo "" > $OUTPUT_FILE
for part in "${parts[@]}"; do
    part=$(echo "$part" | xargs)  # trim whitespace
    file="/stopwords/stopwords_${part}.txt"
    [[ -f "$file" ]] && cat "$file" >> "$OUTPUT_FILE"
done

case $CONFIGSOURCE in
  folder)
    if [ -z "$CONFIG_FOLDER" ]
    then
      echo "CONFIG_FOLDER is required"
      exit 1
    fi

    if ! [ -d "$CONFIG_FOLDER" ]
    then
      echo "CONFIG_FOLDER:$CONFIG_FOLDER does not exists or is not a folder"
      exit 1
    fi
    
    if [ -z "$CONFIG_TARGET_FOLDER" ]
    then
      CONFIG_TARGET_FOLDER=/opt/digiverso/viewer/config
    fi

    echo "Copying configuration from local folder"
    [ -d "$CONFIG_FOLDER" ] && cp -arv --update=none "$CONFIG_FOLDER"/* "$CONFIG_TARGET_FOLDER"/
    ;;

  *)
    echo "Keeping configuration"
    ;;
esac

if [[ -n "$THEME_NAME" && -n "${THEME_PATH-}" ]]; then
  echo "Setting theme to '${THEME_NAME}'"
  THEME_WEBCONTENT_DIR="/opt/digiverso/viewer/themes/${THEME_PATH}/WebContent"
  sed -i 's|mainTheme="[^"]*" discriminatorField="">|mainTheme="'"${THEME_NAME}"'" discriminatorField="">\
          <rootPath>'"${THEME_WEBCONTENT_DIR}/resources/themes"'</rootPath>|' "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
  [ -f "${THEME_WEBCONTENT_DIR}/WEB-INF/web.xml" ] && cp "${THEME_WEBCONTENT_DIR}/WEB-INF/web.xml" "${WEBAPP_DIR}/WEB-INF/web.xml"
  [ -f "${THEME_WEBCONTENT_DIR}/WEB-INF/beans.xml" ] && cp "${THEME_WEBCONTENT_DIR}/WEB-INF/beans.xml" "${WEBAPP_DIR}/WEB-INF/beans.xml"
elif [[ -n "$THEME_NAME" ]]; then
  echo "Setting theme to '${THEME_NAME}'"
  sed -i 's/mainTheme="[^"]*"/mainTheme="'"${THEME_NAME}"'"/' "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
fi

echo "Setting viewer url from environment variable"
BASE_URL="${VIEWER_DOMAIN}${VIEWER_BASE_PATH:+/${VIEWER_BASE_PATH}}"
if [[ "$USE_SSL" == "true" ]]; then
  sed -Ei "s#http://localhost:8080/viewer#https://${BASE_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
  sed -Ei "s#http://localhost:8080/viewer#https://${BASE_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_oai.xml"
else
  sed -Ei "s#http://localhost:8080/viewer#http://${BASE_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_viewer.xml"
  sed -Ei "s#http://localhost:8080/viewer#http://${BASE_URL}#g" "${WEBAPP_DIR}/WEB-INF/classes/config_oai.xml"
fi

export MYSQL_PWS=${DB_PASSWORD}
while ! mysqladmin ping -h "${DB_HOST}" -u "${DB_USER}" --silent; do
      echo "Waiting for database to boot..."
      sleep 2
done

echo "Starting application server..."
exec catalina.sh run
