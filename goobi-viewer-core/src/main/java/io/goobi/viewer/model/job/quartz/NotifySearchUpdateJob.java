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

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.search.SearchHitsNotifier;

public class NotifySearchUpdateJob extends AbstractViewerJob implements Job, IViewerJob {

    private static final Logger logger = LogManager.getLogger(NotifySearchUpdateJob.class);

    @Override
    public String getJobName() {
        return "NotifySearchUpdateJob";
    }

    @Override
    public void execute() {
        try {
            new SearchHitsNotifier().sendNewHitsNotifications();
        } catch (DAOException | PresentationException | IndexUnreachableException | ViewerConfigurationException e) {
            logger.error("Error in job NotifySearchUpdateJob: {}", e.toString());
        }
    }

    @Override
    public String getCronExpression() {
        return "0 42 8,12,17 * * ?";
    }
}
