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
package io.goobi.viewer.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;

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
     * @see Helper#getDataFile(String,String,String)
     * @verifies construct METS file path correctly
     */
    @Test
    public void getSourceFilePath_shouldConstructMETSFilePathCorrectly() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/1/indexed_mets/PPN123.xml",
                Helper.getSourceFilePath("PPN123.xml", "1", SolrConstants._METS));
        Assert.assertEquals("resources/test/data/viewer/indexed_mets/PPN123.xml", Helper.getSourceFilePath("PPN123.xml", null, SolrConstants._METS));
    }

    /**
     * @see Helper#getSourceFilePath(String,String,String)
     * @verifies construct LIDO file path correctly
     */
    @Test
    public void getSourceFilePath_shouldConstructLIDOFilePathCorrectly() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/1/indexed_lido/PPN123.xml",
                Helper.getSourceFilePath("PPN123.xml", "1", SolrConstants._LIDO));
        Assert.assertEquals("resources/test/data/viewer/indexed_lido/PPN123.xml", Helper.getSourceFilePath("PPN123.xml", null, SolrConstants._LIDO));
    }

    /**
     * @see Helper#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if format is unknown
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() throws Exception {
        Helper.getSourceFilePath("1.xml", null, "bla");
    }

    /**
     * @see Helper#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if fileName is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() throws Exception {
        Helper.getSourceFilePath(null, null, SolrConstants._METS);
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
                Helper.buildFullTextUrl("alto/PPN123/00000001.xml"));
    }

    /**
     * @see Helper#deleteRecord(String,boolean)
     * @verifies create delete file correctly
     */
    @Test
    public void deleteRecord_shouldCreateDeleteFileCorrectly() throws Exception {
        Path hotfolder = Paths.get("resources/build", DataManager.getInstance().getConfiguration().getHotfolder());
        if (!Files.isDirectory(hotfolder)) {
            Files.createDirectory(hotfolder);
        }
        Path file = Paths.get(hotfolder.toAbsolutePath().toString(), "PPN123.delete");
        try {
            Assert.assertTrue(Helper.deleteRecord("PPN123", true, hotfolder));
            Assert.assertTrue(Files.isRegularFile(file));
        } finally {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
            }
            if (!Files.isDirectory(hotfolder)) {
                Files.delete(hotfolder);
            }
        }
    }

    /**
     * @see Helper#deleteRecord(String,boolean)
     * @verifies create purge file correctly
     */
    @Test
    public void deleteRecord_shouldCreatePurgeFileCorrectly() throws Exception {
        Path hotfolder = Paths.get("resources/build", DataManager.getInstance().getConfiguration().getHotfolder());
        if (!Files.isDirectory(hotfolder)) {
            Files.createDirectory(hotfolder);
        }
        Path file = Paths.get(hotfolder.toAbsolutePath().toString(), "PPN123.purge");
        try {
            Assert.assertTrue(Helper.deleteRecord("PPN123", false, hotfolder));
            Assert.assertTrue(Files.isRegularFile(file));
        } finally {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
            }
            if (!Files.isDirectory(hotfolder)) {
                Files.delete(hotfolder);
            }
        }
    }

    /**
     * @see Helper#getDataFolder(String,String,String)
     * @verifies return correct folder if no data repository used
     */
    @Test
    public void getDataFolder_shouldReturnCorrectFolderIfNoDataRepositoryUsed() throws Exception {
        Path folder = Helper.getDataFolder("PPN123", "media", null);
        Assert.assertEquals(Paths.get("resources/test/data/viewer/media/PPN123"), folder);
    }

    /**
     * @see Helper#getDataFolder(String,String,String)
     * @verifies return correct folder if data repository used
     */
    @Test
    public void getDataFolder_shouldReturnCorrectFolderIfDataRepositoryUsed() throws Exception {
        {
            // Just the folder name
            Path folder = Helper.getDataFolder("PPN123", "media", "1");
            Assert.assertEquals(Paths.get("resources/test/data/viewer/data/1/media/PPN123"), folder);
        }
        {
            // Absolute path
            Path folder = Helper.getDataFolder("PPN123", "media", "/opt/digiverso/data/1/");
            Assert.assertEquals(Paths.get("/opt/digiverso/data/1/media/PPN123"), folder);
        }
    }

    /**
     * @see Helper#getRepositoryPath(String)
     * @verifies return correct path for empty data repository
     */
    @Test
    public void getRepositoryPath_shouldReturnCorrectPathForEmptyDataRepository() throws Exception {
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getViewerHome(), Helper.getRepositoryPath(null));
    }

    /**
     * @see Helper#getRepositoryPath(String)
     * @verifies return correct path for data repository name
     */
    @Test
    public void getRepositoryPath_shouldReturnCorrectPathForDataRepositoryName() throws Exception {
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + "1/", Helper.getRepositoryPath("1"));
    }

    /**
     * @see Helper#getDataFolders(String,String[])
     * @verifies return all requested data folders
     */
    @Test
    public void getDataFolders_shouldReturnAllRequestedDataFolders() throws Exception {
        //TODO auto-generated
        Assert.fail("Not yet implemented");
    }
}