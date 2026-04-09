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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

 class JPAClassLoaderTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(JPAClassLoaderTest.class);

    private File persistenceXmlFile;

    @BeforeEach
    public void setUp() {
        persistenceXmlFile = new File(System.getProperty("java.io.tmpdir") + "/viewer/" + JPAClassLoader.PERSISTENCE_XML);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(persistenceXmlFile.toPath());
    }

    /**
     * @see JPAClassLoader#scanPersistenceXML(String,List)
     * @verifies merge persistence xml files correctly
     */
    @Test
    void scanPersistenceXML_shouldMergePersistenceXmlFilesCorrectly() throws Exception {
        // TODO Fails on Jenkins
        File masterFile = new File("src/main/resources/META-INF/persistence.xml");
        Assertions.assertTrue(masterFile.isFile());
        URL masterUrl = new URI("file:///" + masterFile.getAbsolutePath()).toURL();

        File file = new File("src/test/resources/modules/persistence.xml");
        Assertions.assertTrue(file.isFile());
        URL moduleUrl = new URI("file:///" + file.getAbsolutePath()).toURL();

        Document doc = JPAClassLoader.scanPersistenceXML(masterUrl, Collections.singletonList(moduleUrl));
        Assertions.assertNotNull(doc);
        logger.trace(new XMLOutputter().outputString(doc));
        Element eleRoot = doc.getRootElement();
        Assertions.assertNotNull(eleRoot);
        List<Element> eleListPU = eleRoot.getChildren();
        Assertions.assertEquals(2, eleListPU.size());

        {
            Element elePU1 = eleListPU.get(0);
            Assertions.assertFalse( elePU1.getChildren("class", null).isEmpty());
            Set<String> classes = new HashSet<>();
            for (Element eleClass : elePU1.getChildren("class", null)) {
                classes.add(eleClass.getText());
                logger.trace(eleClass.getText());
            }
            Assertions.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass1"));
            Assertions.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass2"));
            //            {
            //                String xpathExpr =
            //                        "//persistence/persistence-unit[@name='intranda_viewer_tomcat']/class[text()='io.goobi.viewer.model.dummymodule.DummyClass1']";
            //                XPathBuilder<Element> builder = new XPathBuilder<>(xpathExpr, Filters.element());
            //                 builder.setNamespace("persistence", "http://java.sun.com/xml/ns/persistence");
            //                 builder.setNamespace(Namespace.NO_NAMESPACE);
            //                XPathExpression<Element> xpath = builder.compileWith(XPathFactory.instance());
            //                List<Element> eleListClasses = xpath.evaluate(doc);
            //                Assertions.assertEquals(1, eleListClasses.size());
            //            }
        }
        {
            Element elePU2 = eleListPU.get(1);
            Set<String> classes = new HashSet<>();
            for (Element eleClass : elePU2.getChildren("class", null)) {
                classes.add(eleClass.getText());
                logger.trace(eleClass.getText());
            }
            Assertions.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass3"));
            Assertions.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass4"));
        }
    }

    /**
     * @see JPAClassLoader#getResources(String)
     * @verifies write non-empty persistence xml to temp directory
     */
    @Test
    void getResources_shouldWriteNonEmptyPersistenceXmlToTempDirectory() throws Exception {
        JPAClassLoader loader = new JPAClassLoader(Thread.currentThread().getContextClassLoader());
        Enumeration<URL> resources = loader.getResources(JPAClassLoader.PERSISTENCE_XML);

        Assertions.assertTrue(resources.hasMoreElements(), "Should return at least one URL");
        URL url = resources.nextElement();
        Assertions.assertNotNull(url);

        File written = new File(url.toURI());
        Assertions.assertTrue(written.exists(), "persistence.xml should exist at " + written.getAbsolutePath());
        Assertions.assertTrue(written.length() > 0, "persistence.xml must not be empty (was 0 bytes — race condition bug)");
    }

    /**
     * @see JPAClassLoader#getResources(String)
     * @verifies not produce empty file under concurrent access
     */
    @Test
    void getResources_shouldNotProduceEmptyFileUnderConcurrentAccess() throws Exception {
        int threadCount = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger emptyFileCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    // All threads start simultaneously to maximize race condition probability
                    startLatch.await();
                    JPAClassLoader loader = new JPAClassLoader(Thread.currentThread().getContextClassLoader());
                    Enumeration<URL> resources = loader.getResources(JPAClassLoader.PERSISTENCE_XML);
                    if (resources.hasMoreElements()) {
                        File written = new File(resources.nextElement().toURI());
                        if (written.length() == 0) {
                            emptyFileCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                return null;
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        Assertions.assertEquals(0, emptyFileCount.get(),
                "persistence.xml was empty in " + emptyFileCount.get() + " thread(s) — atomic write failed");
    }

}
