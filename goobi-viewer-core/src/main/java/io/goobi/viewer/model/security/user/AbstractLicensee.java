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
package io.goobi.viewer.model.security.user;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.solr.SolrConstants;

public abstract class AbstractLicensee implements ILicensee {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(AbstractLicensee.class);

    /** {@inheritDoc} */
    @Override
    public AccessPermission hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi); //NOSONAR Logging sometimes needed for debugging

        // No privilege name given
        if (StringUtils.isEmpty(privilegeName)) {
            return AccessPermission.granted();
        }

        for (License license : getLicenses()) {
            if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                // LicenseType grants privilege
                if (license.getLicenseType().getPrivileges().contains(privilegeName)) {
                    return AccessPermission.granted()
                            .setTicketRequired(license.isTicketRequired())
                            .setRedirect(license.getLicenseType().isRedirect())
                            .setRedirectUrl(license.getLicenseType().getRedirectUrl());
                }
                // License grants privilege
                if (license.getPrivileges().contains(privilegeName)) {
                    if (StringUtils.isEmpty(license.getConditions())) {
                        return AccessPermission.granted()
                                .setTicketRequired(license.isTicketRequired())
                                .setRedirect(license.getLicenseType().isRedirect())
                                .setRedirectUrl(license.getLicenseType().getRedirectUrl());
                    } else if (StringUtils.isNotEmpty(pi)) {
                        // If PI and Solr condition subquery are present, check via Solr
                        String query = SolrConstants.PI + ":" + pi + " AND (" + license.getConditions() + ")";
                        if (DataManager.getInstance()
                                .getSearchIndex()
                                .getFirstDoc(query, Collections.singletonList(SolrConstants.IDDOC)) != null) {
                            logger.trace("Permission found (query: {})", query);
                            return AccessPermission.granted()
                                    .setTicketRequired(license.isTicketRequired())
                                    .setRedirect(license.getLicenseType().isRedirect())
                                    .setRedirectUrl(license.getLicenseType().getRedirectUrl());
                        }
                    }
                }
            }
        }

        return AccessPermission.denied();
    }

    /**
     * 
     * @param permissionMap Map containing permission check outcome for different license names
     * @return {@link AccessPermission}
     * @should return denied if permissionMap empty
     * @should return denied if all permissions in map denied
     * @should preserve ticketRequired
     * @should preserve redirect metadata
     */
    public static AccessPermission getAccessPermissionFromMap(Map<String, AccessPermission> permissionMap) {
        // It should be sufficient if the user can satisfy one required license
        boolean granted = false;
        boolean ticketRequired = false;
        boolean redirect = false;
        String redirectUrl = null;
        for (Entry<String, AccessPermission> entry : permissionMap.entrySet()) {
            if (entry.getValue().isGranted()) {
                granted = true;
            }
            if (entry.getValue().isTicketRequired()) {
                ticketRequired = true;
            }
            if (entry.getValue().isRedirect()) {
                redirect = true;
            }
            if (StringUtils.isNotEmpty(entry.getValue().getRedirectUrl())) {
                redirectUrl = entry.getValue().getRedirectUrl();
            }
        }
        if (granted) {
            return AccessPermission.granted().setTicketRequired(ticketRequired).setRedirect(redirect).setRedirectUrl(redirectUrl);
        }

        return AccessPermission.denied();
    }
}
