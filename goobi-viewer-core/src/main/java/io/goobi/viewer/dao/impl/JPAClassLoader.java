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
package io.goobi.viewer.dao.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;

/**
 * <p>
 * JPAClassLoader class.
 * </p>
 */
public class JPAClassLoader extends ClassLoader {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(JPAClassLoader.class);

    /** Constant <code>PERSISTENCE_XML="META-INF/persistence.xml"</code> */
    public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
    /** Constant <code>PERSISTENCE_XML_MODULE="META-INF/persistence-module.xml"</code> */
    public static final String PERSISTENCE_XML_MODULE = "META-INF/persistence-module.xml";
    //    private static final String MASTER_URL_SUFFIX = "Viewer/build/classes/" + PERSISTENCE_XML;
    //    private static final String MODULE_URL_SUFFIX = ".jar!/" + PERSISTENCE_XML;

    //    public static String masterUrlSuffix = MASTER_URL_SUFFIX;

    private URL newUrl;

    //    enum Filter {
    //        MASTER {
    //            @Override
    //            public boolean filter(URL url) {
    //                return url.toExternalForm().endsWith(masterUrlSuffix);
    //            }
    //        },
    //        MODULES {
    //            @Override
    //            public boolean filter(URL url) {
    //                return url.toExternalForm().endsWith(MODULE_URL_SUFFIX);
    //            }
    //        };
    //
    //        public abstract boolean filter(URL url);
    //    }

    /**
     * <p>
     * Constructor for JPAClassLoader.
     * </p>
     *
     * @param parent a {@link java.lang.ClassLoader} object.
     */
    public JPAClassLoader(final ClassLoader parent) {
        super(parent);
    }

    //    private List<URL> findPersistenceXMLs(Filter filter) throws IOException {
    //        List<URL> result = new ArrayList<>();
    //
    //        for (final Enumeration<URL> e = super.getResources(PERSISTENCE_XML); e.hasMoreElements();) {
    //            URL url = e.nextElement();
    //            if (filter.filter(url)) {
    //                result.add(url);
    //                logger.info("Found {} persistence.xml in {}", filter, url);
    //            }
    //        }
    //
    //        return result;
    //    }

    private List<URL> findCorePersistenceXMLs() throws IOException {
        List<URL> result = new ArrayList<>();

        for (final Enumeration<URL> e = super.getResources(PERSISTENCE_XML); e.hasMoreElements();) {
            URL url = e.nextElement();
            result.add(url);
            logger.info("Found persistence.xml in {}", url);
        }

        return result;
    }

    private List<URL> findModulePersistenceXMLs() throws IOException {
        List<URL> result = new ArrayList<>();

        for (final Enumeration<URL> e = super.getResources(PERSISTENCE_XML_MODULE); e.hasMoreElements();) {
            URL url = e.nextElement();
            result.add(url);
            logger.info("Found persistence-module.xml in {}", url);
        }

        return result;
    }

    /**
     *
     * @param masterFileUrl
     * @param moduleUrls
     * @return {@link Document containing merged and module persistence.xml
     * @throws IOException
     * @throws JDOMException
     * @should merge persistence xml files correctly
     */
    static Document scanPersistenceXML(URL masterFileUrl, List<URL> moduleUrls) throws IOException, JDOMException {
        logger.trace("scanPersistenceXML(): {}", masterFileUrl);
        Document docMerged = new Document();
        Document docMaster = XmlTools.readXmlFile(masterFileUrl);
        Element eleMasterRoot = docMaster.getRootElement();

        // Collect already defined class names for this persistence unit
        Map<String, Set<String>> masterExistingClasses = new HashMap<>(2);
        Map<String, Element> masterPuMap = new HashMap<>(2);
        for (Element elePU : eleMasterRoot.getChildren("persistence-unit", null)) {
            String puName = elePU.getAttributeValue("name");
            masterPuMap.put(puName, elePU);
            for (Element eleClass : elePU.getChildren("class", null)) {
                Set<String> classNames = masterExistingClasses.get(puName);
                if (classNames == null) {
                    classNames = new HashSet<>();
                    masterExistingClasses.put(puName, classNames);
                }
                classNames.add(eleClass.getText().trim());
            }
        }

        // Iterate over module persistence.xml files
        for (URL url : moduleUrls) {
            logger.trace("Processing {}", url);
            Document docModule = XmlTools.readXmlFile(url);
            // For each persistence unit in the master file check for any new classes in the module file
            for (Element eleModulePU : docModule.getRootElement().getChildren("persistence-unit", null)) {
                String puName = eleModulePU.getAttributeValue("name");
                if (masterPuMap.containsKey(puName) && masterExistingClasses.containsKey(puName)) {
                    Element eleMasterPU = masterPuMap.get(puName);
                    for (Element eleModuleClass : eleModulePU.getChildren("class", null)) {
                        String className = eleModuleClass.getText().trim();
                        if (!masterExistingClasses.get(puName).contains(className)) {
                            eleMasterPU.addContent(new Element("class").setText(className).setNamespace(eleModuleClass.getNamespace()));
                            logger.debug("Added class '{}' to persistence unit '{}'.", className, puName);
                        }
                    }
                } else {
                    logger.debug("Persistence unit {} not found in master persistence.xml", puName);
                }
            }
        }

        eleMasterRoot.detach();
        docMerged.addContent(eleMasterRoot);

        return docMerged;
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        // logger.trace("getResources: {}", name); //NOSONAR Debug
        if (PERSISTENCE_XML.equals(name)) {
            if (newUrl == null) {
                try {
                    List<URL> coreFileUrl = findCorePersistenceXMLs();
                    if (coreFileUrl.isEmpty()) {
                        throw new DAOException("Core persistence.xml");
                    }
                    Document docMerged = scanPersistenceXML(coreFileUrl.get(0), findModulePersistenceXMLs());
                    // logger.trace("persistence.xml\n{}", new XMLOutputter().outputString(docMerged)); //NOSONAR Debug

                    // The base directory must be empty since Hibernate will scan it searching for classes.
                    final File file = new File(System.getProperty("java.io.tmpdir") + "/viewer/" + PERSISTENCE_XML);
                    file.getParentFile().mkdirs();
                    XmlTools.writeXmlFile(docMerged, file.getAbsolutePath());
                    newUrl = file.toURI().toURL();
                    //                    newUrl = new URL("file://" + file.getAbsolutePath());
                    logger.info("URL: {}", newUrl);
                } catch (JDOMException e) {
                    throw new IOException(e.toString());
                } catch (DAOException e) {
                    throw new IOException(e.toString());
                }
            }

            return new Enumeration<URL>() {
                private URL url = newUrl;

                @Override
                public boolean hasMoreElements() {
                    return url != null;
                }

                @Override
                public URL nextElement() {
                    final URL url2 = url;
                    url = null;
                    return url2;
                }
            };
        }

        return super.getResources(name);
    }
}
