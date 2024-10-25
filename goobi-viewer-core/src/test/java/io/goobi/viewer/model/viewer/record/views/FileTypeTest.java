package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.ContentTypeException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.PhysicalElement;

class FileTypeTest {

    @Test
    void test_containedFileTypes() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Map<String, String> filenameMap = Map.of(
                "tiff", "01.tif",
                "fulltext", "02.txt",
                "alto", "03.xml",
                "model", "04.obj",
                "object", "05.gltf");
        Mockito.when(page.getFileNames()).thenReturn(filenameMap);
        Collection<FileType> types = FileType.containedFiletypes(page, true);

        Assertions.assertTrue(types.contains(FileType.IMAGE));
        Assertions.assertTrue(types.contains(FileType.TEXT));
        Assertions.assertTrue(types.contains(FileType.ALTO));
        Assertions.assertTrue(types.contains(FileType.MODEL));
        Assertions.assertFalse(types.contains(FileType.PDF));
    }

    @Test
    void test_getContentTypeFor() throws ContentTypeException {
        Assertions.assertEquals("image/tiff", FileType.getContentTypeFor("file.tif"));
        Assertions.assertEquals("image/jpeg", FileType.getContentTypeFor("file.JPG"));
        Assertions.assertEquals("image/png", FileType.getContentTypeFor("file.png"));
        Assertions.assertEquals("image/jp2", FileType.getContentTypeFor("file.jp2"));
        Assertions.assertEquals("video/mp4", FileType.getContentTypeFor("file.mp4"));
        Assertions.assertEquals("audio/ogg", FileType.getContentTypeFor("file.ogg"));
        Assertions.assertEquals("application/pdf", FileType.getContentTypeFor("file.PDF"));
        Assertions.assertEquals("application/epub+zip", FileType.getContentTypeFor("file.epub"));
        Assertions.assertEquals("model/obj", FileType.getContentTypeFor("file.obj"));
        Assertions.assertEquals("model/gltf", FileType.getContentTypeFor("file.gltf"));
        Assertions.assertEquals("model/glb", FileType.getContentTypeFor("file.glb"));
        Assertions.assertEquals("application/zip", FileType.getContentTypeFor("file.zip"));
        Assertions.assertEquals("text/plain", FileType.getContentTypeFor("file.txt"));
        Assertions.assertEquals("application/xml", FileType.getContentTypeFor("file.xml"));
    }

}
