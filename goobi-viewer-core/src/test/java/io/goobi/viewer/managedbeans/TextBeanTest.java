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
package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.viewer.StructElement;

public class TextBeanTest extends AbstractSolrEnabledTest {

    private TextBean bean;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        bean = new TextBean();
    }

    /**
     * @see TextBean#getAbstract(StructElement,String,String)
     * @verifies return abstract correctly
     */
    @Test
    public void getAbstract_shouldReturnAbstractCorrectly() throws Exception {
        StructElement se = new StructElement();
        se.setPi("DE_2013_Riedel_PolitikUndCo_241__248");
        se.getMetadataFields()
                .put("FILENAME_TEI_LANG_EN", Collections
                        .singletonList("src/test/data/viewer/DE_2013_Riedel_PolitikUndCo_241__248/DE_2013_Riedel_PolitikUndCo_241__248_eng.xml"));

        String result = bean.getAbstract(se, "ProfileDescAbstractLong", "en");
        assertTrue(StringUtils.isNotEmpty(result));
    }

    /**
     * @see TextBean#getAbstract(StructElement,String,String)
     * @verifies throw IllegalArgumentException if language null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getAbstract_shouldThrowIllegalArgumentExceptionIfLanguageNull() throws Exception {
        bean.getAbstract(null, "ProfileDescAbstractLong", null);
    }

    /**
     * @see TextBean#getAbstract(StructElement,String,String)
     * @verifies return null if topDocument null
     */
    @Test
    public void getAbstract_shouldReturnNullIfTopDocumentNull() throws Exception {
        assertNull(bean.getAbstract(null, "ProfileDescAbstractLong", "en"));
    }

    /**
     * @see TextBean#getAbstract(StructElement,String,String)
     * @verifies return null if topDocument has no tei for language
     */
    @Test
    public void getAbstract_shouldReturnNullIfTopDocumentHasNoTeiForLanguage() throws Exception {
        StructElement se = new StructElement();
        se.setPi("DE_2013_Riedel_PolitikUndCo_241__248");
        se.getMetadataFields()
                .put("FILENAME_TEI_LANG_EN", Collections
                        .singletonList("src/test/data/viewer/DE_2013_Riedel_PolitikUndCo_241__248/DE_2013_Riedel_PolitikUndCo_241__248_eng.xml"));

        assertNull(bean.getAbstract(se, "ProfileDescAbstractLong", "jp"));
    }

    /**
     * @see TextBean#getTeiText(StructElement,String)
     * @verifies return null if topDocument null
     */
    @Test
    public void getTeiText_shouldReturnNullIfTopDocumentNull() throws Exception {
        assertNull(bean.getTeiText(null, "en"));
    }

    /**
     * @see TextBean#removeEmptyParagraphs(String)
     * @verifies remove empty paragraph tags correctly
     */
    @Test
    public void removeEmptyParagraphs_shouldRemoveEmptyParagraphTagsCorrectly() throws Exception {
        String tei = "<tei><p>Lorem ipsum</p><p></p><p/><p /><br/><p>foo bar</p></tei>";
        Assert.assertEquals("<tei><p>Lorem ipsum</p><p>foo bar</p></tei>", TextBean.removeEmptyParagraphs(tei));
    }
}