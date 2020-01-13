/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.persistence.PersistenceException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;

/**
 * <p>ContextListener class.</p>
 */
@WebListener
public class ContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ContextListener.class);

    /** Constant <code>PRETTY_FACES_CONFIG_PARAM_NAME="com.ocpsoft.pretty.CONFIG_FILES"</code> */
    public static final String PRETTY_FACES_CONFIG_PARAM_NAME = "com.ocpsoft.pretty.CONFIG_FILES";

    /** Constant <code>prettyConfigFiles="resources/themes/theme-url-mappings.xml"{trunked}</code> */
    public static volatile String prettyConfigFiles =
            "resources/themes/theme-url-mappings.xml, pretty-standard-config.xml, pretty-config-viewer-module-crowdsourcing.xml";

    //    static {
    // ImageIO.scanForPlugins();
    //    }

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Launching {}", Helper.getVersion());
        DataManager.getInstance();
        logger.trace("Temp folder: {}", DataManager.getInstance().getConfiguration().getTempFolder());
        // Add a "member" role, if not yet in the database
        try {
            if (DataManager.getInstance().getDao().getRole("member") == null) {
                logger.info("Role 'member' does not exist yet, adding...");
                if (!DataManager.getInstance().getDao().addRole(new Role("member"))) {
                    logger.error("Could not add static role 'member'.");
                }
            }
            LicenseType.addCoreLicenseTypesToDB();
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
        //        createResources();

        // Scan for all Pretty config files in module JARs
        // TODO This doesn't work if /WEB-INF/lib is mapped to a different folder in tomcat
        try {
            String libPath = sce.getServletContext().getRealPath("/WEB-INF/lib");
            if (libPath != null) {
                logger.debug("Lib path: {}", libPath);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(libPath), "*viewer-module-*.jar")) {
                    for (Path path : stream) {
                        logger.debug("Found module JAR: {}", path.getFileName().toString());
                        try (FileInputStream fis = new FileInputStream(path.toFile()); ZipInputStream zip = new ZipInputStream(fis)) {
                            while (true) {
                                ZipEntry e = zip.getNextEntry();
                                if (e == null) {
                                    break;
                                }
                                String[] nameSplit = e.getName().split("/");
                                if (nameSplit.length > 0) {
                                    String name = nameSplit[nameSplit.length - 1];
                                    if (name.startsWith("pretty-config-")) {
                                        prettyConfigFiles += ", " + name;
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    //                } catch (URISyntaxException e) {
                    //                    logger.error(e.getMessage(), e);
                } catch (FileSystemNotFoundException | ProviderNotFoundException e) {
                    logger.error("Unable to scan theme-jar for pretty config files. Probably an older tomcat");
                }
            } else {
                logger.error("Resource '/WEB-INF/lib' not found.");
            }
            //        } catch (MalformedURLException e) {
            //            logger.error(e.getMessage(), e);
        } finally {
        }

        // Set Pretty config files parameter
        sce.getServletContext().setInitParameter(PRETTY_FACES_CONFIG_PARAM_NAME, prettyConfigFiles);
        logger.debug("Pretty config files: {}", prettyConfigFiles);
        
        //set contentServerConfig
        ContentServerConfiguration.getInstance("contentServerConfig.xml");
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            DataManager.getInstance().getDao().shutdown();
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }

        // Shut all loggers down to prevent memory leaks when re-deploying the context
        LogManager.shutdown();
    }
}
