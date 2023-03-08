package io.goobi.viewer.model.job.mq;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.ProcessDataResolver;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.TaskType;

public class PrerenderPdfMessageHandlerTest {

    Path imageFolder = Paths.get("src/test/resources/data/viewer/images/PPN615391702").toAbsolutePath();
    Path pdfFolder = Paths.get("src/test/resources/output/pdf").toAbsolutePath();
    Path contentServerConfigPath = Paths.get("src/test/resources/localConfig/config_contentServer_variants.xml").toAbsolutePath();
    String pi = "PPN615391702";
    
    @Test
    public void test() throws PresentationException, IndexUnreachableException, IOException {

        if(!Files.exists(pdfFolder)) {
            Files.createDirectories(pdfFolder);
        }
        List<Path> imageFiles = FileTools.listFiles(imageFolder, FileTools.imageNameFilter);
        assertEquals(17, imageFiles.size());
        
        ContentServerConfiguration contentServerConfig = ContentServerConfiguration.getInstance(contentServerConfigPath.toString());
        ProcessDataResolver processDataResolver = Mockito.mock(ProcessDataResolver.class);
        Mockito.when(processDataResolver.getDataFolders(pi, "media", "pdf", "alto")).thenReturn(Map.of("media", imageFolder, "pdf", pdfFolder));
        
        PrerenderPdfMessageHandler handler = new PrerenderPdfMessageHandler(processDataResolver, contentServerConfig);
        
        ViewerMessage ticket = new ViewerMessage(TaskType.PRERENDER_PDF.name());
        ticket.getProperties().put("pi", pi);
        ticket.getProperties().put("config", "small");
        ticket.getProperties().put("force", "true");
        assertEquals(MessageStatus.FINISH, handler.call(ticket));

        List<Path> pdfFiles = FileTools.listFiles(pdfFolder, FileTools.pdfNameFilter);  
        
        assertEquals(imageFiles.size(), pdfFiles.size());
        
    }
    
    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(pdfFolder.toFile());
    }

}
