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
package io.goobi.viewer.managedbeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.viewer.EventElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Provides the metadata for the current structure and event elements.
 */
@Named
@RequestScoped
public class MetadataBean {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(MetadataBean.class);

    @Inject
    private ActiveDocumentBean activeDocumentBean;

    /** Metadata blocks for the docstruct hierarchy from the anchor to the current element. */
    private Map<Integer, List<MetadataElement>> metadataElementMap = new HashMap<>();

    /** Metadata blocks for all docstructs that are included within a specific page. */
    private Map<Integer, List<MetadataElement>> allMetadataElementsforPage = new HashMap<>();
    /** List of LIDO events. */
    private List<EventElement> events = new ArrayList<>();

    private int metadataViewIndex;
    private String metadataViewUrl;
    private MetadataView activeMetadataView;

    /**
     * Empty constructor.
     */
    public MetadataBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param activeDocumentBean the activeDocumentBean to set
     */
    public void setActiveDocumentBean(ActiveDocumentBean activeDocumentBean) {
        this.activeDocumentBean = activeDocumentBean;
    }

    /**
     * <p>
     * loadMetadata.
     * </p>
     *
     * @param index
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String loadMetadata(int index) throws IndexUnreachableException, DAOException {
        // logger.trace("loadMetadata({})", index);
        if (activeDocumentBean == null) {
            return "viewMetadata";
        }

        StructElement currentElement = activeDocumentBean.getCurrentElement();
        if (currentElement == null) {
            return "viewMetadata";
        }

        logger.trace("loadMetadata for: {}", currentElement.getLabel());
        List<MetadataElement> metadataElementList = metadataElementMap.get(index);
        if (metadataElementList == null) {
            metadataElementList = new ArrayList<>();
            metadataElementMap.put(index, metadataElementList);
        }

        try {
            Locale locale = BeanUtils.getLocale();
            MetadataElement metadataElement = new MetadataElement(currentElement, index, locale, activeDocumentBean.getSelectedRecordLanguage());
            metadataElementList.add(metadataElement);

            // Retrieve any struct elements above the current and generate metadata for each of them
            StructElement se = currentElement;
            while (se.getParent() != null) {
                se = se.getParent();
                metadataElementList.add(new MetadataElement(se, index, locale, activeDocumentBean.getSelectedRecordLanguage()));
            }
            Collections.reverse(metadataElementList);

            // Retrieve events of the top element
            events = se.generateEventElements(locale);
            Collections.sort(events);
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            Messages.error(e.getMessage());
        }

        return "viewMetadata";
    }

    /**
     * Convenience method for {@link #getMetadataElementList(int) getMetadataElementList(0)} 
     * @return the first metadata element list
     */
    public List<MetadataElement> getMetadataElementList() {
        return getMetadataElementList(0);
    }

    
    /**
     * <p>
     * Getter for the field <code>metadataElementList</code>.
     * </p>
     *
     * @param index
     * @return the metadataElementList
     */
    public List<MetadataElement> getMetadataElementList(int index) {
        //        logger.trace("getMetadataElementList({})", index);
        if (metadataElementMap.get(index) == null) {
            // Only reload if empty, otherwise a c:forEach (used by p:tabView) will cause a reload on every iteration
            try {
                loadMetadata(index);
            } catch (IndexUnreachableException | DAOException e) {
                logger.error("Error loading metadatalist ", e);
                return Collections.emptyList();
            }

        }
        return metadataElementMap.get(index);
    }

