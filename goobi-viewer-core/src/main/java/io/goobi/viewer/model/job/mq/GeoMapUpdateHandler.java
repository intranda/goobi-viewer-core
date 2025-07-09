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
package io.goobi.viewer.model.job.mq;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.contexts.ContextNotActiveException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.storage.ApplicationBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.maps.FeatureSet;
import io.goobi.viewer.model.maps.GeoMap;

public class GeoMapUpdateHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(GeoMapUpdateHandler.class);
    private static final long GEOMAP_TIME_TO_LIVE = 90;

    public GeoMapUpdateHandler() {
        // Empty constructor
    }

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {
        try {
            ApplicationBean applicationBean = BeanUtils.getPersistentStorageBean();
            IDAO dao = DataManager.getInstance().getDao();
            if (applicationBean == null) {
                throw new PresentationException("PersistentStorageBean not loaded. Cannot store geomaps");
            } else if (dao == null) {
                throw new PresentationException("DAO not loaded. Cannot load CMS Geomaps");
            } else {
                for (GeoMap geomap : dao.getAllGeoMaps()) {
                    updateMapInCache(applicationBean, geomap);
                }
            }
            return MessageStatus.FINISH;
        } catch (PresentationException | DAOException e) {
            logger.error("Error updating cms geomaps: {}", e.toString());
            return MessageStatus.ERROR;
        }
    }

    public static void updateMapInCache(GeoMap geomap) throws PresentationException {
        ApplicationBean applicationBean = BeanUtils.getPersistentStorageBean();
        if (applicationBean == null) {
            throw new PresentationException("PersistentStorageBean not loaded. Cannot store geomaps");
        } else {
            updateMapInCache(applicationBean, geomap);
        }
    }

    private static void updateMapInCache(ApplicationBean applicationBean, GeoMap geomap) throws PresentationException {
        List<FeatureSet> featureSets = new ArrayList<>(geomap.getFeatureSets());
        for (FeatureSet featureSet : featureSets) {
            try {
                featureSet.updateFeatures();
                featureSet.getFeaturesAsString();
            } catch (ContextNotActiveException e) {
                logger.info("No active faces context for search bean.");
            }
        }
        applicationBean.put("cms_geomap_" + geomap.getId(), geomap);
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.CACHE_GEOMAPS.name();
    }

    public static boolean shouldUpdateGeomaps() throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        boolean useHeatmap = DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps();
        return !useHeatmap && dao.getAllGeoMaps()
                .stream()
                .map(GeoMap::getFeatureSets)
                .flatMap(List::stream)
                .anyMatch(FeatureSet::isQueryResultSet);
    }

    public static long getGeoMapTimeToLive() {
        return GEOMAP_TIME_TO_LIVE;
    }

}
