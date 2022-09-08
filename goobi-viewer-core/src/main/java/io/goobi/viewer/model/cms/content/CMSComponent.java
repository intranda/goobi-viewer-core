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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.goobi.viewer.controller.AlphabetIterator;

public class CMSComponent {

    private ContentItemPublicationState publicationState = ContentItemPublicationState.ADMINISTRATOR;
    
    private int order = 0;
    
    private List<String> cssClasses = new ArrayList<>();
    
    
    
    private List<CMSContentItem> contentItems;
    
    public CMSComponent(String componentName, CMSContent... contents) {
        Iterator<String> idIterator = new AlphabetIterator();
        for (CMSContent content : contents) {
            CMSContentItem item = new CMSContentItem(idIterator.next(), content);
        }
    }
}
