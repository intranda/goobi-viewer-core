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
package io.goobi.viewer.model.search;

import io.goobi.viewer.model.search.FacetItem.FacetType;

/**
 * @author florian
 *
 */
public interface IFacetItem {

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getQueryEscapedLink()
     */
    String getQueryEscapedLink();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getEscapedLink()
     */
    String getEscapedLink();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getUrlEscapedLink()
     */
    String getUrlEscapedLink();
    
    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getType()
     */
    FacetType getType();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getField()
     */
    String getField();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setField(java.lang.String)
     */
    void setField(String field);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getFullValue()
     */
    String getFullValue();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getValue()
     */
    String getValue();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setValue(java.lang.String)
     */
    void setValue(String value);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getValue2()
     */
    String getValue2();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setValue2(java.lang.String)
     */
    void setValue2(String value2);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getLink()
     */
    String getLink();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setLink(java.lang.String)
     */
    void setLink(String link);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getLabel()
     */
    String getLabel();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setLabel(java.lang.String)
     */
    IFacetItem setLabel(String label);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getTranslatedLabel()
     */
    String getTranslatedLabel();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setTranslatedLabel(java.lang.String)
     */
    void setTranslatedLabel(String translatedLabel);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getCount()
     */
    long getCount();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setCount(long)
     */
    IFacetItem setCount(long count);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getCount()
     */
    boolean isGroup();

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setCount(boolean)
     */
    IFacetItem setGroup(boolean group);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#isHierarchial()
     */
    boolean isHierarchial();
    
    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#isBoolean()
     */
    boolean isBooleanType();
}
