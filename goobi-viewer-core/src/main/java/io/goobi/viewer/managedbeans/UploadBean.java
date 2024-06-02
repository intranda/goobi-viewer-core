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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.UploadException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.job.upload.UploadJob;

/**
 * <p>UploadBean class.</p>
 */
@Named
@ViewScoped
public class UploadBean implements Serializable {

    private static final long serialVersionUID = -766868003675598285L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(UserBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private UserBean userBean;

    private TableDataProvider<UploadJob> lazyModelUploadJobs;

    private UploadJob currentUploadJob;

    /**
     * Required setter for ManagedProperty injection
     *
     * @param userBean the userBean to set
     */
    public void setBreadcrumbBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * Initialize all campaigns as lazily loaded list
     *
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    @PostConstruct
    public void init() throws DAOException {
        if (lazyModelUploadJobs == null) {
            lazyModelUploadJobs = new TableDataProvider<>(new TableDataSource<UploadJob>() {

                @Override
                public List<UploadJob> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    logger.trace("getEntries<UploadJob>, {}-{}", first, first + pageSize);
                    try {

                        if (userBean != null && userBean.getUser() != null) {
                            return DataManager.getInstance().getDao().getUploadJobsForCreatorId(userBean.getUser().getId());
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage());
                    }
                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    try {
                        return DataManager.getInstance().getDao().getUploadJobsForCreatorId(userBean.getUser().getId()).size();
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    }
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    //
                }
            });
            lazyModelUploadJobs.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        }
    }

    /**
     * <p>Getter for the field <code>lazyModelUploadJobs</code>.</p>
     *
     * @return the lazyModelUploadJobs
     */
    public TableDataProvider<UploadJob> getLazyModelUploadJobs() {
        return lazyModelUploadJobs;
    }

    /**
     * <p>Getter for the field <code>currentUploadJob</code>.</p>
     *
     * @return the currentUploadJob
     */
    public UploadJob getCurrentUploadJob() {
        if (currentUploadJob == null) {
            newUploadJobAction();
        }
        return currentUploadJob;
    }

    /**
     * <p>Setter for the field <code>currentUploadJob</code>.</p>
     *
     * @param currentUploadJob the currentUploadJob to set
     */
    public void setCurrentUploadJob(UploadJob currentUploadJob) {
        this.currentUploadJob = currentUploadJob;
    }

    /**
     * Sets <code>currentUploadJob</code> to a fresh instance.
     */
    public void newUploadJobAction() {
        logger.trace("newUploadJobAction");
        this.currentUploadJob = new UploadJob();
        if (userBean != null && userBean.getUser() != null) {
            this.currentUploadJob.setCreatorId(userBean.getUser().getId());
            this.currentUploadJob.setEmail(userBean.getUser().getEmail());
        }
    }

    /**
     * Creates new Goobi workflow process via REST and uploads the images.
     *
     * @return Navigation target
     */
    public String createProcessAction() {
        logger.trace("createProcessAction");
        if (currentUploadJob == null) {
            return "";
        }

        try {
            currentUploadJob.createProcess();
            currentUploadJob.uploadFiles();
            Messages.info("user__upload_content_success_msg");
        } catch (UploadException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        } catch (HTTPException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        } finally {
            // Any outcome should reset currentUploadJob, otherwise a new PI won't be generated
            setCurrentUploadJob(null);
        }

        return "user/upload?redirect=true";
    }

}
