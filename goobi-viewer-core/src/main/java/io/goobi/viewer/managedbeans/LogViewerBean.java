package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.model.log.LogFile;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean for the log viewer admin page.
 * All four LogFile values are always returned — the JS client handles
 * the "file not available" case gracefully via empty API response.
 */
@Named
@ViewScoped
public class LogViewerBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(LogViewerBean.class);

    public List<LogFile> getAvailableLogFiles() {
        return Arrays.asList(LogFile.values());
    }
}
