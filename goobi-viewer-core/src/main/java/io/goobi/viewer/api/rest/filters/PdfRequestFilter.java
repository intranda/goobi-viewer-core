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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentExceptionMapper.ErrorMessage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.WatermarkHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * <p>
 * Request filter for PDF download requests. Checks whether the request has privileges to access the pdf and whether the download quote for the pdf is
 * reached
 * </p>
 */
@Provider
@ContentServerPdfBinding
public class PdfRequestFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(PdfRequestFilter.class);

    private static final String ATTRIBUTE_PDF_QUOTA = "pdf_quota";
    private static final String INSUFFICIENT_QUOTA_PREFIX = "Insufficient download quota for record '";

    @Context
    private HttpServletRequest servletRequest;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        try {

            if (DataManager.getInstance().getConfiguration().isPdfApiDisabled()) {
                throw new ServiceNotAllowedException("PDF API is disabled");
            }

            //Path requestPath = Paths.get(request.getUriInfo().getPath());
            //            String requestPath = request.getUriInfo().getPath();

            String pi = null;
            String divId = null;
            String imageName = null;
            String privName = IPrivilegeHolder.PRIV_DOWNLOAD_PDF;
            if (servletRequest.getAttribute("pi") != null) {
                pi = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_PI);
                divId = (String) servletRequest.getAttribute(FilterTools.ATTRIBUTE_LOGID);
                if (servletRequest.getAttribute("filename") != null) {
                    imageName = (String) servletRequest.getAttribute("filename");
                    // Check different privilege between born digital and page PDF files
                    if (imageName != null && imageName.toLowerCase().endsWith(".pdf")) {
                        privName = IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES;
                    } else {
                        privName = IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF;
                    }
                }
            }
            filterForAccessConditions(pi, divId, privName);
            filterForDownloadQuota(pi, divId, imageName, servletRequest);
            addRequestParameters(pi, divId, imageName, request);
        } catch (ServiceNotAllowedException | IndexUnreachableException | PresentationException | DAOException e) {
            String mediaType = MediaType.APPLICATION_JSON;
            Response response = Response.status(Status.FORBIDDEN).type(mediaType).entity(new ErrorMessage(Status.FORBIDDEN, e, false)).build();
            request.abortWith(response);
        }
    }

    /**
     * Set watermarkText and watermarkId properties to request object.
     *
     * @param pi
     * @param divId
     * @param imageName
     * @param request
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private static void addRequestParameters(String pi, String divId, String imageName, ContainerRequestContext request)
            throws IndexUnreachableException, PresentationException, DAOException {
        if (StringUtils.isNotBlank(pi)) {
            WatermarkHandler watermarkHandler = BeanUtils.getImageDeliveryBean().getPdf().getWatermarkHandler();
            StructElement topDocument = Optional.ofNullable(DataManager.getInstance().getSearchIndex().getDocumentByPI(pi))
                    .map(StructElement::create)
                    .orElse(null);
            if (topDocument != null) {
                watermarkHandler
                        .getFooterIdIfExists(topDocument)
                        .ifPresent(footerId -> request.setProperty("param:watermarkId", footerId));
                if (StringUtils.isNotBlank(imageName)) {
                    String actualImageName = getFirstImageName(imageName);
                    Optional<PhysicalElement> page = Optional.ofNullable(AbstractPageLoader.create(topDocument).getPageForFileName(actualImageName));
                    page.flatMap(watermarkHandler::getWatermarkTextIfExists)
                            .ifPresent(text -> request.setProperty("param:watermarkText", text));

                    page
                            .ifPresent(p -> {

                                Path indexedSourceFile = Paths.get(DataFileTools.getSourceFilePath(p.getPi() + ".xml", p.getDataRepository(),
                                        (actualImageName != null && topDocument.getSourceDocFormat() != null) ? topDocument.getSourceDocFormat()
                                                : SolrConstants.SOURCEDOCFORMAT_METS));
                                if (Files.exists(indexedSourceFile)) {
                                    request.setProperty("param:metsFile", indexedSourceFile.toUri());
                                }

                            });

                } else {
                    Optional.ofNullable(DataManager.getInstance().getSearchIndex().getDocumentByPIAndLogId(pi, divId))
                            .map(StructElement::create)
                            .flatMap(watermarkHandler::getWatermarkTextIfExists)
                            .ifPresent(text -> request.setProperty("param:watermarkText", text));
                }
            }
        }
    }

    /**
     * If the imageName is actually a list of names, return the first name.
     *
     * @param imageName
     * @return First image name
     */
    private static String getFirstImageName(String imageName) {
        if (imageName.contains("$")) {
            return imageName.split("$")[0];
        }
        return imageName;
    }

    /**
     *
     * @param pi Record identifiers
     * @param divId Structure element ID
     * @param contentFileName
     * @param request Servlet request
     * @throws ServiceNotAllowedException
     */
    void filterForDownloadQuota(String pi, String divId, String contentFileName, HttpServletRequest request)
            throws ServiceNotAllowedException {
        logger.trace("filterForDownloadQuota({}, {}, {})", pi, divId, contentFileName);
        try {
            int percentage = AccessConditionUtils.getPdfDownloadQuotaForRecord(pi);
            logger.trace("percentage: {}", percentage);
            // IF 100% allowed, skip all further checks
            if (percentage == 100) {
                return;
            }

            // Full record PDF
            if (StringUtils.isEmpty(divId) && StringUtils.isEmpty(contentFileName)) {
                throw new ServiceNotAllowedException(INSUFFICIENT_QUOTA_PREFIX + pi + "': " + percentage + "%");
            }

            int numTotalRecordPages = (int) DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount("+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.DOCTYPE + ":PAGE");

            if (StringUtils.isNotEmpty(divId) && StringUtils.isEmpty(contentFileName)) {
                // Chapter PDF
                String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.LOGID + ":" + divId + " +" + SolrConstants.DOCTYPE
                        + ":PAGE";
                SolrDocumentList docs = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")),
                                Arrays.asList(SolrConstants.ORDER, SolrConstants.FILENAME));
                logger.trace(query);
                if (docs.isEmpty()) {
                    throw new RecordNotFoundException("Document not found: " + pi + "/" + divId);
                }
                // Check each page that belongs to the requested docstruct
                for (SolrDocument doc : docs) {
                    String fileName = (String) doc.getFieldValue(SolrConstants.FILENAME);
                    if (StringUtils.isEmpty(fileName)) {
                        logger.error("File name not found for page belonging to {}/{}", pi, divId);
                    }
                    if (!checkPageAllowed(pi, fileName, percentage, numTotalRecordPages, request)) {
                        logger.trace("Insufficient download quota");
                        throw new ServiceNotAllowedException(INSUFFICIENT_QUOTA_PREFIX + pi + "': " + percentage + "%");
                    }
                }
            } else if (StringUtils.isEmpty(divId) && StringUtils.isNotEmpty(contentFileName)) {
                // Page PDF
                if (!checkPageAllowed(pi, contentFileName, percentage, numTotalRecordPages, request)) {
                    logger.trace("Insufficient download quota");
                    throw new ServiceNotAllowedException(INSUFFICIENT_QUOTA_PREFIX + pi + "': " + percentage + "%");
                }
            }
        } catch (PresentationException | IndexUnreachableException | DAOException | RecordNotFoundException e) {
            logger.error(e.getMessage());
            throw new ServiceNotAllowedException(e.getMessage());
        }
    }

    /**
     *
     * @param pi Record identifier
     * @param pageFile Page file name
     * @param percentage Allowed percentage of pages for PDF download
     * @param numTotalRecordPages
     * @param request HTTP servlet request object
     * @return true if page allowed as part of the quota; false otherwise
     * @should return false if session unavailable
     * @should return false if no session attribute exists yet
     * @should return true if page already part of quota
     * @should return false if quota already filled
     * @should return true and add page to map if quota not yet filled
     */
    @SuppressWarnings("unchecked")
    static boolean checkPageAllowed(String pi, String pageFile, int percentage, int numTotalRecordPages, HttpServletRequest request) {
        logger.trace("checkPageAllowed({}, {}, {}, {})", pi, pageFile, percentage, numTotalRecordPages);
        if (request == null || request.getSession() == null) {
            logger.trace("session not found");
            return false;
        }
        try {
            Map<String, Set<String>> quotaMap = (Map<String, Set<String>>) request.getSession().getAttribute(ATTRIBUTE_PDF_QUOTA);
            if (quotaMap == null) {
                quotaMap = new HashMap<>();
                request.getSession().setAttribute(ATTRIBUTE_PDF_QUOTA, quotaMap);
            }
            if (quotaMap.get(pi) == null) {
                quotaMap.put(pi, new HashSet<>());
            }
            // Page already allowed as part of the quota
            if (quotaMap.get(pi).contains(pageFile)) {
                logger.trace("Page {} already allowed for {}", pageFile, pi);
                return true;
            }
            // Quota already filled and requested page is not part of it
            int allowedPages = getNumAllowedPages(percentage, numTotalRecordPages);
            logger.trace("Allowed pages for {}: {}", pi, allowedPages);
            if (quotaMap.get(pi).size() >= allowedPages) {
                logger.trace("Quota already filled");
                return false;
            }
            // Add file to quotas
            quotaMap.get(pi).add(pageFile);
            logger.trace("Page {} allowed for {} and added to map", pageFile, pi);
            return true;
        } catch (ClassCastException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Calculates the maximum number of allowed pages from the total number of pages for a record and the given percentage.
     *
     * @param percentage Allowed percentage for downloads
     * @param numTotalRecordPages Total record pages
     * @return Maximum number of pages for the given percentage
     * @should return 0 if percentage 0
     * @should return 0 if number of pages 0
     * @should return number of pages if percentage 100
     * @should calculate number correctly
     *
     */
    static int getNumAllowedPages(int percentage, int numTotalRecordPages) {
        if (percentage < 0) {
            throw new IllegalArgumentException("percentage may not be less than 0");
        }
        if (numTotalRecordPages < 0) {
            throw new IllegalArgumentException("numTotalRecordPages may not be less than 0");
        }
        if (numTotalRecordPages == 0 || percentage == 0) {
            return 0;
        }
        if (percentage == 100) {
            return numTotalRecordPages;
        }

        return (int) Math.floor(((double) numTotalRecordPages / 100 * percentage));
    }

    /**
     * @param pi
     * @param divId
     * @param privName
     * @throws ServiceNotAllowedException
     * @throws IndexUnreachableException
     */
    private void filterForAccessConditions(String pi, String divId, String privName) throws ServiceNotAllowedException {
        logger.trace("filterForAccessConditions: session:{} pi:{} priv:{}", servletRequest.getSession().getId(), pi, privName);
        AccessPermission access = AccessPermission.denied();
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, divId, privName, servletRequest);
            if (access.isGranted() && access.isTicketRequired() && !AccessConditionUtils.isHasDownloadTicket(pi, servletRequest.getSession())) {
                logger.trace("Agent has no download ticket for PI: {}", pi);
                access.setGranted(false);
            }
        } catch (IndexUnreachableException | DAOException e) {
            throw new ServiceNotAllowedException("Serving this image is currently impossibe due to ");
        } catch (RecordNotFoundException e) {
            throw new ServiceNotAllowedException("Record not found in index: " + pi);
        }

        if (!access.isGranted()) {
            logger.trace("Access denied for {}/{}", pi, privName);
            throw new ServiceNotAllowedException("Serving this content is restricted due to access conditions");
        }
    }

}
