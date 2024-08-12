package io.goobi.viewer.model.viewer.record.views;

import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.PhysicalElement;

class FileTypeTest {

    @Test
    void test() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        PhysicalElement page = Mockito.mock(PhysicalElement.class);
        Map<String, String> filenameMap = Map.of(
                "tiff", "01.tif",
                "fulltext", "02.txt",
                "alto", "03.xml",
                "model", "04.obj",
                "object", "05.gltf");
        Mockito.when(page.getFileNames()).thenReturn(filenameMap);
        Collection<FileType> types = FileType.containedFiletypes(page);

        Assertions.assertTrue(types.contains(FileType.IMAGE));
        Assertions.assertTrue(types.contains(FileType.TEXT));
        Assertions.assertTrue(types.contains(FileType.ALTO));
        Assertions.assertTrue(types.contains(FileType.MODEL));
        Assertions.assertFalse(types.contains(FileType.PDF));
    }

    @Test
    void test3D() {
        String f1 = "04.ogg";
        String f2 = "05.gltf";
        String f3 = "06.glb";

        System.out.println(FileType.sortByFileType(List.of(f1)).keySet().stream().findAny().orElse(null));
        System.out.println(URLConnection.guessContentTypeFromName(f1));
    }

}
