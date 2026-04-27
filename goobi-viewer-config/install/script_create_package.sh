#!/bin/bash

## CONFIG ##
VIEWERDBNAME="viewer"
VIEWERFOLDER="/opt/digiverso/viewer"
SOLRURL=""
MYSQLUSER=viewer
MYSQLPASS=CHANGEME

USAGE="script_create_package.sh -d VIEWERDBNAME -f /path/to/viewer -s https://viewer.example.org/solr/"
while getopts "d:f:s:h" OPCOES; do
        case $OPCOES in
                d ) VIEWERDBNAME="${OPTARG}";;
                f ) VIEWERFOLDER="${OPTARG}";;
                s ) SOLRURL="${OPTARG}";;
                h ) echo "$USAGE"
                    exit 0;;
                * ) echo "$USAGE"
                    exit 1;;
        esac
done


TMPDIR=/tmp/viewerconfig
TEMPFILENAME="${VIEWERDBNAME}_developer.zip"
SQLDUMP=${TMPDIR}/viewer.sql

## ERROR HANDLING ##
type -P xmlstarlet &>/dev/null || { echo "ERROR: xmlstarlet does not exist. Aborting" >&2; exit 1; }
type -P zip &>/dev/null || { echo "ERROR: zip does not exist. Aborting" >&2; exit 1; }

if [ ! -d "${VIEWERFOLDER}" ] ; then
  echo "ERROR: The given path ${VIEWERFOLDER} does not exist or is not a directory." >&2;
  exit 1
fi

if [ ! -f "${VIEWERFOLDER}/config/config_viewer.xml" ] ; then
  echo "ERROR: There is no file config_viewer.xml at the given path ${VIEWERFOLDER}/config/." >&2;
  exit 1
fi


if [ -f /tmp/viewerconfig.lock ]; then
        echo "Script already running..." >&2;
        exit 1
fi
touch /tmp/viewerconfig.lock

if [ ! -d "${TMPDIR}" ] ; then
	mkdir "${TMPDIR}"
fi

# Copy needed files
cp ${VIEWERFOLDER}/config/config_viewer.xml ${TMPDIR}

for f in ${VIEWERFOLDER}/config/messages_*.properties; do
        [ -e "${f}" ] && cp ${f} ${TMPDIR}
done

if [ -f ${VIEWERFOLDER}/config/config_viewer-module-crowdsourcing.xml ]; then
  cp ${VIEWERFOLDER}/config/config_viewer-module-crowdsourcing.xml ${TMPDIR}
fi

/usr/bin/mysqldump -u ${MYSQLUSER} -p${MYSQLPASS} ${VIEWERDBNAME} --ignore-table=viewer.crowdsourcing_fulltexts >> ${SQLDUMP}

## Create superuser goobi@intranda.com with password 'viewer'
echo 'DELETE FROM viewer_users WHERE email = "goobi@intranda.com";' >> ${SQLDUMP}
echo 'INSERT INTO viewer_users (active,email,password_hash,score,superuser) SELECT 1,"goobi@intranda.com", "$2a$10$Z5GTNKND9ZbuHt0ayDh0Remblc7pKUNlqbcoCxaNgKza05fLtkuYO",0,1 WHERE NOT EXISTS (SELECT 1 FROM viewer_users WHERE email = "goobi@intranda.com");' >> ${SQLDUMP}

# Extract information from config_viewer.xml
# If the RESTURL is missing the script will stop here because xmlstarlet will exit with 1.
# please add the information mentioned here and run again:
#   https://docs.goobi.io/goobi-viewer-de/devop/1#2019-07-06
THEMENAME=$(xmlstarlet sel -t -v '//config/viewer/theme/@mainTheme' ${VIEWERFOLDER}/config/config_viewer.xml)
RESTURL=$(xmlstarlet sel -t -v '//config/urls/rest' ${VIEWERFOLDER}/config/config_viewer.xml)

# as the solr is always asked via http and localhost but sometimes in rare cases does not have collection1
# we need to generate the developer URL from the base url from REST and folders from solr
#
# ... but only in case we did not
sed -i "s|<solr>.*</solr>|<solr>${SOLRURL}</solr>|g" ${TMPDIR}/config_viewer.xml

# replace rest with iiif
sed -i "s|<rest>\(.*\)</rest>|<iiif>\1</iiif>|g" ${TMPDIR}/config_viewer.xml

# add rest to localhost url
sed -i 's|</iiif>|</iiif>\n<rest>http://localhost:8080/viewer/api/v1/</rest>|g' ${TMPDIR}/config_viewer.xml

# remove theme/rootPath configuration. This is done to avoid errors when deplying the config on a system with no local theme repository
sed -i '/<rootPath>.*<\/rootPath>/d' ${TMPDIR}/config_viewer.xml

# create zip file
zip -rqj /tmp/${TEMPFILENAME} ${TMPDIR}

# delete temp dir
rm -rf ${TMPDIR}

# must write this to output to inform viewer of path to zip file
echo "/tmp/${TEMPFILENAME}"

rm /tmp/viewerconfig.lock
