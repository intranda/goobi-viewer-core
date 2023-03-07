package io.goobi.viewer.model.iiif.presentation.v3.builder;

import de.intranda.api.annotation.wa.collection.AnnotationPage;

/**
 * An {@link AnnotationPage} without '@context' attribute, to use for annotation pages embedded within other documents
 * @author florian
 *
 */
public class InternalAnnotationPage extends AnnotationPage {

    @Override
    public String getContext() {
        return null;
    }
    
    public InternalAnnotationPage(AnnotationPage orig) {
        super(orig.getId());
        this.setItems(orig.getItems());
        this.setPartOf(orig.getPartOf());
        this.setNext(orig.getNext());
        this.setPrev(orig.getPrev());
        this.setStartIndex(orig.getStartIndex());
    }
}
