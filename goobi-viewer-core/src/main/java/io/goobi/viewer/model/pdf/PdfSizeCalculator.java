package io.goobi.viewer.model.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.presentation.contentServlet.controller.GetMetsPageCountAction;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.MetsPdfRequest;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.FileSizeCalculator;
import io.goobi.viewer.model.viewer.Dataset;

/**
 * Calculates and stores estimated pdf file sizes based on image file size
 */
public class PdfSizeCalculator {

    private static final String UNKNOWN = "unknown";

    private static final Logger logger = LogManager.getLogger(PdfSizeCalculator.class);

    private final Dataset dataset;
    private String recordSize = null;
    private final HashMap<String, String> sectionSizes = new HashMap<>();

    public PdfSizeCalculator(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getPdfSize() {
        if (StringUtils.isBlank(recordSize)) {
            this.recordSize = calculatePdfSize(null);
        }
        return this.recordSize;
    }

    public String getPdfSize(String logId) {
        if (StringUtils.isBlank(logId)) {
            return getPdfSize();
        } else {
            return this.sectionSizes.computeIfAbsent(logId, this::calculatePdfSize);
        }
    }

    private String calculatePdfSize(String logId) {
        try {
            Map<String, String> params = Map.of("imageSource",
                    dataset.getMediaFolderPath().getParent().toString(), "pdfSource", dataset.getPdfFolderPath().getParent().toString(), "altoSource",
                    dataset.getAltoFolderPath().getParent().toString());
            MetsPdfRequest request = new MetsPdfRequest(PathConverter.toURI(dataset.getMetadataFilePath()), logId, false, params);
            long size = new GetMetsPageCountAction(ContentServerCacheManager.getInstance()).getPdfInfo(request).getSize();
            return FileSizeCalculator.formatSize(size);
        } catch (URISyntaxException | ContentLibException | IOException | NullPointerException e) {
            // Log PI and full stack trace to aid debugging when folder paths are null or content server is unreachable
            logger.error("Error getting pdf file sizes for PI '{}'", dataset.getPi(), e);
            return UNKNOWN;
        }

    }

}
