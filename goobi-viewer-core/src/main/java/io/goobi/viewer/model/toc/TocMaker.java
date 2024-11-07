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
package io.goobi.viewer.model.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * TocMaker class.
 * </p>
 */
public final class TocMaker {

    private static final Logger logger = LogManager.getLogger(TocMaker.class);

    private static final String[] REQUIRED_FIELDS = { SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT, SolrConstants.DATAREPOSITORY,
            SolrConstants.DOCSTRCT, SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT, SolrConstants.ISANCHOR, SolrConstants.ISWORK,
            SolrConstants.LABEL, SolrConstants.LOGID, SolrConstants.MIMETYPE, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.THUMBNAIL,
            SolrConstants.THUMBPAGENO, SolrConstants.THUMBPAGENOLABEL, SolrConstants.TITLE };

    private static final int ANCHOR_THUMBNAIL_HEIGHT = 60;
    private static final int ANCHOR_THUMBNAIL_WIDTH = 50;

    private static Pattern patternVolumeLabel = Pattern.compile(StringTools.REGEX_BRACES);

    /** Private constructor. */
    private TocMaker() {
        //
    }

    /**
     * Returns a list of fields to be used as the field filter for Solr queries. The list constists of statically defined fields in REQUIRED_FIELDS
     * and any additional fields configured for the TOC label.
     *
     * @should return both static and configured fields
     * @param template a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    protected static List<String> getSolrFieldsToFetch(String template) {
        logger.trace("getSolrFieldsToFetch: {}", template);
        Set<String> ret = new HashSet<>(Arrays.asList(REQUIRED_FIELDS));

        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration(template);
        if (metadataList != null && !metadataList.isEmpty()) {
            for (MetadataParameter param : metadataList.get(0).getParams()) {
                if (StringUtils.isNotEmpty(param.getKey())) {
                    ret.add(param.getKey());
                    ret.add(param.getKey() + "_LANG_EN");
                    ret.add(param.getKey() + "_LANG_DE");
                    ret.add(param.getKey() + "_LANG_FR");
                    ret.add(param.getKey() + "_LANG_ES");
                    ret.add(param.getKey() + "_LANG_PT");
                    ret.add(param.getKey() + "_LANG_HR");
                    ret.add(param.getKey() + "_LANG_AR");
                    // TODO Add all available language versions
                }
            }
        }
        // Add ancestor identifier fields to the required fields list
        List<String> ancestorFields = DataManager.getInstance().getConfiguration().getAncestorIdentifierFields();
        if (ancestorFields != null) {
            ret.addAll(ancestorFields);
        }

        return new ArrayList<>(ret);
    }

    /**
     * Generate the TOC.
     *
     * @param toc The TOC object (only required to set the number of volumes for anchor TOCs.
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param addAllSiblings If true and <code>structElement</code> has a parent, other siblings will be listed as well and can be navigated.
     * @param mimeType Mime type determines the target URL of the TOC element.
     * @param tocCurrentPage Current page of a paginated TOC.
     * @param hitsPerPage Hits per page of a paginated TOC.
     * @should generate volume TOC with siblings correctly
     * @should generate volume TOC without siblings correctly
     * @should generate anchor TOC correctly
     * @should paginate anchor TOC correctly
     * @should throw IllegalArgumentException if structElement is null
     * @should throw IllegalArgumentException if toc is null
     * @return a {@link java.util.LinkedHashMap} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static Map<String, List<TOCElement>> generateToc(TOC toc, StructElement structElement, boolean addAllSiblings, String mimeType,
            int tocCurrentPage, int hitsPerPage) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("generateToc");
        if (structElement == null) {
            throw new IllegalArgumentException("structElement may not me null");
        }
        if (toc == null) {
            throw new IllegalArgumentException("toc may not me null");
        }

        logger.trace("generateToc: {}", structElement.getPi());
        LinkedHashMap<String, List<TOCElement>> ret = new LinkedHashMap<>();
        ret.put(StringConstants.DEFAULT_NAME, new ArrayList<>());

        // TODO Allow METS_MARC once PDF generation from MARCXML supported
        boolean sourceFormatPdfAllowed = SolrConstants.SOURCEDOCFORMAT_METS.equals(structElement.getSourceDocFormat());
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(new StringBuilder(SolrConstants.IDDOC).append(':').append(structElement.getLuceneId()).toString(),
                        getSolrFieldsToFetch(structElement.getDocStructType()));
        if (doc == null) {
            return ret;
        }

        if (structElement.isGroup()) {
            // Group
            int level = 0;
            // Try LABEL first (should equal MD_TITLE or MD_SERIESTITLE)
            IMetadataValue label = new SimpleMetadataValue(structElement.getLabel());
            // Shelfmark fallback
            if (StringUtils.isEmpty(label.toString())) {
                label.setValue(structElement.getMetadataValue("MD_SHELFMARK"));
            }
            // PI fallback
            if (StringUtils.isEmpty(label.toString())) {
                label.setValue(structElement.getPi());
            }
            String footerId = getFooterId(doc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
            String docstruct = "_GROUPS";
            ret.get(StringConstants.DEFAULT_NAME)
                    .add(new TOCElement(label, null, null, String.valueOf(structElement.getLuceneId()), null, level, structElement.getPi(), null,
                            false, true, false, mimeType, docstruct, footerId));
            // ++level;
            buildGroupToc(ret.get(StringConstants.DEFAULT_NAME), DataManager.getInstance().getConfiguration().getRecordGroupIdentifierFields(),
                    structElement.getPi(), sourceFormatPdfAllowed, mimeType);
        } else if (structElement.isAnchor()) {
            // MultiVolume
            int numVolumes = buildAnchorToc(ret, doc, sourceFormatPdfAllowed, mimeType, tocCurrentPage, hitsPerPage);
            toc.setTotalTocSize(numVolumes);
            toc.setCurrentPage(tocCurrentPage);
        } else {
            // Stand-alone or volume
            ret.put(StringConstants.DEFAULT_NAME, buildToc(doc, structElement, addAllSiblings, mimeType, sourceFormatPdfAllowed));
        }

        logger.trace("generateToc end: {} groups, {} elements in DEFAULT", ret.size(), ret.get(StringConstants.DEFAULT_NAME).size());
        return ret;
    }

    /**
     * Builds a TOC tree for non-anchor and non-group documents. Adds clickable sibling elements, if so requested.
     *
     * @param doc
     * @param structElement
     * @param addAllSiblings
     * @param mimeType
     * @param sourceFormatPdfAllowed
     * @return List<TOCElement>
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private static List<TOCElement> buildToc(SolrDocument doc, StructElement structElement, boolean addAllSiblings, String mimeType,
            boolean sourceFormatPdfAllowed) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("buildToc");
        List<List<TOCElement>> ret = new ArrayList<>();

        int level = 0;
        List<String> mainDocumentChain = new ArrayList<>();
        mainDocumentChain.add((String) doc.getFieldValue(SolrConstants.IDDOC));

        //                if (structElement.isGroupMember()) {
        //                    // If this record belongs to groups, start with the groups
        //                    for (String groupIdField : structElement.getGroupMemberships().keySet()) {
        //                        logger.debug("adding toc element: " + groupIdField);
        //                        ret.add(new TocElementFlat(structElement.getGroupMemberships().get(groupIdField),
        //                        null, null, String.valueOf(structElement
        //                                .getLuceneId()), false, null, level, structElement.getGroupMemberships().get(groupIdField), null, null,
        //                                sourceFormatPdfAllowed, true));
        //
        //                    }
        //                    ++level;
        //                }

        List<String> ancestorFields = DataManager.getInstance().getConfiguration().getAncestorIdentifierFields();
        if (!ancestorFields.contains(SolrConstants.PI_PARENT)) {
            // Always support anchors in the TOC tree
            ancestorFields.add(0, SolrConstants.PI_PARENT);
        }

        // int mainRecordLevel = 0; // currently not in use
        for (String ancestorField : ancestorFields) {
            logger.trace("ancestor field: {}", ancestorField);
            // Collect ancestor hierarchy in a list
            List<SolrDocument> ancestorList = new ArrayList<>();
            SolrDocument currentDoc = doc;
            String queryField = SolrConstants.PI;
            if (ancestorField.startsWith(SolrConstants.IDDOC)) {
                queryField = SolrConstants.IDDOC;
            }
            while (currentDoc != null && currentDoc.getFieldValues(ancestorField) != null) {
                StringBuilder sbQuery = new StringBuilder(queryField).append(':').append(currentDoc.getFieldValues(ancestorField).iterator().next());
                logger.trace("Ancestor query: {}", sbQuery);
                // Get parent doc
                currentDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(sbQuery.toString(), null);
                if (currentDoc != null) {
                    ancestorList.add(currentDoc);
                }
            }

            List<TOCElement> tree = new ArrayList<>();
            String footerId = getFooterId(structElement, DataManager.getInstance().getConfiguration().getWatermarkIdField());
            if (!ancestorList.isEmpty()) {
                // Add ancestors, if found
                // mainRecordLevel += ancestorList.size();
                for (SolrDocument ancestor : ancestorList) {
                    mainDocumentChain.add((String) ancestor.getFieldValue(SolrConstants.IDDOC));
                }
                //                mainDocumentChain.addAll(ancestorList);
                SolrDocument topAncestor = ancestorList.get(ancestorList.size() - 1);
                populateTocTree(tree, mainDocumentChain, topAncestor, level, true, sourceFormatPdfAllowed, mimeType, ancestorField, addAllSiblings,
                        footerId);
            } else {
                // No ancestors found, just populate the main record TOC
                populateTocTree(tree, mainDocumentChain, doc, level, true, sourceFormatPdfAllowed, mimeType, ancestorField, addAllSiblings, footerId);
            }
            ret.add(tree);
        }

        // Return the largest TOC tree
        List<TOCElement> bestTree = null;

        for (List<TOCElement> tree : ret) {
            if (bestTree == null || tree.size() > bestTree.size()) {
                bestTree = tree;
            }
        }

        return bestTree;
    }

    /**
     * A group is a series, etc.
     *
     * @param ret
     * @param groupIdFields
     * @param groupIdValue
     * @param sourceFormatPdfAllowed
     * @param mimeType
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    private static void buildGroupToc(List<TOCElement> ret, List<String> groupIdFields, String groupIdValue, boolean sourceFormatPdfAllowed,
            String mimeType) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("addMembersToGroup: {}", groupIdValue);
        if (ret == null) {
            throw new IllegalArgumentException("ret may not be null");
        }
        if (groupIdFields == null) {
            throw new IllegalArgumentException("ret may not be null");
        }
        if (groupIdFields.isEmpty()) {
            logger.warn("No recordGroupIdentifierFields configured, cannot build group TOC.");
            return;
        }

        String template = "_GROUPS";
        List<String> returnFields = getSolrFieldsToFetch(template);
        returnFields.addAll(groupIdFields); // add all groupid fields to return list
        List<StringPair> sortFields = new ArrayList<>(groupIdFields.size());
        StringBuilder sbQuery = new StringBuilder(SearchHelper.ALL_RECORDS_QUERY).append(" +(");
        for (String groupIdField : groupIdFields) {
            sbQuery.append(' ')
                    .append(groupIdField)
                    .append(':')
                    .append(groupIdValue);

            String groupSortField = groupIdField.replace(SolrConstants.PREFIX_GROUPID, SolrConstants.PREFIX_GROUPORDER);
            sortFields.add(new StringPair(groupSortField, "asc"));
            returnFields.add(groupSortField); // add each sorting field to return list
        }
        sbQuery.append(')');
        logger.trace("Group TOC query: {}", sbQuery);

        SolrDocumentList groupMemberDocs = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, sortFields, returnFields);
        if (groupMemberDocs == null || groupMemberDocs.isEmpty()) {
            logger.trace("No group records found for {}", groupIdValue);
            return;
        }

        // Create a manually sorted map of docs, since the order can be contained in different GROUPORDER_* fields
        Map<Integer, SolrDocument> docOrderMap = createOrderedGroupDocMap(groupMemberDocs, groupIdFields, groupIdValue);

        HttpServletRequest request = BeanUtils.getRequest();
        for (int order : docOrderMap.keySet()) {
            SolrDocument doc = docOrderMap.get(order);
            // IMetadataValue label = new MultiLanguageMetadataValue(SolrSearchIndex.getMetadataValuesForLanguage(doc, SolrConstants.TITLE));
            IMetadataValue label = buildLabel(doc, template);
            String numberSort = doc.getFieldValue(SolrConstants.CURRENTNOSORT) != null
                    ? String.valueOf(doc.getFieldValue(SolrConstants.CURRENTNOSORT)) : null;
            String numberText =
                    doc.getFieldValue(SolrConstants.CURRENTNO) != null ? (String) doc.getFieldValue(SolrConstants.CURRENTNO) : numberSort;
            String volumeIddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
            String logId = (String) doc.getFieldValue(SolrConstants.LOGID);
            String topStructPi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            String thumbnailFile = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
            String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);

            if (label.isEmpty()) {
                if (StringUtils.isNotEmpty(numberText)) {
                    label.setValue(new StringBuilder(label.getValue().orElse("")).append(" (").append(numberText).append(')').toString());
                } else {
                    label = new SimpleMetadataValue("-");
                }
            }

            String footerId = getFooterId(doc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
            String thumbnailUrl = null;
            if (StringUtils.isNotEmpty(topStructPi) && StringUtils.isNotEmpty(thumbnailFile)) {
                ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
                StructElement struct = new StructElement(volumeIddoc, doc);
                thumbnailUrl = thumbs.getThumbnailUrl(struct, ANCHOR_THUMBNAIL_WIDTH, ANCHOR_THUMBNAIL_HEIGHT);
            }
            label.mapEach(StringEscapeUtils::unescapeHtml4);
            boolean accessPermissionPdf;
            try {
                accessPermissionPdf = sourceFormatPdfAllowed && AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi,
                        logId, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request).isGranted();
            } catch (RecordNotFoundException e) {
                logger.error("Record not found in index: {}", topStructPi);
                continue;
            }
            ret.add(new TOCElement(label, "1", null, volumeIddoc, logId, 1, topStructPi, thumbnailUrl, accessPermissionPdf, false,
                    thumbnailUrl != null, mimeType, docStructType, footerId));
        }
    }

    /**
     * Create a manually sorted map of docs, since the order can be contained in different GROUPORDER_* fields.
     *
     * @param groupMemberDocs
     * @param groupIdFields
     * @param groupIdValue
     * @return Map<Integer, SolrDocument>
     * @should create correctly sorted map
     */
    static Map<Integer, SolrDocument> createOrderedGroupDocMap(List<SolrDocument> groupMemberDocs, List<String> groupIdFields,
            String groupIdValue) {
        if (groupMemberDocs == null || groupMemberDocs.isEmpty()) {
            return Collections.emptyMap();
        }
        if (groupIdFields == null || groupIdFields.isEmpty()) {
            return Collections.emptyMap();
        }
        if (StringUtils.isEmpty(groupIdValue)) {
            return Collections.emptyMap();
        }

        Map<Integer, SolrDocument> ret = new TreeMap<>();
        int fallbackOrder = 0;
        groupMemberDocs = groupMemberDocs.stream().sorted((d1, d2) -> getLabel(d1).compareTo(getLabel(d2))).toList();
        for (SolrDocument doc : groupMemberDocs) {
            String groupIdField = null;
            for (String field : groupIdFields) {
                if (groupIdValue.equals(doc.getFieldValue(field))) {
                    groupIdField = field;
                    break;
                }
            }
            if (groupIdField == null) {
                logger.warn("Group ID field not found on IDDOC {}", doc.getFieldValue(SolrConstants.IDDOC));
                continue;
            }
            String groupSortField = groupIdField.replace(SolrConstants.PREFIX_GROUPID, SolrConstants.PREFIX_GROUPORDER);
            Integer order = (Integer) doc.getFieldValue(groupSortField);
            if (order == null) {
                logger.warn("No {} on group member {}", groupSortField, doc.getFieldValue("PI"));
                order = fallbackOrder++;
            }
            ret.put(order, doc);
        }

        return ret;
    }

