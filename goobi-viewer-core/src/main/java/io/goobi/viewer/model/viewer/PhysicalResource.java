package io.goobi.viewer.model.viewer;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.FileSizeCalculator;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

public class PhysicalResource {

    private static final Logger logger = LogManager.getLogger(PhysicalResource.class);

    private final Path filePath;
    private final String pi;
    private final String orderLabel;
    private final Long order;
    private final Long fileSize;
    private final String mimeType;
    private Boolean downloadTicketRequired = null;

    protected PhysicalResource(Path filePath, String pi, String orderLabel, Long order, Long fileSize, String mimeType) {
        super();
        this.filePath = filePath;
        this.pi = pi;
        this.orderLabel = orderLabel;
        this.order = order;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public static PhysicalResource create(SolrDocument doc) {
        String pi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI_TOPSTRUCT);
        String filename = SolrTools.getSingleFieldStringValue(doc, SolrConstants.FILENAME);
        try {
            Path filePath = DataFileTools.getMediaFolder(pi).resolve(filename);
            String mimeType = FileTools.getMimeTypeFromFile(filePath);
            Long fileSize = SolrTools.getSingleFieldLongValue(doc, SolrConstants.MDNUM_FILESIZE);
            String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.ORDERLABEL);
            Long order = SolrTools.getSingleFieldLongValue(doc, SolrConstants.ORDER);

            return new PhysicalResource(filePath, pi, label, order, fileSize, mimeType);
        } catch (IOException | PresentationException | IndexUnreachableException e) {
            logger.error("Error creating pyhsical resource for PI={} and filename={}", pi, filename, e);
            return null;
        }
    }

    public String getFileName() {
        return this.filePath.getFileName().toString();
    }

    public String getOrderLabel() {
        return this.orderLabel;
    }

    public String getFileSizeAsString() {
        return FileSizeCalculator.formatSize(this.fileSize);
    }

    public String getMimeType() {
        return mimeType;
    }

    public BaseMimeType getBaseMimeType() {
        BaseMimeType baseMimeType = BaseMimeType.getByName(mimeType);
        if (BaseMimeType.UNKNOWN.equals(baseMimeType)) {
            return BaseMimeType.IMAGE;
        }
        return baseMimeType;
    }

    public String getUrl() throws IllegalRequestException {
        return BeanUtils.getImageDeliveryBean()
                .getMedia()
                .getMediaUrl(getBaseMimeType().getName(), BaseMimeType.getSpecificMimeType(this.mimeType), pi, getFileName());
    }

    public boolean isDownloadTicketRequired() throws IndexUnreachableException, DAOException {
        isAccessPermissionBornDigital();

        // If license requires a download ticket, check agent session for loaded ticket
        if (Boolean.TRUE.equals(downloadTicketRequired) && FacesContext.getCurrentInstance() != null
                && FacesContext.getCurrentInstance().getExternalContext() != null) {
            boolean hasTicket = AccessConditionUtils.isHasDownloadTicket(pi, BeanUtils.getSession());
            return !hasTicket;
        }

        return downloadTicketRequired;
    }

    private boolean isAccessPermissionBornDigital() throws IndexUnreachableException, DAOException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            AccessPermission access = AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, getFileName(),
                    IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
            downloadTicketRequired = access.isTicketRequired();
            return access.isGranted();
        }
        logger.trace("FacesContext not found");

        downloadTicketRequired = false; // maybe set to true?
        return false;
    }

}
