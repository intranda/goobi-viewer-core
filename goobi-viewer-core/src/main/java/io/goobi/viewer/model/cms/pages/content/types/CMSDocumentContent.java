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

import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_document")
@DiscriminatorValue("document")
public class CMSDocumentContent extends CMSMediaContent {

    public CMSDocumentContent() {
        super();
    }

    public CMSDocumentContent(CMSDocumentContent orig) {
        super(orig);
    }

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getDocumentFilter();
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getDocumentTypes();
    }

    @Override
    public CMSContent copy() {
        return new CMSDocumentContent(this);
    }
}
