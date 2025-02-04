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
package io.goobi.viewer.model.cms.recordnotes;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Class holding a formatted text related to a single PI which may be edited in the admin/cms-backend and displayed in a (sidebar) widget
 *
 * @author florian
 *
 */
@Entity
@Table(name = "cms_record_notes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "note_type",
        discriminatorType = DiscriminatorType.STRING)
public abstract class CMSRecordNote implements Serializable {

    private static final long serialVersionUID = 7410745839101681842L;

    private static final Logger logger = LogManager.getLogger(CMSRecordNote.class);

    /**
     * Auto-generated database id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cms_record_note_id")
    private Long id;

    /**
     * Title of the note, plaintext
     */
    @Column(name = "note_title", nullable = true, columnDefinition = "TINYTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText noteTitle = new TranslatedText();

    /**
     * The actual note. May contain html text
     */
    @Column(name = "note_text", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText noteText = new TranslatedText();

    @Column(name = "display_note", nullable = false, columnDefinition = "boolean default true")
    private boolean displayNote = true;

    @Column(name = "style_class", nullable = true)
    private String styleClass = "";

    protected CMSRecordNote() {
    }

    /**
     * @param pi
     */
    protected CMSRecordNote(String pi) {
        this();
    }

    /**
     * @param source
     */
    protected CMSRecordNote(CMSRecordNote source) {
        this.id = source.id;
        this.noteTitle = new TranslatedText(source.noteTitle);
        this.noteText = new TranslatedText(source.noteText);
        this.displayNote = source.displayNote;
        this.styleClass = source.styleClass;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CMSRecordNote other = (CMSRecordNote) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getId()
     */
    public Long getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#setId(java.lang.Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getNoteTitle()
     */
    public TranslatedText getNoteTitle() {
        return noteTitle;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getNoteText()
     */
    public TranslatedText getNoteText() {
        return noteText;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#isDisplayNote()
     */
    public boolean isDisplayNote() {
        return displayNote;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#setDisplayNote(boolean)
     */
    public void setDisplayNote(boolean displayNote) {
        this.displayNote = displayNote;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public abstract boolean isSingleRecordNote();

    public abstract boolean isMultiRecordNote();

    public abstract boolean matchesFilter(String filter);

}
