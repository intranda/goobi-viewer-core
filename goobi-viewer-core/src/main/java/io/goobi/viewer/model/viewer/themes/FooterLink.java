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
package io.goobi.viewer.model.viewer.themes;

/**
 * @author florian
 *
 */
public class FooterLink {

    public static enum Service {
        contact("admin__themes__footer_contact__label", "admin__themes__footer_contact__help"),
        legal("admin__themes__footer_legal__label", "admin__themes__footer_legal__help"),
        privacy("admin__themes__footer_privacy__label", " admin__themes__footer_privacy__help");
        
        private String labelKey, helpKey;
        
        private Service(String labelKey, String helpKey) {
            this.labelKey = labelKey;
            this.helpKey = helpKey;
        }
        public String getLabelKey() {
            return labelKey;
        }
        public String getHelpKey() {
            return helpKey;
        }
    }
    
    private final Service service;
    private String linkUrl;
    
    public FooterLink(Service service) {
        this.service = service;
    }
    public Service getService() {
        return service;
    }
    public String getLinkUrl() {
        return linkUrl;
    }
    public void setLinkUrl(String url) {
        this.linkUrl = url;
    }
    
}
