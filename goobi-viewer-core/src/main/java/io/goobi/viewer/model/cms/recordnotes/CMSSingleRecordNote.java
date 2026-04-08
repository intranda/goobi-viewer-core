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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue.ValuePair;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Class holding a formatted text related to a single PI which may be edited in the admin/cms-backend and displayed in a (sidebar) widget.
 *
 * @author Florian Alpers
 */
@Entity
@DiscriminatorValue("SINGLE")
public class CMSSingleRecordNote extends CMSRecordNote {

    private static final long serialVersionUID = -4019734190447608999L;

    private static final Logger logger = LogManager.getLogger(CMSSingleRecordNote.class);

    /**
     * PI of the record this note relates to. Should be effectively final, but can't be for DAO compatibility
     */
    @Column(name = "record_pi")
    private String recordPi;

    /**
     * Title of the record this note relates to; used for searching in notes. This is mulitlangual since record titles may be multilangual too
     */
    @Column(name = "record_title", columnDefinition = "varchar(4096)", nullable = true)
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText recordTitle = new TranslatedText();

    public CMSSingleRecordNote() {
    }

    /**
     * @param pi persistent identifier of the record
     */
    public CMSSingleRecordNote(String pi) {
        super();
        this.recordPi = pi;
    }

    /**
     * @param source the note to copy from
     */
    public CMSSingleRecordNote(CMSRecordNote source) {
        super(source);
        if (source instanceof CMSSingleRecordNote note) {
            this.recordPi = note.recordPi;
            this.recordTitle = new TranslatedText(((CMSSingleRecordNote) source).recordTitle);
        }
    }

    
    public String getRecordPi() {
        return recordPi;
    }

    public void setRecordPi(String recordPi) {
        this.recordPi = recordPi;
    }

    
    public TranslatedText getRecordTitle() {
        return recordTitle;
    }

    public void setRecordTitle(TranslatedText recordTitle) {
        this.recordTitle = recordTitle;
    }

    @Override
    public boolean isSingleRecordNote() {
        return true;
    }

    @Override
    public boolean isMultiRecordNote() {
        return false;
    }

    @Override
    public boolean matchesFilter(String filter) {
        if (StringUtils.isNotBlank(filter)) {
            return getNoteTitle().getValues()
                    .stream()
                    .map(ValuePair::getValue)
                    .anyMatch(title -> title.toLowerCase().contains(filter.toLowerCase()))
                    || getRecordPi().toLowerCase().contains(filter.toLowerCase())
                    || getRecordTitle().getValues()
                            .stream()
                            .map(ValuePair::getValue)
                            .anyMatch(title -> title.toLowerCase().contains(filter.toLowerCase()));
        }
        return true;
    }

    @Override
    public CMSRecordNote copy() {
        return new CMSSingleRecordNote(this);
    }

}
