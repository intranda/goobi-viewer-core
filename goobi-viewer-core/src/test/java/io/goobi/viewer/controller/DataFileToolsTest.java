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
package io.goobi.viewer.controller;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.solr.SolrConstants;

class DataFileToolsTest extends AbstractTest {

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct METS file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructMETSFilePathCorrectly() throws Exception {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_METS));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @see DataFileTools#getDataFolders(String,String[])
     * @verifies return all requested data folders
     */
    @Test
    void getDataFolders_shouldReturnAllRequestedDataFolders() throws Exception {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_METS_MARC));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_METS_MARC));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct LIDO file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructLIDOFilePathCorrectly() throws Exception {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_LIDO));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_LIDO));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies construct DenkXweb file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructDenkXwebFilePathCorrectly() throws Exception {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if fileName is null
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DataFileTools.getSourceFilePath(null, null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies throw IllegalArgumentException if format is unknown
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DataFileTools.getSourceFilePath("1.xml", null, "bla"));
    }

    /**
     * @see DataFileTools#getDataFolder(String,String,String)
     * @verifies return correct folder if no data repository used
     */
    @Test
    void getDataFolder_shouldReturnCorrectFolderIfNoDataRepositoryUsed() throws Exception {
        Path folder = DataFileTools.getDataFolder("PPN123", "media", null);
        Assertions.assertEquals(Paths.get("src/test/resources/data/viewer/media/PPN123"), folder);
    }

    /**
     * @see DataFileTools#getDataFolder(String,String,String)
     * @verifies return correct folder if data repository used
     */
    @Test
    void getDataFolder_shouldReturnCorrectFolderIfDataRepositoryUsed() throws Exception {
        {
            // Just the folder name
            Path folder = DataFileTools.getDataFolder("PPN123", "media", "1");
            Assertions.assertEquals(Paths.get("src/test/resources/data/viewer/data/1/media/PPN123"), folder);
        }
        {
            // Absolute path
            Path folder = DataFileTools.getDataFolder("PPN123", "media", "/opt/digiverso/data/1/");
            Assertions.assertEquals(Paths.get("/opt/digiverso/data/1/media/PPN123"), folder);
        }
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for empty data repository
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForEmptyDataRepository() throws Exception {
        Assertions.assertEquals(DataManager.getInstance().getConfiguration().getViewerHome(), DataFileTools.getDataRepositoryPath(null));
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for data repository name
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForDataRepositoryName() throws Exception {
        Assertions.assertEquals(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + "1/",
                DataFileTools.getDataRepositoryPath("1"));
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for absolute data repository path
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForAbsoluteDataRepositoryPath() throws Exception {
        Assertions.assertEquals("/opt/digiverso/viewer/1/", DataFileTools.getDataRepositoryPath("/opt/digiverso/viewer/1"));
    }

    /**
     * @see DataFileTools#sanitizeFileName(String)
     * @verifies remove everything but the file name from given path
     */
    @Test
    void sanitizeFileName_shouldRemoveEverythingButTheFileNameFromGivenPath() throws Exception {
        Assertions.assertEquals("foo.bar", DataFileTools.sanitizeFileName("/opt/digiverso/foo.bar"));
        Assertions.assertEquals("foo.bar", DataFileTools.sanitizeFileName("../../foo.bar"));
        Assertions.assertEquals("foo.bar", DataFileTools.sanitizeFileName("/foo.bar"));
    }
}
