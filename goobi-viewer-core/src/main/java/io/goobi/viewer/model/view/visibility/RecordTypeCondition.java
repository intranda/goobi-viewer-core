package io.goobi.viewer.model.view.visibility;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.resourcebuilders.TocResourceBuilder;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.viewer.ViewManager;

public class RecordTypeCondition {

    private static final Logger logger = LogManager.getLogger(TocResourceBuilder.class);

    private final String docTypeMatch;
    private final Integer numPagesMin;
    private final Integer numPagesMax;
    private final String mimeTypeMatch;

    public RecordTypeCondition(String docTypeMatch, Integer numPagesMin, Integer numPagesMax, String mimeTypeMatch) {
        super();
        this.docTypeMatch = docTypeMatch;
        this.numPagesMin = numPagesMin;
        this.numPagesMax = numPagesMax;
        this.mimeTypeMatch = mimeTypeMatch;
    }

    public String getDocTypeMatch() {
        return docTypeMatch;
    }

    public String getMimeTypeMatch() {
        return mimeTypeMatch;
    }

    public Integer getNumPagesMin() {
        return numPagesMin;
    }

    public Integer getNumPagesMax() {
        return numPagesMax;
    }

    public boolean matches(ViewManager viewManager) {
        if (StringUtils.isNotBlank(docTypeMatch) && !viewManager.getTopStructElement().getDocStructType().matches(docTypeMatch)) {
            return false;
        }
        try {
            if (this.numPagesMin != null && this.numPagesMin > viewManager.getPageLoader().getNumPages()) {
                return false;
            }
            if (this.numPagesMax != null && this.numPagesMax < viewManager.getPageLoader().getNumPages()) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.warn("Error matching record type to viewManager: {}", e.toString());
            return false;
        }
        if (StringUtils.isNotBlank(mimeTypeMatch) && !viewManager.getMimeType().matches(mimeTypeMatch)) {
            return false;
        }
        return true;
    }

}
