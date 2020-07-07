package io.goobi.viewer.controller;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class DataFileToolsTest extends AbstractTest {

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct METS file path correctly
     */
    @Test
    public void getSourceFilePath_shouldConstructMETSFilePathCorrectly() throws Exception {
        Assert.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants._METS));
        Assert.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants._METS));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct LIDO file path correctly
     */
    @Test
    public void getSourceFilePath_shouldConstructLIDOFilePathCorrectly() throws Exception {
        Assert.assertEquals("src/test/resources/data/viewer/data/1/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants._LIDO));
        Assert.assertEquals("src/test/resources/data/viewer/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants._LIDO));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct DenkXweb file path correctly
     */
    @Test
    public void getSourceFilePath_shouldConstructDenkXwebFilePathCorrectly() throws Exception {
        Assert.assertEquals("src/test/resources/data/viewer/data/1/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants._DENKXWEB));
        Assert.assertEquals("src/test/resources/data/viewer/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants._DENKXWEB));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if fileName is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() throws Exception {
        DataFileTools.getSourceFilePath(null, null, SolrConstants._METS);
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if format is unknown
     */
    @Test(expected = IllegalArgumentException.class)
    public void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() throws Exception {
        DataFileTools.getSourceFilePath("1.xml", null, "bla");
    }

    /**
     * @see DataFileTools#getDataFolder(String,String,String)
     * @verifies return correct folder if no data repository used
     */
    @Test
    public void getDataFolder_shouldReturnCorrectFolderIfNoDataRepositoryUsed() throws Exception {
        Path folder = DataFileTools.getDataFolder("PPN123", "media", null);
        Assert.assertEquals(Paths.get("src/test/resources/data/viewer/media/PPN123"), folder);
    }

    /**
     * @see DataFileTools#getDataFolder(String,String,String)
     * @verifies return correct folder if data repository used
     */
    @Test
    public void getDataFolder_shouldReturnCorrectFolderIfDataRepositoryUsed() throws Exception {
        {
            // Just the folder name
            Path folder = DataFileTools.getDataFolder("PPN123", "media", "1");
            Assert.assertEquals(Paths.get("src/test/resources/data/viewer/data/1/media/PPN123"), folder);
        }
        {
            // Absolute path
            Path folder = DataFileTools.getDataFolder("PPN123", "media", "/opt/digiverso/data/1/");
            Assert.assertEquals(Paths.get("/opt/digiverso/data/1/media/PPN123"), folder);
        }
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for empty data repository
     */
    @Test
    public void getDataRepositoryPath_shouldReturnCorrectPathForEmptyDataRepository() throws Exception {
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getViewerHome(), DataFileTools.getDataRepositoryPath(null));
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for data repository name
     */
    @Test
    public void getDataRepositoryPath_shouldReturnCorrectPathForDataRepositoryName() throws Exception {
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + "1/", DataFileTools.getDataRepositoryPath("1"));
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for absolute data repository path
     */
    @Test
    public void getDataRepositoryPath_shouldReturnCorrectPathForAbsoluteDataRepositoryPath() throws Exception {
        Assert.assertEquals("/opt/digiverso/viewer/1/", DataFileTools.getDataRepositoryPath("/opt/digiverso/viewer/1"));
    }
}