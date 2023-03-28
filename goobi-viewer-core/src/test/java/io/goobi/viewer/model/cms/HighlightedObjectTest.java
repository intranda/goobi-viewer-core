package io.goobi.viewer.model.cms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.HighlightedObjectData.ImageMode;
import io.goobi.viewer.model.cms.media.CMSMediaItem;

class HighlightedObjectTest {

    @Test
    void test_getCorrectImageURI_uploadedImage() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ThumbnailHandler thumbs = new ThumbnailHandler(URI.create("https:/viewer.goobi.io/api/v2/"), "/viewer/static/");
        
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData(), thumbs);
        object.getData().setImageMode(ImageMode.UPLOADED_IMAGE);
        
        CMSMediaItem mediaItem = new CMSMediaItem();
        mediaItem.setFileName("image.png");
        object.setMediaItem(mediaItem);
        
        URI uri = object.getImageURI(1200, 1400);
        String uriPath = uri.getPath();
        assertEquals("/viewer/api/v1/cms/media/files/image.png/full/!1200,1400/0/default.png", uriPath);
    }
    
    @Test
    void test_getCorrectImageURI_recordRepresentative() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ThumbnailHandler thumbs = new ThumbnailHandler(URI.create("https:/viewer.goobi.io/api/v2/"), "/viewer/static/");
        
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData(), thumbs);
        object.getData().setImageMode(ImageMode.RECORD_REPRESENTATIVE);
        object.getData().setRecordIdentifier("PPN12345");
        
        URI uri = object.getImageURI(1200, 1400);
        String uriPath = uri.getPath();
        assertEquals("/viewer.goobi.io/api/v2/records/PPN12345/representative/full/!1200,1400/0/default.jpg", uriPath);
    }
    
    @Test
    void test_alwayActive() {
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData());
        object.getData().setDateStart(null);
        object.getData().setDateEnd(null);
        assertFalse(object.isPast());
        assertTrue(object.isPresent());
        assertFalse(object.isFuture());
    }
    
    @Test
    void test_isPresent() {
        LocalDateTime now = LocalDate.of(2023, 4, 15).atStartOfDay();
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData());
        
        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));
        
        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(null);
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));
        
        object.getData().setDateStart(null);
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertFalse(object.isPast(now));
        assertTrue(object.isCurrent(now));
        assertFalse(object.isFuture(now));
    }
    
    @Test
    void test_isFuture() {
        LocalDateTime now = LocalDate.of(2023, 3, 15).atStartOfDay();
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData());
        
        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertFalse(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertTrue(object.isFuture(now));
        
        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(null);
        assertFalse(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertTrue(object.isFuture(now));
        
    }
    
    @Test
    void test_isPast() {
        LocalDateTime now = LocalDate.of(2023, 5, 15).atStartOfDay();
        HighlightedObject object = new HighlightedObject(new HighlightedObjectData());
        
        object.getData().setDateStart(LocalDate.of(2023, 4, 1));
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertTrue(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertFalse(object.isFuture(now));
        
        object.getData().setDateStart(null);
        object.getData().setDateEnd(LocalDate.of(2023, 5, 1));
        assertTrue(object.isPast(now));
        assertFalse(object.isCurrent(now));
        assertFalse(object.isFuture(now));
        
    }
    
}