    /**
     * Returns a list of <code>MetadataElement</code>s for all structure elements contained on the given page (as opposed to just the immediate
     * hierarchy down to the first element that BEGINS on the current page, such as returned by <code>getMetadataElementList</code>.
     * 
     * @param index Metadata view index
     * @param order Page number
     * @return List of <code>MetadataElement</code>s for all structure elements contained on the given page
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should return metadata elements for all contained docstructs
     */
    @Deprecated
    public List<MetadataElement> getAllMetadataElementsForPage(int index, int order) throws IndexUnreachableException, DAOException {
        if (allMetadataElementsforPage.get(order) != null) {
            return allMetadataElementsforPage.get(order);
        }
        if (activeDocumentBean == null) {
            return Collections.emptyList();
        }

        StructElement topElement = activeDocumentBean.getTopDocument();
        if (topElement == null) {
            return Collections.emptyList();
        }

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + topElement.getPi() + " +" + SolrConstants.THUMBPAGENO + ":[1 TO " + order + "]";
        logger.trace("All metadata elements query: {}", query);
        SolrDocumentList docs;
        try {
            docs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.LOGID, "asc")), null);
            if (docs.isEmpty()) {
                return Collections.emptyList();
            }

            for (SolrDocument doc : docs) {
                int thumbPage = (int) doc.getFieldValue(SolrConstants.THUMBPAGENO);
                int numPages = (int) doc.getFieldValue(SolrConstants.NUMPAGES);
                if (thumbPage < 1) {
                    continue;
                }
                if (thumbPage == order || thumbPage + numPages - 1 >= order) {
                    StructElement se = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc);
                    Locale locale = BeanUtils.getLocale();
                    MetadataElement metadataElement = new MetadataElement(se, index, locale, activeDocumentBean.getSelectedRecordLanguage());
                    if (allMetadataElementsforPage.get(order) == null) {
                        allMetadataElementsforPage.put(order, new ArrayList<>(docs.size()));
                    }
                    allMetadataElementsforPage.get(order).add(metadataElement);
                }
            }

            return allMetadataElementsforPage.get(order);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());

        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getTopMetadataElement.
     * </p>
     *
     * @param index Metadata view index
     * @return a {@link io.goobi.viewer.model.metadata.MetadataElement} object.
     */
    public MetadataElement getTopMetadataElement(int index) {
        List<MetadataElement> metadataElementList = getMetadataElementList(index);
        if (metadataElementList == null || metadataElementList.isEmpty()) {
            return null;
        }

        return metadataElementList.get(index);
    }

    /**
     * Returns the last element in <code>metadataElementList</code>, which is the bottom element in the hierarchy. If the element contains no side bar
     * metadata, the next higher element is checked until an element with sidebar metadata is found. TODO for some reason this method is called 6-15
     * times per page
     *
     * @param index Metadata view index
     * @return a {@link io.goobi.viewer.model.metadata.MetadataElement} object.
     */
    public MetadataElement getBottomMetadataElement(int index) {
        List<MetadataElement> metadataElementList = getMetadataElementList(index);
        if (metadataElementList == null || metadataElementList.isEmpty()) {
            return null;
        }

        int i = metadataElementList.size() - 1;
        while (!metadataElementList.get(i).isHasSidebarMetadata() && i > 0) {
            i--;
        }
        // logger.debug("i: " + i);
        return metadataElementList.get(i);
    }

    /**
     * Convenience method for the metadata page/link label key, depending on the document type.
     * 
     * @return Message key for the label
     */
    public String getDefaultMetadataLabel() {
        if (activeDocumentBean != null && activeDocumentBean.getViewManager().getTopDocument().isLidoRecord()) {
            return "metadata";
        }

        return "bibData";
    }

    /**
     * <p>
     * Getter for the field <code>events</code>.
     * </p>
     *
     * @return the events
     */
    public List<EventElement> getEvents() {
        return events;
    }

    /**
     * <p>
     * Setter for the field <code>events</code>.
     * </p>
     *
     * @param events the events to set
     */
    public void setEvents(List<EventElement> events) {
        this.events = events;
    }

    /**
     * <p>
     * displayChildStructs.
     * </p>
     *
     * @return a boolean.
     */
    public boolean displayChildStructs() {
        return true;
    }

    /**
     * <p>
     * setSelectedRecordLanguage.
     * </p>
     *
     * @param selectedRecordLanguage a {@link java.lang.String} object.
     */
    public void setSelectedRecordLanguage(String selectedRecordLanguage) {
        for (int index : metadataElementMap.keySet()) {
            List<MetadataElement> metadataElementList = metadataElementMap.get(index);
            if (metadataElementList != null) {
                metadataElementList.forEach(element -> element.setSelectedRecordLanguage(selectedRecordLanguage));
            }
        }
    }

    /**
     * 
     * @return List of available <code>MetadataView</code>s
     */
    public List<MetadataView> getMetadataViews() {
        return DataManager.getInstance().getConfiguration().getMetadataViews();
    }

    /**
     * @return the metadataViewUrl
     */
    public String getMetadataViewUrl() {
        return metadataViewUrl;
    }

    /**
     * @param metadataViewUrl the metadataViewUrl to set
     */
    public void setMetadataViewUrl(String metadataViewUrl) {
        logger.debug("setMetadataViewUrl({})", metadataViewUrl);
        try {
            this.metadataViewUrl = metadataViewUrl;
            List<MetadataView> views = DataManager.getInstance().getConfiguration().getMetadataViews();
            if (StringUtils.isEmpty(metadataViewUrl)) {
                if (!views.isEmpty()) {
                    activeMetadataView = views.get(0);
                    return;
                }
            } else {
                for (MetadataView view : views) {
                    if (metadataViewUrl.equals(view.getUrl())) {
                        activeMetadataView = view;
                        return;
                    }
                }
            }

            activeMetadataView = null;
        } finally {
            logger.debug("setMetadataViewUrl END");
        }
    }

    /**
     * @return the activeMetadataView
     */
    public MetadataView getActiveMetadataView() {
        return activeMetadataView;
    }

    /**
     * @param activeMetadataView the activeMetadataView to set
     */
    public void setActiveMetadataView(MetadataView activeMetadataView) {
        this.activeMetadataView = activeMetadataView;
    }
}
