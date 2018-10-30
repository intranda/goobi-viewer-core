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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

public class HierarchicalBrowseDcElement extends BrowseDcElement {

    private static final long serialVersionUID = -4369053276327316515L;

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalBrowseDcElement.class);

    private List<HierarchicalBrowseDcElement> children = new ArrayList<>();
    private HierarchicalBrowseDcElement parent = null;
    private boolean opensInNewWindow;

    /**
     * @param name
     * @param number
     * @param sortField
     * @throws PresentationException
     */
    public HierarchicalBrowseDcElement(String name, long number, String field, String sortField) throws PresentationException {
        super(name, number, field, sortField);
    }
    
    public HierarchicalBrowseDcElement(HierarchicalBrowseDcElement blueprint) {
        super(blueprint);
        blueprint.children.stream().map(child -> new HierarchicalBrowseDcElement(child)).forEach(child -> this.addChild(child));
    }

    public List<HierarchicalBrowseDcElement> getChildren() {
        return children;
    }

    public void setChildren(List<HierarchicalBrowseDcElement> children) {
        this.children = children;
    }

    public HierarchicalBrowseDcElement getParent() {
        return parent;
    }

    public void setParent(HierarchicalBrowseDcElement parent) {
        this.parent = parent;
    }

    /**
     * @param dc
     */
    public void addChild(HierarchicalBrowseDcElement dc) {
        this.children.add(dc);
        dc.setParent(this);

    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseDcElement#isHasSubelements()
     */
    @Override
    public boolean isHasSubelements() {
        return children.size() > 0;
    }

    public boolean isOpensInNewWindow() {
        return opensInNewWindow && isHasSubelements();
    }

    public void setOpensInNewWindow(boolean opensInNewWindow) {
        this.opensInNewWindow = opensInNewWindow;
    }

    public boolean isRedirectsToWork() {
        return !isOpensInNewWindow() && getNumberOfVolumes() == 1;
    }

    public boolean isOpensInSearch() {
        return !isOpensInNewWindow() && !isRedirectsToWork();
    }

    /**
     * @return
     */
    public Collection<? extends HierarchicalBrowseDcElement> getAllVisibleDescendents(boolean checkAllDescendents) {
        List<HierarchicalBrowseDcElement> list = new ArrayList<>();
        if (checkAllDescendents || isShowSubElements()) {
            for (HierarchicalBrowseDcElement child : children) {
                if (isShowSubElements()) {
                    list.add(child);
                }
                list.addAll(child.getAllVisibleDescendents(checkAllDescendents));
            }
        }
        return list;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseDcElement#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        for (HierarchicalBrowseDcElement child : children) {
            sb.append("\n\t").append(child);
        }
        return sb.toString();
    }

    /**
     * @return
     */
    public Collection<HierarchicalBrowseDcElement> getChildrenAndVisibleDescendants() {
        List<HierarchicalBrowseDcElement> list = new ArrayList<>();
        for (HierarchicalBrowseDcElement child : children) {
            list.add(child);
            list.addAll(child.getAllVisibleDescendents(false));
        }
        return list;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseDcElement#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass().equals(this.getClass())) {
            return ((HierarchicalBrowseDcElement) obj).getName().equalsIgnoreCase(this.getName());
        }
        return false;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseDcElement#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * @param b
     * @return
     */
    public List<HierarchicalBrowseDcElement> getAllDescendents(final boolean includeMyself) {
        List<HierarchicalBrowseDcElement> list = getChildren().stream()
                .flatMap(child -> child.getAllDescendents(true).stream())
                .collect(Collectors.toList());
        if(includeMyself) {            
            list.add(0, this);
        }
        return list;
                
    }


}
