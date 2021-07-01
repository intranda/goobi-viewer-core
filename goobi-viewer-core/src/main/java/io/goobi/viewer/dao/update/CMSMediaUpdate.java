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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSMediaItem;

/**
 * Converts {@link CMSMediaItem cms_media_items.link_url} from the LONGBLOB datatype (URI in java) to TEXT (String in java).
 * Extracts the link texts from all entries and writes them into the table again as Text
 * 
 * @author florian
 *
 */
public class CMSMediaUpdate implements IModelUpdate {

    private static final Logger logger = LoggerFactory.getLogger(CMSMediaUpdate.class);

    private static final String DATATYPE_OLD = "longblob";
    private static final String DATATYPE_NEW = "text";
    private static final String URL_REGEX = "(\\/|http)[\\w\\d-/.:%+!?#]+(?=x$)";

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
     */
    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        logger.debug("Checking database for deprecated cms_media_items.link_url datatype");
        List<String> types = dao.createNativeQuery("SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_NAME = 'cms_media_items' AND COLUMN_NAME = 'link_url' ").getResultList();
        if (types.contains(DATATYPE_OLD)) {
            logger.debug("Updating cms_media_items.link_url datatype from " + DATATYPE_OLD + " to " + DATATYPE_NEW);

            List<Object[]> results =
                    dao.createNativeQuery("SELECT cms_media_item_id, link_url FROM cms_media_items WHERE link_url IS NOT NULL").getResultList();
            Map<Long, String> linkUrlMap = new HashMap();
            for (Object[] res : results) {
                if (res[0] instanceof Long && res[1] instanceof byte[]) {
                    String value = parseUrl((byte[]) res[1]);
                    if(value != null) {                        
                        linkUrlMap.put((Long) res[0], value);
                    } else {
                        logger.warn("Encountered link url in cms_media_items which could not be parsed at cms_media_item_id = {}", res[0]);
                    }
                    //must delete row anyway because otherwise changing type will fail
                    dao.startTransaction();
                    dao.createNativeQuery("UPDATE cms_media_items SET link_url = NULL WHERE cms_media_item_id = " + res[0]).executeUpdate();
                    dao.commitTransaction();
                }
            }
            dao.startTransaction();
            dao.createNativeQuery("ALTER TABLE cms_media_items MODIFY COLUMN link_url " + DATATYPE_NEW).executeUpdate();
            dao.commitTransaction();

            for (Long id : linkUrlMap.keySet()) {
                try {
                dao.startTransaction();
                dao.createNativeQuery("UPDATE cms_media_items SET link_url = '" + linkUrlMap.get(id) + "' WHERE cms_media_item_id = " + id)
                        .executeUpdate();
                dao.commitTransaction();
                logger.trace("Updated cms_media_items value at cms_media_item_id = '{}' to '{}'", id, linkUrlMap.get(id));
                } catch(Throwable e) {
                    logger.error("Error attempting to update cms_media_items value at cms_media_item_id = '{}' to '{}'",  id, linkUrlMap.get(id));
                }
            }

            logger.debug("Done converting  cms_media_items.link_url datatype");
        }

        return true;
    }


    protected String parseUrl(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte d : bytes) {
            char c = (char) d;
            if (c > 0 && Character.isDefined(c)) {
                sb.append(c);
            }
        }
        return parseUrl(sb.toString());
    }
    
    protected String parseUrl(String blob) {
        String url = StringTools.findFirstMatch(blob, URL_REGEX, 0).orElse(null);
        return url;
    }

}
