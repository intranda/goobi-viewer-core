package io.goobi.viewer.controller.model;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;

class ImageViewConditionTest {

    ViewAttributes condition;

    @BeforeEach
    void setup() {

        PageType pageType = PageType.viewFullscreen;
        MimeType mimeType = new MimeType("image/jpeg");
        String docStructType = "volume";
        String collection = "dc.image";
        int pageCount = 10;

        ViewManager viewManager = Mockito.mock(ViewManager.class);
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        StructElement docStruct = Mockito.mock(StructElement.class);

        Mockito.when(viewManager.getCurrentPage()).thenReturn(page);
        Mockito.when(viewManager.getTopStructElement()).thenReturn(docStruct);
        Mockito.when(page.getMediaType()).thenReturn(mimeType);
        Mockito.when(docStruct.getNumPages()).thenReturn(pageCount);
        Mockito.when(docStruct.getCollections()).thenReturn(List.of(collection));
        Mockito.when(docStruct.getDocStructType()).thenReturn(docStructType);

        condition = new ViewAttributes(viewManager, pageType);
    }

    /**
     * @verifies match when mime type and page count both satisfied
     */
    @Test
    public void matchesConfiguration_shouldMatchWhenMimeTypeAndPageCountBothSatisfied() throws ConfigurationException {
        XMLConfiguration node = loadConfig("<condition><mimeType>image/jpeg</mimeType><pageCount>[10,20]</pageCount></condition>");
        Assertions.assertTrue(condition.matchesConfiguration(node));
    }

    /**
     * @verifies match when view and collection both satisfied
     */
    @Test
    public void matchesConfiguration_shouldMatchWhenViewAndCollectionBothSatisfied() throws ConfigurationException {
        XMLConfiguration node =
                loadConfig("<condition><view>viewFullscreen</view><collection>dc.image</collection><collection>dc.pdf</collection></condition>");
        Assertions.assertTrue(condition.matchesConfiguration(node));
    }

    /**
     * @verifies match when view and doc type both satisfied
     */
    @Test
    public void matchesConfiguration_shouldMatchWhenViewAndDocTypeBothSatisfied() throws ConfigurationException {
        XMLConfiguration node = loadConfig("<condition><view>viewFullscreen</view><docType>volume</docType></condition>");
        Assertions.assertTrue(condition.matchesConfiguration(node));
    }

    /**
     * @verifies match when condition is empty
     */
    @Test
    public void matchesConfiguration_shouldMatchWhenConditionIsEmpty() throws ConfigurationException {
        XMLConfiguration node = loadConfig("<condition></condition>");
        Assertions.assertTrue(condition.matchesConfiguration(node));
    }

    XMLConfiguration loadConfig(String configString) throws ConfigurationException {
        XMLConfiguration node = new XMLConfiguration();
        new FileHandler(node).load(new StringReader(configString));
        return node;
    }

}
