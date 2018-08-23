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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

/**
 * Each instance of this class represents a structure element. This class extends <code>StructElementStub</code> and contains additional
 * memory-intensive members such as the corresponding Solr document, references to parent and child StructElements and full-text.
 */
public class StructElement extends StructElementStub implements Comparable<StructElementStub>, Serializable {

    private static final long serialVersionUID = 9048792944197887061L;

    private static final Logger logger = LoggerFactory.getLogger(StructElement.class);

    /** If false; the Solr document with the given IDDOC does not exist in the index. */
    private boolean exists = false;
    private DocType docType = null;;
    /** True if full-text is available for this record (top-level structure elements only). */
    private boolean fulltextAvailable = false;
    /** True if ALTO is available for this record. */
    private Boolean altoAvailable = null;
    /** True if NER-enriched ALTO is available for this record. */
    private Boolean nerAvailable = null;
    /** True if this element has a child elements. */
    private Boolean hasChildren = null;
    /** Ancestor identifier fields (such as anchors). */
    private final Map<String, String> ancestors = new HashMap<>();
    /** Group membership identifiers for this record. Contains the identifier field name and value. */
    private final Map<String, String> groupMemberships = new HashMap<>();
    /** Labels of the groups to which this record belongs. */
    private final Map<String, String> groupLabels = new HashMap<>();

    /**
     * Empty constructor for unit tests.
     */
    public StructElement() {
    }

    /**
     *
     *
     * @param luceneId {@link Long}
     * @throws IndexUnreachableException
     */
    public StructElement(long luceneId) throws IndexUnreachableException {
        super(luceneId);
        init(null);
    }

    public StructElement(long luceneId, SolrDocument doc) throws IndexUnreachableException {
        super(luceneId);
        init(doc);
    }

    public StructElement(long luceneId, SolrDocument doc, SolrDocument docToMerge) throws IndexUnreachableException {
        super(luceneId);
        if (docToMerge != null) {
            if (docToMerge.getFieldValue(SolrConstants.LABEL) != null) {
                doc.addField(SolrConstants.LABEL, docToMerge.getFieldValue(SolrConstants.LABEL));
            }
            if (docToMerge.getFieldValue(SolrConstants.TITLE) != null) {
                doc.addField(SolrConstants.TITLE, docToMerge.getFieldValue(SolrConstants.TITLE));
            }
            if (docToMerge.getFieldValue(SolrConstants.LOGID) != null) {
                logid = (String) docToMerge.getFieldValue(SolrConstants.LOGID);
                doc.addField(SolrConstants.LOGID, logid);
            }
            // Only add DOCSTRCT if the page doc has none yet (Indexer older than 2.0.20120619)
            if (docToMerge.getFieldValue(SolrConstants.DOCSTRCT) != null && doc.getFieldValue(SolrConstants.DOCSTRCT) == null) {
                doc.addField(SolrConstants.DOCSTRCT, docToMerge.getFieldValue(SolrConstants.DOCSTRCT));
            }
            if (docToMerge.getFieldValue(SolrConstants.CURRENTNO) != null) {
                volumeNo = (String) docToMerge.getFieldValue(SolrConstants.CURRENTNO);
                doc.addField(SolrConstants.CURRENTNO, volumeNo);
            }
            if (docToMerge.getFieldValue(SolrConstants.CURRENTNOSORT) != null) {
                volumeNoSort = String.valueOf(docToMerge.getFieldValue(SolrConstants.CURRENTNOSORT));
                doc.addField(SolrConstants.CURRENTNOSORT, volumeNoSort);
            }
            if (docToMerge.getFieldValue(SolrConstants.THUMBNAIL) != null) {
                doc.addField(SolrConstants.THUMBNAIL, docToMerge.getFieldValue(SolrConstants.THUMBNAIL));
            }
            if (docToMerge.getFieldValue(SolrConstants.THUMBPAGENO) != null) {
                doc.addField(SolrConstants.THUMBPAGENO, docToMerge.getFieldValue(SolrConstants.THUMBPAGENO));
            }
            if (docToMerge.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
                doc.addField(SolrConstants.IDDOC_PARENT, docToMerge.getFieldValue(SolrConstants.IDDOC_PARENT));
            }
            if (docToMerge.getFieldValue(SolrConstants.ISWORK) != null) {
                doc.addField(SolrConstants.ISWORK, docToMerge.getFieldValue(SolrConstants.ISWORK));
            }
        }
        init(doc);
    }

