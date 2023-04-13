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
package io.goobi.viewer.model.toc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Table of contents and associated functionality for a record.
 */
public class TOC implements Serializable {

    private static final long serialVersionUID = 2615373293377347746L;

    private static final Logger logger = LogManager.getLogger(TOC.class);

    /** TOC element map. */
    private Map<String, List<TOCElement>> tocElementMap;

    private boolean treeBuilt = false;

    private int tocVisible = -1;

    private int tocInvisible = -1;

    private int maxTocDepth = 0;

    private int totalTocSize = 0;

    private int currentPage = 1;

    /**
     * <p>
     * Constructor for TOC.
     * </p>
     */
    public TOC() {
        logger.trace("new TOC()");
    }

    /**
     * <p>
     * generate.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param addAllSiblings a boolean.
     * @param mimeType a {@link java.lang.String} object.
     * @param tocCurrentPage a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void generate(StructElement structElement, boolean addAllSiblings, String mimeType, int tocCurrentPage)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        synchronized (this) {
            tocElementMap = TocMaker.generateToc(this, structElement, addAllSiblings, mimeType, tocCurrentPage,
                    DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage());
        }
    }

    /**
     * <p>
     * getGroupNames.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getGroupNames() {
        if (tocElementMap != null) {
            return new ArrayList<>(tocElementMap.keySet());
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getViewForGroup.
     * </p>
     *
     * @param group a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<TOCElement> getViewForGroup(String group) {
        if (tocElementMap != null) {
            return tocElementMap.get(group);
        }

        return null;
    }

    /**
     * <p>
     * getTreeViewForGroup.
     * </p>
     *
     * @param group a {@link java.lang.String} object.
     * @should call buildTree and set maxTocDepth correctly
     * @return a {@link java.util.List} object.
     */
    public List<TOCElement> getTreeViewForGroup(String group) {
        if (!treeBuilt) {
            int visibleLevel = DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel();
            int collapseThreshold = DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold();
            int lowestLevelToCollapse = DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength();
            Long currentElementIdDoc = BeanUtils.getActiveDocumentBean().getViewManager().getCurrentStructElementIddoc();
            buildTree(group, visibleLevel, collapseThreshold, lowestLevelToCollapse, currentElementIdDoc);
        }
        return getViewForGroup(group);
    }

    /**
     * <p>
     * getFlatView.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<TOCElement> getFlatView() {
        // logger.trace("getFlatView");
        return getViewForGroup(StringConstants.DEFAULT_NAME);
    }

    /**
     * <p>
     * getTreeView.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<TOCElement> getTreeView() {
        return getTreeViewForGroup(StringConstants.DEFAULT_NAME);
    }

    /**
     * <p>
     * getTreeViewSidebar.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    @Deprecated
    public List<TOCElement> getTreeViewSidebar() {
        // logger.trace("getTreeViewSidebar");
        return getTreeView();
    }

    /**
     *
     * @param group
     * @param visibleLevel
     * @param collapseThreshold
     * @param lowestLevelToCollapse
     * @param currentElementIdDoc
     */
    protected void buildTree(String group, int visibleLevel, int collapseThreshold, int lowestLevelToCollapse, Long currentElementIdDoc) {
        logger.trace("buildTree");
        if (group == null) {
            throw new IllegalArgumentException("group may not be null");
        }

        synchronized (this) {
            if (tocElementMap != null) {
                //                long start = System.nanoTime();
                int lastLevel = 0;
                int lastParent = 0;
                for (TOCElement tocElement : tocElementMap.get(group)) {
                    // Current element index
                    int index = tocElementMap.get(group).indexOf(tocElement);
                    tocElement.setID(index);
                    if (tocElement.getLevel() > maxTocDepth) {
                        maxTocDepth = tocElement.getLevel();
                    }

                    if (lastLevel < tocElement.getLevel() && index > 0) {
                        tocElement.setParentId(lastParent);
                        tocElementMap.get(group).get(index - 1).setHasChild(true);
                        if (tocElement.getLevel() > visibleLevel) {
                            tocElementMap.get(group).get(index - 1).setExpanded(false);
                            tocElement.setVisible(false);
                        } else {
                            tocElementMap.get(group).get(index - 1).setExpanded(true);
                        }

                        for (int i = index + 1; i < tocElementMap.get(group).size(); i++) {
                            TOCElement tc = tocElementMap.get(group).get(i);
                            if (tc.getLevel() == tocElement.getLevel()) {
                                // Elements on the same level as the current element get the same parent ID and are set visible
                                tc.setParentId(tocElement.getParentId());
                            }
                            if (tc.getLevel() > visibleLevel) {

                                tc.setVisible(false);
                            }
                        }

                    }
                    lastParent = index;
                    lastLevel = tocElement.getLevel();
                }
                //                long end = System.nanoTime();
                //                logger.trace("Time for initial collapse: {} ns", (end - start));
                collapseTocForLength(collapseThreshold, lowestLevelToCollapse);
                treeBuilt = true;
            }
        }
        uncollapseCurrentElementAncestors(tocElementMap.get(group), currentElementIdDoc);
    }

