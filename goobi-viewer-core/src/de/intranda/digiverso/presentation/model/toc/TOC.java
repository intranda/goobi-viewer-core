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
package de.intranda.digiverso.presentation.model.toc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Table of contents and associated functionality for a record.
 */
public class TOC implements Serializable {

    private static final long serialVersionUID = 2615373293377347746L;

    private static final Logger logger = LoggerFactory.getLogger(TOC.class);

    public static final String DEFAULT_GROUP = "_DEFAULT";

    /** TOC element map. */
    private Map<String, List<TOCElement>> tocElementMap;

    private boolean treeBuilt = false;

    private int tocVisible = -1;

    private int tocInvisible = -1;

    private int maxTocDepth = 0;

    private int totalTocSize = 0;

    private int currentPage = 1;

    public TOC() {
        logger.trace("new TOC()");
    }

    /**
     * 
     * @param structElement
     * @param addAllSiblings
     * @param mimeType
     * @param tocCurrentPage
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public void generate(StructElement structElement, boolean addAllSiblings, String mimeType, int tocCurrentPage)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        synchronized (this) {
            tocElementMap = TocMaker.generateToc(this, structElement, addAllSiblings, mimeType, tocCurrentPage,
                    DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage());
        }
    }

    public List<String> getGroupNames() {
        if (tocElementMap != null) {
            List<String> groups = new ArrayList<>(tocElementMap.keySet());
            //            groups.remove(DEFAULT_GROUP);
            return groups;
        }

        return Collections.emptyList();
    }

    public List<TOCElement> getViewForGroup(String group) {
        if (tocElementMap != null) {
            return tocElementMap.get(group);
        }

        return null;
    }

    /**
     * 
     * @param group
     * @return
     * @should call buildTree and set maxTocDepth correctly
     */
    public List<TOCElement> getTreeViewForGroup(String group) {
        if (!treeBuilt) {
            int visibleLevel = DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel();
            int collapseThreshold = DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold();
            int lowestLevelToCollapse = DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength();
            buildTree(group, visibleLevel, collapseThreshold, lowestLevelToCollapse);
        }
        return getViewForGroup(group);
    }

    public List<TOCElement> getFlatView() {
        // logger.trace("getFlatView");
        return getViewForGroup(DEFAULT_GROUP);
    }

    public List<TOCElement> getTreeView() {
        return getTreeViewForGroup(DEFAULT_GROUP);
    }

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
     */
    private void buildTree(String group, int visibleLevel, int collapseThreshold, int lowestLevelToCollapse) {
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
    }

