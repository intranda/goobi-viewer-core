package io.goobi.viewer.model.archives;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class ArchiveManagerTest extends AbstractTest{

    BasexEADParser eadParser;
    
    @Before
    public void before() {
        try {
            Document doc = XmlTools.readXmlFile("src/test/resources/data/EAD_Export_Tektonik.XML");
            BasexEADParser tempParser = new BasexEADParser(null, null);
            ArchiveEntry root = tempParser.readConfiguration(DataManager.getInstance().getConfiguration().getArchiveMetadataConfig())
                    .parseEadFile(doc);
            
            eadParser = new BasexEADParser(null, null) {
                public List<ArchiveResource> getPossibleDatabases() {
                    return Arrays.asList(
                            new ArchiveResource("database 1", "resource 1", ZonedDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.systemDefault()).format(ArchiveResource.DATE_TIME_FORMATTER), "10"),
                            new ArchiveResource("database 1", "resource 2", ZonedDateTime.now().format(ArchiveResource.DATE_TIME_FORMATTER), "10")
                            );
                }
                
                public ArchiveEntry loadDatabase(ArchiveResource database) {
                    return root;
                }
            };
            
        } catch (IOException | JDOMException | PresentationException | IndexUnreachableException | ConfigurationException e) {
            fail(e.toString());
        }
    }
    
    @Test
    public void testGetDatabases() {
        ArchiveManager archiveManager = new ArchiveManager(eadParser, null);
        assertEquals(2, archiveManager.getDatabases().size());
    }
    
    @Test
    public void testGetDatabase() {
        {            
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser, null));
            ArchiveTree tree = archiveManager.getArchiveTree("database 1", "resource 1");
            assertNotNull(tree);
        }
        {            
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser, null));
            ArchiveTree tree = archiveManager.getArchiveTree("database 1", "resource 2");
            assertNotNull(tree);
        }
        {            
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser, null));
            ArchiveTree tree = archiveManager.getArchiveTree("database 1", "resource 3");
            assertNull(tree);
        }
    }
    
    @Test
    public void testUpdateDatabase() throws IllegalStateException, ConfigurationException, IOException, HTTPException, JDOMException {
        {            
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser, null));
            archiveManager.getArchiveTree("database 1", "resource 1");
            archiveManager.getArchiveTree("database 1", "resource 1");
            Mockito.verify(archiveManager, Mockito.times(1)).loadDatabase(Mockito.any(), Mockito.any());
        }
        {            
            ArchiveManager archiveManager = Mockito.spy(new ArchiveManager(eadParser, null));
            archiveManager.getArchiveTree("database 1", "resource 2");
            archiveManager.getArchiveTree("database 1", "resource 2");
            Mockito.verify(archiveManager, Mockito.times(2)).loadDatabase(Mockito.any(), Mockito.any());
        }
    }


}
