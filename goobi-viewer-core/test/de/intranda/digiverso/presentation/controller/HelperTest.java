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

import java.util.Date;

import org.joda.time.MutableDateTime;
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
     * @see Helper#getLocalDate(Date,String)
     * @verifies format date correctly for the given language
     */
    @Test
    public void getLocalDate_shouldFormatDateCorrectlyForTheGivenLanguage() throws Exception {
        MutableDateTime date = new MutableDateTime();
        date.setYear(1980);
        date.setMonthOfYear(7);
        date.setDayOfMonth(10);
        date.setHourOfDay(13);
        date.setMinuteOfHour(15);
        date.setSecondOfMinute(30);
        Assert.assertEquals("10.07.1980 13:15:30", Helper.getLocalDate(date.toDate(), "de"));
        Assert.assertEquals("07/10/1980 1:15:30 PM", Helper.getLocalDate(date.toDate(), "en"));
    }

    /**
     * @see Helper#getLocalDate(Date,String)
     * @verifies use English format for unknown languages
     */
    @Test
    public void getLocalDate_shouldUseEnglishFormatForUnknownLanguages() throws Exception {
        MutableDateTime date = new MutableDateTime();
        date.setYear(1980);
        date.setMonthOfYear(7);
        date.setDayOfMonth(10);
        date.setHourOfDay(13);
        date.setMinuteOfHour(15);
        date.setSecondOfMinute(30);
        Assert.assertEquals("07/10/1980 1:15:30 PM", Helper.getLocalDate(date.toDate(), "eu"));
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
     * @see Helper#getImageUrl(String,String,String,int,int,boolean,boolean)
     * @verifies build URL correctly without repository
     */
    @Test
    public void getImageUrl_shouldBuildURLCorrectlyWithoutRepository() throws Exception {
        Assert.assertEquals(
                "contentServerWrapper_value?action=image&sourcepath=PPN123456789/00000001.tif&width=600&height=800&rotate=180&resolution=72&format=jpg&thumbnail=true&ignoreWatermark=true",
                Helper.getImageUrl("PPN123456789", "00000001.tif", null, 600, 800, 180, true, true));
    }

    /**
     * @see Helper#getImageUrl(String,String,String,int,int,boolean,boolean)
     * @verifies build URL correctly with repository
     */
    @Test
    public void getImageUrl_shouldBuildURLCorrectlyWithRepository() throws Exception {
        Assert.assertEquals(
                "contentServerWrapper_value?action=image&sourcepath=file:/resources/test/data/viewer/data/REPO/media/PPN123456789/00000001.tif&width=600&height=800&rotate=180&resolution=72&format=jpg&thumbnail=true&ignoreWatermark=true",
                Helper.getImageUrl("PPN123456789", "00000001.tif", "REPO", 600, 800, 180, true, true));
    }

    /**
     * @see Helper#getImageUrl(String,String,String,int,int,boolean,boolean)
     * @verifies throw IllegalArgumentException when pi is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getImageUrl_shouldThrowIllegalArgumentExceptionWhenPiIsNull() throws Exception {
        Helper.getImageUrl(null, "00000001.tif", null, 600, 800, 0, false, false);
    }

    /**
     * @see Helper#getImageUrl(String,String,String,int,int,boolean,boolean)
     * @verifies throw IllegalArgumentException when fileName is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getImageUrl_shouldThrowIllegalArgumentExceptionWhenFileNameIsNull() throws Exception {
        Helper.getImageUrl("PPN123456789", null, null, 600, 800, 0, false, false);
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
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getContentRestApiUrl() + "document/-/alto/PPN123/00000001.xml/",
                Helper.buildFullTextUrl(null, "alto/PPN123/00000001.xml"));
    }
}