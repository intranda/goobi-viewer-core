package io.goobi.viewer.dao.impl;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.dao.impl.JPAClassLoader;

public class JPAClassLoaderTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(JPAClassLoaderTest.class);

    /**
     * @see JPAClassLoader#scanPersistenceXML(String,List)
     * @verifies merge persistence xml files correctly
     */
    @Test
    public void scanPersistenceXML_shouldMergePersistenceXmlFilesCorrectly() throws Exception {
        // TODO Fails on Jenkins
        File masterFile = new File("src/main/resources/META-INF/persistence.xml");
        Assert.assertTrue(masterFile.isFile());
        URL masterUrl = new URL("file:///" + masterFile.getAbsolutePath());

        File file = new File("src/test/resources/modules/persistence.xml");
        Assert.assertTrue(file.isFile());
        URL moduleUrl = new URL("file:///" + file.getAbsolutePath());

        Document doc = JPAClassLoader.scanPersistenceXML(masterUrl, Collections.singletonList(moduleUrl));
        Assert.assertNotNull(doc);
        logger.trace(new XMLOutputter().outputString(doc));
        Element eleRoot = doc.getRootElement();
        Assert.assertNotNull(eleRoot);
        List<Element> eleListPU = eleRoot.getChildren();
        Assert.assertEquals(2, eleListPU.size());

        {
            Element elePU1 = eleListPU.get(0);
            Assert.assertFalse( elePU1.getChildren("class", null).isEmpty());
            Set<String> classes = new HashSet<>();
            for (Element eleClass : elePU1.getChildren("class", null)) {
                classes.add(eleClass.getText());
                logger.trace(eleClass.getText());
            }
            Assert.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass1"));
            Assert.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass2"));
            //            {
            //                String xpathExpr =
            //                        "//persistence/persistence-unit[@name='intranda_viewer_tomcat']/class[text()='io.goobi.viewer.model.dummymodule.DummyClass1']";
            //                XPathBuilder<Element> builder = new XPathBuilder<>(xpathExpr, Filters.element());
            //                 builder.setNamespace("persistence", "http://java.sun.com/xml/ns/persistence");
            //                 builder.setNamespace(Namespace.NO_NAMESPACE);
            //                XPathExpression<Element> xpath = builder.compileWith(XPathFactory.instance());
            //                List<Element> eleListClasses = xpath.evaluate(doc);
            //                Assert.assertEquals(1, eleListClasses.size());
            //            }
        }
        {
            Element elePU2 = eleListPU.get(1);
            Set<String> classes = new HashSet<>();
            for (Element eleClass : elePU2.getChildren("class", null)) {
                classes.add(eleClass.getText());
                logger.trace(eleClass.getText());
            }
            Assert.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass3"));
            Assert.assertTrue(classes.contains("io.goobi.viewer.model.dummymodule.DummyClass4"));
        }
    }

}
