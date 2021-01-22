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

import java.util.Locale;

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
import io.goobi.viewer.model.misc.TranslatedText;

/**
 * Class holding a formatted text related to a single PI 
 * which may be edited in the admin/cms-backend and displayed in a (sidebar) widget
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "cms_record_notes")
public class CMSRecordNote {

    private static final Logger logger = LoggerFactory.getLogger(CMSRecordNote.class);

    /**
     * Auto-generated database id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_record_note_id")
    private Long id;

    /**
     * PI of the record this note relates to
     */
    @Column(name = "record_pi", nullable = false)
    private String recordPi;
    
    /**
     * Title of the record this note relates to; used for searching in notes. This is mulitlangual since record titles may be multilangual too
     */
    @Column(name = "record_title", nullable = true, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText recordTitle = new TranslatedText();
    
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

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the recordPi
     */
    public String getRecordPi() {
        return recordPi;
    }

    /**
     * @param recordPi the recordPi to set
     */
    public void setRecordPi(String recordPi) {
        this.recordPi = recordPi;
    }

    /**
     * @return the recordTitle
     */
    public TranslatedText getRecordTitle() {
        return recordTitle;
    }

    /**
     * @return the noteTitle
     */
    public TranslatedText getNoteTitle() {
        return noteTitle;
    }

    /**
     * @return the noteText
     */
    public TranslatedText getNoteText() {
        return noteText;
    }
    
    
}
