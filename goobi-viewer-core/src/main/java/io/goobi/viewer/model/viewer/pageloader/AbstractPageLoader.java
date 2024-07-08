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
package io.goobi.viewer.model.viewer.pageloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.PhysicalElementBuilder;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * <p>
 * Abstract AbstractPageLoader class.
 * </p>
 */
public abstract class AbstractPageLoader implements IPageLoader {

    private static final long serialVersionUID = 7546256768016555405L;

    private static final Logger logger = LogManager.getLogger(AbstractPageLoader.class);

    /** All fields to be fetched when loading page documents. Any new required fields must be added to this array. */
    protected static final String[] FIELDS = { SolrConstants.PI_TOPSTRUCT, SolrConstants.PHYSID, SolrConstants.ORDER, SolrConstants.ORDERLABEL,
            SolrConstants.IDDOC_OWNER, SolrConstants.MIMETYPE, SolrConstants.FILEIDROOT, SolrConstants.FILENAME, SolrConstants.FILENAME_ALTO,
            SolrConstants.FILENAME_FULLTEXT, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.FILENAME + "_JPEG", SolrConstants.FILENAME_MPEG,
            SolrConstants.FILENAME_MPEG3, SolrConstants.FILENAME_MP4, SolrConstants.FILENAME_OGG, SolrConstants.FILENAME + "_TIFF",
            SolrConstants.FILENAME_WEBM, SolrConstants.FULLTEXTAVAILABLE, SolrConstants.DATAREPOSITORY, SolrConstants.IMAGEURN, SolrConstants.WIDTH,
            SolrConstants.HEIGHT, SolrConstants.ACCESSCONDITION, SolrConstants.MDNUM_FILESIZE, SolrConstants.BOOL_IMAGEAVAILABLE,
            SolrConstants.BOOL_DOUBLE_IMAGE };

    /**
     * Creates and returns the appropriate loader instance for the given <code>StructElement</code>. Only creates loaders that load pages.
     *
     * @param topStructElement Top level <code>StructElement</code> of the record
     * @return Appropriate page loader implementation for the given record topStructElement
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public static AbstractPageLoader create(StructElement topStructElement) throws IndexUnreachableException, PresentationException, DAOException {
        return create(topStructElement, true);
    }

    /**
     * Creates and returns the appropriate loader instance for the given <code>StructElement</code>.
     *
     * @param topStructElement Top level <code>StructElement</code> of the record
     * @param loadPages If true, created an appropriate page loader; if false (e.g. for TOC building), create a dummy loader
     * @return Appropriate page loader implementation for the given record topStructElement
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @should return EagerPageLoader if page count below threshold
     * @should return LeanPageLoder if page count at or above threshold
     */
    public static AbstractPageLoader create(StructElement topStructElement, boolean loadPages)
            throws IndexUnreachableException, PresentationException, DAOException {
        if (!loadPages) {
            // Page loader that skips loading any pages for speed (e.g. TOC creation via REST)
            return new EmptyPageLoader(topStructElement);
        }

        return create(topStructElement, Collections.emptyList());
    }

