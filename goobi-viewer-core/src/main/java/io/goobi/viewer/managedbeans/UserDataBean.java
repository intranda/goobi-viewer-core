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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.search.Search;

@Named
@SessionScoped
public class UserDataBean implements Serializable {

    private static final long serialVersionUID = -766868003675598285L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(UserBean.class);
    
    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private UserBean userBean;
    
    private TableDataProvider<PersistentAnnotation> lazyModelAnnotations;
    
    /**
     * Initialize all campaigns as lazily loaded list
     */
    @PostConstruct
    public void init() {
        if (lazyModelAnnotations == null) {
            lazyModelAnnotations = new TableDataProvider<>(new TableDataSource<PersistentAnnotation>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<PersistentAnnotation> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder,
                        Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                            sortOrder = SortOrder.DESCENDING;
                        }
                        
                        filters.put("creatorId", String.valueOf(userBean.getUser().getId()));
                        // TODO or reviewerId

                        List<PersistentAnnotation> ret =
                                DataManager.getInstance().getDao().getAnnotations(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                        return ret;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance().getDao().getAnnotationCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of campaigns", e);
                        }
                    }
                    return numCreatedPages.orElse(0l);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelAnnotations.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            lazyModelAnnotations.setFilters("targetPI_body"); // TODO campaign name in current language
        }
    }

    /**
     * Returns saved searches for the logged in user.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return searches for correct user
     * @should return null if no user logged in
     */
    public List<Search> getSearches() throws DAOException {
        if (userBean == null || userBean.getUser() == null) {
            return null;
        }

        return DataManager.getInstance().getDao().getSearches(userBean.getUser());
    }

    /**
     * 
     * @return
     * @throws DAOException
     */
    public List<PersistentAnnotation> getAnnotations() throws DAOException {
        if (userBean == null || userBean.getUser() == null) {
            return Collections.emptyList();
        }

        return DataManager.getInstance().getDao().getAnnotationsForUserId(userBean.getUser().getId());
    }

    /**
     * Deletes the given persistent user search.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteSearchAction(Search search) throws DAOException {
        if (search == null) {
            return "";
        }

        logger.debug("Deleting search query: {}", search.getId());
        if (DataManager.getInstance().getDao().deleteSearch(search)) {
            String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteSuccess", null);
            Messages.info(msg.replace("{0}", search.getName()));
        } else {
            String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteFailure", null);
            Messages.error(msg.replace("{0}", search.getName()));
        }

        return "";
    }
    
    /**
     * <p>
     * Getter for the field <code>lazyModelAnnotations</code>.
     * </p>
     *
     * @return the lazyModelAnnotations
     */
    public TableDataProvider<PersistentAnnotation> getLazyModelAnnotations() {
        return lazyModelAnnotations;
    }
}