    private void collapseTocForLength(int collapseThreshold, int lowestLevelToCollapse) {
        if (collapseThreshold > 0 && tocElementMap != null) {
            //        long start = System.nanoTime();
            int index = 0;
            int hideLevel = -1;
            boolean hide = false;
            for (index = 0; index < tocElementMap.get(DEFAULT_GROUP).size(); index++) {
                TOCElement tocElem = tocElementMap.get(DEFAULT_GROUP).get(index);

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
                    for (int i = index; i < tocElementMap.get(DEFAULT_GROUP).size(); i++) {
                        TOCElement tempElem = tocElementMap.get(DEFAULT_GROUP).get(i);
                        if (tempElem.getLevel() < tocElem.getLevel()) {
                            break;
                        } else if (tempElem.getLevel() == tocElem.getLevel()) {
                            levelLength++;
                        }
                    }
                    if (levelLength > collapseThreshold) {
                        tocElementMap.get(DEFAULT_GROUP).get(index - 1).setExpanded(false); //collapse parent
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
    }

    /**
     * Recalculates the visibility of TOC elements after a +/- button has been pressed.
     *
     * @return
     */
    public TOCElement getActiveElement() {
        TOCElement activeTocElement = null;
        if (tocElementMap != null) {
            if (tocVisible != -1) {
                expandTree(tocVisible);
                activeTocElement = tocElementMap.get(DEFAULT_GROUP).get(tocVisible);
                activeTocElement.setExpanded(true);
                tocVisible = -1;
            }
            if (tocInvisible != -1) {
                collapseTree(tocInvisible);
                activeTocElement = tocElementMap.get(DEFAULT_GROUP).get(tocInvisible);
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
            int level = tocElementMap.get(DEFAULT_GROUP).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(DEFAULT_GROUP).size(); i++) {
                TOCElement child = tocElementMap.get(DEFAULT_GROUP).get(i);
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
        logger.trace("expandTree: {}", parentId);
        if (tocElementMap != null) {
            int level = tocElementMap.get(DEFAULT_GROUP).get(parentId).getLevel();
            for (int i = parentId + 1; i < tocElementMap.get(DEFAULT_GROUP).size(); i++) {
                TOCElement child = tocElementMap.get(DEFAULT_GROUP).get(i);
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
     *
     */
    public void expandAll() {
        logger.trace("expandAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(DEFAULT_GROUP)) {
                tcElem.setVisible(true);
                if (tcElem.isHasChild()) {
                    tcElem.setExpanded(true);
                }
            }
        }
    }

    /**
     *
     */
    public void collapseAll() {
        logger.trace("collapseAll");
        if (tocElementMap != null) {
            for (TOCElement tcElem : tocElementMap.get(DEFAULT_GROUP)) {
                if (tcElem.getLevel() == 0) {
                    tcElem.setExpanded(false);
                } else {
                    tcElem.setVisible(false);
                }
            }
        }
    }

    public void setChildVisible(int id) {
        this.tocVisible = id;
    }

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

    public List<TOCElement> getTocElements() {
        if (tocElementMap != null) {
            return tocElementMap.get(DEFAULT_GROUP);
        }

        return null;
    }

    /**
     * @return the tocVisible
     */
    public int getTocVisible() {
        return tocVisible;
    }

    /**
     * @param tocVisible the tocVisible to set
     */
    public void setTocVisible(int tocVisible) {
        this.tocVisible = tocVisible;
    }

    /**
     * @return the tocInvisible
     */
    public int getTocInvisible() {
        return tocInvisible;
    }

    /**
     * @param tocInvisible the tocInvisible to set
     */
    public void setTocInvisible(int tocInvisible) {
        this.tocInvisible = tocInvisible;
    }

    /**
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
     * @param maxTocDepth the maxTocDepth to set
     */
    public void setMaxTocDepth(int maxTocDepth) {
        this.maxTocDepth = maxTocDepth;
    }

    /**
     * @return the totalTocSize
     */
    public int getTotalTocSize() {
        return totalTocSize;
    }

    /**
     * @param totalTocSize the totalTocSize to set
     */
    public void setTotalTocSize(int totalTocSize) {
        this.totalTocSize = totalTocSize;
    }

    /**
     * @return the currentPage
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Returns the number of paginator pages for the given TOC size and elements per page.
     *
     * @return
     * @should calculate number correctly
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

    public boolean hasChildren() {
        if (tocElementMap == null || tocElementMap.get(DEFAULT_GROUP) == null || tocElementMap.get(DEFAULT_GROUP).isEmpty()) {
            return false;
        } else if (tocElementMap.get(DEFAULT_GROUP).size() == 1 && !tocElementMap.get(DEFAULT_GROUP).get(0).isHasChild()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the label of the first found TOCElement that has the given PI as its topStructPi.
     * 
     * @param pi
     * @return
     * @should return correct label
     */
    public String getLabel(String pi) {
        return getLabel(pi, MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /**
     * Returns the label in the given language of the first found TOCElement that has the given PI as its topStructPi.
     * 
     * @param pi
     * @return
     * @should return correct label
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
     * @param pi
     * @return
     * @should return correct label
     */
    public String getLabel(String pi, Locale locale) {
        return getLabel(pi, locale.getLanguage());
    }
}
