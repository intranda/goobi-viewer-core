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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.Highlight;
import io.goobi.viewer.model.cms.HighlightData;
import io.goobi.viewer.model.metadata.MetadataElement;

/**
 * Managed bean handling {@link Highlight} objects
 */
@Named
@SessionScoped
public class HighlightsBean implements Serializable {

    private static final String DAO_FIELD_DATE_START = "dateStart";
    private static final long serialVersionUID = -6647395682752991930L;
    private static final Logger logger = LogManager.getLogger(HighlightsBean.class);
    private static final int NUM_ITEMS_PER_PAGE = 12;
    private static final String ALL_OBJECTS_SORT_FIELD = DAO_FIELD_DATE_START;
    private static final SortOrder ALL_OBJECTS_SORT_ORDER = SortOrder.DESCENDING;
    private static final String CURRENT_OBJECTS_SORT_FIELD = DAO_FIELD_DATE_START;
    private static final SortOrder CURRENT_OBJECTS_SORT_ORDER = SortOrder.ASCENDING;

    private TableDataProvider<Highlight> allObjectsProvider;
    private TableDataProvider<Highlight> currentObjectsProvider;

    private transient Highlight selectedObject = null;
    private final Random random = new Random(); //NOSONAR   generated numbers have no security relevance
    private EditStatus editStatus = EditStatus.SELECT_TARGET;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private transient IDAO dao;
    @Inject
    private ImageDeliveryBean imaging;

    /**
     * Status of editing the {@link HighlightsBean#getSelectedObject() selected highlight} in the administration backend
     */
    public enum EditStatus {
        /**
         * User still needs to select whether the highlight should be based on a record or a url
         */
        SELECT_TARGET,
        /**
         * A new hightlight is being created after {@link #SELECT_TARGET} is completed
         */
        CREATE,
        /**
         * An existing hightlight is being edited
         */
        EDIT;
    }

    /**
     * Empty defaul constructor. The required properties are being injected automatically
     */
    public HighlightsBean() {
    }

    /**
     * Testing constructor explicitly initializing required properties
     * 
     * @param dao The {@link IDAO} in which to store the highlight data
     * @param navigationHelper {@link NavigationHelper} to handle URL resolving
     * @param imaging {@link ImageDeliveryBean} to handle image URL creation
     */
    public HighlightsBean(IDAO dao, NavigationHelper navigationHelper, ImageDeliveryBean imaging) {
        this.dao = dao;
        this.navigationHelper = navigationHelper;
        this.imaging = imaging;
    }

