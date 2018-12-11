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
package de.intranda.digiverso.presentation.model.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter.MetadataParameterType;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.StructElementStub;

public class TocMaker {

    private static final Logger logger = LoggerFactory.getLogger(TocMaker.class);

    private static final String[] REQUIRED_FIELDS = { SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT, SolrConstants.DATAREPOSITORY,
            SolrConstants.DOCSTRCT, SolrConstants.IDDOC, SolrConstants.IDDOC_PARENT, SolrConstants.ISANCHOR, SolrConstants.ISWORK,
            SolrConstants.LABEL, SolrConstants.LOGID, SolrConstants.MIMETYPE, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.THUMBNAIL,
            SolrConstants.THUMBPAGENO, SolrConstants.THUMBPAGENOLABEL, SolrConstants.TITLE };

    private static Pattern patternVolumeLabel = Pattern.compile(Helper.REGEX_BRACES);

    /**
     * Returns a list of fields to be used as the field filter for Solr queries. The list constists of statically defined fields in REQUIRED_FIELDS
     * and any additional fields configured for the TOC label.
     *
     * @return
     * @should return both static and configured fields
     */
    protected static List<String> getSolrFieldsToFetch(String template) {
        logger.trace("getSolrFieldsToFetch: {}", template);
        Set<String> ret = new HashSet<>(Arrays.asList(REQUIRED_FIELDS));

        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration(template);
        if (metadataList != null && !metadataList.isEmpty()) {
            for (MetadataParameter param : metadataList.get(0).getParams()) {
                if (StringUtils.isNotEmpty(param.getKey())) {
                    ret.add(param.getKey());
                }
            }
        }
        // Add ancestor identifier fields to the required fields list
        List<String> ancestorFields = DataManager.getInstance().getConfiguration().getAncestorIdentifierFields();
        if (ancestorFields != null) {
            ret.addAll(ancestorFields);
        }
        //        ret.add("MD_TITLE_LANG_EN");
        //        ret.add("MD_TITLE_LANG_DE");
        return new ArrayList<>(ret);
    }

    /**
     * Generate the TOC.
     *
     * @param toc The TOC object (only required to set the number of volumes for anchor TOCs.
     * @param structElement
     * @param addAllSiblings If true and <code>structElement</code> has a parent, other siblings will be listed as well and can be navigated.
     * @param mimeType Mime type determines the target URL of the TOC element.
     * @param tocCurrentPage Current page of a paginated TOC.
     * @param hitsPerPage Hits per page of a paginated TOC.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @throws Exception in case of errors.
     * @should generate volume TOC correctly with siblings correctly
     * @should generate volume TOC correctly without siblings correctly
     * @should generate anchor TOC correctly
     * @should paginate anchor TOC correctly
     * @should throw IllegalArgumentException if structElement is null
     * @should throw IllegalArgumentException if toc is null
     */
    public static LinkedHashMap<String, List<TOCElement>> generateToc(TOC toc, StructElement structElement, boolean addAllSiblings, String mimeType,
            int tocCurrentPage, int hitsPerPage) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("generateToc");
        if (structElement == null) {
            throw new IllegalArgumentException("structElement may not me null");
        }
        if (toc == null) {
            throw new IllegalArgumentException("toc may not me null");
        }

        logger.trace("generateToc: {}", structElement.getPi());
        LinkedHashMap<String, List<TOCElement>> ret = new LinkedHashMap<>();
        ret.put(TOC.DEFAULT_GROUP, new ArrayList<TOCElement>());