    private static String getLabel(SolrDocument doc) {
        String label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL);
        if (StringUtils.isBlank(label)) {
            label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
        }
        return Optional.ofNullable(label).orElse("");
    }

    /**
     * Adds TOC elements for volumes that belong to the anchor document with the given IDDOC.
     *
     * @param ret
     * @param anchorDoc
     * @param sourceFormatPdfAllowed
     * @param mimeType
     * @param tocCurrentPage
     * @param hitsPerPage
     * @return Number of hits
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private static int buildAnchorToc(Map<String, List<TOCElement>> ret, SolrDocument anchorDoc, boolean sourceFormatPdfAllowed,
            final String mimeType, int tocCurrentPage, final int hitsPerPage) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("buildAnchorToc");
        String iddoc = (String) anchorDoc.getFieldValue(SolrConstants.IDDOC);
        String anchorDocstructType = (String) anchorDoc.getFieldValue(SolrConstants.DOCSTRCT);
        String logId = (String) anchorDoc.getFieldValue(SolrConstants.LOGID);
        String topStructPiLocal = (String) anchorDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

        if (tocCurrentPage < 1) {
            throw new IllegalArgumentException("page must be >=1");
        }

        String query = new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(iddoc).toString();
        int hits = (int) DataManager.getInstance().getSearchIndex().getHitCount(query);
        int offset = 0;
        int useHitsPerPage = hitsPerPage;
        if (useHitsPerPage <= 0) {
            useHitsPerPage = SolrSearchIndex.MAX_HITS;
        } else {
            // Using paginator
            offset = useHitsPerPage * (tocCurrentPage - 1);
            if (offset > hits) {
                offset = hits;
            }

        }

        List<String> volumeFieldList = getSolrFieldsToFetch("_VOLUMES");
        // Add TOC volume grouping field for the given volume docstruct type to the list of fields to return
        String tocGroupField = DataManager.getInstance().getConfiguration().getTocVolumeGroupFieldForTemplate(anchorDocstructType);
        if (tocGroupField != null) {
            volumeFieldList.add(tocGroupField);
            logger.trace("group field: {}", tocGroupField);
        }
        QueryResponse queryResponse = DataManager.getInstance()
                .getSearchIndex()
                .search(query, offset, useHitsPerPage,
                        DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(anchorDocstructType), null, volumeFieldList);
        if (queryResponse != null) {
            HttpServletRequest request = BeanUtils.getRequest();
            for (SolrDocument volumeDoc : queryResponse.getResults()) {
                String topStructPi = (String) volumeDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                // Skip volumes that may not be listed
                try {
                    if (FacesContext.getCurrentInstance() != null
                            && !AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi, null, IPrivilegeHolder.PRIV_LIST,
                                    request).isGranted()) {
                        continue;
                    }
                } catch (RecordNotFoundException e) {
                    logger.error("Record not found in index: {}", topStructPi);
                    continue;
                }
                // Determine the TOC group for this volume based on the grouping field, if configured
                String groupName = StringConstants.DEFAULT_NAME;
                if (tocGroupField != null) {
                    String groupValue = String.valueOf(volumeDoc.getFieldValue(tocGroupField));
                    if (StringUtils.isNotEmpty(groupValue)) {
                        groupName = groupValue;
                    }
                }
                //                logger.trace("group name: {}", groupName); //NOSONAR Debug
                List<TOCElement> groupList = ret.get(groupName);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    ret.put(groupName, groupList);
                }

                String volumeIddoc = (String) volumeDoc.getFieldValue(SolrConstants.IDDOC);
                String volumeLogId = (String) volumeDoc.getFieldValue(SolrConstants.LOGID);
                String thumbnailUrl = (String) volumeDoc.getFieldValue(SolrConstants.THUMBNAIL);
                String volumeMimeType = (String) volumeDoc.getFieldValue(SolrConstants.MIMETYPE);
                logger.trace("volume mime type: {}", volumeMimeType);

                ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
                StructElement struct = new StructElement(volumeIddoc, volumeDoc);
                thumbnailUrl = thumbs.getThumbnailUrl(struct, ANCHOR_THUMBNAIL_WIDTH, ANCHOR_THUMBNAIL_HEIGHT);

                String footerId = getFooterId(volumeDoc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
                String docStructType = (String) volumeDoc.getFieldValue(SolrConstants.DOCSTRCT);

                IMetadataValue volumeLabel = buildLabel(volumeDoc, docStructType);
                volumeLabel.mapEach(l -> StringEscapeUtils.unescapeHtml4(l));
                boolean accessPermissionPdf;
                try {
                    accessPermissionPdf = sourceFormatPdfAllowed && AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi,
                            volumeLogId, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request).isGranted();
                } catch (RecordNotFoundException e) {
                    logger.error("Record not found in index: {}", topStructPi);
                    continue;
                }
                TOCElement tocElement = new TOCElement(volumeLabel, "1", null, volumeIddoc, volumeLogId, 1, topStructPi, thumbnailUrl,
                        accessPermissionPdf, false, thumbnailUrl != null, volumeMimeType, docStructType, footerId);
                tocElement.getMetadata().put(SolrConstants.DOCSTRCT, docStructType);
                tocElement.getMetadata().put(SolrConstants.CURRENTNO, (String) volumeDoc.getFieldValue(SolrConstants.CURRENTNO));
                tocElement.getMetadata().put(SolrConstants.TITLE, (String) volumeDoc.getFirstValue(SolrConstants.TITLE));
                groupList.add(tocElement);
                logger.trace("TOC element added: {}", tocElement.getTopStructPi());

                // Collect group IDs to which this volume might belong
                List<String> groupIds = new ArrayList<>();
                for (String fieldName : volumeDoc.getFieldNames()) {
                    if (fieldName.startsWith(SolrConstants.PREFIX_GROUPID)) {
                        for (Object objValue : volumeDoc.getFieldValues(fieldName)) {
                            groupIds.add((String) objValue);
                        }
                    }
                }
                tocElement.setGroupIds(groupIds);
            }
        }

        // Add first volume's mime type to anchor
        String useMimeType = mimeType;
        if (!ret.isEmpty() && StringUtils.isEmpty(useMimeType)) {
            for (String key : ret.keySet()) {
                for (TOCElement tocElement : ret.get(key)) {
                    if (tocElement.getRecordMimeType() != null) {
                        useMimeType = tocElement.getRecordMimeType();
                        logger.trace("mime type found: {}", useMimeType);
                        break;
                    }
                    if (StringUtils.isNotEmpty(useMimeType)) {
                        break;
                    }
                }
            }
        }
        // Add anchor document
        IMetadataValue label = buildLabel(anchorDoc, (String) anchorDoc.getFirstValue(SolrConstants.DOCSTRCT));
        String footerId = getFooterId(anchorDoc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
        ret.get(StringConstants.DEFAULT_NAME)
                .add(0, new TOCElement(label, null, null, String.valueOf(iddoc), logId, 0, topStructPiLocal, null, sourceFormatPdfAllowed, true,
                        false, useMimeType, anchorDocstructType, footerId));

        return hits;
    }

    /**
     *
     * @param ret mainDocumentChain Solr documents that comprise the path from the top ancestor to the loaded record.
     * @param mainDocumentChain
     * @param doc
     * @param level
     * @param addChildren
     * @param sourceFormatPdfAllowed
     * @param mimeType
     * @param ancestorField
     * @param addAllSiblings
     * @param footerId
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private static void populateTocTree(List<TOCElement> ret, List<String> mainDocumentChain, SolrDocument doc, int level, boolean addChildren,
            boolean sourceFormatPdfAllowed, String mimeType, String ancestorField, boolean addAllSiblings, String footerId)
            throws PresentationException, IndexUnreachableException, DAOException {
        Map<String, List<SolrDocument>> childrenMap = new HashMap<>();
        String pi = (String) doc.getFieldValue(SolrConstants.PI);
        if (pi == null) {
            logger.error("No PI found for: {}", doc.getFieldValue(SolrConstants.IDDOC));
        }
        logger.trace("populateTocTree: {}; number of items in toc: {}", pi, ret.size());

        // Check PDF download permissions for all docstructs and save into map
        Map<String, AccessPermission> pdfPermissionMap = null;
        if (sourceFormatPdfAllowed && DataManager.getInstance().getConfiguration().isTocPdfEnabled()) {
            pdfPermissionMap =
                    AccessConditionUtils.checkAccessPermissionByIdentiferForAllLogids(pi, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, BeanUtils.getRequest());
        }

        // Real children (struct elements of the main record)
        String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
        if (mainDocumentChain != null && !mainDocumentChain.isEmpty() && iddoc.equals(mainDocumentChain.get(0))) {
            String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':')
                    .append(pi)
                    .append(" AND ")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.DOCSTRCT)
                    .append(" AND NOT(")
                    .append(SolrConstants.IDDOC)
                    .append(':')
                    .append(iddoc)
                    .append(')')
                    .toString();
            // logger.trace("Child doc query: {}", query); //NOSONAR Debug
            // TODO determine child docstruct type before fetching the child docs to determine the required fields
            SolrDocumentList docs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, 0, SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.THUMBPAGENO, "asc")), null,
                            null)
                    .getResults();
            logger.trace("Real children: {} (found: {})", query, docs.size());
            if (!docs.isEmpty()) {
                for (SolrDocument childDoc : docs) {
                    String iddocParent = (String) childDoc.getFieldValue(SolrConstants.IDDOC_PARENT);
                    if (iddocParent != null) {
                        List<SolrDocument> children = childrenMap.computeIfAbsent(iddocParent, k -> new ArrayList<>());
                        children.add(childDoc);
                    } else {
                        logger.warn("Document {} has no {}", childDoc.getFieldValue(SolrConstants.IDDOC), SolrConstants.IDDOC_PARENT);
                    }
                }
            }
        }

        // Add current doc and recursively build the tree from the children map
        addTocElementsRecusively(ret, childrenMap, doc, level, addChildren, pdfPermissionMap, mimeType, footerId);

        // Loosely referenced children (e.g. anchor volumes)
        if (StringUtils.isNotEmpty(ancestorField)) {
            String queryValue;
            if (ancestorField.startsWith(SolrConstants.IDDOC)) {
                queryValue = (String) doc.getFieldValue(SolrConstants.IDDOC);
            } else {
                queryValue = pi;
            }
            // logger.trace("sort {} by {}", SolrSearchIndex.getSingleFieldStringValue(doc, LuceneConstants.DOCSTRCT), //NOSONAR Debug
            //  DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate
            // (SolrSearchIndex.getSingleFieldStringValue(doc, LuceneConstants.DOCSTRCT)));
            // TODO determine child docstruct type before fetching the child docs to determine the required fields
            SolrDocumentList childDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .search(new StringBuilder(ancestorField).append(':').append(queryValue).toString(), SolrSearchIndex.MAX_HITS,
                            DataManager.getInstance()
                                    .getConfiguration()
                                    .getTocVolumeSortFieldsForTemplate(SolrTools.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT)),
                            null);
            boolean addSiblings = addAllSiblings && mainDocumentChain.contains(iddoc);
            logger.trace("Loose children of {}: {}; add siblings: {}", queryValue, childDocs.size(), addSiblings);
            if (!childDocs.isEmpty()) {
                for (SolrDocument childDoc : childDocs) {
                    // Add child, if either all siblings are requested or the path leads to the main record
                    if (addSiblings || mainDocumentChain.contains(childDoc.getFieldValue(SolrConstants.IDDOC))) {
                        populateTocTree(ret, mainDocumentChain, childDoc, level + 1, addChildren, sourceFormatPdfAllowed, mimeType, ancestorField,
                                addSiblings, footerId);
                    }
                }
            }
        }
    }

    /**
     *
     * @param ret
     * @param childrenMap
     * @param doc
     * @param level
     * @param addChildren
     * @param pdfPermissionMap
     * @param mimeType
     * @param footerId
     * @throws PresentationException
     */
    private static void addTocElementsRecusively(List<TOCElement> ret, Map<String, List<SolrDocument>> childrenMap, SolrDocument doc, int level,
            boolean addChildren, Map<String, AccessPermission> pdfPermissionMap, String mimeType, String footerId) throws PresentationException {
        String logId = (String) doc.getFieldValue(SolrConstants.LOGID);
        String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
        String docstructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
        String pageNo = null;
        if (doc.getFieldValue(SolrConstants.THUMBPAGENO) != null) {
            pageNo = String.valueOf(doc.getFieldValue(SolrConstants.THUMBPAGENO));
        }
        String pageNoLabel = "-";
        if (doc.getFieldValue(SolrConstants.THUMBPAGENOLABEL) != null) {
            pageNoLabel = (String) doc.getFieldValue(SolrConstants.THUMBPAGENOLABEL);
        }
        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        //        String sourceDocFormat = (String) doc.getFieldValue(LuceneConstants.SOURCEDOCFORMAT);
        boolean isAnchor = false;
        if (doc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) doc.getFieldValue(SolrConstants.ISANCHOR)) {
            isAnchor = true;
            // pi = (String) doc.getFieldValue(LuceneConstants.PI);
        }

        IMetadataValue label = buildLabel(doc, docstructType);
        boolean accessPermissionPdf = false;
        if (pdfPermissionMap != null && logId != null && pdfPermissionMap.get(logId) != null) {
            accessPermissionPdf = pdfPermissionMap.get(logId).isGranted();
        }
        TOCElement tocElement = new TOCElement(label, pageNo, pageNoLabel, iddoc, logId, level, pi, null, accessPermissionPdf, isAnchor,
                pageNo != null, mimeType, docstructType, footerId);
        tocElement.getMetadata().put(SolrConstants.DOCSTRCT, docstructType);
        tocElement.getMetadata().put(SolrConstants.CURRENTNO, (String) doc.getFieldValue(SolrConstants.CURRENTNO));
        tocElement.getMetadata().put("MD_TITLE", (String) doc.getFirstValue("MD_TITLE"));
        if (!ret.contains(tocElement)) {
            ret.add(tocElement);
            // logger.trace("TOC element added: {}/{}: '{}'; IDDOC:{}", ret.size() - 1, level, label, iddoc); //NOSONAR Debug

            // Child elements
            if (addChildren && childrenMap != null && childrenMap.get(iddoc) != null && !childrenMap.get(iddoc).isEmpty()) {
                // logger.trace("Adding {} children for {}", childrenMap.get(iddoc).size(), iddoc); //NOSONAR Debug
                for (SolrDocument childDoc : childrenMap.get(iddoc)) {
                    addTocElementsRecusively(ret, childrenMap, childDoc, level + 1, true, pdfPermissionMap, mimeType, footerId);
                }
            }
        }
    }

    /**
     * <p>
     * getFirstFieldValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param footerIdField a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @SuppressWarnings("rawtypes")
    public static String getFirstFieldValue(SolrDocument doc, String footerIdField) {
        Object object = doc.getFieldValue(footerIdField);
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        } else if (object instanceof List) {
            List list = (List) object;
            if (list.isEmpty()) {
                return null;
            }
            if (list.get(0) instanceof String) {
                return (String) list.get(0);
            }
        }
        throw new IllegalArgumentException("Unable to parse string result from " + object);
    }

    public static IMetadataValue buildTocElementLabel(SolrDocument doc) {
        String template = Optional.ofNullable(doc.getFieldValue(SolrConstants.DOCSTRCT)).orElse("").toString();
        return buildLabel(doc, template);
    }

    /**
     * Generates the label for this TOC element either from a configured layout or hardcoded old style.
     *
     * @param doc
     * @param template
     * @return {@link IMetadataValue} containing constructed label
     * @should build configured label correctly
     * @should fill remaining parameters correctly if docstruct fallback used
     */
    static IMetadataValue buildLabel(SolrDocument doc, String template) {
        // logger.trace("buildLabel: {}", template); //NOSONAR Debug
        List<Metadata> labelConfigList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration(template);
        IMetadataValue label = new MultiLanguageMetadataValue();

        // Fallback if no template found
        if (labelConfigList == null || labelConfigList.isEmpty()) {
            label.setValue(SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL));
            if (StringUtils.isEmpty(label.toString())) {
                label.setValue(SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE));
                if (StringUtils.isEmpty(label.toString())) {
                    label.setValue(SolrTools.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT));
                }
            }
            return label;
        }

        // Configurable label layout
        Metadata labelConfig = labelConfigList.get(0);
        for (MetadataParameter param : labelConfig.getParams()) {
            // logger.trace("param key: {}", param.getKey()); //NOSONAR Debug
            IMetadataValue value;
            switch (param.getType()) {
                case TRANSLATEDFIELD:
                    if (doc.getFirstValue(param.getKey()) != null) {
                        // Translate index field value, if available
                        value = ViewerResourceBundle.getTranslations(String.valueOf(doc.getFirstValue(param.getKey())));
                    } else if (param.getAltKey() != null && doc.getFirstValue(param.getAltKey()) != null) {
                        // Translate alternative index field value, if available
                        value = ViewerResourceBundle.getTranslations(String.valueOf(doc.getFirstValue(param.getAltKey())));
                    } else if (StringUtils.isNotBlank(param.getDefaultValue())) {
                        // Translate key, if no index field found
                        value = ViewerResourceBundle.getTranslations(param.getDefaultValue());
                    } else {
                        value = new SimpleMetadataValue();
                    }
                    break;
                case FIELD:
                    value = createMultiLanguageValue(doc, param.getKey(), param.getAltKey());
                    break;
                default:
                    value = new SimpleMetadataValue();
                    value.setValue(SolrTools.getSingleFieldStringValue(doc, param.getKey()));
                    // logger.trace("value: {}:{}", param.getKey(), value.getValue()); //NOSONAR Debug
                    break;
            }

            // Special case: If LABEL is missing, use DOCSTRCT.
            if (StringUtils.isEmpty(value.toString())
                    && (SolrConstants.LABEL.equals(param.getKey()) || SolrConstants.LABEL.equals(param.getAltKey()))) {
                // Docstruct fallback should always be translated
                String docstruct = SolrTools.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT);
                value = ViewerResourceBundle.getTranslations(docstruct);
            }

            String placeholder = new StringBuilder("{").append(param.getKey()).append("}").toString();
            // logger.trace("placeholder: {}", placeholder); //NOSONAR Debug
            if (!value.isEmpty() && StringUtils.isNotEmpty(param.getPrefix())) {
                String prefix = ViewerResourceBundle.getTranslation(param.getPrefix(), null);
                value.addPrefix(prefix);
            }
            if (!value.isEmpty() && StringUtils.isNotEmpty(param.getSuffix())) {
                String suffix = ViewerResourceBundle.getTranslation(param.getSuffix(), null);
                value.addSuffix(suffix);
            }
            Set<String> languages = new HashSet<>(value.getLanguages());
            languages.addAll(label.getLanguages());
            // Replace master value placeholders in the label object
            Map<String, String> languageLabelMap = new HashMap<>();
            for (String language : languages) {
                String langValue = label.getValue(language)
                        .orElse(label.getValue().orElse(labelConfig.getMasterValue()))
                        .replace(placeholder, value.getValue(language).orElse(value.getValue().orElse("")));
                languageLabelMap.put(language, langValue);
            }
            for (Entry<String, String> entry : languageLabelMap.entrySet()) {
                label.setValue(entry.getValue(), entry.getKey());
            }
        }

        //convert to SImpleMetadataValue if only one value exists
        if (label.getValues().size() == 1) {
            return new SimpleMetadataValue(label.getValue().orElse(""));
        }

        return label;
    }

    /**
     * <p>
     * createMultiLanguageValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field Index field
     * @param altField Fallback index field
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public static IMetadataValue createMultiLanguageValue(SolrDocument doc, String field, String altField) {
        IMetadataValue value;
        value = new MultiLanguageMetadataValue();
        Map<String, List<String>> valueMap = SolrTools.getMetadataValuesForLanguage(doc, field);
        if (valueMap.isEmpty() && StringUtils.isNotEmpty(altField)) {
            valueMap = SolrTools.getMetadataValuesForLanguage(doc, altField);
        }
        valueMap.entrySet().stream().forEach(entry -> {
            String language = entry.getKey();
            String langValue = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            value.setValue(langValue, language);
        });

        return value;
    }

    /**
     * TODO Unused
     * 
     * @param labelConfig
     * @return List<String>
     * @should parse all field names correctly
     */
    static List<String> parseVolumeLabelConfig(String labelConfig) {
        if (labelConfig == null) {
            throw new IllegalArgumentException("labelConfig may not be null");
        }

        List<String> ret = new ArrayList<>();
        Matcher m = patternVolumeLabel.matcher(labelConfig);
        while (m.find()) {
            ret.add(labelConfig.substring(m.start() + 1, m.end() - 1));
        }

        return ret;
    }

    /**
     * Returns the first value of the given field in the given doc.
     *
     * @param doc
     * @param fields
     * @return Footer id
     */
    static String getFooterId(SolrDocument doc, List<String> fields) {
        String ret = null;
        for (String field : fields) {
            List<String> footerIdValues = SolrTools.getMetadataValues(doc, field);
            if (footerIdValues != null && !footerIdValues.isEmpty()) {
                ret = footerIdValues.get(0);
                break;
            }
        }

        return ret;
    }

    /**
     *
     * Returns the first value of the given field in the given struct element
     *
     * @param doc
     * @param fields
     * @return Footer id
     */
    static String getFooterId(StructElement doc, List<String> fields) {
        String ret = null;
        for (String field : fields) {
            List<String> footerIdValues = doc.getMetadataValues(field);
            if (footerIdValues != null && !footerIdValues.isEmpty()) {
                ret = footerIdValues.get(0);
                break;
            }
        }

        return ret;
    }
}