    /**
     * called after initialization to load listing of existing highlights
     */
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        if (allObjectsProvider == null || currentObjectsProvider == null) {
            initProviders(now);
        }
    }

    void initProviders(LocalDateTime now) {
        allObjectsProvider = TableDataProvider.initDataProvider(NUM_ITEMS_PER_PAGE, ALL_OBJECTS_SORT_FIELD, ALL_OBJECTS_SORT_ORDER,
                (first, pageSize, sortField, descending, filters) -> dao
                        .getHighlights(first, pageSize, sortField, descending, filters)
                        .stream()
                        .map(Highlight::new)
                        .collect(Collectors.toList()));
        currentObjectsProvider = TableDataProvider.initDataProvider(Integer.MAX_VALUE, CURRENT_OBJECTS_SORT_FIELD, CURRENT_OBJECTS_SORT_ORDER,
                (first, pageSize, sortField, descending, filters) -> dao
                        .getHighlightsForDate(now)
                        .stream()
                        .filter(HighlightData::isEnabled)
                        .map(Highlight::new)
                        .collect(Collectors.toList()));
    }

    /**
     * Get a {@link TableDataProvider} to all saved {@link Highlight highlights}
     * 
     * @return a {@link TableDataProvider}
     */
    public TableDataProvider<Highlight> getAllObjectsProvider() {
        return allObjectsProvider;
    }

    /**
     * Get a {@link TableDataProvider} to all current {@link Highlight highlights}. That is all highlights which are valid for the current date and
     * set to active
     * 
     * @return a {@link TableDataProvider}
     */
    public TableDataProvider<Highlight> getCurrentObjectsProvider() {
        return currentObjectsProvider;
    }

    /**
     * Get the URL to the highlighted object. Either the record page URL of the URL given in highlight creation
     * 
     * @param the highlight object
     * @return the URL
     */
    public String getUrl(Highlight object) {
        if (object != null) {
            switch (object.getData().getTargetType()) {
                case RECORD:
                    return navigationHelper.getImageUrl() + "/" + object.getData().getRecordIdentifier() + "/";
                case URL:
                    try {
                        URI uri = new URI(object.getData().getTargetUrl());
                        if (!uri.isAbsolute()) {
                            uri = UriBuilder.fromPath("/").path(object.getData().getTargetUrl()).scheme("https").build();
                        }
                        return uri.toString();
                    } catch (URISyntaxException e) {
                        logger.error("Highlight target url {} is not a valid url", object.getData().getTargetUrl());
                    }
            }
        }
        return "";
    }

    /**
     * Delete a {@link Highlight}
     * 
     * @param object
     */
    public void deleteObject(Highlight object) {
        try {
            dao.deleteHighlight(object.getData().getId());
            Messages.info("cms___highlights__delete__success");
        } catch (DAOException e) {
            logger.error("Error deleting object", e);
            Messages.error("cms___highlights__delete__error");
        }
    }

    /**
     * Get the currently selected {@link Highlight}
     * 
     * @return a {@link Highlight}
     */
    public Highlight getSelectedObject() {
        return selectedObject;
    }

    /**
     * Set the {@link Highlight} selected for editing
     * 
     * @param selectedObject
     */
    public void setSelectedObject(Highlight selectedObject) {
        this.selectedObject = selectedObject;
        if (this.selectedObject != null) {
            this.selectedObject.setSelectedLocale(BeanUtils.getDefaultLocale());
            if (this.selectedObject.getData().getId() == null) {
                setEditStatus(EditStatus.SELECT_TARGET);
            } else {
                setEditStatus(EditStatus.EDIT);
            }
        }
    }

    /**
     * Set the {@link Highlight} selected for editing via its database id
     * 
     * @param id
     */
    public void setSelectedObjectId(long id) {

        try {
            HighlightData data = dao.getHighlight(id);
            if (data != null) {
                setSelectedObject(new Highlight(data));
            } else {
                setSelectedObject(null);
            }
        } catch (DAOException e) {
            logger.error("Error setting highlighted object", e);
            setSelectedObject(null);
        }
    }

    /**
     * Create a new {@link Highlight} and set as the selected highlight
     */
    public void setNewSelectedObject() {
        HighlightData data = new HighlightData();
        setSelectedObject(new Highlight(data));
    }

    /**
     * Check if the currently selected highlight has been persisted already
     * 
     * @return true if {@link #getSelectedObject()} has no database id and has thus not been persisted yet
     */
    public boolean isNewObject() {
        return this.selectedObject != null && this.selectedObject.getData().getId() == null;
    }

    /**
     * Persist the given {@link Highlight} to the database
     * 
     * @param object
     * @throws DAOException
     */
    public void saveObject(Highlight object) throws DAOException {
        boolean saved = false;
        boolean redirect = false;
        if (object != null && object.getData().getId() != null) {
            saved = dao.updateHighlight(object.getData());
        } else if (object != null) {
            saved = dao.addHighlight(object.getData());
            redirect = true;
        }
        if (saved) {
            Messages.info(ViewerResourceBundle.getTranslationWithParameters("button__save__success", null, true, object.toString()));
        } else {
            Messages.error(
                    ViewerResourceBundle.getTranslationWithParameters("button__save__error", null, true,
                            object != null ? object.toString() : "null"));
        }
        if (redirect) {
            PrettyUrlTools.redirectToUrl(PrettyUrlTools.getAbsolutePageUrl("adminCmsHighlightsEdit", object.getData().getId()));
        }
    }

    /**
     * If a {@link Highlight} has been selected and it points to a record (rather than a URL), retrieve the metadata for this record
     * 
     * @return A {@link MetadataElement} with metadata for the related record if one exists. Otherwise null
     */
    public MetadataElement getMetadataElement() {
        if (this.selectedObject != null) {
            try {
                return this.selectedObject.getMetadataElement();
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Unable to reetrive metadata elemement for {}. Reason: {}",
                        this.getSelectedObject().getData().getName().getTextOrDefault(),
                        e.getMessage());
                Messages.error(null, "Unable to reetrive metadata elemement for {}. Reason: {}",
                        getSelectedObject().getData().getName().getTextOrDefault(), e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Get the currently displayed highlight. This highlight is randomly chosen from all highlights valid for the current day which are set to enabled
     * 
     * @return a {@link Highlight}
     * @throws DAOException
     */
    public Highlight getCurrentHighlight() throws DAOException {
        List<Highlight> currentObjects = dao.getHighlightsForDate(LocalDateTime.now())
                .stream()
                .filter(HighlightData::isEnabled)
                .map(Highlight::new)
                .collect(Collectors.toList());
        if (!currentObjects.isEmpty()) {
            int randomIndex = random.nextInt(currentObjects.size());
            return currentObjects.get(randomIndex);
        }
        return null;
    }

    /**
     * Get the URL of a representative image for the record related to the currently selected highlight if a highlight is selected and it refers to a
     * record. Otherwise return null
     * 
     * @return A URL to the record for the selected highlight if one exists. Otherwise null
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public URI getRecordRepresentativeURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getRecordRepresentativeURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * Get the URL of a representative image for the record related to the currently selected highlight if a highlight is selected and it refers to a
     * record. Otherwise return null
     * 
     * @return A URL to the record for the selected highlight if one exists. Otherwise null
     * @param width the desired width of the image. Chose '0' for original image width
     * @param height the desired height of the image. Chose '0' for original image height
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public URI getRecordRepresentativeURI(int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (getSelectedObject() != null && StringUtils.isNotBlank(getSelectedObject().getData().getRecordIdentifier())) {
            return Optional.ofNullable(imaging.getThumbs().getThumbnailUrl(getSelectedObject().getData().getRecordIdentifier(), width, height))
                    .map(URI::create)
                    .orElse(null);
        }
        return null;
    }

    /**
     * Get all objected contained in {@link #getCurrentObjectsProvider()}
     * 
     * @return A list of all currently active {@link Highlight}s
     */
    public List<Highlight> getCurrentObjects() {
        return this.getCurrentObjectsProvider().getPaginatorList();
    }

    /**
     * Get the current {@link EditStatus}
     * 
     * @return a {@link EditStatus}
     */
    public EditStatus getEditStatus() {
        return editStatus;
    }

    /**
     * Set the {@link #getEditStatus()}
     * 
     * @param editStatus
     */
    public void setEditStatus(EditStatus editStatus) {
        this.editStatus = editStatus;
    }

    /**
     * Get all {@link Highlight}s which are not valid for the given date but were before. Only hightlights with {@link Highlight#isEnabled()} true are
     * included
     * 
     * @param date the date up to which to return the highlights (exclusively)
     * @return A list of {@link Highlight}s
     * @throws DAOException
     */
    public List<Highlight> getHighlightsBefore(LocalDate date) throws DAOException {
        return dao.getPastHighlightsForDate(0, Integer.MAX_VALUE, DAO_FIELD_DATE_START, true, Map.of(), date.atStartOfDay())
                .stream()
                .filter(HighlightData::isEnabled)
                .map(Highlight::new)
                .collect(Collectors.toList());
    }

}
