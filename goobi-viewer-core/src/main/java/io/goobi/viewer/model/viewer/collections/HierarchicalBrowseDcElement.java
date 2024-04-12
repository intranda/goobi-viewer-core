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
package io.goobi.viewer.model.viewer.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * <p>
 * HierarchicalBrowseDcElement class.
 * </p>
 */
public class HierarchicalBrowseDcElement extends BrowseDcElement {

    private static final long serialVersionUID = -4369053276327316515L;

    private static final Logger logger = LogManager.getLogger(HierarchicalBrowseDcElement.class);

    private List<HierarchicalBrowseDcElement> children = new ArrayList<>();
    private HierarchicalBrowseDcElement parent = null;
    private boolean opensInNewWindow;

    /**
     * <p>
     * Constructor for HierarchicalBrowseDcElement.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param number a long.
     * @param field a {@link java.lang.String} object.
     * @param sortField a {@link java.lang.String} object.
     * @param splittingChar
     * @param displayNumberOfVolumesLevel
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public HierarchicalBrowseDcElement(String name, long number, String field, String sortField, String splittingChar,
            int displayNumberOfVolumesLevel) throws PresentationException {
        super(name, number, field, sortField, splittingChar, displayNumberOfVolumesLevel);
    }

    /**
     * <p>
     * Constructor for HierarchicalBrowseDcElement.
     * </p>
     *
     * @param blueprint a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public HierarchicalBrowseDcElement(HierarchicalBrowseDcElement blueprint) {
        super(blueprint);
        blueprint.children.stream().map(HierarchicalBrowseDcElement::new).forEach(this::addChild);
    }

    /**
     * <p>
     * Getter for the field <code>children</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getChildren() {
        return children;
    }

    public List<HierarchicalBrowseDcElement> getChildren(boolean includeMyself) {
        if (includeMyself) {
            List<HierarchicalBrowseDcElement> list = new ArrayList<>(this.children);
            list.add(0, this);
            return list;
        }

        return getChildren();
    }

    /**
     * <p>
     * Setter for the field <code>children</code>.
     * </p>
     *
     * @param children a {@link java.util.List} object.
     */
    public void setChildren(List<HierarchicalBrowseDcElement> children) {
        this.children = children;
    }

    /**
     * <p>
     * Getter for the field <code>parent</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public HierarchicalBrowseDcElement getParent() {
        return parent;
    }

    /**
     * <p>
     * Setter for the field <code>parent</code>.
     * </p>
     *
     * @param parent a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void setParent(HierarchicalBrowseDcElement parent) {
        this.parent = parent;
    }

    /**
     * <p>
     * addChild.
     * </p>
     *
     * @param dc a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     */
    public void addChild(HierarchicalBrowseDcElement dc) {
        this.children.add(dc);
        dc.setParent(this);

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseDcElement#isHasSubelements()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isHasSubelements() {
        return !children.isEmpty();
    }

    /**
     * <p>
     * isOpensInNewWindow.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isOpensInNewWindow() {
        return opensInNewWindow && isHasSubelements();
    }

    /**
     * <p>
     * Setter for the field <code>opensInNewWindow</code>.
     * </p>
     *
     * @param opensInNewWindow a boolean.
     */
    public void setOpensInNewWindow(boolean opensInNewWindow) {
        this.opensInNewWindow = opensInNewWindow;
    }

    /**
     * <p>
     * isRedirectsToWork.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRedirectsToWork() {
        return DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork() && !isOpensInNewWindow() && getNumberOfVolumes() == 1;
    }

    /**
     * <p>
     * isOpensInSearch.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isOpensInSearch() {
        return !isOpensInNewWindow() && !isRedirectsToWork();
    }

    /**
     * <p>
     * getAllVisibleDescendents.
     * </p>
     *
     * @param checkAllDescendents a boolean.
     * @return a {@link java.util.Collection} object.
     */
    public Collection<HierarchicalBrowseDcElement> getAllVisibleDescendents(boolean checkAllDescendents) {
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
     * @see io.goobi.viewer.model.viewer.BrowseDcElement#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        for (HierarchicalBrowseDcElement child : children) {
            sb.append("\n\t").append(child);
        }
        return sb.toString();
    }

    /**
     * <p>
     * getChildrenAndVisibleDescendants.
     * </p>
     *
     * @return a {@link java.util.Collection} object.
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
     * @see io.goobi.viewer.model.viewer.BrowseDcElement#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
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
     * @see io.goobi.viewer.model.viewer.BrowseDcElement#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * <p>
     * getAllDescendents.
     * </p>
     *
     * @param includeMyself a boolean.
     * @return a {@link java.util.List} object.
     */
    public List<HierarchicalBrowseDcElement> getAllDescendents(final boolean includeMyself) {
        List<HierarchicalBrowseDcElement> list =
                getChildren().stream().flatMap(child -> child.getAllDescendents(true).stream()).collect(Collectors.toList());
        if (includeMyself) {
            list.add(0, this);
        }
        return list;

    }

    public String getQuery() {
        return "{field}:({name} {name}.*)"
                .replace("{field}", getSortField())
                .replace("{name}", getName());
    }

}
