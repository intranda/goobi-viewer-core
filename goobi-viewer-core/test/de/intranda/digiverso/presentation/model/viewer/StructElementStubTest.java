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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.Collections;
import java.util.Locale;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.managedbeans.ContextMocker;

public class StructElementStubTest extends AbstractSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);
    }

    /**
     * @see StructElementStub#generateContextObject(String,StructElementStub)
     * @verifies generate string element correctly
     */
    @Test
    public void generateContextObject_shouldGenerateStringElementCorrectly() throws Exception {
        StructElement element = new StructElement(1387459019070L);
        StructElementStub stub = element.createStub();
        Assert.assertEquals(element.getDocStructType(), stub.getDocStructType());
        System.out.println(stub.getDocStructType());
        Assert.assertEquals(
                "ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:book&rft.title=Universit%C3%A4t+und+Technische+Hochschule&rft.au=Klein%2C+Felix&rft.tpages=16",
                stub.generateContextObject(null, element.getTopStruct().createStub()));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return locale specific title if so requested
     */
    @Test
    public void getLabel_shouldReturnLocaleSpecificTitleIfSoRequested() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assert.assertEquals("english title", element.getLabel("en"));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return label if no locale specific title found
     */
    @Test
    public void getLabel_shouldReturnLabelIfNoLocaleSpecificTitleFound() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assert.assertEquals("label", element.getLabel("de"));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return label if locale is null
     */
    @Test
    public void getLabel_shouldReturnLabelIfLocaleIsNull() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assert.assertEquals("label", element.getLabel(null));
    }
}