    /**
     *
     *
     * @param list
     * @param currentElementIdDoc
     */
    private void uncollapseCurrentElementAncestors(List<TOCElement> list, Long currentElementIdDoc) {
        if (currentElementIdDoc != null) {
            TOCElement currentElement = getElement(list, currentElementIdDoc);
            if (currentElement != null) {
                currentElement.setVisible(true);
                int parentId = currentElement.getParentId();
                TOCElement parent = getElement(list, parentId);
                //recursivley expand all direct ancestors
                while (parent != null) {
                    parent.setExpanded(true);
                    expandTree(parentId);
                    parentId = parent.getParentId();
                    parent = getElement(list, parentId);
                }
            }
        }
    }

    /**
     * 
     * @param list
     * @param iddoc
     * @return
     */
    private static TOCElement getElement(List<TOCElement> list, Long iddoc) {
        return list.stream().filter(ele -> ele.getIddoc().equals(iddoc.toString())).findAny().orElse(null);
    }

    /**
     * 
     * @param list
     * @param id
     * @return
     */
    private static TOCElement getElement(List<TOCElement> list, int id) {
        return list.stream().filter(ele -> ele.getID() == id).findAny().orElse(null);
    }

    /**
     * 
     * @param collapseThreshold
     * @param lowestLevelToCollapse
     */
    private void collapseTocForLength(int collapseThreshold, int lowestLevelToCollapse) {
        if (collapseThreshold <= 0 || tocElementMap == null) {
            return;
        }

        //        long start = System.nanoTime();
        int index = 0;
        int hideLevel = -1;
        boolean hide = false;
        for (index = 0; index < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); index++) {
            TOCElement tocElem = tocElementMap.get(StringConstants.DEFAULT_NAME).get(index);

            if (tocElem.getLevel() < hideLevel || tocElem.getLevel() < lowestLevelToCollapse) {
                //if we return above the hidden level, reset flags
                hide = false;
                hideLevel = -1;
            } else if (hide) {
                //if hide flag is set from previous sibling, hide and collapse this element
                tocElem.setExpanded(false);
                tocElem.setVisible(false);
            } else {
                //else check if we need to hide this and following siblings
                int levelLength = 0;
                for (int i = index; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                    TOCElement tempElem = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                    if (tempElem.getLevel() < tocElem.getLevel()) {
                        break;
                    } else if (tempElem.getLevel() == tocElem.getLevel()) {
                        levelLength++;
                    }
                }
                if (levelLength > collapseThreshold) {
                    tocElementMap.get(StringConstants.DEFAULT_NAME).get(index - 1).setExpanded(false); //collapse parent
                    hideLevel = tocElem.getLevel();
                    hide = true;
                    tocElem.setExpanded(false);
                    tocElem.setVisible(false);
                }
                if (levelLength > collapseThreshold) {
                    tocElementMap.get(StringConstants.DEFAULT_NAME).get(index - 1).setExpanded(false); //collapse parent
                    hideLevel = tocElem.getLevel();
                    hide = true;
                    tocElem.setExpanded(false);
                    tocElem.setVisible(false);
                }
            }
        }

