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
package de.intranda.digiverso.presentation.controller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HelperTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see Helper#generateMD5(String)
     * @verifies hash string correctly
     */
    @Test
    public void generateMD5_shouldHashStringCorrectly() throws Exception {
        Assert.assertEquals("098f6bcd4621d373cade4e832627b4f6", Helper.generateMD5("test"));
    }

    /**
     * @see Helper#escapeHtmlChars(String)
     * @verifies escape all characters correctly
     */
    @Test
    public void escapeHtmlChars_shouldEscapeAllCharactersCorrectly() throws Exception {
        Assert.assertEquals("&lt;i&gt;&quot;A&amp;B&quot;&lt;/i&gt;", Helper.escapeHtmlChars("<i>\"A&B\"</i>"));
    }


    /**
     * @see Helper#removeDiacriticalMarks(String)
     * @verifies remove diacritical marks correctly
     */
    @Test
    public void removeDiacriticalMarks_shouldRemoveDiacriticalMarksCorrectly() throws Exception {
        Assert.assertEquals("aaaaoooouuuueeeeßn", Helper.removeDiacriticalMarks("äáàâöóòôüúùûëéèêßñ"));
    }

    /**
     * @see Helper#getDataFile(String,String,String)
     * @verifies construct METS file path correctly
     */
    @Test
    public void getDataFilePath_shouldConstructMETSFilePathCorrectly() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/1/indexed_mets/PPN123.xml", Helper.getDataFilePath("PPN123.xml", "1",
                SolrConstants._METS));
        Assert.assertEquals("resources/test/data/viewer/indexed_mets/PPN123.xml", Helper.getDataFilePath("PPN123.xml", null, SolrConstants._METS));
    }

    /**
     * @see Helper#getDataFilePath(String,String,String)
     * @verifies construct LIDO file path correctly
     */
    @Test
    public void getDataFilePath_shouldConstructLIDOFilePathCorrectly() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/1/indexed_lido/PPN123.xml", Helper.getDataFilePath("PPN123.xml", "1",
                SolrConstants._LIDO));
        Assert.assertEquals("resources/test/data/viewer/indexed_lido/PPN123.xml", Helper.getDataFilePath("PPN123.xml", null, SolrConstants._LIDO));
    }

    /**
     * @see Helper#getDataFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if format is unknown
     */
    @Test(expected = IllegalArgumentException.class)
    public void getDataFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() throws Exception {
        Helper.getDataFilePath("1.xml", null, "bla");
    }

    /**
     * @see Helper#getDataFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if fileName is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getDataFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() throws Exception {
        Helper.getDataFilePath(null, null, SolrConstants._METS);
    }

    /**
     * @see Helper#getTextFilePath(String,String,String,String)
     * @verifies return correct path
     */
    @Test
    public void getTextFilePath_shouldReturnCorrectPath() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/1/alto/PPN123/1.xml", Helper.getTextFilePath("PPN123", "1.xml", "1",
                SolrConstants.FILENAME_ALTO));
        Assert.assertEquals("resources/test/data/viewer/data/1/fulltext/PPN123/1.txt", Helper.getTextFilePath("PPN123", "1.txt", "1",
                SolrConstants.FILENAME_FULLTEXT));
        Assert.assertEquals("resources/test/data/viewer/data/1/tei/PPN123/1.xml", Helper.getTextFilePath("PPN123", "1.xml", "1",
                SolrConstants.FILENAME_TEI));
    }

    /**
     * @see Helper#parseMultipleIpAddresses(String)
     * @verifies filter multiple addresses correctly
     */
    @Test
    public void parseMultipleIpAddresses_shouldFilterMultipleAddressesCorrectly() throws Exception {
        Assert.assertEquals("3.3.3.3", Helper.parseMultipleIpAddresses("1.1.1.1, 2.2.2.2, 3.3.3.3"));
    }

    /**
     * @see Helper#buildFullTextUrl(String,String)
     * @verifies build url correctly
     */
    @Test
    public void buildFullTextUrl_shouldBuildUrlCorrectly() throws Exception {
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getContentRestApiUrl() + "document/-/alto/PPN123/00000001.xml/", Helper
                .buildFullTextUrl(null, "alto/PPN123/00000001.xml"));
    }
}