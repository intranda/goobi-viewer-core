package io.goobi.viewer.model.archives;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Before;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class ArchiveManagerTest extends AbstractTest{

    BasexEADParser eadParser;
    
    @Before
    public void before() {
        try {
            Document doc = XmlTools.readXmlFile("src/test/resources/data/EAD_Export_Tektonik.XML");
            eadParser = new BasexEADParser(null, null);
            eadParser.readConfiguration(DataManager.getInstance().getConfiguration().getArchiveMetadataConfig())
                    .parseEadFile(doc);
        } catch (IOException | JDOMException | PresentationException | IndexUnreachableException | ConfigurationException e) {
            fail(e.toString());
        }
    }
    
    //@Test
    public void test() {
        ArchiveManager archiveManager = new ArchiveManager(eadParser, null);
        assertEquals(1, archiveManager.getDatabases().size());
    }

}
