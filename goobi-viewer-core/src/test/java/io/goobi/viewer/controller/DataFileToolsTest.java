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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.solr.SolrConstants;

class DataFileToolsTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @verifies return indexed_mets path with dataRepository when given and without when null
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldReturnIndexed_metsPathWithDataRepositoryWhenGivenAndWithoutWhenNull() {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_METS));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @see DataFileTools#getSourceFilePath(String,String,String)
     * @verifies return correct source file paths for METS MARC format
     */
    @Test
    void getSourceFilePath_shouldReturnCorrectSourceFilePathsForMetsMarcFormat() {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_METS_MARC));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_METS_MARC));
    }

    /**
     * @verifies return indexed_lido path with dataRepository when given and without when null
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldReturnIndexed_lidoPathWithDataRepositoryWhenGivenAndWithoutWhenNull() {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_LIDO));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_lido/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_LIDO));
    }

    /**
     * @verifies return indexed_ead path with dataRepository when given and without when null
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldReturnIndexed_eadPathWithDataRepositoryWhenGivenAndWithoutWhenNull() {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_ead/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_EAD));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_ead/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_EAD));
    }

    /**
     * @verifies return indexed_denkxweb path with dataRepository when given and without when null
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldReturnIndexed_denkxwebPathWithDataRepositoryWhenGivenAndWithoutWhenNull() {
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_denkxweb/PPN123.xml",
                DataFileTools.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
    }

    /**
     * @verifies throw IllegalArgumentException if fileName is null
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DataFileTools.getSourceFilePath(null, null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @verifies throw IllegalArgumentException if format is unknown
     * @see DataFileTools#getSourceFilePath(String, String, String)
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DataFileTools.getSourceFilePath("1.xml", null, "bla"));
    }

    /**
     * @verifies return correct folder if no data repository used
     */
    @Test
    void getDataFolder_shouldReturnCorrectFolderIfNoDataRepositoryUsed() {
        Path folder = DataFileTools.getDataFolder("PPN123", "media", null);
        Assertions.assertEquals(Paths.get("src/test/resources/data/viewer/media/PPN123"), folder);
    }

    /**
     * @verifies return correct folder if data repository used
     */
    @Test
    void getDataFolder_shouldReturnCorrectFolderIfDataRepositoryUsed() {
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
     * @verifies return correct path for empty data repository
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForEmptyDataRepository() {
        Assertions.assertEquals(DataManager.getInstance().getConfiguration().getViewerHome(), DataFileTools.getDataRepositoryPath(null));
    }

    /**
     * @verifies return correct path for data repository name
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForDataRepositoryName() {
        Assertions.assertEquals(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + "1/",
                DataFileTools.getDataRepositoryPath("1"));
    }

    /**
     * @see DataFileTools#getDataRepositoryPath(String)
     * @verifies return correct path for absolute data repository path
     */
    @Test
    void getDataRepositoryPath_shouldReturnCorrectPathForAbsoluteDataRepositoryPath() {
        Assertions.assertEquals("/opt/digiverso/viewer/1/", DataFileTools.getDataRepositoryPath("/opt/digiverso/viewer/1"));
    }

    /**
     * @throws IOException
     * @see DataFileTools#loadTei(String,String)
     * @verifies return TEI XML string containing TEI namespace for given document and language
     */
    @Test
    void loadTei_shouldReturnTEIXMLStringContainingTEINamespaceForGivenDocumentAndLanguage() throws IOException {
        String tei = DataFileTools.loadTei("DE_2013_Riedel_PolitikUndCo_241__248", "eng");
        Assertions.assertNotNull(tei);
        Assertions.assertTrue(tei.contains("<TEI xmlns="));
    }

    /**
     * @verifies throw record not found exception if pi not found
     * @see DataFileTools#loadMei(String, HttpServletRequest)
     */
    @Test
    void loadMei_shouldThrowRecordNotFoundExceptionIfPiNotFound() {
        Assertions.assertThrows(RecordNotFoundException.class, () -> DataFileTools.loadMei("no_such_pi", null));
    }

    /**
     * @throws IOException
     * @throws RecordNotFoundException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws AccessDeniedException
     * @verifies return null if record has no mei
     * @see DataFileTools#loadMei(String, HttpServletRequest)
     */
    @Test
    void loadMei_shouldReturnNullIfRecordHasNoMei()
            throws AccessDeniedException, DAOException, IOException, IndexUnreachableException, PresentationException, RecordNotFoundException {
        String mei = DataFileTools.loadMei(AbstractSolrEnabledTest.PI_KLEIUNIV, null);
        Assertions.assertNull(mei);
    }
}
