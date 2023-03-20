package io.goobi.viewer.model.cms;

import static org.junit.Assert.assertEquals;

import java.net.URI;

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

}
