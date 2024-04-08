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
package io.goobi.viewer.model.job.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.ProcessDataResolver;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.TaskType;

class PrerenderPdfMessageHandlerTest {

    Path imageFolder = Paths.get("src/test/resources/data/viewer/images/PPN615391702").toAbsolutePath();
    Path pdfFolder = Paths.get("src/test/resources/output/pdf").toAbsolutePath();
    Path contentServerConfigPath = Paths.get("src/test/resources/localConfig/config_contentServer_variants.xml").toAbsolutePath();
    String pi = "PPN615391702";

    @Test
    void test() throws PresentationException, IndexUnreachableException, IOException {

        if (!Files.exists(pdfFolder)) {
            Files.createDirectories(pdfFolder);
        }
        List<Path> imageFiles = FileTools.listFiles(imageFolder, FileTools.IMAGE_NAME_FILTER);
        assertEquals(17, imageFiles.size());

        ContentServerConfiguration contentServerConfig = ContentServerConfiguration.getInstance(contentServerConfigPath.toString());
        ProcessDataResolver processDataResolver = Mockito.mock(ProcessDataResolver.class);
        Mockito.when(processDataResolver.getDataFolders(pi, "media", "pdf", "alto")).thenReturn(Map.of("media", imageFolder, "pdf", pdfFolder));

        PrerenderPdfMessageHandler handler = new PrerenderPdfMessageHandler(processDataResolver, contentServerConfig);

        ViewerMessage ticket = new ViewerMessage(TaskType.PRERENDER_PDF.name());
        ticket.getProperties().put("pi", pi);
        ticket.getProperties().put("variant", "small");
        ticket.getProperties().put("force", "true");
        assertEquals(MessageStatus.FINISH, handler.call(ticket, null));

        List<Path> pdfFiles = FileTools.listFiles(pdfFolder, FileTools.PDF_NAME_FILTER);

        assertEquals(imageFiles.size(), pdfFiles.size());

    }

    @AfterEach
    void cleanup() throws IOException {
        FileUtils.deleteDirectory(pdfFolder.toFile());
    }

}