    private final void init(SolrDocument doc) throws IndexUnreachableException {
        try {
            if (doc == null) {
                doc = getDocument();
            }
            metadataFields = SolrSearchIndex.getFieldValueMap(doc);
            pi = getMetadataValue(SolrConstants.PI);
            if (pi != null) {
                pi = pi.intern();
            }
            docType = DocType.getByName(getMetadataValue(SolrConstants.DOCTYPE));
            logid = getMetadataValue(SolrConstants.LOGID);
            work = Boolean.valueOf(getMetadataValue(SolrConstants.ISWORK));
            anchor = Boolean.valueOf(getMetadataValue(SolrConstants.ISANCHOR));
            if (anchor) {
                String numVolumeString = getMetadataValue(SolrConstants.NUMVOLUMES);
                if (numVolumeString != null) {
                    numVolumes = Long.valueOf(numVolumeString);
                } else {
                    logger.warn(
                            "{} not found on anchor record '{}', retrieving the number of volumes by counting the indexed volume records. Re-index the record for faster loading.",
                            SolrConstants.NUMVOLUMES, pi);
                    SolrDocumentList resp = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.ISWORK).append(":true AND ")
                                    .append(SolrConstants.PI_PARENT)
                                    .append(':')
                                    .append(getPi())
                                    .toString(), 0, null, null);
                    numVolumes = resp.getNumFound();
                }
                // logger.debug("Volumes found for " + pi + ": " + numVolumes);
            }
            docStructType = getMetadataValue(SolrConstants.DOCSTRCT);
            if (docStructType != null) {
                docStructType.intern();
            }
            volumeNo = getMetadataValue(SolrConstants.CURRENTNO);
            volumeNoSort = getMetadataValue(SolrConstants.CURRENTNOSORT);
            dataRepository = getMetadataValue(SolrConstants.DATAREPOSITORY);
            partnerId = getMetadataValue(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
            sourceDocFormat = getMetadataValue(SolrConstants.SOURCEDOCFORMAT);
            fulltextAvailable = Boolean.valueOf(getMetadataValue(SolrConstants.FULLTEXTAVAILABLE));
            label = getMetadataValue(SolrConstants.LABEL);
            if (StringUtils.isEmpty(label)) {
                label = getMetadataValue("MD_TITLE");
                if (StringUtils.isEmpty(label)) {
                    label = getMetadataValue(SolrConstants.DOCSTRCT);
                }
            }
            // Determine the ancestor and group field names and identifiers
            for (String fieldName : doc.getFieldNames()) {
                if (DataManager.getInstance().getConfiguration().getAncestorIdentifierFields().contains(fieldName)) {
                    Collection<Object> fieldValues = doc.getFieldValues(fieldName);
                    for (Object o : fieldValues) {
                        ancestors.put(fieldName, (String) o);
                        if (SolrConstants.PI_PARENT.equals(fieldName)) {
                            volume = true;
                        }
                    }
                } else if (DataManager.getInstance().getConfiguration().getRecordGroupIdentifierFields().contains(fieldName)) {
                    groupMemberships.put(fieldName, (String) doc.getFieldValue(fieldName));
                }
            }

        } catch (PresentationException e) {
            // Catch exception to skip the rest of the code block, but do not do anything (already logged elsewhere)
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }
    }

    /**
     * Returns the SolrDocument that represents this struct element.
     *
     * @return {@link SolrDocument}
     * @throws IndexUnreachableException
     * @throws PresentationException .
     */
    private SolrDocument getDocument() throws PresentationException, IndexUnreachableException {
        // logger.trace("getDocument(): {}", luceneId);
        SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(
                new StringBuilder(SolrConstants.IDDOC).append(':').append(luceneId).toString(), 1, null, null);
        if (!hits.isEmpty()) {
            exists = true;
            return hits.get(0);
        }
        logger.warn("Document not found in index: {}", luceneId);
        throw new PresentationException("errDocNotFound");
    }

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isHasParentOrChildren() throws PresentationException, IndexUnreachableException {
        return isHasParent() || isHasChildren();
    }

    /**
     *
     * @return
     */
    public boolean isHasParent() {
        return getMetadataValue(SolrConstants.IDDOC_PARENT) != null;
    }

    /**
     * Loads and returns the immediate parent StructElement of this element.
     *
     * @return {@link StructElement}
     * @throws IndexUnreachableException
     * @throws PresentationException -
     * @should return parent correctly
     */
    public StructElement getParent() throws IndexUnreachableException {
        //        logger.trace("getParent");
        StructElement parent = null;
        try {
            String parentIddoc = getMetadataValue(SolrConstants.IDDOC_PARENT);
            if (parentIddoc != null) {
                parent = new StructElement(Long.valueOf(parentIddoc), null);
            }
        } catch (NumberFormatException e) {
            logger.error("Malformed number with get the parent element for Lucene IDDOC: {}", luceneId);
        }

        return parent;
    }

    /**
     * Checks whether the Solr document represented by this StructElement has child elements in the index.
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isHasChildren() throws IndexUnreachableException, PresentationException {
        if (hasChildren == null) {
            if (DataManager.getInstance().getSearchIndex().getHitCount(
                    new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString()) > 0) {
                hasChildren = true;
            } else {
                hasChildren = false;
            }
        }

        return hasChildren;
    }

    /**
     * Loads immediate child StructElements for the given element.
     *
     * @return {@link ArrayList}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should load all children
     */
    @Deprecated
    public List<StructElement> getChildren(List<String> fieldList) throws PresentationException, IndexUnreachableException {
        logger.trace("getChildren");
        List<StructElement> children = new ArrayList<>();
        try {
            SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(
                    new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString(), fieldList);
            if (hits.isEmpty()) {
                hasChildren = false;
            } else {
                hasChildren = true;
                for (SolrDocument doc : hits) {
                    children.add(new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc));
                }
            }
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
        }

        return children;
    }

    /**
     * Returns a StructElement that represents the top non-anchor element of the hierarchy (ISWORK=true).
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should retrieve top struct correctly
     */
    public StructElement getTopStruct() throws PresentationException, IndexUnreachableException {
        StructElement topStruct = this;
        if (!work) {
            String topstructIddoc = getMetadataValue(SolrConstants.IDDOC_TOPSTRUCT);
            try {
                if (topstructIddoc != null) {
                    topStruct = new StructElement(Long.valueOf(topstructIddoc), null);
                }
            } catch (NumberFormatException e) {
                logger.error("Malformed number with get the topstruct element for Lucene IDDOC: {}", topstructIddoc);
            }
        }

        return topStruct;
    }

    public boolean isGroupMember() {
        return !groupMemberships.isEmpty();
    }

    public boolean isGroup() {
        return DocType.GROUP.equals(docType);
    }

    /**
     * Returns the label for the group with the given identifier.
     *
     * @param groupIdentifier
     * @return
     * @throws IndexUnreachableException
     */
    public String getGroupLabel(String groupIdentifier) throws IndexUnreachableException {
        if (groupIdentifier == null) {
            throw new IllegalArgumentException("groupIdentifier may not be null");
        }
        if (groupLabels.get(groupIdentifier) == null) {
            try {
                SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + groupIdentifier,
                        Collections.singletonList(SolrConstants.LABEL));
                if (doc != null) {
                    String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                    if (label != null) {
                        groupLabels.put(groupIdentifier, label);
                    }
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
            // If label doesn't exist or there has been an error while retrieving it, use the identifier.
            if (groupLabels.get(groupIdentifier) == null) {
                groupLabels.put(groupIdentifier, groupIdentifier);
            }
        }

        return groupLabels.get(groupIdentifier);
    }

    /**
     * @return the exists
     */
    public boolean isExists() {
        return exists;
    }

    public boolean isDeleted() {
        return getMetadataValue(SolrConstants.DATEDELETED) != null;
    }

    /**
     * Returns the identifier of the record to which this struct element belongs.
     *
     * @return {@link String} PI/PPN
     */
    @Override
    public String getPi() {
        if (pi != null && !pi.equals("null")) {
            return pi;
        }

        return getMetadataValue(SolrConstants.PI_TOPSTRUCT);
    }

    @Override
    public int getImageNumber() {
        int no = 0;
        try {
            no = Integer.parseInt(getMetadataValue(SolrConstants.THUMBPAGENO));
        } catch (NumberFormatException e) {
            no = 0;
        }

        return no;
    }

    /**
     * 
     * @param width
     * @param height
     * @return Image URL
     * @throws ViewerConfigurationException 
     * @should construct url correctly
     */
    public String getImageUrl(int width, int height) throws ViewerConfigurationException {
        String filename = getMetadataValue(SolrConstants.THUMBNAIL);
        if (filename != null) {
            return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(this, width, height);
        }

        return null;
    }

    /**
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<EventElement> generateEventElements(Locale locale) throws IndexUnreachableException {
        logger.trace("generateEventElements");
        try {
            // Return all fields here because the metadata fields are needed to populate the event objects
            SolrDocumentList result = DataManager.getInstance()
                    .getSearchIndex()
                    .search(new StringBuilder(SolrConstants.IDDOC_OWNER).append(':')
                            .append(getLuceneId())
                            .append(" AND ")
                            .append(SolrConstants.DOCTYPE)
                            .append(':')
                            .append(DocType.EVENT)
                            .toString(), SolrSearchIndex.MAX_HITS, null, null);
            logger.trace("{} events found", result.size());
            List<EventElement> ret = new ArrayList<>(result.size());
            for (SolrDocument doc : result) {
                EventElement event = new EventElement(doc, locale);
                ret.add(event);
            }
            return ret;
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     * @should return true if current record is volume
     * @should return false if current record is not volume
     */
    public boolean isAnchorChild() throws IndexUnreachableException {
        if (isWork() && isHasParent()) {
            return true;
        }

        return false;
    }

    public String getCollection() {
        return this.getMetadataValue(SolrConstants.DC);
    }

    public List<String> getCollections() {
        return this.getMetadataValues(SolrConstants.DC);
    }

    /**
     * @return the fulltextAvailable
     */
    public boolean isFulltextAvailable() {
        // logger.debug("fulltext available: " + fulltextAvailable);
        return fulltextAvailable;
    }

    /**
     * @param fulltextAvailable the fulltextAvailable to set
     */
    public void setFulltextAvailable(boolean fulltextAvailable) {
        this.fulltextAvailable = fulltextAvailable;
    }

    /**
     * Returns true if the record has any ALTO documents indexed in its pages; false otherwise.
     * 
     * @return the altoAvailable
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isAltoAvailable() throws IndexUnreachableException, PresentationException {
        if (altoAvailable == null) {
            altoAvailable = DataManager.getInstance().getSearchIndex().getHitCount(
                    SolrConstants.PI_TOPSTRUCT + ":" + pi + " AND " + SolrConstants.FILENAME_ALTO + ":*") > 0;
        }

        return altoAvailable;
    }

    /**
     * Returns true if the record has any NE_* tags indexed in its pages; false otherwise.
     * 
     * @return the nerAvailable
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isNerAvailable() throws IndexUnreachableException, PresentationException {
        if (nerAvailable == null) {
            nerAvailable = DataManager.getInstance().getSearchIndex().getHitCount(
                    SolrConstants.PI_TOPSTRUCT + ":" + pi + " AND (NE_person" + SolrConstants._UNTOKENIZED + ":* OR NE_location"
                            + SolrConstants._UNTOKENIZED + ":* OR NE_corporation" + SolrConstants._UNTOKENIZED + ":*)") > 0;
        }

        return nerAvailable;
    }

    @Deprecated
    public String getTitle() {
        String answer = getLabel();
        if (StringUtils.isEmpty(answer)) {
            return "-";
        }
        return answer;
    }

    /**
     * Returns a stub representation of this object that only contains simple members to conserve memory.
     *
     * @should create stub correctly
     */
    public StructElementStub createStub() {
        StructElementStub ret = new StructElementStub(luceneId);
        ret.setPi(getPi());
        ret.setLogid(logid);
        ret.setDocStructType(getDocStructType());
        ret.setSourceDocFormat(sourceDocFormat);
        ret.setImageNumber(getImageNumber());
        ret.setWork(work);
        ret.setAnchor(anchor);
        ret.setVolume(volume);
        ret.setLabel(label);
        ret.setMetadataFields(metadataFields);

        return ret;
    }

    /**
     * @return the ancestors
     */
    public Map<String, String> getAncestors() {
        return ancestors;
    }

    /**
     * @return the groupMemberships
     */
    public Map<String, String> getGroupMemberships() {
        return groupMemberships;
    }

    public String getDisplayLabel() {
        String label = getMetadataValue(SolrConstants.LABEL);
        if (StringUtils.isEmpty(label)) {
            label = getMetadataValue(SolrConstants.TITLE);
            if (StringUtils.isEmpty(label)) {
                label = getDocStructType();
            }
        }

        return label;
    }

    public IMetadataValue getMultiLanguageDisplayLabel() {
        IMetadataValue label = getMultiLanguageMetadataValue(SolrConstants.LABEL);
        if (label.isEmpty()) {
            label = getMultiLanguageMetadataValue(SolrConstants.TITLE);
            if (label.isEmpty()) {
                label = IMetadataValue.getTranslations(getDocStructType());
            }
        }

        return label;
    }

    /**
     * Returns the group field name of a group document.
     */
    public String getGroupIdField() {
        return getMetadataValue(SolrConstants.GROUPTYPE);
    }

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct value
     * @should return null if StructElement not anchor
     * @should throw IllegalArgumentException if field is null
     */
    public String getFirstVolumeFieldValue(String field) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (anchor) {
            SolrDocument docParent = DataManager.getInstance().getSearchIndex().getFirstDoc(
                    new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString(), Collections.singletonList(field));
            if (docParent == null) {
                logger.warn("Anchor has no child element: Cannot determine appropriate value");
            } else {
                return SolrSearchIndex.getSingleFieldStringValue(docParent, field);
            }
        }

        return null;
    }
    

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct value
     * @should return null if StructElement not anchor
     * @should throw IllegalArgumentException if field is null
     */
    public StructElement getFirstVolume(List<String> fields) throws PresentationException, IndexUnreachableException {

        if (anchor) {
            List<StringPair> sortFields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(getDocStructType());

            SolrDocument docVolume = DataManager.getInstance().getSearchIndex().getFirstDoc(
                    new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString(), fields, sortFields);
            if (docVolume == null) {
                logger.warn("Anchor has no child element: Cannot determine appropriate value");
            } else {
                String iddoc = SolrSearchIndex.getSingleFieldStringValue(docVolume, SolrConstants.IDDOC);
                if (StringUtils.isNotBlank(iddoc)) {
                    StructElement volume = new StructElement(Long.parseLong(iddoc), docVolume);
                    return volume;
                }
            }
        }

        return null;
    }

    public String getFirstPageFieldValue(String field) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (work) {
            String query = new StringBuilder("DOCTYPE:PAGE AND ").append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).toString();
            List<StringPair> sortFields = Collections.singletonList(new StringPair("ORDER", ORDER.asc.name()));
            SolrDocumentList pages = DataManager.getInstance().getSearchIndex().search(query, 1, sortFields, null);
            if (!pages.isEmpty()) {
                return SolrSearchIndex.getSingleFieldStringValue(pages.get(0), field);
            }
        } else {
            try {
                int thumbPageNo = Integer.parseInt(getMetadataValue(SolrConstants.THUMBPAGENO));
                String topStructId = getMetadataValue(SolrConstants.PI_TOPSTRUCT);
                String query = new StringBuilder("DOCTYPE:PAGE AND ").append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(topStructId)
                        .append(" AND ORDER: " + thumbPageNo)
                        .toString();
                SolrDocumentList pages = DataManager.getInstance().getSearchIndex().search(query, 1, null, null);
                if (!pages.isEmpty()) {
                    return SolrSearchIndex.getSingleFieldStringValue(pages.get(0), field);
                }
            } catch (NullPointerException | NumberFormatException e) {
            }
        }

        return null;
    }

    /**
     * If the given StructElement contains access conditions, the logged in user must satisfy these conditions to be allowed to view the thumbnail.
     *
     * @param structElement
     * @return
     */
    public boolean mayShowThumbnail() {
        // TODO why does this always return true?
        return true;
    }

}
