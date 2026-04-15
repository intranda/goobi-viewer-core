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
package io.goobi.viewer.model.job.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.Dataset;

class PdfDownloadJobTest extends AbstractSolrEnabledTest {

    private static final String PI = "02008031921530";

    /**
     * @verifies folder contains pdf
     */
    @Test
    void containsPdfs_shouldFolderContainsPdf(@TempDir Path directory) throws IOException {

        Files.createFile(directory.resolve("file1.pdf"));
        Files.createFile(directory.resolve("file2.pdf"));
        Files.createFile(directory.resolve("file3.tif"));

        Assertions.assertTrue(PdfDownloadJob.containsPdfs(directory));

    }

    /**
     * @verifies folder contains no pdf
     */
    @Test
    void containsPdfs_shouldFolderContainsNoPdf(@TempDir Path directory) throws IOException {

        Files.createFile(directory.resolve("file1.jpg"));
        Files.createFile(directory.resolve("file2.txt"));
        Files.createFile(directory.resolve("file3.tif"));

        Assertions.assertFalse(PdfDownloadJob.containsPdfs(directory));

    }

    /**
     * @verifies create pdf
     */
    @Test
    void create_shouldCreatePdf(@TempDir Path targetDir)
            throws PresentationException, IndexUnreachableException, RecordNotFoundException, IOException, ContentLibException {

        Path mediaFolder = Path.of(DataManager.getInstance().getConfiguration().getViewerHome()).resolve("data/1/media").resolve(PI);
        Path metsPath = Path.of(DataManager.getInstance().getConfiguration().getViewerHome()).resolve("data/1/indexed_mets").resolve(PI + ".xml");

        //        Dataset work = Mockito.mock(Dataset.class);
        //        Mockito.when(work.getMediaFolderPath()).thenReturn(mediaFolder);
        //        Mockito.when(work.getMetadataFilePath()).thenReturn(metsPath);

        Dataset work = DataFileTools.getDataset(PI);

        PdfDownloadJob job = new PdfDownloadJob(PI, null, null, false, targetDir);
        job.create(work);
        Path pdfFile = targetDir.resolve(job.getFilename());
        Assertions.assertTrue(Files.exists(pdfFile));
        Assertions.assertTrue(Files.size(pdfFile) > 0);
    }

}
