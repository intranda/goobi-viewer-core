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

package io.goobi.viewer.model.job.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.upload.UploadJob;

public class UpdateUploadJobsJob extends AbstractViewerJob implements Job, IViewerJob {

    private static final Logger logger = LogManager.getLogger(PurgeExpiredDownloadTicketsJob.class);

    @Override
    public String getJobName() {
        return "UpdateUploadJobsJob";
    }

    @Override
    public void execute() {
        int countChecked = 0;
        int countUpdated = 0;
        try {
            for (UploadJob uj : DataManager.getInstance().getDao().getUploadJobsWithStatus(JobStatus.WAITING)) {
                if (uj.updateStatus()) {
                    DataManager.getInstance().getDao().updateUploadJob(uj);
                    countUpdated++;
                }
                countChecked++;
            }
        } catch (DAOException | IndexUnreachableException | PresentationException e) {
            logger.error(e);
        }
        logger.debug("{} upload jobs checked, {} updated.", countChecked, countUpdated);
    }

    @Override
    public String getCronExpression() {
        return "0 45 0 * * ?";
    }

}