    /**
     * 
     * @param topStructElement
     * @param pageNosToLoad List of page numbers to load; empty list means all pages
     * @return Appropriate page loader implementation for the given record topStructElement
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public static AbstractPageLoader create(StructElement topStructElement, List<Integer> pageNosToLoad)
            throws IndexUnreachableException, PresentationException, DAOException {
        int numPages = topStructElement.getNumPages();
        if (pageNosToLoad.isEmpty() && numPages < DataManager.getInstance().getConfiguration().getPageLoaderThreshold()) {
            return new EagerPageLoader(topStructElement);
        }
        logger.debug("Record has {} pages, using a lean page loader to limit memory usage.", numPages);
        return new LeanPageLoader(topStructElement, numPages);
    }

    /**
     * Replaces the static variable placeholders (the ones that don't change depending on the page) of the given label format with values.
     *
     * @param format a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @should replace numpages correctly
     * @should replace message keys correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    protected String buildPageLabelTemplate(String format, Locale locale) throws IndexUnreachableException {
        if (format == null) {
            throw new IllegalArgumentException("format may not be null");
        }
        String labelTemplate = format.replace("{numpages}", String.valueOf(getNumPages()));
        Pattern p = Pattern.compile("\\{msg\\..*?\\}");
        Matcher m = p.matcher(labelTemplate);
        while (m.find()) {
            String key = labelTemplate.substring(m.start() + 5, m.end() - 1);
            labelTemplate = labelTemplate.replace(labelTemplate.substring(m.start(), m.end()), ViewerResourceBundle.getTranslation(key, locale));
        }
        return labelTemplate;
    }

    /**
     * <p>
     * loadPage.
     * </p>
     *
     * @param topElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static PhysicalElement loadPage(StructElement topElement, int page) throws PresentationException, IndexUnreachableException {
        if (topElement.isAnchor() || topElement.isGroup()) {
            logger.debug("Anchor or group document, no pages.");
            return null;
        }

        String pi = topElement.getPi();
        logger.trace("Loading pages for '{}'...", pi);
        List<String> fields = new ArrayList<>(Arrays.asList(FIELDS));

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("+")
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(topElement.getPi())
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE)
                .append(" +")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(page);
        SolrDocumentList result = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), fields);
        if (result == null || result.isEmpty()) {
            return null;
        }

        return loadPageFromDoc(result.get(0), pi, topElement, null);
    }

    /**
     * <p>
     * loadPageFromDoc.
     * </p>
     *
     * @param doc Solr document from which to construct the page
     * @param pi Record identifier
     * @param topElement StructElement of the top record element
     * @param pageOwnerIddocMap Optional map containing relationships between pages and owner IDDOCs
     * @return Constructed PhysicalElement
     */
    protected static PhysicalElement loadPageFromDoc(SolrDocument doc, String pi, StructElement topElement, Map<Integer, Long> pageOwnerIddocMap) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        // PHYSID
        String physId = "";
        if (doc.getFieldValue(SolrConstants.PHYSID) != null) {
            physId = (String) doc.getFieldValue(SolrConstants.PHYSID);
        }
        // ORDER
        int order = (Integer) doc.getFieldValue(SolrConstants.ORDER);
        // ORDERLABEL
        String orderLabel = "";
        if (doc.getFieldValue(SolrConstants.ORDERLABEL) != null) {
            orderLabel = (String) doc.getFieldValue(SolrConstants.ORDERLABEL);
        }
        // IDDOC_OWNER
        if (doc.getFieldValue(SolrConstants.IDDOC_OWNER) != null && pageOwnerIddocMap != null) {
            String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC_OWNER);
            pageOwnerIddocMap.put(order, Long.valueOf(iddoc));
        }
        // Mime type
        String mimeType = null;
        if (doc.getFieldValue(SolrConstants.MIMETYPE) != null) {
            mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
        }
        // Main file name
        String fileName = "";
        if (doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED) != null) {
            fileName = (String) doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
        } else if (doc.getFieldValue(SolrConstants.FILENAME) != null) {
            fileName = (String) doc.getFieldValue(SolrConstants.FILENAME);
        }

        String dataRepository = "";
        if (doc.getFieldValue(SolrConstants.DATAREPOSITORY) != null) {
            dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
        } else if (topElement != null) {
            dataRepository = topElement.getDataRepository();
        }

        // URN
        String urn = "";
        if (doc.getFieldValue(SolrConstants.IMAGEURN) != null && !doc.getFirstValue(SolrConstants.IMAGEURN).equals("NULL")) {
            urn = (String) doc.getFieldValue(SolrConstants.IMAGEURN);
        }
        StringBuilder sbPurlPart = new StringBuilder();
        sbPurlPart.append('/').append(pi).append('/').append(order).append('/');

        PhysicalElement pe = new PhysicalElementBuilder().setPi(pi)
                .setPhysId(physId)
                .setFilePath(fileName)
                .setOrder(order)
                .setOrderLabel(orderLabel)
                .setUrn(urn)
                .setPurlPart(sbPurlPart.toString())
                .setMimeType(mimeType)
                .setDataRepository(dataRepository)
                .build();

        if (doc.getFieldValue(SolrConstants.WIDTH) != null) {
            pe.setWidth((Integer) doc.getFieldValue(SolrConstants.WIDTH));
        }
        if (doc.getFieldValue(SolrConstants.HEIGHT) != null) {
            pe.setHeight((Integer) doc.getFieldValue(SolrConstants.HEIGHT));
        }

        // Full-text filename
        pe.setFulltextFileName((String) doc.getFirstValue(SolrConstants.FILENAME_FULLTEXT));
        // ALTO filename
        pe.setAltoFileName((String) doc.getFirstValue(SolrConstants.FILENAME_ALTO));
        // TIFF filename
        pe.setFilePathTiff((String) doc.getFirstValue(SolrConstants.FILENAME + "_TIFF"));
        // JPEG filename
        pe.setFilePathJpeg((String) doc.getFirstValue(SolrConstants.FILENAME + "_JPEG"));

        // Access conditions
        if (doc.getFieldValues(SolrConstants.ACCESSCONDITION) != null) {
            for (Object o : doc.getFieldValues(SolrConstants.ACCESSCONDITION)) {
                String accessCondition = (String) o;
                if (StringUtils.isNotEmpty(accessCondition)) {
                    pe.getAccessConditions().add(accessCondition);
                }
            }
        }

        // File names for different formats (required for A/V)
        String filenameRoot = new StringBuilder(SolrConstants.FILENAME).append('_').toString();
        for (String fieldName : doc.getFieldNames()) {
            if (fieldName.startsWith(filenameRoot)) {
                // logger.trace("Format: {}", fieldName); //NOSONAR Debug
                String format = fieldName.split("_")[1].toLowerCase();
                String value = (String) doc.getFieldValue(fieldName);
                pe.getFileNames().put(format, value);
            }
        }

        // METS file ID root
        if (doc.getFieldValue(SolrConstants.FILEIDROOT) != null) {
            pe.setFileIdRoot((String) doc.getFieldValue(SolrConstants.FILEIDROOT));
        }

        // File size
        if (doc.getFieldValue("MDNUM_FILESIZE") != null) {
            pe.setFileSize((long) doc.getFieldValue("MDNUM_FILESIZE"));
        }

        // Full-text available
        if (doc.containsKey(SolrConstants.FULLTEXTAVAILABLE)) {
            pe.setFulltextAvailable((boolean) doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE));
        }

        // Image available
        if (doc.containsKey(SolrConstants.BOOL_IMAGEAVAILABLE)) {
            pe.setHasImage((boolean) doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE));
        }

        // Double page view
        if (doc.containsKey(SolrConstants.BOOL_DOUBLE_IMAGE)) {
            pe.setDoubleImage((boolean) doc.getFieldValue(SolrConstants.BOOL_DOUBLE_IMAGE));
        }

        return pe;
    }

    /**
     *
     * @param labelTemplate Label template with placeholders
     * @param pageNo Page number
     * @param orderLabel Page label
     * @param nextPageNo Optional next page number
     * @param nextOderLabel Optional next page label
     * @return {@link SelectItem}
     * @should construct single page item correctly
     * @should construct double page item correctly
     */
    protected static SelectPageItem buildPageSelectItem(String labelTemplate, int pageNo, String orderLabel, Integer nextPageNo,
            String nextOderLabel) {
        if (labelTemplate == null) {
            throw new IllegalArgumentException("labelTemplate may not be null");
        }

        SelectPageItem si = new SelectPageItem();
        if (nextPageNo != null && nextOderLabel != null) {
            si.setLabel(labelTemplate.replace("{order}", pageNo + "-" + nextPageNo)
                    .replace("{orderlabel}", orderLabel + " - " + nextOderLabel));
            si.setValue(pageNo + "-" + nextPageNo);
        } else {
            si.setLabel(labelTemplate.replace("{order}", String.valueOf(pageNo)).replace("{orderlabel}", orderLabel));
            si.setValue(String.valueOf(pageNo));
        }

        return si;
    }
}
