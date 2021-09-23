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
public class CMSContentItemUpdate implements IModelUpdate {

    private static final Logger logger = LoggerFactory.getLogger(CMSContentItemUpdate.class);

    private static final String DATATYPE_OLD = "varchar";
    private static final String DATATYPE_NEW = "varchar(4096)";

    /* (non-Javadoc)
     * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
     */
    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        logger.debug("Checking database for deprecated cms_content_items.ignore_collections datatype");
        List<String> types = dao.createNativeQuery("SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_NAME = 'cms_content_items' AND COLUMN_NAME = 'ignore_collections' ").getResultList();
        if (types.contains(DATATYPE_OLD)) {
            logger.debug("Updating cms_content_items.ignore_collections datatype from " + DATATYPE_OLD + " to " + DATATYPE_NEW);

            //Delete existing data and store in temporary map
            List<Object[]> results =
                    dao.createNativeQuery("SELECT cms_content_item_id, ignore_collections FROM cms_content_items WHERE ignore_collections IS NOT NULL").getResultList();
            Map<Long, String> valueMap = new HashMap();
            for (Object[] res : results) {
                if (res[0] instanceof Long && res[1] instanceof String) {
                    valueMap.put((Long) res[0], (String) res[1]);
                    dao.startTransaction();
                    dao.createNativeQuery("UPDATE cms_content_items SET ignore_collections = NULL WHERE cms_content_item_id = " + res[0]).executeUpdate();
                    dao.commitTransaction();
                }
            }
            dao.startTransaction();
            dao.createNativeQuery("ALTER TABLE cms_content_items MODIFY COLUMN ignore_collections " + DATATYPE_NEW).executeUpdate();
            dao.commitTransaction();

            for (Long id : valueMap.keySet()) {
                try {
                dao.startTransaction();
                dao.createNativeQuery("UPDATE cms_content_items SET ignore_collections = '" + valueMap.get(id) + "' WHERE cms_content_item_id = " + id)
                        .executeUpdate();
                dao.commitTransaction();
                logger.trace("Updated ignore_collections value at cms_content_item_id = '{}' to '{}'", id, valueMap.get(id));
                } catch(Throwable e) {
                    logger.error("Error attempting to update cms_content_items value at cms_content_item_id = '{}' to '{}'",  id, valueMap.get(id));
                }
            }

            logger.debug("Done converting  cms_content_items.ignore_collections datatype");
        }

        return true;
    }

}
