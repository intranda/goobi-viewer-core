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
package de.intranda.digiverso.presentation.servlets.oembed;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class RichOEmbedResponse extends OEmbedResponse {

    private String html;

    public RichOEmbedResponse(StructElement se) {
        this.type = "rich";
        generateHtml(se);
    }

    /**
     * 
     * @param se
     */
    private void generateHtml(StructElement se) {
        if (se == null) {
            throw new IllegalArgumentException("se may not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div>");
        sb.append("<img src=\"\">");
        sb.append("<h3>").append(se.getLabel()).append("</h3>");

        se.getPi();

        sb.append("</div>");
        html = sb.toString();
    }

    /**
     * @return the html
     */
    public String getHtml() {
        return html;
    }

    /**
     * @param html the html to set
     */
    public void setHtml(String html) {
        this.html = html;
    }
}
