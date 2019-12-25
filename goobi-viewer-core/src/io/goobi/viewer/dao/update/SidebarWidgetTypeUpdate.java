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

import java.util.List;

import javax.persistence.Query;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSSidebarElement;

/**
 * Sets a default value for all {@link io.goobi.viewer.model.cms.CMSSidebarElement#widgetType} which governs the exact class to be used for entities from that table
 *
 * @author florian
 */
public class SidebarWidgetTypeUpdate implements IModelUpdate{

	/* (non-Javadoc)
	 * @see io.goobi.viewer.dao.update.IModelUpdate#update(io.goobi.viewer.dao.IDAO)
	 */
	/** {@inheritDoc} */
	@Override
	public boolean update(IDAO dao) throws DAOException {
		return createDiscriminatorRow(dao);
	}
	


	/**
	 * @throws DAOException
	 * 
	 */
	private boolean createDiscriminatorRow(IDAO dao) throws DAOException {
		dao.startTransaction();
		try {
			Query q1 = dao.createQuery("SELECT element.type FROM CMSSidebarElement element WHERE element.widgetType IS NOT NULL");
			List results = q1.getResultList();
			if(results == null  || results.isEmpty()) {
				Query q = dao.createQuery("UPDATE CMSSidebarElement element SET element.widgetType = '"
						+ CMSSidebarElement.class.getSimpleName() + "' WHERE element.widgetType IS NULL");				
				q.executeUpdate();
				return true;
			} else {
				return false;
			}
		} finally {
			dao.commitTransaction();
		}

	}

}
