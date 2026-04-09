/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.SocketBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Servlet context listener that initialises and tears down application-wide resources and services on deployment and undeployment.
 */
@WebListener
public class ContextListener implements ServletContextListener {

    private static final int PRETTY_CONFIG_FILES_STRING_THRESHOLD = 1_000_000;

    private static final Logger logger = LogManager.getLogger(ContextListener.class);

    /** Constant <code>PRETTY_FACES_CONFIG_PARAM_NAME="com.ocpsoft.pretty.CONFIG_FILES"</code>. */
    public static final String PRETTY_FACES_CONFIG_PARAM_NAME = "com.ocpsoft.pretty.CONFIG_FILES";

    /** Constant <code>prettyConfigFiles="resources/themes/theme-url-mappings.xml"{trunked}</code>. */
    private volatile String prettyConfigFiles =
            "resources/themes/theme-url-mappings.xml, pretty-standard-config.xml, pretty-config-viewer-module-crowdsourcing.xml";

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Launching {}", () -> Version.asString());
        DataManager.getInstance();
        ViewerResourceBundle.init(sce.getServletContext());
        logger.trace("Temp folder: {}", () -> DataManager.getInstance().getConfiguration().getTempFolder());
        //        createResources();

        // Scan for all Pretty config files in module JARs
        // TODO This doesn't work if /WEB-INF/lib is mapped to a different folder in tomcat
        String libPath = sce.getServletContext().getRealPath("/WEB-INF/lib");
        if (libPath != null) {
            logger.debug("Lib path: {}", libPath);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(libPath), "*viewer-module-*.jar")) {
                StringBuilder sbPrettyConfigFiles = new StringBuilder(prettyConfigFiles);
                for (Path path : stream) {
                    logger.debug("Found module JAR: {}", path.getFileName());
                    try (FileInputStream fis = new FileInputStream(path.toFile()); ZipInputStream zip = new ZipInputStream(fis)) {
                        while (prettyConfigFiles.length() < PRETTY_CONFIG_FILES_STRING_THRESHOLD) {
                            ZipEntry e = zip.getNextEntry(); //NOSONAR only viewer jars are scanned, which we control; only entry names written
                            if (e == null) {
                                break;
                            }
                            String[] nameSplit = e.getName().split("/");
                            if (nameSplit.length > 0) {
                                String name = nameSplit[nameSplit.length - 1];
                                if (name.startsWith("pretty-config-")) {
                                    sbPrettyConfigFiles.append(", ").append(name);
                                }
                            }
                        }
                    }
                }
                prettyConfigFiles = sbPrettyConfigFiles.toString();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (FileSystemNotFoundException | ProviderNotFoundException e) {
                logger.error("Unable to scan theme-jar for pretty config files. Probably an older tomcat");
            }
        } else {
            logger.error("Resource '/WEB-INF/lib' not found.");
        }

        // Create local message files
        ViewerResourceBundle.createLocalMessageFiles();
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Shut down the task thread pool first so no running task can re-open resources
        // that are about to be closed (e.g. the Solr client).
        DataManager.getInstance().getRestApiJobManager().shutdown();
        SocketBean.shutdownExecutor();
        SearchBean.shutdown();

        try {
            DataManager.getInstance().getDao().shutdown();
            logger.info("Successfully stopped DAO");
        } catch (DAOException e) {
            logger.error("Error stopping DAO", e);
        }
        // Close EhCache. ContentServerCacheManager.close() also shuts down EhCache's static
        // async-flush executor (MappedPageSource.ASYNC_FLUSH_EXECUTOR) so the
        // "MappedByteBufferSource Async Flush Thread" terminates cleanly on undeploy.
        ContentServerCacheManager.getInstance().close();
        try {
            DataManager.getInstance().closeSearchIndex();
            logger.info("Successfully closed Solr client");
            // Http2SolrClient uses Jetty threads (h2sc-*) and HttpClient scheduler threads
            // that do not terminate synchronously on close(). Wait briefly so Tomcat does not
            // report them as memory leaks.
            Thread.getAllStackTraces().keySet().stream()
                    .filter(t -> t.getName().matches("h2sc-.*|HttpClient@[0-9a-f]+-scheduler-.*"))
                    .forEach(t -> {
                        try {
                            t.join(5000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        } catch (IOException e) {
            logger.error("Error closing Solr client", e);
        }
        DataManager.getInstance().getLanguageHelper().shutdown();
        ViewerResourceBundle.shutdown();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            try {
                DriverManager.deregisterDriver(drivers.nextElement());
            } catch (SQLException e) {
                logger.error("Error deregistering JDBC driver", e);
            }
        }
    }
}
