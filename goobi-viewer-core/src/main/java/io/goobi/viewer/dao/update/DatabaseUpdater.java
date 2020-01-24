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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSSidebarElement;

/**
 * Management tool to updated deprecated viewer database setups to the one required by the viewer. to be run at viewer start, right after initializing the dao
 * Appplies a fixed list of {@link IModelUpdate IModelUpdates} to the {@link io.goobi.viewer.dao.IDAO} which are responsible to make the required changes
 *
 * @author florian
 */
public class DatabaseUpdater {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdater.class);

	private static final IModelUpdate[] updates = {new SidebarWidgetTypeUpdate(), new CMSCategoryUpdate()};
	
	private final IDAO dao;

	/**
	 * <p>Constructor for DatabaseUpdater.</p>
	 *
	 * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
	 */
	public DatabaseUpdater(IDAO dao) {
		this.dao = dao;
	}

	/**
	 * <p>update.</p>
	 */
	public void update(){
		for (IModelUpdate update : updates) {
			try {
				if(update.update(dao)) {					
					logger.info("Successfully updated database using " + update.getClass().getSimpleName());
				}
			} catch (SQLException | DAOException e) {
				logger.error("Failed to update database using " + update.getClass().getSimpleName(), e);
			}
		}
	}


}
