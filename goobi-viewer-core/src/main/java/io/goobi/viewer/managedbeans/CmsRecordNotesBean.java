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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * Bean used for listing and retrieving {@link CMSRecordNote}s
 *
 * @author florian
 *
 */
@Named
@SessionScoped
public class CmsRecordNotesBean implements Serializable {

    private static final long serialVersionUID = 1436349423447175132L;

    private static final Logger logger = LogManager.getLogger(CmsRecordNotesBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 10;

    public static final String PI_TITLE_FILTER = "PI_OR_TITLE";

    @Inject
    private ImageDeliveryBean images;

    @Inject
    private NavigationHelper navigationHelper;

    private TableDataProvider<CMSRecordNote> dataProvider;

    public CmsRecordNotesBean() {

    }

    /**
     * @param images
     */
    public CmsRecordNotesBean(ImageDeliveryBean images) {
        this.images = images;
    }

    @PostConstruct
    public void init() {
        if (dataProvider == null) {
            initDataProvider();
        }
    }

    /**
     * @return the dataProvider
     */
    public TableDataProvider<CMSRecordNote> getDataProvider() {
        return dataProvider;
    }

    /**
     * get the thumbnail url for the record related to the note
     *
     * @param note
     * @return Thumbnail URL
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getThumbnailUrl(CMSSingleRecordNote note) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (StringUtils.isNotBlank(note.getRecordPi())) {
            return images.getThumbs().getThumbnailUrl(note.getRecordPi());
        }
        return "";
    }

    /**
     * get the thumbnail url for the record related to the note for given width and height
     *
     * @param note
     * @param width
     * @param height
     * @return Thumbnail URL
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getThumbnailUrl(CMSSingleRecordNote note, int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (StringUtils.isNotBlank(note.getRecordPi())) {
            return images.getThumbs().getThumbnailUrl(note.getRecordPi(), width, height);
        }
        return "";
    }

    public boolean deleteNote(CMSRecordNote note) throws DAOException {
        if (note != null && note.getId() != null) {
            return DataManager.getInstance().getDao().deleteRecordNote(note);
        }
        return false;
    }

    /**
     * 
     * @param note
     * @return Record URL
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getRecordUrl(CMSSingleRecordNote note) throws PresentationException, IndexUnreachableException {
        if (note != null) {
            SolrDocument doc =
                    DataManager.getInstance().getSearchIndex().getFirstDoc("+" + SolrConstants.PI + ":\"" + note.getRecordPi() + '"', null);
            if (doc != null) {
                return IdentifierResolver.constructUrl(doc, false);
            }

            return navigationHelper.getMetadataUrl() + "/" + note.getRecordPi() + "/";
        }
        return "";
    }

    private void initDataProvider() {
        dataProvider = new TableDataProvider<>(new TableDataSource<CMSRecordNote>() {

            private Optional<Long> numCreatedPages = Optional.empty();

            @Override
            public List<CMSRecordNote> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                    Map<String, String> filters) {
                try {
                    String useSortField = sortField;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "id";
                    }

                    return DataManager.getInstance()
                            .getDao()
                            .getRecordNotes(first, pageSize, useSortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numCreatedPages.isPresent()) {
                    try {
                        List<CMSRecordNote> notes = DataManager.getInstance()
                                .getDao()
                                .getRecordNotes(0, Integer.MAX_VALUE, null, false, filters);
                        numCreatedPages = Optional.of((long) notes.size());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of cms pages", e);
                    }
                }
                return numCreatedPages.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numCreatedPages = Optional.empty();
            }
        });
        dataProvider.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        dataProvider.getFilter(PI_TITLE_FILTER);
        //            lazyModelPages.addFilter("CMSCategory", "name");
    }

    public List<CMSRecordNote> getNotesForRecord(String pi) throws DAOException {
        List<CMSRecordNote> notes = new ArrayList<>();
        notes.addAll(DataManager.getInstance().getDao().getRecordNotesForPi(pi, true));
        notes.addAll(DataManager.getInstance()
                .getDao()
                .getAllMultiRecordNotes(true)
                .stream()
                .filter(note -> note.matchesRecord(pi))
                .collect(Collectors.toList()));
        return notes;
    }

    public String getSearchUrlForNote(CMSMultiRecordNote note) {
        String query = BeanUtils.escapeCriticalUrlChracters(note.getQueryForSearch());
        return PrettyUrlTools.getAbsolutePageUrl("newSearch5", "-", query, "1", "-", "-");
    }
}
