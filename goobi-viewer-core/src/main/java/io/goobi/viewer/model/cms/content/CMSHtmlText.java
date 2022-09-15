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
package io.goobi.viewer.model.cms.content;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_htmltext")
public class CMSHtmlText extends CMSContent {

    private static final String BACKEND_COMPONENT_NAME = "htmltext";
    
    @Column(name = "text", nullable = true, columnDefinition = "LONGTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text = new TranslatedText();

    public CMSHtmlText() {
        //empty
    }
    
    public CMSHtmlText(CMSHtmlText orig) {
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
        CMSHtmlText copy = new CMSHtmlText(this);
        return copy;
    }
    
}
