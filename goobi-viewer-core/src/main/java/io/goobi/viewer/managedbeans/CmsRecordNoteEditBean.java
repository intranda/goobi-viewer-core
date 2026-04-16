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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Florian Alpers
 */
@Named
@ViewScoped
public class CmsRecordNoteEditBean implements Serializable, IPolyglott {

    private static final long serialVersionUID = -8850189223154382470L;

    private static final Logger logger = LogManager.getLogger(CmsRecordNoteEditBean.class);

    private CMSRecordNote note = null;
    private Locale selectedLocale = BeanUtils.getLocale();
    private MetadataElement metadataElement = null;
    private String returnUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.adminCmsRecordNotes.getName();

    private String currentNoteType = RECORD_NOTE_TYPE_SINGLE;

    private static final String RECORD_NOTE_TYPE_SINGLE = "SINGLE";
    private static final String RECORD_NOTE_TYPE_MULTI = "MULTI";

    /** Disables Faces/forwarding in tests. */
    private boolean testMode = false;

    @Inject
    private transient FacesContext facesContext;

    
    public CMSRecordNote getNote() {
        return note;
    }

    
    public void setNote(CMSRecordNote note) {
        if (note == null) {
            this.note = null;
        } else if (this.note == null || !this.note.equals(note)) {
            this.note = note.copy();
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
        setNote(new CMSSingleRecordNote(pi));
    }

    public void setRecordQuery(String query) {
        setNote(new CMSMultiRecordNote(query));
    }

    public String getRecordIdentifier() {
        return Optional.ofNullable(this.note)
                .filter(CMSSingleRecordNote.class::isInstance)
                .map(n -> ((CMSSingleRecordNote) note).getRecordPi())
                .orElse("");
    }

    public String getRecordQuery() {
        return Optional.ofNullable(this.note)
                .filter(CMSMultiRecordNote.class::isInstance)
                .map(n -> ((CMSMultiRecordNote) note).getQuery())
                .orElse("");
    }

    
    public String getReturnUrl() {
        return returnUrl;
    }

    
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
     * Saves the selected note to the database.
     * 
     * @should do nothing if node null
     * @should persist a new note to the database and make it retrievable by PI
     * @should persist changed PI on an existing note when saving
     */
    public void save() {
        try {
            if (this.note != null && this.note.getId() != null) {
                CMSRecordNote persistentNote = this.note.copy();
                boolean success = DataManager.getInstance().getDao().updateRecordNote(persistentNote);
                if (success) {
                    Messages.info(null, "button__save__success", persistentNote.getNoteTitle().getText());
                } else {
                    Messages.error("button__save__error");
                }
            } else if (this.note != null) {
                CMSRecordNote persistentNote = this.note.copy();
                boolean success = DataManager.getInstance().getDao().addRecordNote(persistentNote);
                if (success) {
                    Messages.info(null, "button__save__success", persistentNote.getNoteTitle().getText());
                } else {
                    Messages.error("button__save__error");
                }
            } else {
                logger.warn("Attempting to save note, but no note is selected");
                return;
            }
            if (!testMode) {
                String url = PrettyUrlTools.getAbsolutePageUrl("adminCmsRecordNotes");
                try {
                    facesContext.getExternalContext().redirect(url);
                } catch (IOException | NullPointerException e) {
                    logger.error("Error redirecting to database url {}: {}", url, e.toString());
                }
            }
        } catch (DAOException e) {
            logger.error("Error saving RecordNote", e);
            Messages.error(null, "button__save__success", e.toString());
        }
    }

    /**
     *
     * @return true if either no note has been created yet (record identifier not yet entered) or if the note has not been persisted yet.
     */
    public boolean isNewNote() {
        return this.note == null || this.note.getId() == null;
    }

    public MetadataElement getMetadataElement() {
        if (this.metadataElement == null && this.note != null && this.note instanceof CMSSingleRecordNote n) {
            try {
                this.metadataElement = loadMetadataElement(n.getRecordPi(), 0);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Unable to reetrive metadata elemement for {}. Reason: {}", n.getRecordTitle().getText(), e.getMessage());
                Messages.error(null, "Unable to reetrive metadata elemement for {}. Reason: {}", n.getRecordTitle().getText(), e.getMessage());
            }
        }
        return this.metadataElement;
    }

    /**
     * @param recordPi Persistent identifier of the record to load
     * @param index Metadata view index
     * @return Loaded {@link MetadataElement}
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private MetadataElement loadMetadataElement(String recordPi, int index) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(recordPi)) {
            return null;
        }

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(recordPi);
        if (solrDoc == null) {
            return null;
        }

        if (this.note != null && note instanceof CMSSingleRecordNote) {
            ((CMSSingleRecordNote) this.note).setRecordTitle(createRecordTitle(solrDoc));
        }
        StructElement structElement = new StructElement(solrDoc);
        return new MetadataElement().init(structElement, index, BeanUtils.getLocale()).setSelectedRecordLanguage(getSelectedLocale().getLanguage());
    }

    /**
     * @param solrDoc Solr document from which to extract the record title
     * @return {@link TranslatedText}
     */
    private TranslatedText createRecordTitle(SolrDocument solrDoc) {
        IMetadataValue label = TocMaker.buildTocElementLabel(solrDoc);
        return createRecordTitle(label);
    }

    /**
     * @param label Metadata value containing the record title
     * @return {@link TranslatedText}
     * @should create title label multi language
     * @should create title label single value
     */
    public TranslatedText createRecordTitle(IMetadataValue label) {
        if (label instanceof MultiLanguageMetadataValue mLabel) {
            return new TranslatedText(mLabel);
        }

        return new TranslatedText(((SimpleMetadataValue) label).getValue().orElse(""));
    }

    /**
     * Return true if text and title are both complete (not empty) for both title and text. If locale is not the default locale, text/title counts as
     * complete if they are empty as long as the corresponding field in the default language is also empty.
     *
     * @param locale the locale for which to check completeness
     * @return true if title and text of the note are complete; false otherwise;
      * @should confirm filled texts are complete
      * @should return false if not all fields filled
      * @should return true if same fields filled as in default language
      * @should return true if all fields filled
     */
    @Override
    public boolean isComplete(Locale locale) {

        if (this.note != null) {
            if (IPolyglott.getDefaultLocale().equals(locale)) {
                return this.note.getNoteTitle().isComplete(locale) && this.note.getNoteText().isComplete(locale);
            }
            return (this.note.getNoteTitle().isComplete(locale) || !this.note.getNoteTitle().isComplete(IPolyglott.getDefaultLocale()))
                    && (this.note.getNoteText().isComplete(locale) || !this.note.getNoteText().isComplete(IPolyglott.getDefaultLocale()));
        }

        return false;
    }

    @Override
    public boolean isValid(Locale locale) {
        return isComplete(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.note.getNoteTitle().isEmpty(locale) && this.note.getNoteText().isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.selectedLocale;
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
        setSelectedLocale(this.note, this.selectedLocale, BeanUtils.getDefaultLocale());
    }

    /**
     * Set all note texts to the given locale unless the note texts are not filled ("valid") for the defaultLocale. In this case set them to the
     * defaultLocale.
     *
     * @param note Note whose text locales are to be set
     * @param locale Desired locale to select
     * @param defaultLocale Fallback locale used if texts are not valid for the desired locale
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

    
    public String getCurrentNoteType() {
        return currentNoteType;
    }

    
    public void setCurrentNoteType(String currentNoteType) {
        this.currentNoteType = currentNoteType;
    }

    public boolean isMultiRecordNote() {
        return this.note instanceof CMSMultiRecordNote;
    }

    public boolean isSingleRecordNote() {
        return this.note instanceof CMSSingleRecordNote;
    }

    /**
     * For unit tests.
     * 

     */
    void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

}