        // TODO Remove the check for METS once format-agnostic way of generating PDFs has been implemented
        boolean sourceFormatPdfAllowed = StructElementStub.SOURCE_DOC_FORMAT_METS.equals(structElement.getSourceDocFormat());
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(
                new StringBuilder(SolrConstants.IDDOC).append(':').append(structElement.getLuceneId()).toString(),
                getSolrFieldsToFetch(structElement.getDocStructType()));
        if (doc != null) {
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
                //                String docstruct = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String docstruct = "_GROUPS";
                ret.get(TOC.DEFAULT_GROUP).add(new TOCElement(label, null, null, String.valueOf(structElement.getLuceneId()), null, level,
                        structElement.getPi(), null, false, true, false, mimeType, docstruct, footerId));
                ++level;
                buildGroupToc(ret.get(TOC.DEFAULT_GROUP), structElement.getGroupIdField(), structElement.getPi(), sourceFormatPdfAllowed, mimeType);
            } else if (structElement.isAnchor()) {
                // MultiVolume
                int numVolumes = buildAnchorToc(ret, doc, sourceFormatPdfAllowed, mimeType, tocCurrentPage, hitsPerPage);
                toc.setTotalTocSize(numVolumes);
            } else {
                // Standalone or volume
                ret.put(TOC.DEFAULT_GROUP, buildToc(doc, structElement, addAllSiblings, mimeType, sourceFormatPdfAllowed));
            }
        }

        logger.trace("generateToc end: {} groups, {} elements in DEFAULT", ret.size(), ret.get(TOC.DEFAULT_GROUP).size());
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
        //                        ret.add(new TocElementFlat(structElement.getGroupMemberships().get(groupIdField), null, null, String.valueOf(structElement
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

        int mainRecordLevel = 0; // currently not in use
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
                logger.trace("Ancestor query: {}", sbQuery.toString());
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
                mainRecordLevel += ancestorList.size();
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
     * @param groupIdField
     * @param groupIdValue
     * @param sourceFormatPdfAllowed
     * @param mimeType
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    private static void buildGroupToc(List<TOCElement> ret, String groupIdField, String groupIdValue, boolean sourceFormatPdfAllowed, String mimeType)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        logger.trace("addMembersToGroup: {}", groupIdValue);
        String template = "_GROUPS";
        String groupSortField = groupIdField.replace(SolrConstants.GROUPID_, SolrConstants.GROUPORDER_);
        SolrDocumentList groupMemberDocs = DataManager.getInstance().getSearchIndex().search(
                new StringBuilder("(").append(SolrConstants.ISWORK)
                        .append(":true OR ")
                        .append(SolrConstants.ISANCHOR)
                        .append(":true) AND ")
                        .append(groupIdField)
                        .append(':')
                        .append(groupIdValue)
                        .toString(),
                SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(groupSortField, "asc")), getSolrFieldsToFetch(template));
        if (groupMemberDocs != null) {
            HttpServletRequest request = BeanUtils.getRequest();
            for (SolrDocument doc : groupMemberDocs) {
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
                String dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);

                //                String sourceDocFormat = (String) doc.getFieldValue(LuceneConstants.SOURCEDOCFORMAT);

                if (label.isEmpty()) {
                    if (StringUtils.isNotEmpty(numberText)) {
                        label.setValue(new StringBuilder(label.getValue().orElse("")).append(" (").append(numberText).append(')').toString());
                    } else {
                        label = new SimpleMetadataValue("-");
                    }
                }

                String footerId = getFooterId(doc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
                int thumbWidth = DataManager.getInstance().getConfiguration().getMultivolumeThumbnailWidth();
                int thumbHeight = DataManager.getInstance().getConfiguration().getMultivolumeThumbnailHeight();
                String thumbnailUrl = null;
                if (StringUtils.isNotEmpty(topStructPi) && StringUtils.isNotEmpty(thumbnailFile)) {
                    ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
                    StructElement struct = new StructElement(Long.valueOf(volumeIddoc), doc);
                    thumbnailUrl = thumbs.getThumbnailUrl(struct, thumbWidth, thumbHeight);
                }
                label.mapEach(value -> StringEscapeUtils.unescapeHtml(value));
                boolean accessPermissionPdf = sourceFormatPdfAllowed && AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi,
                        logId, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request);
                ret.add(new TOCElement(label, "1", null, volumeIddoc, logId, 1, topStructPi, thumbnailUrl, accessPermissionPdf, false,
                        thumbnailUrl != null, mimeType, docStructType, footerId));
            }
        }
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
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private static int buildAnchorToc(Map<String, List<TOCElement>> ret, SolrDocument anchorDoc, boolean sourceFormatPdfAllowed, String mimeType,
            int tocCurrentPage, int hitsPerPage) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("buildAnchorToc");
        String iddoc = (String) anchorDoc.getFieldValue(SolrConstants.IDDOC);
        String anchorDocstructType = (String) anchorDoc.getFieldValue(SolrConstants.DOCSTRCT);
        String logId = (String) anchorDoc.getFieldValue(SolrConstants.LOGID);
        String topStructPiLocal = (String) anchorDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);

        if (tocCurrentPage < 1) {
            throw new IllegalArgumentException("page must be >=1");
        }
        //        List<String> anchorFieldList = getSolrFieldsToFetchForAnchor(anchorDocstructType);

        String query = new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(iddoc).toString();
        int hits = (int) DataManager.getInstance().getSearchIndex().getHitCount(query);
        int offset = 0;
        if (hitsPerPage <= 0) {
            hitsPerPage = SolrSearchIndex.MAX_HITS;
        } else {
            // Using paginator
            offset = hitsPerPage * (tocCurrentPage - 1);
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
        QueryResponse queryResponse = DataManager.getInstance().getSearchIndex().search(query, offset, hitsPerPage,
                DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(anchorDocstructType), null, volumeFieldList);
        if (queryResponse != null) {
            HttpServletRequest request = BeanUtils.getRequest();
            for (SolrDocument volumeDoc : queryResponse.getResults()) {
                String topStructPi = (String) volumeDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                // Skip volumes that may not be listed
                if (FacesContext.getCurrentInstance() != null
                        && !AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi, null, IPrivilegeHolder.PRIV_LIST, request)) {
                    continue;
                }
                // Determine the TOC group for this volume based on the grouping field, if configured
                String groupName = TOC.DEFAULT_GROUP;
                if (tocGroupField != null) {
                    String groupValue = String.valueOf(volumeDoc.getFieldValue(tocGroupField));
                    if (StringUtils.isNotEmpty(groupValue)) {
                        groupName = groupValue;
                    }
                }
                //                logger.trace("group name: {}", groupName);
                List<TOCElement> groupList = ret.get(groupName);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    ret.put(groupName, groupList);
                }

                String numberSort = volumeDoc.getFieldValue(SolrConstants.CURRENTNOSORT) != null
                        ? String.valueOf(volumeDoc.getFieldValue(SolrConstants.CURRENTNOSORT)) : null;
                String numberText = volumeDoc.getFieldValue(SolrConstants.CURRENTNO) != null
                        ? (String) volumeDoc.getFieldValue(SolrConstants.CURRENTNO) : numberSort;
                String volumeIddoc = (String) volumeDoc.getFieldValue(SolrConstants.IDDOC);
                String volumeLogId = (String) volumeDoc.getFieldValue(SolrConstants.LOGID);
                String thumbnailUrl = (String) volumeDoc.getFieldValue(SolrConstants.THUMBNAIL);
                String volumeMimeType = (String) volumeDoc.getFieldValue(SolrConstants.MIMETYPE);
                logger.trace("volume mime type: {}", volumeMimeType);
                String dataRepository = (String) volumeDoc.getFieldValue(SolrConstants.DATAREPOSITORY);

                int thumbWidth = DataManager.getInstance().getConfiguration().getMultivolumeThumbnailWidth();
                int thumbHeight = DataManager.getInstance().getConfiguration().getMultivolumeThumbnailHeight();

                ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
                StructElement struct = new StructElement(Long.valueOf(volumeIddoc), volumeDoc);
                thumbnailUrl = thumbs.getThumbnailUrl(struct, thumbWidth, thumbHeight);

                String footerId = getFooterId(volumeDoc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
                String docStructType = (String) volumeDoc.getFieldValue(SolrConstants.DOCSTRCT);

                IMetadataValue volumeLabel = buildLabel(volumeDoc, docStructType);
                volumeLabel.mapEach(l -> StringEscapeUtils.unescapeHtml(l));
                boolean accessPermissionPdf = sourceFormatPdfAllowed && AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topStructPi,
                        volumeLogId, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request);
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
                    if (fieldName.startsWith(SolrConstants.GROUPID_)) {
                        for (Object objValue : volumeDoc.getFieldValues(fieldName)) {
                            groupIds.add((String) objValue);
                        }
                    }
                }
                tocElement.setGroupIds(groupIds);
            }
        }

        // Add first volume's mime type to anchor
        if (!ret.isEmpty() && StringUtils.isEmpty(mimeType)) {
            for (String key : ret.keySet()) {
                for (TOCElement tocElement : ret.get(key)) {
                    if (tocElement.getRecordMimeType() != null) {
                        mimeType = tocElement.getRecordMimeType();
                        logger.trace("mime type found: {}", mimeType);
                        break;
                    }
                    if (StringUtils.isNotEmpty(mimeType)) {
                        break;
                    }
                }
            }
        }
        // Add anchor document
        IMetadataValue label = buildLabel(anchorDoc, (String) anchorDoc.getFirstValue(SolrConstants.DOCSTRCT));
        String footerId = getFooterId(anchorDoc, DataManager.getInstance().getConfiguration().getWatermarkIdField());
        ret.get(TOC.DEFAULT_GROUP).add(0, new TOCElement(label, null, null, String.valueOf(iddoc), logId, 0, topStructPiLocal, null,
                sourceFormatPdfAllowed, true, false, mimeType, anchorDocstructType, footerId));

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
        logger.trace("populateTocTree: {}", pi);

        // Check PDF download permissions for all docstructs and save into map
        Map<String, Boolean> pdfPermissionMap = null;
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
            // logger.trace("Child doc query: {}", query);
            String docstruct = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT);
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
                        List<SolrDocument> children = childrenMap.get(iddocParent);
                        if (children == null) {
                            children = new ArrayList<>();
                            childrenMap.put(iddocParent, children);
                        }
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
            String query = new StringBuilder(ancestorField).append(':').append(queryValue).toString();
            // logger.trace("Loose children query: {}", query);
            // logger.trace("sort {} by {}", SolrSearchIndex.getSingleFieldStringValue(doc, LuceneConstants.DOCSTRCT), DataManager.getInstance()
            //                    .getConfiguration().getTocVolumeSortFieldsForTemplate(SolrSearchIndex.getSingleFieldStringValue(doc, LuceneConstants.DOCSTRCT)));
            String docstruct = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT);
            // TODO determine child docstruct type before fetching the child docs to determine the required fields
            SolrDocumentList childDocs =
                    DataManager.getInstance().getSearchIndex().search(new StringBuilder(ancestorField).append(':').append(queryValue).toString(),
                            SolrSearchIndex.MAX_HITS, DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(
                                    SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT)),
                            null);
            logger.trace("Loose children of {}: {}", queryValue, childDocs.size());
            if (!childDocs.isEmpty()) {
                for (SolrDocument childDoc : childDocs) {
                    // Add child, if either all siblings are requested or the path leads to the main record
                    if (addAllSiblings || mainDocumentChain.contains(childDoc.getFieldValue(SolrConstants.IDDOC))) {
                        populateTocTree(ret, mainDocumentChain, childDoc, level + 1, addChildren, sourceFormatPdfAllowed, mimeType, ancestorField,
                                addAllSiblings, footerId);
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
     * @param sourceFormatPdfAllowed
     * @param mimeType
     * @param footerId
     * @throws PresentationException
     */
    private static void addTocElementsRecusively(List<TOCElement> ret, Map<String, List<SolrDocument>> childrenMap, SolrDocument doc, int level,
            boolean addChildren, Map<String, Boolean> pdfPermissionMap, String mimeType, String footerId) throws PresentationException {
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
        if (pdfPermissionMap != null && logId != null && pdfPermissionMap.get(logId)) {
            accessPermissionPdf = pdfPermissionMap.get(logId);
        }
        TOCElement tocElement = new TOCElement(label, pageNo, pageNoLabel, iddoc, logId, level, pi, null, accessPermissionPdf, isAnchor,
                pageNo != null, mimeType, docstructType, footerId);
        tocElement.getMetadata().put(SolrConstants.DOCSTRCT, docstructType);
        tocElement.getMetadata().put(SolrConstants.CURRENTNO, (String) doc.getFieldValue(SolrConstants.CURRENTNO));
        tocElement.getMetadata().put("MD_TITLE", (String) doc.getFirstValue("MD_TITLE"));
        if (!ret.contains(tocElement)) {
            ret.add(tocElement);
            // logger.trace("TOC element added: {}/{}: '{}'; IDDOC:{}", ret.size() - 1, level, label, iddoc);

            // Child elements
            if (addChildren && childrenMap != null && childrenMap.get(iddoc) != null && !childrenMap.get(iddoc).isEmpty()) {
                logger.trace("Adding {} children for {}", childrenMap.get(iddoc).size(), iddoc);
                for (SolrDocument childDoc : childrenMap.get(iddoc)) {
                    addTocElementsRecusively(ret, childrenMap, childDoc, level + 1, true, pdfPermissionMap, mimeType, footerId);
                }
            }
        }
    }

    /**
     * @param doc
     * @param footerIdField
     * @return
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

    /**
     * Generates the label for this TOC element either from a configured layout or hardcoded old style.
     *
     * @param doc
     * @param template
     * @should build configured label correctly
     */
    static IMetadataValue buildLabel(SolrDocument doc, String template) {
        IMetadataValue label = new MultiLanguageMetadataValue();
        // logger.trace("buildLabel: {}", template);
        List<Metadata> labelConfigList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration(template);
        if (labelConfigList != null && !labelConfigList.isEmpty()) {
            // Configurable label layout
            Metadata labelConfig = labelConfigList.get(0);
            label.setValue(labelConfig.getMasterValue());
            for (MetadataParameter param : labelConfig.getParams()) {
                // logger.trace("param key: {}", param.getKey());
                IMetadataValue value;
                if (MetadataParameterType.MULTILANGUAGEFIELD.equals(param.getType())) {
                    value = createMultiLanguageValue(doc, param.getKey());
                } else {
                    value = new SimpleMetadataValue();
                    value.setValue(SolrSearchIndex.getSingleFieldStringValue(doc, param.getKey()));
                    // logger.trace("value: {}:{}", param.getKey(), value.getValue());
                }
                // Special case: If LABEL is missing, use MD_TITLE. If MD_TITLE is missing, use DOCSTRCT.
                if (StringUtils.isEmpty(value.toString()) && SolrConstants.LABEL.equals(param.getKey())) {
                    if (MetadataParameterType.MULTILANGUAGEFIELD.equals(param.getType())) {
                        value = createMultiLanguageValue(doc, SolrConstants.TITLE);
                    } else {
                        value.setValue(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.TITLE));
                    }
                    if (StringUtils.isEmpty(value.toString())) {
                        // Docstruct fallback should always be translated
                        String docstruct = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT);
                        value = IMetadataValue.getTranslations(docstruct);
                        //                        value.setValue(Helper.getTranslation(docstruct, null));
                    }
                }
                // Translate parameter value, if so configured and not already translated
                if (MetadataParameterType.TRANSLATEDFIELD.equals(param.getType()) && !(value instanceof MultiLanguageMetadataValue)) {
                    //                    value.setValue(Helper.getTranslation(value.getValue().orElse(""), null));
                    value = IMetadataValue.getTranslations(value.getValue().orElse(""));
                }
                String placeholder = new StringBuilder("{").append(param.getKey()).append("}").toString();
                // logger.trace("placeholder: {}", placeholder);
                // logger.trace("param value: {}", param.getKey());
                if (!value.isEmpty() && StringUtils.isNotEmpty(param.getPrefix())) {
                    String prefix = Helper.getTranslation(param.getPrefix(), null);
                    value.addPrefix(prefix);
                }
                if (!value.isEmpty() && StringUtils.isNotEmpty(param.getSuffix())) {
                    String suffix = Helper.getTranslation(param.getSuffix(), null);
                    value.addSuffix(suffix);
                }
                if (MetadataParameterType.MULTILANGUAGEFIELD.equals(param.getType())) {
                    for (String language : value.getLanguages()) {
                        String langValue = label.getValue(language).orElse(label.getValue().orElse("")).replace(placeholder,
                                value.getValue(language).orElse(value.getValue().orElse("")));
                        label.setValue(langValue, language);
                    }
                } else {
                    Set<String> languages = new HashSet<>(value.getLanguages());
                    languages.addAll(label.getLanguages());
                    for (String language : languages) {
                        String langValue = label.getValue(language).orElse(label.getValue().orElse(""));
                        langValue = langValue.replace(placeholder, value.getValue(language).orElse(value.getValue().orElse("")));
                        //                        String langValue =
                        //                                label.getValue(language).orElse(label.getValue().orElse("")).replace(placeholder, value.getValue().orElse(""));
                        label.setValue(langValue, language);
                    }
                }
            }
        } else {
            // Old style layout
            label.setValue(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.LABEL));
            if (StringUtils.isEmpty(label.toString())) {
                label.setValue(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.TITLE));
                if (StringUtils.isEmpty(label.toString())) {
                    label.setValue(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT));
                } else if (DataManager.getInstance().getConfiguration().isTocAlwaysDisplayDocstruct()) {
                    label.setValue(
                            new StringBuilder(Helper.getTranslation(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT), null))
                                    .append(": ")
                                    .append(label.getValue())
                                    .toString());
                }
            } else if (DataManager.getInstance().getConfiguration().isTocAlwaysDisplayDocstruct()) {
                label.setValue(new StringBuilder(Helper.getTranslation(SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.DOCSTRCT), null))
                        .append(": ")
                        .append(label.getValue())
                        .toString());
            }
        }

        // logger.trace("label: {}", label);
        return label;
    }

    /**
     * @param doc
     * @param param
     * @return
     */
    public static IMetadataValue createMultiLanguageValue(SolrDocument doc, String field) {
        IMetadataValue value;
        value = new MultiLanguageMetadataValue();
        Map<String, List<String>> valueMap = SolrSearchIndex.getMetadataValuesForLanguage(doc, field);
        valueMap.entrySet().stream().forEach(entry -> {
            String language = entry.getKey();
            String langValue = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            value.setValue(langValue, language);
        });
        return value;
    }

    /**
     *
     * @param labelConfig
     * @param pattern
     * @return
     * @should parse all field names correctly
     */
    static List<String> parseVolumeLabelConfig(String labelConfig) {
        if (labelConfig == null) {
            throw new IllegalArgumentException("labelConfig may not be null");
        }

        List<String> ret = new ArrayList<>();
        Matcher m = patternVolumeLabel.matcher(labelConfig);
        List<String> labelFields = new ArrayList<>();
        while (m.find()) {
            labelFields.add(labelConfig.substring(m.start() + 1, m.end() - 1));
        }

        return ret;
    }

    /**
     * Returns the first value of the given field in the given doc.
     * 
     * @param doc
     * @param field
     * @return
     */
    static String getFooterId(SolrDocument doc, List<String> fields) {
        String ret = null;
        for (String field : fields) {
            List<String> footerIdValues = SolrSearchIndex.getMetadataValues(doc, field);
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
     * @param field
     * @return
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
