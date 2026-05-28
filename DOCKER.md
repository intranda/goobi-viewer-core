# Goobi viewer Docker Image

## Install Goobi viewer based on Docker

Please make sure that you have [Docker](https://www.docker.com/) installed on your machine to execute the following
commands to install the Goobi viewer in your Docker environment.

```bash
# download the docker-compose file from the repository e.g. using wget
mkdir goobi-viewer && wget -O goobi-viewer/docker-compose.yml https://raw.githubusercontent.com/intranda/goobi-viewer-core/develop/docker-compose.yml

# go into the checked out directory
cd goobi-viewer

# open the docker-compose.yml file in your favourite editor and change the default passwords to a value of your choice
gedit docker-compose.yml

# start the viewer application stack
docker compose up -d
```

The Goobi viewer is now running and can be accessed through a web browser using the following data:

| Information | Description                                          |
| ----------- | ---------------------------------------------------- |
| URL:        | <http://localhost:8080/viewer>                       |
| Login:      | `${VIEWER_USERMAIL}` (value from docker-compose.yml) |
| Password:   | `${VIEWER_USERPASS}` (value from docker-compose.yml) |

The stack consists of the following services:

| Service     | Description                                            |
| ----------- | ------------------------------------------------------ |
| `viewer`    | The Goobi viewer web application (Tomcat 10 + Java 21) |
| `viewer-db` | MariaDB database used by the viewer                    |
| `indexer`   | Goobi viewer indexer, writes records to Solr           |
| `solr`      | Apache Solr search index                               |
| `zookeeper` | Zookeeper coordinator for Solr                         |

## Stop Goobi viewer and restart it later again

To stop a running Goobi viewer instance please make sure that you are inside of the directory `goobi-viewer` to
execute this command:

```bash
# stop Goobi viewer
docker compose stop
```

To start a stopped Goobi viewer instance later again please make sure that you are in the directory `goobi-viewer`
again as shown above. Then execute this command:

```bash
# restart Goobi viewer
docker compose start
```

## Uninstall Goobi viewer from Docker

To uninstall the Goobi viewer from your system please execute the following commands from inside the
directory `goobi-viewer`:

```bash
cd goobi-viewer

# remove running containers and the network configuration
docker compose down

# delete all the application data
cd .. && sudo rm -rf goobi-viewer

# cleanup the Goobi viewer Docker images
docker image rm intranda/goobi-viewer:latest
docker image rm intranda/goobi-viewer-indexer:latest
docker image rm intranda/goobi-viewer-docker-solr:latest

# cleanup the database Docker image
docker image rm mariadb:latest
```

## Configuration via environment variables

The viewer container is configured through environment variables that are evaluated by the entrypoint script
([`goobi-viewer-config/docker/run.sh`](goobi-viewer-config/docker/run.sh)) on startup. The script writes the values
into the Tomcat configuration (`server.xml`, `context.xml`, the JNDI resource definition) and into
`config_viewer.xml` / `config_oai.xml` inside the deployed web application.

### Database connection

| Variable      | Default   | Required | Description                                             |
| ------------- | --------- | -------- | ------------------------------------------------------- |
| `DB_HOST`     | viewer-db | no       | Hostname of the MariaDB/MySQL server (e.g. `viewer-db`) |
| `DB_PORT`     | 3306      | no       | Database port (typically `3306`)                        |
| `DB_NAME`     | viewer    | no       | Name of the viewer database                             |
| `DB_USER`     | viewer    | no       | Database user                                           |
| `DB_PASSWORD` | CHANGEME  | yes      | Password for `DB_USER`                                  |

The startup script waits for the database to become reachable before launching Tomcat.

### Solr connection

| Variable    | Default                                 | Required | Description                                                                                                          |
| ----------- | --------------------------------------- | -------- | -------------------------------------------------------------------------------------------------------------------- |
| `SOLR_HOST` | `solr`                                  | no       | Solr hostname; Defaults to `solr` to use the shipped solr container.                                                 |
| `SOLR_URL`  | `http://${SOLR_HOST}:8983/solr/current` | no       | Full Solr core URL. Constructed from the SOLR_HOST if **not** explicitly specified. Set to use custom solr instance. |

### Web application URL and deployment

| Variable                 | Default          | Required | Description                                                                                                                                                                                      |
| ------------------------ | ---------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `VIEWER_DOMAIN`          | `localhost:8080` | no       | Public hostname (and optional port) under which the viewer is reachable                                                                                                                          |
| `VIEWER_BASE_PATH`       | `/viewer`        | no       | Context path. Use `/` to deploy as `ROOT`. Nested paths like `/foo/bar` are mapped to Tomcat's `foo#bar` convention. A redirect from `/` to the configured base path is installed automatically. |
| `USE_SSL`                | `false`          | no       | When `true`, configured URLs use `https://` instead of `http://`                                                                                                                                 |
| `TOMCAT_SAMESITECOOKIES` | `strict`         | no       | Value for the SameSite cookie attribute (e.g. `strict`, `lax`, `none`)                                                                                                                           |

### Theme

| Variable     | Default     | Required                        | Description                                                                                                                                                                                                                                      |
| ------------ | ----------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `THEME_NAME` | `reference` | no                              | Name of the active theme (written to `mainTheme` in `config_viewer.xml`)                                                                                                                                                                         |
| `THEME_DIR`  | —           | yes, if custom theme is mounted | Optional. Directory name of a theme that ships its `WebContent` as a Tomcat pre-resource. Path is relative to `/opt/digiverso/viewer/themes/`. When set, the theme's `web.xml` and `beans.xml` (if present) are copied into the deployed webapp. |

### Initial superuser

| Variable          | Default              | Required | Description                                                                                          |
| ----------------- | -------------------- | -------- | ---------------------------------------------------------------------------------------------------- |
| `VIEWER_USERMAIL` | `goobi@intranda.com` | no       | Email address of the initial superuser if VIEWER_USERPASS is set; Otherwise user creation is skipped |
| `VIEWER_USERPASS` | —                    | no       | Password of the initial superuser. If empty, no user is created or modified.                         |

### Search / language

| Variable         | Default | Required | Description                                                                                                                                                             |
| ---------------- | ------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `STOPWORDS_LANG` | `de`    | no       | Comma-separated list of language codes (e.g. `de,en,fr`). The matching `stopwords_<lang>.txt` files are concatenated into `/opt/digiverso/viewer/config/stopwords.txt`. |

### API token

| Variable    | Default         | Required            | Description                                                                                                                                                                                   |
| ----------- | --------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `API_TOKEN` | random 32 chars | no, but recommended | Token written into `<token>` in `config_viewer.xml`. If unset, a random token is generated on each container start. Recommended to either set or override via local `config_viewer.xml` file. |

### Development

| Variable | Default | Required | Description                                                                                                          |
| -------- | ------- | -------- | -------------------------------------------------------------------------------------------------------------------- |
| `DEV`    | `false` | no       | When `true`, enables Tomcat developer options (hot reloading for theme developers). **Do not enable in production.** |

## Volumes

The viewer container expects the following bind mounts (see `docker-compose.yml`):

| Mount inside container   | Purpose                                                      |
| ------------------------ | ------------------------------------------------------------ |
| `/opt/digiverso/viewer`  | Viewer working data, configuration, media, hotfolder, themes |
| `/opt/digiverso/indexer` | Shared indexer working directory                             |
| `/opt/digiverso/logs`    | Application log files                                        |
| `/usr/local/tomcat/logs` | Tomcat access and catalina logs                              |

## Application Configuration

The main configuration file is `config_viewer.xml` inside the deployed web application. The container ships
defaults that are patched at startup with the values from the environment variables described above. For
persistent customizations, place a `config_viewer.xml` (and any other config file) in the bind-mounted
`/opt/digiverso/viewer/config` directory or bind-mount an entire config directory directly to it. <br>
However, it is recommended to use the environment variables as much as possible, as any changes to the config_viewer.xml might have to be manually migrated during updates.

## Information and Feedback

- Goobi Website: <https://goobi.io>
- Goobi Community: <https://community.goobi.io/>
- Goobi Documentation: <https://docs.goobi.io>
