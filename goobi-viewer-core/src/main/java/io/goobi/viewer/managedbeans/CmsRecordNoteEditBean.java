/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSRecordNote;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CmsRecordNoteEditBean implements Serializable, IPolyglott {

    private static final long serialVersionUID = -8850189223154382470L;

    private static final Logger logger = LoggerFactory.getLogger(CmsRecordNotesBean.class);

    private CMSRecordNote note = null;
    private Locale selectedLocale = BeanUtils.getLocale();
    private MetadataElement metadataElement = null;
    private String returnUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.adminCmsRecordNotes.getName();

    /**
     * @return the note
     */
    public CMSRecordNote getNote() {
        return note;
    }

    /**
     * @param note the note to set
     */
    public void setNote(CMSRecordNote note) {
        if (this.note == null || !this.note.equals(note)) {
            this.note = note;
            this.selectedLocale = setSelectedLocale(this.note, this.selectedLocale, BeanUtils.getDefaultLocale());
        }
    }

    public void setNoteId(Long id) throws DAOException {
        if (id != null) {
            setNote(DataManager.getInstance().getDao().getRecordNote(id));
        }
    }

    public Long getNoteId() {
        if (this.note != null) {
            return this.note.getId();
        }

        return null;
    }

    public void setRecordIdentifier(String pi) {
        setNote(new CMSRecordNote(pi));
    }

    public String getRecordIdentifier() {
        return Optional.ofNullable(this.note).map(CMSRecordNote::getRecordPi).orElse("");
    }

    /**
     * @return the returnUrl
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * @param returnUrl the returnUrl to set
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * 
     * @return true if {@link #note} is not null, because it always has a record identifier
     */
    public boolean isRecordSelected() {
        return this.note != null;
    }

    /**
     * Save the selected note to the database
     * 
     * @return false if saving was not successful
     */
    public boolean save() {
        try {
            if (this.note != null && this.note.getId() != null) {
                boolean success = DataManager.getInstance().getDao().updateRecordNote(note);
                if (success) {
                    Messages.info(null, "button__save__success", this.note.getNoteTitle().getText());
                } else {
                    Messages.error("button__save__error");
                }
                return success;
            } else if (this.note != null) {
                boolean success = DataManager.getInstance().getDao().addRecordNote(note);
                if (success) {
                    Messages.info(null, "button__save__success", this.note.getNoteTitle().getText());
                } else {
                    Messages.error("button__save__error");
                }
                return success;
            } else {
                logger.warn("Attempting to save note, but no note is selected");
                return false;
            }
        } catch (DAOException e) {
            logger.error("Error saving RecordNote", e);
            Messages.error(null, "button__save__success", e.toString());
            return false;
        }
    }

    /**
     * 
     * @return true if either no note has been created yet (record identifier not yet entered) or if the note has not been persisted yet.
     */
    public boolean isNewNote() {
        return this.note == null || this.note.getId() == null;
    }

    public MetadataElement getMetadataElement() throws PresentationException, IndexUnreachableException, DAOException {
        if (this.metadataElement == null && this.note != null) {
            this.metadataElement = loadMetadataElement(this.note.getRecordPi(), 0);
        }
        return this.metadataElement;
    }

    /**
     * @param recordPi
     * @param index Metadata view index
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private MetadataElement loadMetadataElement(String recordPi, int index) throws PresentationException, IndexUnreachableException, DAOException {
        if (StringUtils.isBlank(recordPi)) {
            return null;
        }

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(recordPi);
        if (solrDoc == null) {
            return null;
        }

        if (this.note != null) {
            this.note.setRecordTitle(createRecordTitle(solrDoc));
        }
        StructElement structElement = new StructElement(solrDoc);
        return new MetadataElement().init(structElement, index, BeanUtils.getLocale()).setSelectedRecordLanguage(getSelectedLocale().getLanguage());
    }

    /**
     * @param note2
     * @param metadataElement2
     */
    private TranslatedText createRecordTitle(SolrDocument solrDoc) {
        IMetadataValue label = TocMaker.buildTocElementLabel(solrDoc);
        return createRecordTitle(label);
    }

    /**
     * @param label
     * @return
     */
    public TranslatedText createRecordTitle(IMetadataValue label) {
        if (label instanceof MultiLanguageMetadataValue) {
            MultiLanguageMetadataValue mLabel = (MultiLanguageMetadataValue) label;
            TranslatedText labelAsText = new TranslatedText(mLabel);
            return labelAsText;
        }

        TranslatedText labelAsText = new TranslatedText(((SimpleMetadataValue) label).getValue().orElse(""));
        return labelAsText;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isComplete(java.util.Locale)
     */
    @Override
    public boolean isComplete(Locale locale) {
        return this.note != null &&
                this.note.getNoteTitle().isComplete(locale) &&
                this.note.getNoteText().isComplete(locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isValid(java.util.Locale)
     */
    @Override
    public boolean isValid(Locale locale) {
        return isComplete(locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return this.note.getNoteTitle().isEmpty(locale) && this.note.getNoteText().isEmpty(locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#getSelectedLocale()
     */
    @Override
    public Locale getSelectedLocale() {
        return this.selectedLocale;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#setSelectedLocale(java.util.Locale)
     */
    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
        setSelectedLocale(this.note, this.selectedLocale, BeanUtils.getDefaultLocale());
    }

    /**
     * Set all note texts to the given locale unless the note texts are not filled ("valid") for the defaultLocale. In this case set them to the
     * defaultLocale
     * 
     * @param note2
     * @return the given locale if texts are valid for the default locale, otherwise the default locale
     */
    private static Locale setSelectedLocale(CMSRecordNote note, Locale locale, Locale defaultLocale) {
        if (note != null && locale != null && defaultLocale != null) {
            if (note.getNoteText().isValid(defaultLocale) && note.getNoteTitle().isValid(defaultLocale)) {
                note.getNoteText().setSelectedLocale(locale);
                note.getNoteTitle().setSelectedLocale(locale);
                return locale;
            }
            note.getNoteText().setSelectedLocale(defaultLocale);
            note.getNoteTitle().setSelectedLocale(defaultLocale);
            return defaultLocale;
        }

        return locale;
    }

}
