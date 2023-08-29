package io.goobi.viewer.model.job.mq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.maps.FeatureSet;
import io.goobi.viewer.model.maps.GeoMap;

public class GeoMapUpdateHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(GeoMapUpdateHandler.class);

    public GeoMapUpdateHandler() {
    }

    @Override
    public MessageStatus call(ViewerMessage ticket) {
        try {            
            PersistentStorageBean applicationBean = BeanUtils.getPersistentStorageBean();
            IDAO dao = DataManager.getInstance().getDao();
            Configuration config = DataManager.getInstance().getConfiguration();
            if (applicationBean == null) {
                throw new PresentationException("PersistentStorageBean not loaded. Cannot store geomaps");
            } else if (dao == null) {
                throw new PresentationException("DAO not loaded. Cannot load CMS Geomaps");
            } else {
                for (GeoMap geomap : dao.getAllGeoMaps()) {
                    for (FeatureSet featureSet : geomap.getFeatureSets()) {
                        featureSet.getFeaturesAsString();
                        applicationBean.getIfRecentOrPut("cms_geomap_" + geomap.getId(), geomap, 0);
                    }
                };
                return MessageStatus.FINISH;
            }
        } catch(PresentationException | DAOException e) {
            logger.error("Error updating cms geomaps: {}", e.toString());
            return MessageStatus.ERROR;
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.CACHE_GEOMAPS.name();
    }

}
