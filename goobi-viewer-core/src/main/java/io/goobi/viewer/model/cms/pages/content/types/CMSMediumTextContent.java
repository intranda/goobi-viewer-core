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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.TranslatableCMSContent;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * CMS content for longer texts, holding up to 16,777,215 characters (all translations combined)
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "cms_content_text_medium")
@DiscriminatorValue("mediumtext")
public class CMSMediumTextContent extends CMSContent implements TranslatableCMSContent {

    private static final String BACKEND_COMPONENT_NAME = "htmltext";

    @Column(name = "mediumtext_text", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text = new TranslatedText();

    public CMSMediumTextContent() {
        super();
    }

    public CMSMediumTextContent(CMSMediumTextContent orig) {
        super(orig);
        this.text = new TranslatedText(orig.text);
    }

    @Override
    public String getBackendComponentName() {
        return BACKEND_COMPONENT_NAME;
    }

    public TranslatedText getText() {
        return text;
    }

    public void setText(TranslatedText text) {
        this.text = text;
    }

    @Override
    public CMSContent copy() {
        return new CMSMediumTextContent(this);
    }

    @Override
    public boolean isComplete(Locale locale) {
        return this.text.isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return this.text.isValid(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.text.isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.text.getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.text.setSelectedLocale(locale);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        if (StringUtils.isEmpty(outputFolderPath)) {
            throw new IllegalArgumentException("hotfolderPath may not be null or emptys");
        }
        if (StringUtils.isEmpty(namingScheme)) {
            throw new IllegalArgumentException("namingScheme may not be null or empty");
        }
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<File> ret = new ArrayList<>();
        Path cmsDataDir = Paths.get(outputFolderPath, namingScheme + IndexerTools.SUFFIX_CMS);
        if (!Files.isDirectory(cmsDataDir)) {
            Files.createDirectory(cmsDataDir);
        }

        List<File> filesWritten = new ArrayList<>();
        for (String language : text.getLanguages()) {
            String string = text.getValue(language).orElse(null);
            if (StringUtils.isNotBlank(string)) {
                File file = new File(cmsDataDir.toFile(), this.getId() + "-" + language + ".xml");
                FileUtils.writeStringToFile(file, string, StringTools.DEFAULT_ENCODING);
                filesWritten.add(file);
            }
        }

        return ret;
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return null;
    }

    @Override
    public String getData(Integer w, Integer h) {
        return getText().getTextOrDefault();
    }

    @Override
    public boolean isEmpty() {
        return Optional.ofNullable(text).map(TranslatedText::isEmpty).orElse(true);
    }

}
