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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Regression tests for ProcessDataResolver.getSourceFilePath - this was previously the only place where
 * SOURCEDOCFORMAT_EAD was not handled, causing IllegalArgumentException during widget_downloads.xhtml rendering
 * for EAD records (see ActiveDocumentBean.isAccessPermissionEpub -> ocrFolderExists -> getRecordDataset).
 */
class ProcessDataResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies construct METS file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructMetsFilePathCorrectly() {
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_mets/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_METS));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_mets/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies construct LIDO file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructLidoFilePathCorrectly() {
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_lido/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_LIDO));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_lido/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_LIDO));
    }

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies construct EAD file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructEadFilePathCorrectly() {
        // Regression test: EAD format must not throw IllegalArgumentException and must resolve to indexed_ead.
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_ead/PPN101.xml",
                resolver.getSourceFilePath("PPN101.xml", "1", SolrConstants.SOURCEDOCFORMAT_EAD));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_ead/PPN101.xml",
                resolver.getSourceFilePath("PPN101.xml", null, SolrConstants.SOURCEDOCFORMAT_EAD));
    }

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies construct DenkXweb file path correctly
     */
    @Test
    void getSourceFilePath_shouldConstructDenkXwebFilePathCorrectly() {
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertEquals("src/test/resources/data/viewer/data/1/indexed_denkxweb/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", "1", SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
        Assertions.assertEquals("src/test/resources/data/viewer/indexed_denkxweb/PPN123.xml",
                resolver.getSourceFilePath("PPN123.xml", null, SolrConstants.SOURCEDOCFORMAT_DENKXWEB));
    }

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies throw IllegalArgumentException if fileName is null
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFileNameIsNull() {
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> resolver.getSourceFilePath(null, null, SolrConstants.SOURCEDOCFORMAT_METS));
    }

    /**
     * @see ProcessDataResolver#getSourceFilePath(String, String, String)
     * @verifies throw IllegalArgumentException if format is unknown
     */
    @Test
    void getSourceFilePath_shouldThrowIllegalArgumentExceptionIfFormatIsUnknown() {
        ProcessDataResolver resolver = new ProcessDataResolver();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> resolver.getSourceFilePath("1.xml", null, "bla"));
    }
}
