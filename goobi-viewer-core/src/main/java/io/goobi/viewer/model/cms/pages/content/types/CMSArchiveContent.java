package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("archive")
public class CMSArchiveContent extends CMSContent {

    private static final String COMPONENT_NAME = "archive";

    @Column(name = "archive_id")
    private String archiveId = "";
    @Column(name = "open_in_popup")
    private boolean openInPopup = false;

    public CMSArchiveContent() {
        super();
    }

    public CMSArchiveContent(CMSArchiveContent orig) {
        super(orig);
        this.archiveId = orig.archiveId;
        this.openInPopup = orig.openInPopup;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSArchiveContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(this.archiveId);
    }

    @Override
    public String getData(Integer width, Integer height) {
        return this.archiveId;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public boolean isOpenInPopup() {
        return openInPopup;
    }

    public void setOpenInPopup(boolean openInPopup) {
        this.openInPopup = openInPopup;
    }

}
