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
package io.goobi.viewer.managedbeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.metadata.MetadataValue;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.viewer.EventElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Provides the metadata for the current structure and event elements.
 */
@Named
@RequestScoped
public class MetadataBean {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(MetadataBean.class);

    @Inject
    private ActiveDocumentBean activeDocumentBean;

    /** Metadata blocks for the docstruct hierarchy from the anchor to the current element. */
    private Map<Integer, List<MetadataElement>> metadataElementMap = new HashMap<>();
    private Locale currentMetadataLocale;

    /** List of LIDO events. */
    private List<EventElement> events = new ArrayList<>();

    private String metadataViewUrl;
    private MetadataView activeMetadataView;

    /**
     * Empty constructor.
     */
    public MetadataBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection.
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
     * @param locale
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String loadMetadata(int index, Locale locale) throws IndexUnreachableException, DAOException {
        // logger.trace("loadMetadata({})", index); //NOSONAR Debug
        if (activeDocumentBean == null) {
            return "viewMetadata";
        }

        StructElement currentElement = activeDocumentBean.getCurrentElement();
        if (currentElement == null) {
            return "viewMetadata";
        }

        logger.trace("loadMetadata for: {}", currentElement.getLabel());
        List<MetadataElement> metadataElementList = metadataElementMap.computeIfAbsent(index, k -> new ArrayList<>());
        metadataElementList.clear(); // Clear hierarchy to avoid duplicates when switching language
        try {
            metadataElementList.add(new MetadataElement().init(currentElement, index, locale)
                    .setSelectedRecordLanguage(activeDocumentBean.getSelectedRecordLanguage()));

            // Retrieve any struct elements above the current and generate metadata for each of them
            StructElement se = currentElement;
            while (se.getParent() != null) {
                se = se.getParent();
                metadataElementList
                        .add(new MetadataElement().init(se, index, locale).setSelectedRecordLanguage(activeDocumentBean.getSelectedRecordLanguage()));
            }
            Collections.reverse(metadataElementList);

            // Retrieve events of the top element
            events = se.generateEventElements(locale, false);
            Collections.sort(events);
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            Messages.error(e.getMessage());
        }

        return "viewMetadata";
    }

    /**
     * Convenience method for {@link #getMetadataElementList(int) getMetadataElementList(0)}.
     *
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
        // logger.trace("getMetadataElementList({})", index); //NOSONAR Debug
        Locale locale = BeanUtils.getLocale();

        if (metadataElementMap.get(index) == null || !Objects.equals(locale, this.currentMetadataLocale)) {
            // Only reload if empty, otherwise a c:forEach (used by p:tabView) will cause a reload on every iteration
            try {
                loadMetadata(index, locale);
                this.currentMetadataLocale = locale; //store locale used for translations so it can be checked for changes later on
            } catch (IndexUnreachableException | DAOException e) {
                logger.error("Error loading metadatalist ", e);
                return Collections.emptyList();
            }

        }
        return metadataElementMap.get(index);
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

        return metadataElementList.get(i);
    }

    /**
     * Convenience method for the metadata page/link label key, depending on the document type.
     *
     * @return Message key for the label
     */
    public String getDefaultMetadataLabel() {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded() && activeDocumentBean.getViewManager().getTopStructElement() != null
                && activeDocumentBean.getViewManager().getTopStructElement().isLidoRecord()) {
            return "metadata";
        }

