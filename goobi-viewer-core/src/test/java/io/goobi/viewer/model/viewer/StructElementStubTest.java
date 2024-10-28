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
package io.goobi.viewer.model.viewer;

import java.util.Collections;
import java.util.Locale;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.ContextMocker;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.StructElementStub;

class StructElementStubTest extends AbstractSolrEnabledTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
//        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
//        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);
    }

    /**
     * @see StructElementStub#generateContextObject(String,StructElementStub)
     * @verifies generate string element correctly
     */
    @Test
    void generateContextObject_shouldGenerateStringElementCorrectly() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        StructElementStub stub = element.createStub();
        Assertions.assertEquals(element.getDocStructType(), stub.getDocStructType());
        Assertions.assertEquals(
                "ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:book&rft.title=Universit%C3%A4t+und+Technische+Hochschule&rft.au=Klein%2C+Felix&rft.tpages=16",
                stub.generateContextObject(null, element.getTopStruct().createStub()));
    }

    /**
     * @see StructElementStub#generateContextObject(String,StructElementStub)
     * @verifies return unknown format if topstruct null
     */
    @Test
    void generateContextObject_shouldReturnUnknownFormatIfTopstructNull() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        StructElementStub stub = element.createStub();
        Assertions.assertEquals(element.getDocStructType(), stub.getDocStructType());
        Assertions.assertTrue(stub.generateContextObject(null, null)
                .startsWith(
                        "ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:unknown"));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return locale specific title if so requested
     */
    @Test
    void getLabel_shouldReturnLocaleSpecificTitleIfSoRequested() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assertions.assertEquals("english title", element.getLabel("en"));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return label if no locale specific title found
     */
    @Test
    void getLabel_shouldReturnLabelIfNoLocaleSpecificTitleFound() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assertions.assertEquals("label", element.getLabel("de"));
    }

    /**
     * @see StructElementStub#getLabel(Locale)
     * @verifies return label if locale is null
     */
    @Test
    void getLabel_shouldReturnLabelIfLocaleIsNull() throws Exception {
        StructElement element = new StructElement();
        element.setLabel("label");
        element.getMetadataFields().put("MD_TITLE", Collections.singletonList("title"));
        element.getMetadataFields().put("MD_TITLE_LANG_EN", Collections.singletonList("english title"));
        Assertions.assertEquals("label", element.getLabel(null));
    }
}
