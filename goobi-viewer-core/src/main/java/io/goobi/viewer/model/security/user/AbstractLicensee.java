package io.goobi.viewer.model.security.user;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.solr.SolrConstants;

public abstract class AbstractLicensee implements ILicensee {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AbstractLicensee.class);

    /** {@inheritDoc} */
    @Override
    public AccessPermission hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi);

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
     * @param permissionMap
     * @return
     */
    public AccessPermission getAccessPermissionFromMap(Map<String, AccessPermission> permissionMap) {
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
