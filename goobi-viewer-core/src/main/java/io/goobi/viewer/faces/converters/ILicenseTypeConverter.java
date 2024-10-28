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
package io.goobi.viewer.faces.converters;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.ILicenseType;
import io.goobi.viewer.model.security.LicenseType;

/**
 * <p>
 * Converter for either LicenseType or Campaign objects.
 * </p>
 */
@FacesConverter("iLicenseTypeConverter")
public class ILicenseTypeConverter implements Converter<ILicenseType> {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(ILicenseTypeConverter.class);

    /** {@inheritDoc} */
    @Override
    public final ILicenseType getAsObject(final FacesContext context, final UIComponent component, final String value) {
        try {
            // value should contain class name as prefix to the identifier
            if (value.startsWith(LicenseType.class.getSimpleName())) {
                long id = Long.valueOf(value.substring(LicenseType.class.getSimpleName().length()));
                return DataManager.getInstance().getDao().getLicenseType(id);
            } else if (value.startsWith(Campaign.class.getSimpleName())) {
                long id = Long.valueOf(value.substring(Campaign.class.getSimpleName().length()));
                return DataManager.getInstance().getDao().getCampaign(id);
            }
            //            throw new IllegalArgumentException("'" + value + "' is not a valid value.");
            return null;
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
            return null;
        } catch (DAOException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String getAsString(final FacesContext context, final UIComponent component, final ILicenseType object) {
        if (object == null) {
            return null;
        }

        // Use class name as prefix to the identifier
        if (object instanceof LicenseType) {
            LicenseType licenseType = (LicenseType) object;
            try {
                return LicenseType.class.getSimpleName() + String.valueOf(licenseType.getId());
            } catch (NumberFormatException nfe) {
                return null;
            }
        } else if (object instanceof Campaign) {
            Campaign campaign = (Campaign) object;
            try {
                return Campaign.class.getSimpleName() + String.valueOf(campaign.getId());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        throw new IllegalArgumentException("Object '" + object.getClass().getName() + "' is not a LicenseType or Campaign.");
    }
}
