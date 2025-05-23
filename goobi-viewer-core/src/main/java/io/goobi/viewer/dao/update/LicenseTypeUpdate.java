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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.security.LicenseType;

public class LicenseTypeUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(LicenseTypeUpdate.class);

    private static final String LICENSE_TYPE_CAMPAIGNS = "licenseType_crowdsourcing_campaigns";

    /** {@inheritDoc} */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        return performUpdates(dao);
    }

    /**
     * <p>
     * persistData.
     * </p>
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws SQLException if in case of errors
     */
    private static boolean performUpdates(IDAO dao) throws DAOException, SQLException {
        boolean ret = false;
        // Remove obsolete core license type for crowdsourcing campaigns
        LicenseType ltCampaigns = dao.getLicenseType(LICENSE_TYPE_CAMPAIGNS);
        if (ltCampaigns != null) {
            int count = dao.executeUpdate("DELETE FROM licenses WHERE license_type_id=" + ltCampaigns.getId());
            if (count > 0) {
                ret = true;
                logger.info("{} licenses using license type '{}' deleted.", count, LICENSE_TYPE_CAMPAIGNS);
            }
            count = dao.executeUpdate("DELETE FROM license_types WHERE name='" + LICENSE_TYPE_CAMPAIGNS + "'");
            if (count > 0) {
                ret = true;
                logger.info("License type '{}' deleted.", LICENSE_TYPE_CAMPAIGNS);
            }
        }

        // Remove LicenseType.conditions
        if (dao.columnsExists("license_types", "conditions")) {
            dao.executeUpdate("ALTER TABLE license_types DROP COLUMN conditions;");
            ret = true;
        }

        return ret;
    }
}