        return "bibData";
    }

    /**
     * Convenience method for the sidebar metadata widget label key, depending on the document type.
     *
     * @return Message key for the label
     */
    public String getDefaultSidebarMetadataLabel() {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded() && activeDocumentBean.getViewManager().getTopStructElement() != null
                && activeDocumentBean.getViewManager().getTopStructElement().isLidoRecord()) {
            return "metadata";
        }

        return "sidebarBibData";
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
        for (Entry<Integer, List<MetadataElement>> entry : metadataElementMap.entrySet()) {
            List<MetadataElement> metadataElementList = entry.getValue();
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

    /**
     * 
     * @param fields
     * @return Given strings as List<String>
     */
    public List<String> getComplexMetadataFieldsToList(String... fields) {
        if (fields != null) {
            return Arrays.asList(fields);
        }

        return List.of("MD_DATESTART", "MD_DATEEND", "MD_TYPE");
    }

    /**
     * Returns a list of {@link String} values for <code>subFieldName</code> of a grouped metadata field <code>mainFieldName</code> where the subfield
     * value of MD_ORDER matches the given <code>order</code> value.
     * 
     * @param metadataViewIndex Index of the requested metadataView where the requested metadata is configured
     * @param mainFieldName Main metadata field
     * @param language Optional metadata field language
     * @param subFieldName Child metadata field
     * @param order Page number
     * @return Metadata values of subFieldName; empty list if none found
     */
    public List<String> getMetadataValuesForPage(int metadataViewIndex, String mainFieldName, String language, String subFieldName, int order) {
        List<MetadataElement> metadataElements = getMetadataElementList(metadataViewIndex);
        if (metadataElements != null && !metadataElements.isEmpty()) {
            Metadata md = metadataElements.get(0).getMetadata(mainFieldName, language);
            if (md != null && md.isGroup() && !md.isBlank()) {
                for (MetadataValue val : md.getValues()) {
                    if (String.valueOf(order).equals(val.getParamValue("MD_ORDER"))) {
                        return val.getParamValues(subFieldName);
                    }
                }
            } else if (md != null && !md.isBlank()) {
                return md.getValues().stream().map(pv -> pv.getCombinedValue()).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns the first {@link String} values for <code>subFieldName</code> of a grouped metadata field <code>mainFieldName</code> where the subfield
     * value of MD_ORDER matches the given <code>order</code> value.
     * 
     * @param metadataViewIndex Index of the requested metadataView where the requested metadata is configured
     * @param mainFieldName Main metadata field
     * @param language Optional metadata field language
     * @param subFieldName Child metadata field
     * @param order Page number
     * @return First value of subFieldName; null if none found
     */
    public String getFirstMetadataValueForPage(int metadataViewIndex, String mainFieldName, String language, String subFieldName, int order) {
        List<String> values = getMetadataValuesForPage(metadataViewIndex, mainFieldName, language, subFieldName, order);
        if (!values.isEmpty()) {
            return values.get(0);
        }

        return null;
    }

    /**
     * Returns the first {@link String} values for <code>subFieldName</code> of a grouped metadata field <code>mainFieldName</code>
     * 
     * @param metadataViewIndex Index of the requested metadataView where the requested metadata is configured
     * @param mainFieldName Main metadata field
     * @param language Optional metadata field language
     * @param subFieldName Child metadata field
     * @return First value of subFieldName; null if none found
     */
    public String getFirstMetadataValue(int metadataViewIndex, String mainFieldName, String language, String subFieldName) {
        List<String> values = getMetadataValues(metadataViewIndex, mainFieldName, subFieldName, language);
        if (!values.isEmpty()) {
            return values.get(0);
        }

        return null;
    }

    /**
     * Returns a list of {@link String} values for <code>subFieldName</code> of a grouped metadata field <code>mainFieldName</code>
     * 
     * @param metadataViewIndex Index of the requested metadataView where the requested metadata is configured
     * @param mainFieldName Main metadata field
     * @param language Optional metadata field language
     * @param subFieldName Child metadata field
     * @return Metadata values of subFieldName; empty list if none found
     */
    public List<String> getMetadataValues(int metadataViewIndex, String mainFieldName, String subFieldName, String language) {
        List<MetadataElement> metadataElements = getMetadataElementList(metadataViewIndex);
        if (metadataElements != null && !metadataElements.isEmpty()) {
            Metadata md = metadataElements.get(0).getMetadata(mainFieldName, language);
            if (md != null && !md.isBlank()) {
                if (StringUtils.isNotBlank(subFieldName)) {
                    return md.getValues().stream().map(pv -> pv.getParamValues(subFieldName)).flatMap(List::stream).collect(Collectors.toList());
                }
                return md.getValues().stream().map(pv -> pv.getCombinedValue()).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    public List<Integer> range(int start, int end) {
        logger.trace("range: {} - {}", start, end);
        List<Integer> ret = new ArrayList<>(end - start);
        for (int i = start; i < end; ++i) {
            ret.add(i);
        }

        return ret;
    }
}
