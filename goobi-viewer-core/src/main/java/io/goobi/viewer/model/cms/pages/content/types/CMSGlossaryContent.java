/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.glossary.Glossary;
import io.goobi.viewer.model.glossary.GlossaryManager;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_glossary")
@DiscriminatorValue("glossary")
public class CMSGlossaryContent extends CMSContent {

    private static final Logger logger = LogManager.getLogger(CMSGlossaryContent.class);

    private static final String COMPONENT_NAME = "glossary";

    @Column(name = "glossary", length = 120)
    private String glossaryName;

    public CMSGlossaryContent() {
        super();
    }

    private CMSGlossaryContent(CMSGlossaryContent orig) {
        super(orig);
        this.glossaryName = orig.glossaryName;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public String getGlossaryName() {
        return glossaryName;
    }

    public void setGlossaryName(String glossaryName) {
        this.glossaryName = glossaryName;
    }

    /**
     * <p>
     * getGlossary.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.glossary.Glossary} object.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws org.json.JSONException if any.
     */
    public Glossary getGlossary() throws ContentNotFoundException, IOException, JSONException {
        try {
            return new GlossaryManager().getGlossary(getGlossaryName());
        } catch (ContentNotFoundException | IOException | JSONException e) {
            logger.error("Error loading glossary {}. Reason: {}", this.glossaryName, e.toString());
            return null;
        }
    }

    @Override
    public CMSContent copy() {
        return new CMSGlossaryContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return "";
    }

    @Override
    public String getData(Integer w, Integer h) {
        try {
            return new GlossaryManager().getGlossaryAsJson(getGlossaryName());
        } catch (ContentNotFoundException | IOException e) {
            logger.error("Failed to load glossary {}", getGlossaryName(), e);
            return "";
        }

    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isEmpty(glossaryName);
    }
}
