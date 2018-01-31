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
package de.intranda.digiverso.presentation.managedbeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.metadata.MetadataElement;
import de.intranda.digiverso.presentation.model.viewer.EventElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

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

    private List<MetadataElement> metadataElementList;
    private List<EventElement> events = new ArrayList<>();

    /** Empty constructor. */
    public MetadataBean() {
        // the emptiness inside
    }

    /**
     * @throws IndexUnreachableException
     *
     */
    @PostConstruct
    public void init() {
        // PostConstruct methods may not throw exceptions
        try {
            loadMetadata();
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
        } catch (DAOException e) {
            logger.debug("DAOException thrown here");
        }
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
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public final String loadMetadata() throws IndexUnreachableException, DAOException {
        metadataElementList = new ArrayList<>();
        if (activeDocumentBean == null) {
            activeDocumentBean = new ActiveDocumentBean();
        }
        StructElement currentElement = activeDocumentBean.getCurrentElement();
        if (currentElement != null) {
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
        }
        return "viewMetadata";
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @param metadataElementList the metadataElementList to set
     */
    public void setMetadataElementList(List<MetadataElement> metadataElementList) {
        this.metadataElementList = metadataElementList;
    }

    /**
     * @return the metadataElementList
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public List<MetadataElement> getMetadataElementList() throws IndexUnreachableException, DAOException {
        if (metadataElementList == null) {
            // Only reload if empty, otherwise a c:forEach (used by p:tabView) will cause a reload on every iteration
            loadMetadata();

        }
        return metadataElementList;
    }

    public MetadataElement getTopMetadataElement() {
        if (metadataElementList != null && !metadataElementList.isEmpty()) {
            return metadataElementList.get(0);
        }

        return null;
    }

    /**
     * Returns the last element in <code>metadataElementList</code>, which is the bottom element in the hierarchy. If the element contains no side bar
     * metadata, the next higher element is checked until an element with sidebar metadata is found. TODO for some reason this method is called 6-15
     * times per page
     *
     * @return
     */
    public MetadataElement getBottomMetadataElement() {
        if (metadataElementList != null && !metadataElementList.isEmpty()) {
            int index = metadataElementList.size() - 1;
            while (!metadataElementList.get(index).isHasSidebarMetadata() && index > 0) {
                index--;
            }
            // logger.debug("index: " + index);
            return metadataElementList.get(index);
        }

        return null;
    }

    /**
     * @return
     * @throws IndexUnreachableException
     */
    private int getBreadcrumbStartLevel() throws IndexUnreachableException {
        return activeDocumentBean.hasAnchor() ? 2 : 1;
    }

    /**
     * @return the events
     */
    public List<EventElement> getEvents() {
        return events;
    }

    /**
     * @param events the events to set
     */
    public void setEvents(List<EventElement> events) {
        this.events = events;
    }

    @Deprecated
    public boolean displayBreadCrumbs() throws IndexUnreachableException {
        int cutoff = getBreadcrumbStartLevel();
        return DataManager.getInstance().getConfiguration().getDisplayBibdataBreadcrumbs() && cutoff > 0 && metadataElementList.size() > cutoff;
    }

    public boolean displayChildStructs() {
        return true;
    }
}
