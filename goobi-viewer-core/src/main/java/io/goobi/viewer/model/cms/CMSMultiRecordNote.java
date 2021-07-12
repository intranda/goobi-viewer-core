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
package io.goobi.viewer.model.cms;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class holding a formatted text related to a single PI which may be edited in the admin/cms-backend and displayed in a (sidebar) widget
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "cms_record_notes")
public class CMSMultiRecordNote implements IRecordNote {

    private static final Logger logger = LoggerFactory.getLogger(CMSMultiRecordNote.class);

    /**
     * Auto-generated database id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_record_note_id")
    private Long id;

    /**
     * PI of the record this note relates to. Should be effectively final, but can't be for DAO campatibility
     */
    @Column(name = "query", nullable = false)
    private String query;

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

    public CMSMultiRecordNote() {
    }

    /**
     * @param pi
     */
    public CMSMultiRecordNote(String query) {
        this();
        this.query = query;
    }

    /**
     * @param o
     */
    public CMSMultiRecordNote(CMSMultiRecordNote source) {
        this.id = source.id;
        this.query = source.query;
        this.noteTitle = new TranslatedText(source.noteTitle);
        this.noteText = new TranslatedText(source.noteText);
        this.displayNote = source.displayNote;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CMSMultiRecordNote other = (CMSMultiRecordNote) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getId()
     */
    @Override
    public Long getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#setId(java.lang.Long)
     */
    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getNoteTitle()
     */
    @Override
    public TranslatedText getNoteTitle() {
        return noteTitle;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#getNoteText()
     */
    @Override
    public TranslatedText getNoteText() {
        return noteText;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#isDisplayNote()
     */
    @Override
    public boolean isDisplayNote() {
        return displayNote;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.IRecordNote#setDisplayNote(boolean)
     */
    @Override
    public void setDisplayNote(boolean displayNote) {
        this.displayNote = displayNote;
    }
}