        //        long end = System.nanoTime();
        //          logger.trace("Time for length collapse: {} ns", (end - start))
    }

    /**
     * Recalculates the visibility of TOC elements after a +/- button has been pressed.
     *
     * @return a {@link io.goobi.viewer.model.toc.TOCElement} object.
     */
    public TOCElement getActiveElement() {
        TOCElement activeTocElement = null;
        if (tocElementMap != null) {
            if (tocVisible != -1) {
                expandTree(tocVisible);
                activeTocElement = tocElementMap.get(StringConstants.DEFAULT_NAME).get(tocVisible);
                activeTocElement.setExpanded(true);
                tocVisible = -1;
            }
            if (tocInvisible != -1) {
                collapseTree(tocInvisible);
                activeTocElement = tocElementMap.get(StringConstants.DEFAULT_NAME).get(tocInvisible);
                activeTocElement.setExpanded(false);
                tocInvisible = -1;
            }
        }

        return activeTocElement;
    }

    /**
     * Collapses all elements below the element with the given ID.
     *
     * @param parentId
     */
    private void collapseTree(int parentId) {
        logger.trace("collapseTree: {}", parentId);
        if (tocElementMap != null) {
            int level = tocElementMap.get(StringConstants.DEFAULT_NAME).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                TOCElement child = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                if (child.getLevel() > level) {
                    child.setVisible(false);
                } else {
                    // Rest of the elements are irrelevant because they belong
                    // to a different subtree on the same level
                    break;
                }
            }
        }
    }

    /**
     * Recursively expands the child elements of the element with the given ID.
     *
     * @param parentId
     */
    private void expandTree(int parentId) {
        // logger.trace("expandTree: {}", parentId);
        if (tocElementMap != null) {
            int level = tocElementMap.get(StringConstants.DEFAULT_NAME).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(StringConstants.DEFAULT_NAME).size(); i++) {
                TOCElement child = tocElementMap.get(StringConstants.DEFAULT_NAME).get(i);
                if (child.getLevel() == level + 1) {
                    // Set immediate children visible
                    child.setVisible(true);
                    // Elements further down the tree are handled recursively
                    if (child.isHasChild() && child.isExpanded()) {
                        expandTree(child.getID());
                    }
                } else if (child.getLevel() <= level) {
                    // Rest of the elements are irrelevant because they belong
                    // to a different subtree on the same level
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * expandAll.
     * </p>
     */
    public void expandAll() {
        logger.trace("expandAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(StringConstants.DEFAULT_NAME)) {
                tcElem.setVisible(true);
                if (tcElem.isHasChild()) {
                    tcElem.setExpanded(true);
                }
            }
        }
    }

    /**
     * <p>
     * collapseAll.
     * </p>
     */
    public void collapseAll() {
        logger.trace("collapseAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(StringConstants.DEFAULT_NAME)) {
                if (tcElem.getLevel() == 0) {
                    tcElem.setExpanded(false);
                } else {
                    tcElem.setVisible(false);
                }
            }
        }
    }

    /**
     * <p>
     * setChildVisible.
     * </p>
     *
     * @param id a int.
     */
    public void setChildVisible(int id) {
        this.tocVisible = id;
    }

    /**
     * <p>
     * setChildInvisible.
     * </p>
     *
     * @param id a int.
     */
    public void setChildInvisible(int id) {
        this.tocInvisible = id;
    }

    /**
     * @return the tocElementMap
     */
    Map<String, List<TOCElement>> getTocElementMap() {
        return tocElementMap;
    }

    /**
     * @param tocElementMap the tocElementMap to set
     */
    void setTocElementMap(Map<String, List<TOCElement>> tocElementMap) {
        this.tocElementMap = tocElementMap;
    }

    /**
     * <p>
     * getTocElements.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<TOCElement> getTocElements() {
        if (tocElementMap != null) {
            return tocElementMap.get(StringConstants.DEFAULT_NAME);
        }

        return null;
    }

    /**
     *
     * @param iddoc IDDOC of the element to find
     * @return Index of the element with the matching IDDOC within the list of elements; -1 if none found
     */
    public int findTocElementIndexByIddoc(String iddoc) {
        // logger.trace("findTocElementIndexByIddoc: {}", iddoc);
        if (StringUtils.isEmpty(iddoc)) {
            return -1;
        }

        int index = 0;
        for (TOCElement element : getTocElements()) {
            if (iddoc.equals(element.getIddoc())) {
                return index;
            }
            index++;
        }

        return -1;
    }

    /**
     * <p>
     * Getter for the field <code>tocVisible</code>.
     * </p>
     *
     * @return the tocVisible
     */
    public int getTocVisible() {
        return tocVisible;
    }

    /**
     * <p>
     * Setter for the field <code>tocVisible</code>.
     * </p>
     *
     * @param tocVisible the tocVisible to set
     */
    public void setTocVisible(int tocVisible) {
        this.tocVisible = tocVisible;
    }

    /**
     * <p>
     * Getter for the field <code>tocInvisible</code>.
     * </p>
     *
     * @return the tocInvisible
     */
    public int getTocInvisible() {
        return tocInvisible;
    }

    /**
     * <p>
     * Setter for the field <code>tocInvisible</code>.
     * </p>
     *
     * @param tocInvisible the tocInvisible to set
     */
    public void setTocInvisible(int tocInvisible) {
        this.tocInvisible = tocInvisible;
    }

    /**
     * <p>
     * Getter for the field <code>maxTocDepth</code>.
     * </p>
     *
     * @return the maxTocDepth
     */
    public int getMaxTocDepth() {
        // If this method is called before getTreeView, the depth will always be 0
        if (!treeBuilt) {
            logger.trace("Building tree to determine the TOC depth");
            getTreeView();
        }
        // logger.trace("getMaxTocDepth: {}", maxTocDepth);
        return maxTocDepth;
    }

    /**
     * <p>
     * Setter for the field <code>maxTocDepth</code>.
     * </p>
     *
     * @param maxTocDepth the maxTocDepth to set
     */
    public void setMaxTocDepth(int maxTocDepth) {
        this.maxTocDepth = maxTocDepth;
    }

    /**
     * <p>
     * Getter for the field <code>totalTocSize</code>.
     * </p>
     *
     * @return the totalTocSize
     */
    public int getTotalTocSize() {
        return totalTocSize;
    }

    /**
     * <p>
     * Setter for the field <code>totalTocSize</code>.
     * </p>
     *
     * @param totalTocSize the totalTocSize to set
     */
    public void setTotalTocSize(int totalTocSize) {
        this.totalTocSize = totalTocSize;
    }

    /**
     * <p>
     * Getter for the field <code>currentPage</code>.
     * </p>
     *
     * @return the currentPage
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage the currentPage to set
     * @should set value to 1 if given value too low
     * @should set value to last page number if given value too high
     */
    public void setCurrentPage(int currentPage) {
        if (currentPage < 1) {
            this.currentPage = 1;
            return;
        }

        int numPages = getNumPages();
        if (currentPage > numPages) {
            this.currentPage = numPages;
        } else {
            this.currentPage = currentPage;
        }
    }

    /**
     * Returns the number of paginator pages for the given TOC size and elements per page.
     *
     * @should calculate number correctly
     * @return a int.
     */
    public int getNumPages() {
        int hitsPerPage = DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage();
        if (hitsPerPage > 0) {
            // logger.trace("numPages: {}/{}={}", totalTocSize, hitsPerPage, totalTocSize / hitsPerPage);
            int num = totalTocSize / hitsPerPage;
            if (totalTocSize % hitsPerPage != 0 || num == 0) {
                num++;
            }
            return num;
        }

        return 1;
    }

    /**
     * <p>
     * hasChildren.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasChildren() {
        if (tocElementMap == null || tocElementMap.get(StringConstants.DEFAULT_NAME) == null
                || tocElementMap.get(StringConstants.DEFAULT_NAME).isEmpty()) {
            return false;
        }

        return !(tocElementMap.get(StringConstants.DEFAULT_NAME).size() == 1 && !tocElementMap.get(StringConstants.DEFAULT_NAME).get(0).isHasChild());
    }

    /**
     * Returns the label of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi a {@link java.lang.String} object.
     * @should return correct label
     * @return a {@link java.lang.String} object.
     */
    public String getLabel(String pi) {
        return getLabel(pi, MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /**
     * Returns the label in the given language of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi a {@link java.lang.String} object.
     * @should return correct label
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getLabel(String pi, String language) {
        if (StringUtils.isEmpty(pi)) {
            return null;
        }

        List<TOCElement> tocElements = getTocElements();
        if (tocElements != null) {
            for (TOCElement element : tocElements) {
                if (pi.equals(element.getTopStructPi())) {
                    return element.getLabel(language);
                }
            }
        }

        return null;
    }

    /**
     * Returns the label in the given locale of the first found TOCElement that has the given PI as its topStructPi.
     *
     * @param pi a {@link java.lang.String} object.
     * @should return correct label
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getLabel(String pi, Locale locale) {
        return getLabel(pi, locale.getLanguage());
    }
}
