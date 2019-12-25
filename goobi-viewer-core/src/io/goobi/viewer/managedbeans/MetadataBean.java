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
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotatedElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.viewer.EventElement;
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

    /** Metadata blocks (one per structure element) */
    private List<MetadataElement> metadataElementList = null;
    /** Events. */
    private List<EventElement> events = new ArrayList<>();

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
     * <p>loadMetadata.</p>
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @return a {@link java.lang.String} object.
     */
    public String loadMetadata() throws IndexUnreachableException, DAOException {
        metadataElementList = new ArrayList<>();
        if (activeDocumentBean == null) {
            activeDocumentBean = new ActiveDocumentBean();
        }
        StructElement currentElement = activeDocumentBean.getCurrentElement();
        if (currentElement == null) {
            return "viewMetadata";
        }
        
        logger.trace("loadMetadata for: {}", currentElement.getLabel());
        try {
            Locale locale = BeanUtils.getLocale();
            MetadataElement metadataElement = new MetadataElement(currentElement, locale, activeDocumentBean.getSelectedRecordLanguage());
            metadataElementList.add(metadataElement);

            // Retrieve any struct elements above the current and generate metadata for each of them
            StructElement se = currentElement;
            while (se.getParent() != null) {
                se = se.getParent();
                metadataElementList.add(new MetadataElement(se, locale, activeDocumentBean.getSelectedRecordLanguage()));
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

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>Setter for the field <code>metadataElementList</code>.</p>
     *
     * @param metadataElementList the metadataElementList to set
     */
    public void setMetadataElementList(List<MetadataElement> metadataElementList) {
        this.metadataElementList = metadataElementList;
    }

    /**
     * <p>Getter for the field <code>metadataElementList</code>.</p>
     *
     * @return the metadataElementList
     */
    public List<MetadataElement> getMetadataElementList() {
        if (metadataElementList == null) {
            // Only reload if empty, otherwise a c:forEach (used by p:tabView) will cause a reload on every iteration
            try {
                loadMetadata();
            } catch (IndexUnreachableException | DAOException e) {
                logger.error("Error loading metadatalist ", e);
                return Collections.emptyList();
            }

        }
        return metadataElementList;
    }

    /**
     * <p>getTopMetadataElement.</p>
     *
     * @return a {@link io.goobi.viewer.model.metadata.MetadataElement} object.
     */
    public MetadataElement getTopMetadataElement() {
        if (getMetadataElementList() != null && !getMetadataElementList().isEmpty()) {
            return getMetadataElementList().get(0);
        }

        return null;
    }

    /**
     * Returns the last element in <code>metadataElementList</code>, which is the bottom element in the hierarchy. If the element contains no side bar
     * metadata, the next higher element is checked until an element with sidebar metadata is found. TODO for some reason this method is called 6-15
     * times per page
     *
     * @return a {@link io.goobi.viewer.model.metadata.MetadataElement} object.
     */
    public MetadataElement getBottomMetadataElement() {
        if (getMetadataElementList() != null && !getMetadataElementList().isEmpty()) {
            int index = getMetadataElementList().size() - 1;
            while (!getMetadataElementList().get(index).isHasSidebarMetadata() && index > 0) {
                index--;
            }
            // logger.debug("index: " + index);
            return getMetadataElementList().get(index);
        }

        return null;
    }

    /**
     * <p>Getter for the field <code>events</code>.</p>
     *
     * @return the events
     */
    public List<EventElement> getEvents() {
        return events;
    }

    /**
     * <p>Setter for the field <code>events</code>.</p>
     *
     * @param events the events to set
     */
    public void setEvents(List<EventElement> events) {
        this.events = events;
    }

    /**
     * <p>displayChildStructs.</p>
     *
     * @return a boolean.
     */
    public boolean displayChildStructs() {
        return true;
    }

    /**
     * <p>setSelectedRecordLanguage.</p>
     *
     * @param selectedRecordLanguage a {@link java.lang.String} object.
     */
    public void setSelectedRecordLanguage(String selectedRecordLanguage) {
       if(metadataElementList != null) {           
           metadataElementList.forEach(element -> element.setSelectedRecordLanguage(selectedRecordLanguage));
       }
    }
}
