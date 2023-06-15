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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
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
import io.goobi.viewer.model.cms.Highlight;
import io.goobi.viewer.model.cms.HighlightData;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;

@Named
@SessionScoped
public class HighlightsBean implements Serializable {

    private static final long serialVersionUID = -6647395682752991930L;
    private static final Logger logger = LogManager.getLogger(HighlightsBean.class);
    private static final int NUM_ITEMS_PER_PAGE = 12;
    private static final String ALL_OBJECTS_SORT_FIELD = "dateStart";
    private static final SortOrder ALL_OBJECTS_SORT_ORDER = SortOrder.DESCENDING;
    private static final String CURRENT_OBJECTS_SORT_FIELD = "dateStart";
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

    public enum EditStatus {
        SELECT_TARGET,
        CREATE,
        EDIT;
    }
    
    public HighlightsBean() {
        
    }
    
    public HighlightsBean(IDAO dao, NavigationHelper navigationHelper, ImageDeliveryBean imaging) {
        this.dao = dao;
        this.navigationHelper = navigationHelper;
        this.imaging = imaging;
    }
    
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

    public TableDataProvider<Highlight> getAllObjectsProvider() {
        return allObjectsProvider;
    }
    
    public TableDataProvider<Highlight> getCurrentObjectsProvider() {
        return currentObjectsProvider;
    }

    public String getUrl(Highlight object) {
        if (object != null) {
            switch(object.getData().getTargetType()) {
                case RECORD:
                    return navigationHelper.getImageUrl() + "/" + object.getData().getRecordIdentifier() + "/";
                case URL:
                    try {                        
                        URI uri = new URI(object.getData().getTargetUrl());
                        if(!uri.isAbsolute()) {
                            uri = UriBuilder.fromPath("/").path(object.getData().getTargetUrl()).scheme("https").build();
                        }
                        return uri.toString();
                    } catch(URISyntaxException e) {
                        logger.error("Highlight target url {} is not a valid url", object.getData().getTargetUrl());
                    }
            }
        }
        return "";
    }

    public void deleteObject(Highlight object) {
        try {
            dao.deleteHighlight(object.getData().getId());
            Messages.info("cms___highlights__delete__success");
        } catch (DAOException e) {
            logger.error("Error deleting object", e);
            Messages.error("cms___highlights__delete__error");
        }
    }

    public Highlight getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(Highlight selectedObject) {
        this.selectedObject = selectedObject;
        if (this.selectedObject != null) {
            this.selectedObject.setSelectedLocale(BeanUtils.getDefaultLocale());
            if(this.selectedObject.getData().getId() == null) {
                setEditStatus(EditStatus.SELECT_TARGET);
            } else {
                setEditStatus(EditStatus.EDIT);
            }
        }
    }

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

    public void setNewSelectedObject() {
        HighlightData data = new HighlightData();
        setSelectedObject(new Highlight(data));
    }

    public boolean isNewObject() {
        return this.selectedObject != null && this.selectedObject.getData().getId() == null;
    }

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
            Messages.info("Successfully saved object " + object);
        } else {
            Messages.error("Failed to save object " + object);
        }
        if (redirect) {
            PrettyUrlTools.redirectToUrl(PrettyUrlTools.getAbsolutePageUrl("adminCmsHighlightsEdit", object.getData().getId()));
        }
    }

    public MetadataElement getMetadataElement() {
        if(this.selectedObject != null) {
            try {                
                return this.selectedObject.getMetadataElement();
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Unable to reetrive metadata elemement for {}. Reason: {}", this.getSelectedObject().getData().getName().getTextOrDefault(),
                        e.getMessage());
                Messages.error(null, "Unable to reetrive metadata elemement for {}. Reason: {}",
                        getSelectedObject().getData().getName().getTextOrDefault(), e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }




    public Highlight getCurrentHighlight() throws DAOException {
        List<Highlight> currentObjects = dao.getHighlightsForDate(LocalDateTime.now())
                .stream()
                .filter(HighlightData::isEnabled)
                .map(Highlight::new)
                .collect(Collectors.toList());
        if (!currentObjects.isEmpty()) {
            int randomIndex = random.nextInt(currentObjects.size());
            return currentObjects.get(randomIndex);
        } else {
            return null;
        }
    }

    public URI getRecordRepresentativeURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getRecordRepresentativeURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    public URI getRecordRepresentativeURI(int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (getSelectedObject() != null && StringUtils.isNotBlank(getSelectedObject().getData().getRecordIdentifier())) {
            return Optional.ofNullable(imaging.getThumbs().getThumbnailUrl(getSelectedObject().getData().getRecordIdentifier(), width, height))
                    .map(URI::create)
                    .orElse(null);
        } else {
            return null;
        }
    }
    
    public List<Highlight> getCurrentObjects() {
        return this.getCurrentObjectsProvider().getPaginatorList();
    }
    
    public EditStatus getEditStatus() {
        return editStatus;
    }
    
    public void setEditStatus(EditStatus editStatus) {
        this.editStatus = editStatus;
    }

}
