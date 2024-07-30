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
package io.goobi.viewer.api.rest.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotImplementedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

public final class FilterTools {

    private static final Logger logger = LogManager.getLogger(FilterTools.class);

    public static final String ATTRIBUTE_PI = "pi";
    public static final String ATTRIBUTE_FILENAME = "filename";
    public static final String ATTRIBUTE_LOGID = "logid";
    public static final String ATTRIBUTE_PAGENO = "pageno";

    public static final int PRIORITY_REDIRECT = 100;

    /**
     * Private constructor.
     */
    private FilterTools() {
        //
    }

    /**
     * @param pi
     * @param request
     * @throws ServiceNotAllowedException
     * @should throw exception if record not found
     */
    public static void filterForConcurrentViewLimit(String pi, HttpServletRequest request) throws ServiceNotAllowedException {
        // logger.trace("filterForConcurrentViewLimit: {}", request.getSession().getId()); //NOSONAR Debug
        HttpSession session = request.getSession();
        // Release all locks for this session except the current record
        if (session != null) {
            DataManager.getInstance().getRecordLockManager().removeLocksForSessionId(session.getId(), Collections.singletonList(pi));
        }
        try {
            List<String> limits = DataManager.getInstance().getRecordLockManager().getRecordLimitsCache().get(pi);
            List<String> accessConditions = DataManager.getInstance().getRecordLockManager().getRecordAccessConditionsCache().get(pi);

            // Retrieve from Solr if record limits and access conditions not yet in cache
            if (limits == null || accessConditions == null) {
                SolrDocument doc = DataManager.getInstance()
                        .getSearchIndex()
                        .getFirstDoc(SolrConstants.PI + ":" + pi,
                                Arrays.asList(SolrConstants.ACCESSCONDITION, SolrConstants.ACCESSCONDITION_CONCURRENTUSE));
                if (doc == null) {
                    throw new RecordNotFoundException("Record not found: " + pi);
                }
                limits = SolrTools.getMetadataValues(doc, (SolrConstants.ACCESSCONDITION_CONCURRENTUSE));
                DataManager.getInstance().getRecordLockManager().getRecordLimitsCache().put(pi, limits);
                accessConditions = SolrTools.getMetadataValues(doc, (SolrConstants.ACCESSCONDITION));
                DataManager.getInstance().getRecordLockManager().getRecordAccessConditionsCache().put(pi, accessConditions);
            }

            // Lock limited view records, if limit exists and record has a license type that has this feature enabled
            if (limits != null && !limits.isEmpty() && AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(
                    accessConditions)) {
                if (session != null) {
                    DataManager.getInstance().getRecordLockManager().lockRecord(pi, session.getId(), Integer.valueOf(limits.get(0)));
                } else {
                    logger.debug("No session found, unable to lock limited view record {}", pi);
                    throw new RecordLimitExceededException(pi + ":" + limits.get(0));
                }
            }
        } catch (PresentationException | IndexUnreachableException | DAOException | RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossible due to " + e.getMessage());
        } catch (RecordLimitExceededException e) {
            throw new ServiceNotAllowedException("Concurrent views limit has been exceeded for record: " + pi);
        }
    }

    /**
     * <p>
     * Check if the request contains a size and region parameter (and is this a IIIF image request) and if so wether they describe a request for a
     * full image not larger than {@link Configuration#getThumbnailImageAccessMaxWidth()}.
     * </p>
     *
     * @param servletRequest The servlet request for the resource
     * @return true if the request is for a IIIF image resource which is considered a thumbnail
     */
    public static boolean isThumbnail(HttpServletRequest servletRequest) {

        String size = (String) servletRequest.getAttribute("iiif-size");
        String region = (String) servletRequest.getAttribute("iiif-region");

        if (StringUtils.isAnyBlank(size, region)) {
            return false;
        }

        int imageWidth = Integer.MAX_VALUE;
        try {
            Scale scale = Scale.getScaleMethod(size);
            imageWidth = Integer.parseInt(scale.getWidth());
        } catch (NumberFormatException | IllegalRequestException | NullPointerException | ServiceNotImplementedException e) {
            //no image width, assume large image
        }

        return "full".equalsIgnoreCase(region) && imageWidth <= DataManager.getInstance().getConfiguration().getThumbnailImageAccessMaxWidth();
    }
}
