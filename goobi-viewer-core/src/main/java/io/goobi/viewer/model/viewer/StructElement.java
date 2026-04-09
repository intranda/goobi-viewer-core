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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.metadata.RelationshipMetadataContainer;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Each instance of this class represents a structure element. This class extends <code>StructElementStub</code> and contains additional
 * memory-intensive members such as the corresponding Solr document, references to parent and child StructElements and full-text.
 */
public class StructElement extends StructElementStub implements Comparable<StructElementStub>, Serializable {

    private static final long serialVersionUID = 9048792944197887061L;

    private static final Logger logger = LogManager.getLogger(StructElement.class);

    /** If false; the Solr document with the given IDDOC does not exist in the index. */
    private boolean exists = false;
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
    /** Metadata describing the polygon that contains this docstruct within a page. */
    private List<ShapeMetadata> shapeMetadata;
    private ComplexMetadataContainer metadataDocuments = null;
    private StructElement topStruct = null;
    /** True if this record has a right-to-left reading direction. */
    private boolean rtl = false;
    private MimeType mimeType = new MimeType();
    private boolean hasImages = false;

    /**
     * Empty constructor for unit tests.
     */
    public StructElement() {
        super();
    }

    /**
     * Creates a new StructElement instance.
     *
     * @param luceneId Solr IDDOC of the document to load
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement(String luceneId) throws IndexUnreachableException {
        super(luceneId);
        init(null);
    }

    /**
     * Creates a new StructElement instance.
     *
     * @param luceneId Solr IDDOC of the document
     * @param doc pre-fetched Solr document; may be null to trigger a fresh load
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement(String luceneId, SolrDocument doc) throws IndexUnreachableException {
        super(luceneId);
        init(doc);
    }

    /**
     * Like {@link #StructElement(String, SolrDocument)}, but get the lucene Id from the SolrDocument.
     *
     * @param doc the Solr document to build the element from
     * @throws IndexUnreachableException
     */
    public StructElement(SolrDocument doc) throws IndexUnreachableException {
        this(getIDDOC(doc), doc);
    }

    public static String getIDDOC(SolrDocument doc) {
        if (doc.containsKey(SolrConstants.IDDOC)) {
            return SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
        }
        throw new IllegalArgumentException("Struct element cannot be initiated by Solr document with missing IDDOC value");
    }

    /**
     * Creates a new StructElement instance.
     *
     * @param luceneId Solr IDDOC of the document
     * @param doc primary Solr document to initialize from
     * @param docToMerge additional Solr document whose fields are merged into doc
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement(String luceneId, SolrDocument doc, SolrDocument docToMerge) throws IndexUnreachableException {
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
            if (docToMerge.getFieldValue(SolrConstants.DOCTYPE) != null && doc.getFieldValue(SolrConstants.DOCTYPE) == null) {
                doc.addField(SolrConstants.DOCTYPE, docToMerge.getFieldValue(SolrConstants.DOCTYPE));
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
            if (docToMerge.getFieldValue(SolrConstants.BOOL_DIRECTION_RTL) != null) {
                doc.addField(SolrConstants.BOOL_DIRECTION_RTL, docToMerge.getFieldValue(SolrConstants.BOOL_DIRECTION_RTL));
            }
        }
        init(doc);
    }

    /**
     * Initializes class properties from the given doc.
     *
     * @param solrDoc SolrDocument
     * @throws IndexUnreachableException
     */
    private final void init(final SolrDocument solrDoc) throws IndexUnreachableException {
        try {
            SolrDocument doc = solrDoc;
            if (doc == null) {
                doc = getDocument();
            }
            metadataFields = SolrTools.getFieldValueMap(doc);
            work = Boolean.valueOf(getMetadataValue(SolrConstants.ISWORK));
            anchor = Boolean.valueOf(getMetadataValue(SolrConstants.ISANCHOR));
            docType = DocType.getByName(getMetadataValue(SolrConstants.DOCTYPE));
            // Only load PI if for topstruct/anchor/group documents to avoid non-resolvable URLs
            if (work || anchor || DocType.GROUP.equals(docType)) {
                pi = getMetadataValue(SolrConstants.PI);
            }
            if (pi != null) {
                pi = pi.intern();
            }
            logid = getMetadataValue(SolrConstants.LOGID);
            if (anchor) {
                String numVolumeString = getMetadataValue(SolrConstants.NUMVOLUMES);
                if (numVolumeString != null) {
                    numVolumes = Long.valueOf(numVolumeString);
                }
            }
            docStructType = getMetadataValue(SolrConstants.DOCSTRCT);
            if (docStructType != null) {
                docStructType.intern();
            }
            cmsPage = "cms_page".equals(docStructType);
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
            hasImages = Boolean.valueOf(getMetadataValue(SolrConstants.BOOL_IMAGEAVAILABLE));
            mimeType = new MimeType(getMetadataValue(SolrConstants.MIMETYPE));
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
                }
                // Use a separate if (not else if) so that a field configured in both
                // ancestorIdentifierFields and recordGroupIdentifierFields (e.g. GROUPID_NEWSPAPER)
                // is correctly recognized as a group membership, enabling the calendar widget
                // for newspapers indexed without a traditional PI_ANCHOR structure.
                if (DataManager.getInstance().getConfiguration().getRecordGroupIdentifierFields().contains(fieldName)) {
                    groupMemberships.put(fieldName, (String) doc.getFieldValue(fieldName));
                }
            }
            rtl = Boolean.valueOf(getMetadataValue(SolrConstants.BOOL_DIRECTION_RTL));
            // Load shape metadata
            // TODO use indicator field in doc to avoid this extra search for non-shape elements
            String iddoc = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC)).map(Object::toString).orElse(null);
            if (iddoc != null) {
                SolrDocumentList shapeDocs =
                        MetadataTools.getGroupedMetadata(iddoc, " +" + SolrConstants.METADATATYPE + ':' + MetadataGroupType.SHAPE.name(), null);
                if (!shapeDocs.isEmpty()) {
                    this.shapeMetadata = new ArrayList<>(shapeDocs.size());
                    for (SolrDocument shapeDoc : shapeDocs) {
                        String label = getLabel();
                        String shape = SolrTools.getSingleFieldStringValue(shapeDoc, "MD_SHAPE");
                        String coords = SolrTools.getSingleFieldStringValue(shapeDoc, "MD_COORDS");
                        String order = String.valueOf(shapeDoc.getFieldValue(SolrConstants.ORDER));
                        this.shapeMetadata.add(new ShapeMetadata(label, shape, coords, getPi(),
                                "null".equals(order) ? getImageNumber() : Integer.parseInt(order), this.logid));
                    }
                }
            }
        } catch (PresentationException e) {
            // Catch exception to skip the rest of the code block, but do not do anything (already logged elsewhere)
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
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
        // logger.trace("getDocument(): {}", luceneId); //NOSONAR Debug
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(SolrConstants.IDDOC + ":" + luceneId, null);
        if (doc != null) {
            exists = true;
            return doc;
        }
        logger.warn(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, luceneId);
        throw new PresentationException("errDocNotFound");
    }

    public ComplexMetadataContainer getMetadataDocuments() throws PresentationException, IndexUnreachableException {
        if (this.metadataDocuments == null) {
            this.metadataDocuments = loadMetadataDocuments();
        }

        return this.metadataDocuments;
    }

    private ComplexMetadataContainer loadMetadataDocuments() throws PresentationException, IndexUnreachableException {
        return RelationshipMetadataContainer.loadRelationshipMetadata(this.pi, DataManager.getInstance().getSearchIndex());
    }

    /**
     * isHasParentOrChildren.
     *
     * @return true if this struct element has a parent element or at least one child element in the index, false otherwise
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasParentOrChildren() throws PresentationException, IndexUnreachableException {
        return isHasParent() || isHasChildren();
    }

    /**
     * isHasParent.
     *
     * @return true if this struct element has a parent element (i.e. IDDOC_PARENT is set in the Solr document), false otherwise
     */
    public boolean isHasParent() {
        return getMetadataValue(SolrConstants.IDDOC_PARENT) != null;
    }

    @Override
    public int getNumPages() {
        String numPages = getMetadataValue(SolrConstants.NUMPAGES);
        if (StringUtils.isNotBlank(numPages)) {
            try {
                return Integer.parseInt(numPages);
            } catch (NumberFormatException e) {
                logger.error("Invalid NUMPAGES value: {}", numPages);
            }
        }
        try {
            return super.getNumPages();
        } catch (IndexUnreachableException e) {
            logger.error(e.toString());

            return 0;
        }
    }

    /**
     * Loads and returns the immediate parent StructElement of this element.
     *
     * @return {@link io.goobi.viewer.model.viewer.StructElement}
     * @should return parent correctly
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getParent() throws IndexUnreachableException {
        //        logger.trace("getParent"); //NOSONAR Debug
        StructElement parent = null;
        try {
            String parentIddoc = getMetadataValue(SolrConstants.IDDOC_PARENT);
            if (parentIddoc != null) {
                parent = new StructElement(parentIddoc, null);
            }
        } catch (NumberFormatException e) {
            logger.error("Malformed number with get the parent element for Lucene IDDOC: {}", luceneId);
        }

        return parent;
    }

    /**
     *
     * @return IDDOC value of the parent document as a {@link Long}
     */
    public String getParentLuceneId() {
        String parentIddoc = getMetadataValue(SolrConstants.IDDOC_PARENT);
        if (StringUtils.isBlank(parentIddoc)) {
            return null;
        }

        try {
            return parentIddoc;
        } catch (NumberFormatException e) {
            logger.error("Malformed number with get the parent element for Lucene IDDOC: {}", luceneId);
            return null;
        }
    }

    /**
     * Checks whether the Solr document represented by this StructElement has child elements in the index.
     *
     * @return true if at least one Solr document references this element as its parent, false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public boolean isHasChildren() throws IndexUnreachableException, PresentationException {
        if (hasChildren == null) {
            if (DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString()) > 0) {
                hasChildren = true;
            } else {
                hasChildren = false;
            }
        }

        return hasChildren;
    }

    /**
     * Returns a StructElement that represents the top non-anchor element of the hierarchy (ISWORK=true). If the element itself is an anchor, itself
     * will be returned. If no topStruct element is found because no metadata {@link io.goobi.viewer.solr.SolrConstants#IDDOC_TOPSTRUCT} is found or
     * because it could not be resolved, null is returned
     *
     * @should retrieve top struct correctly
     * @should return self if topstruct
     * @should return self if anchor
     * @should return self if group
     * @return the top-level StructElement for this record, or null if it cannot be resolved
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getTopStruct() throws PresentationException, IndexUnreachableException {
        if (work || anchor || isGroup()) {
            this.topStruct = this;
            return this;
        }

        if (this.topStruct == null) {
            String topstructIddoc = getMetadataValue(SolrConstants.IDDOC_TOPSTRUCT);
            try {
                if (topstructIddoc != null) {
                    this.topStruct = new StructElement(topstructIddoc, null);
                }
            } catch (NumberFormatException e) {
                logger.error("Malformed number with get the topstruct element for Lucene IDDOC: {}", topstructIddoc);
            }
        }

        return this.topStruct;
    }

    public void setTopStruct(StructElement topStruct) {
        this.topStruct = topStruct;
    }

    /**
     * isGroupMember.
     *
     * @return true if this struct element belongs to at least one group record, false otherwise
     */
    public boolean isGroupMember() {
        return !groupMemberships.isEmpty();
    }

    /**
     * Returns the label for the group with the given identifier.
     *
     * @param groupIdentifier Group record identifier
     * @param altValue Message key to return if no label was found
     * @return label value for the group record; given message key if none found
     * @should return altValue of no label was found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getGroupLabel(String groupIdentifier, String altValue) throws IndexUnreachableException {
        if (groupIdentifier == null) {
            throw new IllegalArgumentException("groupIdentifier may not be null");
        }

        if (groupLabels.get(groupIdentifier) == null) {
            try {
                SolrDocument doc = DataManager.getInstance()
                        .getSearchIndex()
                        .getFirstDoc(SolrConstants.PI + ":" + groupIdentifier, Collections.singletonList(SolrConstants.LABEL));
                if (doc != null) {
                    String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                    if (label != null) {
                        groupLabels.put(groupIdentifier, label);
                    }
                }
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
            // If label doesn't exist or there has been an error while retrieving it, use the given alternative value
            if (groupLabels.get(groupIdentifier) == null && StringUtils.isNotEmpty(altValue)) {
                groupLabels.put(groupIdentifier, altValue);
            }
        }

        return groupLabels.get(groupIdentifier);
    }

    /**
     * isExists.
     *
     * @return true if this struct element exists in the Solr index, false otherwise
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * isDeleted.
     *
     * @return true if this struct element has been marked as deleted (i.e. DATEDELETED is set in the Solr document), false otherwise
     */
    public boolean isDeleted() {
        return getMetadataValue(SolrConstants.DATEDELETED) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @returns the identifier of the record to which this struct element belongs.
     * @should return pi if topstruct
     * @should retriveve pi from topstruct if not topstruct
     */
    @Override
    public String getPi() {
        if (pi != null && !pi.equals("null")) {
            return pi;
        } else if (getMetadataValue(SolrConstants.PI_TOPSTRUCT) != null) {
            pi = getMetadataValue(SolrConstants.PI_TOPSTRUCT);
            return pi;
        } else if (!work && !anchor) {
            try {
                return Optional.ofNullable(this.getTopStruct()).map(StructElement::getPi).orElse(null);
            } catch (PresentationException | IndexUnreachableException e) {
                return null;
            }
        }

        return null;

    }

    /** {@inheritDoc} */
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
     * getImageUrl.
     *
     * @param width a int.
     * @param height a int.
     * @return Image URL
     * @should construct url correctly
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getImageUrl(int width, int height) throws ViewerConfigurationException {
        return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(this, width, height);
    }

    /**
     * generateEventElements.
     *
     * @param locale locale used for translated metadata values
     * @param forSearchHit If true, only search hit metadata will be populated in the event; if false main and sidebar metadata
     * @return a list of event elements linked to this struct element
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<EventElement> generateEventElements(Locale locale, boolean forSearchHit) throws IndexUnreachableException {
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
            if (!result.isEmpty()) {
                logger.trace("{} events found", result.size());
            }
            List<EventElement> ret = new ArrayList<>(result.size());
            for (SolrDocument doc : result) {
                EventElement event = new EventElement(doc, locale, forSearchHit);
                ret.add(event);
            }
            return ret;
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * isAnchorChild.
     *
     * @should return true if current record is volume
     * @should return false if current record is not volume
     * @return true if this struct element is a volume (a work that has a parent anchor record), false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isAnchorChild() throws IndexUnreachableException {
        return isWork() && isHasParent();
    }

    /**
     * getCollection.
     *
     * @return the first collection (DC field) this struct element belongs to
     */
    public String getCollection() {
        return this.getMetadataValue(SolrConstants.DC);
    }

    /**
     * getCollections.
     *
     * @return a list of collection names (DC field values) this struct element belongs to
     */
    public List<String> getCollections() {
        return this.getMetadataValues(SolrConstants.DC);
    }

    /**
     * isFulltextAvailable.
     *
     * @return true if fulltext content is available for this document, false otherwise
     */
    public boolean isFulltextAvailable() {
        return fulltextAvailable;
    }

    /**
     * Setter for the field <code>fulltextAvailable</code>.
     *
     * @param fulltextAvailable true if fulltext content is available for this document
     */
    public void setFulltextAvailable(boolean fulltextAvailable) {
        this.fulltextAvailable = fulltextAvailable;
    }

    /**
     * Returns true if the record has any ALTO documents indexed in its pages; false otherwise.
     *
     * @return true if at least one page has an ALTO file indexed, false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public boolean isAltoAvailable() throws IndexUnreachableException, PresentationException {
        if (altoAvailable == null) {
            altoAvailable = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(SolrConstants.PI_TOPSTRUCT + ":" + pi + " AND " + SolrConstants.FILENAME_ALTO + ":*") > 0;
        }

        return altoAvailable;
    }

    /**
     * Returns true if the record has any NE_* tags indexed in its pages; false otherwise.
     *
     * @return true if at least one page has named entity tags indexed, false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public boolean isNerAvailable() throws IndexUnreachableException, PresentationException {
        if (nerAvailable == null) {
            nerAvailable = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(SolrConstants.PI_TOPSTRUCT + ":" + pi + " AND (NE_PERSON" + SolrConstants.SUFFIX_UNTOKENIZED + ":* OR NE_LOCATION"
                            + SolrConstants.SUFFIX_UNTOKENIZED + ":* OR NE_CORPORATION" + SolrConstants.SUFFIX_UNTOKENIZED + ":*)") > 0;
        }

        return nerAvailable;
    }

    /**
     *
     * @return true if permission granted; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public boolean isAccessPermissionDownloadMetadata() throws IndexUnreachableException, DAOException {
        // logger.trace("isAccessPermissionDownloadMetadata"); //NOSONAR Debug
        return isAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, null);
    }

    /**
     * @param request the request for this resource
     * @return true if permission granted; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public boolean isAccessPermissionDownloadMetadata(HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("isAccessPermissionDownloadMetadata"); //NOSONAR Debug
        return isAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_METADATA, request);
    }

    /**
     *
     * @param privilege Privilege name to check
     * @param request the http request for this resource. If null, the request it retrieved from the current {@link FacesContext} if present
     * @return true if current user has the privilege for this record; false otherwise. Also return false if no record was found
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    boolean isAccessPermission(String privilege, HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("isAccessPermission: {}", privilege); //NOSONAR Debug
        HttpServletRequest usedRequest = request;
        if (request == null) {
            usedRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }
        try {
            return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), logid, privilege, usedRequest).isGranted();
        } catch (RecordNotFoundException e) {
            return false;
        }
    }

    /**
     *
     * @param language the ISO language code to check
     * @return true if a TEI file name is indexed for the given language; false otherwise
     */
    public boolean isHasTeiForLanguage(String language) {
        if (StringUtils.isNotEmpty(language)) {
            String key = SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG + language.toUpperCase();
            // logger.trace("isHasTeiForLanguage: {}", key);
            return getMetadataFields().containsKey(key);
        }

        return getMetadataFields().containsKey(SolrConstants.FILENAME_TEI);
    }

    public boolean isHasTei() {
        return getMetadataFields().keySet().stream().anyMatch(k -> k.startsWith(SolrConstants.FILENAME_TEI));
    }

    public boolean isHasMei() {
        return getMetadataFields().keySet().stream().anyMatch(k -> k.startsWith(SolrConstants.FILENAME_MEI));
    }

    /**
     * Returns a stub representation of this object that only contains simple members to conserve memory.
     *
     * @return the lightweight StructElementStub representation of this element
     * @should create stub correctly
     */
    public StructElementStub createStub() {
        StructElementStub ret = new StructElementStub(luceneId);
        ret.setPi(getPi());
        ret.setLogid(logid);
        ret.setDocStructType(getDocStructType());
        ret.setDocType(docType);
        ret.setSourceDocFormat(sourceDocFormat);
        ret.setImageNumber(getImageNumber());
        ret.setWork(work);
        ret.setAnchor(anchor);
        ret.setVolume(volume);
        ret.setCmsPage(cmsPage);
        ret.setLabel(label);
        ret.setMetadataFields(metadataFields);

        return ret;
    }

    /**
     * Getter for the field <code>ancestors</code>.
     *
     * @return map of ancestor IDDOC values to their display labels
     */
    public Map<String, String> getAncestors() {
        return ancestors;
    }

    /**
     * Getter for the field <code>groupMemberships</code>.
     *
     * @return map of group identifiers to their display labels for this element
     */
    public Map<String, String> getGroupMemberships() {
        return groupMemberships;
    }

    /**
     * getMultiLanguageDisplayLabel.
     *
     * @return the multilingual display label derived from the TITLE or LABEL metadata fields
     */
    public IMetadataValue getMultiLanguageDisplayLabel() {
        IMetadataValue label = getMultiLanguageMetadataValue(SolrConstants.TITLE);
        if (label.isEmpty()) {
            label = getMultiLanguageMetadataValue(SolrConstants.LABEL);
            if (label.isEmpty()) {
                label = ViewerResourceBundle.getTranslations(getDocStructType());
            }
        }

        return label;
    }

    /**
     * Returns the group field name of a group document.
     *
     * @return the GROUPTYPE metadata value identifying the group field of this document
     */
    public String getGroupIdField() {
        return getMetadataValue(SolrConstants.GROUPTYPE);
    }

    /**
     * getFirstVolumeFieldValue.
     *
     * @should return correct value
     * @should return null if StructElement not anchor
     * @should throw IllegalArgumentException if field is null
     * @param field Solr field name to retrieve the value of
     * @return the value of the given Solr field from the first child volume of this anchor element
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFirstVolumeFieldValue(String field) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (anchor) {
            SolrDocument docParent = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(new StringBuilder(SolrConstants.IDDOC_PARENT).append(':').append(luceneId).toString(),
                            Collections.singletonList(field), Collections.singletonList(new StringPair(SolrConstants.CURRENTNOSORT, "asc")));
            if (docParent == null) {
                logger.warn("Anchor (PI: {}) has no child element: Cannot determine appropriate value", pi);
            } else {
                return SolrTools.getSingleFieldStringValue(docParent, field);
            }
        } else if (isGroup()) {
            String groupIdField = getGroupIdField();
            if (StringUtils.isBlank(groupIdField)) {
                logger.warn("Group (PI: {}) has no GROUPTYPE field", getPi());
                return null;
            }
            String groupOrderField = groupIdField.replace(SolrConstants.PREFIX_GROUPID, SolrConstants.PREFIX_GROUPORDER);
            SolrDocument docChild = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(
                            new StringBuilder("+").append(groupIdField).append(":\"").append(getPi())
                                    .append("\" +").append(SolrConstants.ISWORK).append(":true").toString(),
                            Collections.singletonList(field), Collections.singletonList(new StringPair(groupOrderField, "asc")));
            if (docChild == null) {
                logger.warn("Group (PI: {}) has no child element: Cannot determine appropriate value", getPi());
            } else {
                return SolrTools.getSingleFieldStringValue(docChild, field);
            }
        }

        return null;
    }

    /**
     * getFirstVolume.
     *
     * @should return correct value
     * @should return null if StructElement not anchor
     * @should throw IllegalArgumentException if field is null
     * @param fields Solr field names to include in the child document query
     * @return the first child volume StructElement for an anchor, or null if none is found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getFirstVolume(List<String> fields) throws PresentationException, IndexUnreachableException {

        if (anchor) {
            List<StringPair> sortFields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(getDocStructType());

            SolrDocument docVolume = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(new StringBuilder(SolrConstants.IDDOC_PARENT).append(":\"").append(luceneId).append('"').toString(), fields,
                            sortFields);
            if (docVolume == null) {
                logger.warn("Anchor has no child element: Cannot determine appropriate value");
            } else {
                String iddoc = SolrTools.getSingleFieldStringValue(docVolume, SolrConstants.IDDOC);
                if (StringUtils.isNotBlank(iddoc)) {
                    return new StructElement(iddoc, docVolume);
                }
            }
        } else if (isGroup()) {
            String groupIdField = getGroupIdField();
            if (StringUtils.isBlank(groupIdField)) {
                logger.warn("Group (PI: {}) has no GROUPTYPE field", getPi());
                return null;
            }
            String groupOrderField = groupIdField.replace(SolrConstants.PREFIX_GROUPID, SolrConstants.PREFIX_GROUPORDER);
            List<StringPair> sortFields = Collections.singletonList(new StringPair(groupOrderField, "asc"));

            String query = new StringBuilder("+").append(groupIdField).append(":\"").append(getPi())
                    .append("\" +").append(SolrConstants.ISWORK).append(":true").toString();
            logger.trace("Group first volume query: {}", query);
            SolrDocument docVolume = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(query, fields, sortFields);
            if (docVolume == null) {
                logger.warn("Group (PI: {}) has no child element: Cannot determine appropriate value", getPi());
            } else {
                String iddoc = SolrTools.getSingleFieldStringValue(docVolume, SolrConstants.IDDOC);
                if (StringUtils.isNotBlank(iddoc)) {
                    return new StructElement(iddoc, docVolume);
                }
            }
        }

        return null;
    }

    /**
     * getFirstPageFieldValue.
     *
     * @param field Solr field name to retrieve the value of
     * @return the value of the given Solr field from the first physical page of this record
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFirstPageFieldValue(String field) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (work) {
            String query = new StringBuilder("DOCTYPE:PAGE AND ").append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).toString();
            List<StringPair> sortFields = Collections.singletonList(new StringPair("ORDER", ORDER.asc.name()));
            SolrDocumentList pages = DataManager.getInstance().getSearchIndex().search(query, 1, sortFields, null);
            if (!pages.isEmpty()) {
                return SolrTools.getSingleFieldStringValue(pages.get(0), field);
            }
        } else {
            try {
                int thumbPageNo = Integer.parseInt(getMetadataValue(SolrConstants.THUMBPAGENO));
                String topStructId = getMetadataValue(SolrConstants.PI_TOPSTRUCT);
                String query = new StringBuilder().append('+')
                        .append(SolrConstants.DOCTYPE)
                        .append(':')
                        .append(DocType.PAGE.name())
                        .append(" +")
                        .append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(topStructId)
                        .append(" +")
                        .append(SolrConstants.ORDER)
                        .append(':')
                        .append(thumbPageNo)
                        .toString();
                SolrDocumentList pages = DataManager.getInstance().getSearchIndex().search(query, 1, null, null);
                if (!pages.isEmpty()) {
                    return SolrTools.getSingleFieldStringValue(pages.get(0), field);
                }
            } catch (NullPointerException | NumberFormatException e) {
                // logger.trace(e.getMessage()); //NOSONAR Debug
            }
        }

        return null;
    }

    public boolean hasShapeMetadata() {
        return shapeMetadata != null && !shapeMetadata.isEmpty();
    }

    /**
     * 
     * @param order Page order
     * @return List<ShapeMetadata>
     */
    public List<ShapeMetadata> getShapeMetadataForPage(int order) {
        List<ShapeMetadata> ret = new ArrayList<>();
        for (ShapeMetadata smd : shapeMetadata) {
            if (smd.getPageNo() == order) {
                ret.add(smd);
            }
        }

        return ret;
    }

    
    public List<ShapeMetadata> getShapeMetadata() {
        return shapeMetadata;
    }

    
    public void setShapeMetadata(List<ShapeMetadata> shapeMetadata) {
        this.shapeMetadata = shapeMetadata;
    }

    
    public boolean isRtl() {
        return rtl;
    }

    
    public void setRtl(boolean rtl) {
        this.rtl = rtl;
    }

    /**
     * Shape metadata for docstructs that represent only a portion of a page (or several pages).
     */
    public class ShapeMetadata implements Serializable {

        private static final long serialVersionUID = -4043298882984117424L;

        /** Display label. */
        private final String label;
        /** Type of shape (currently only RECT). */
        private final String shape;
        /** Shape coordinates. */
        private final String coords;
        private final String logId;
        private final String structPi;
        private final int pageNo;

        /**
         * Constructor.
         *
         * @param label the human-readable label for this shape
         * @param shape the type of shape (e.g. RECT)
         * @param coords the shape's coordinate string
         * @param pi the persistent identifier of the associated record
         * @param pageNo the physical page number the shape is on
         * @param logId the logical struct ID the shape belongs to
         */
        public ShapeMetadata(String label, String shape, String coords, String pi, int pageNo, String logId) {
            this.label = label;
            this.shape = shape;
            this.coords = coords;
            this.logId = logId;
            this.structPi = pi;
            this.pageNo = pageNo;
        }

        
        public String getLabel() {
            return label;
        }

        
        public String getShape() {
            return shape;
        }

        
        public String getCoords() {
            return coords;
        }

        
        public String getUrl() {
            PageType pageType =
                    Optional.ofNullable(BeanUtils.getNavigationHelper()).map(NavigationHelper::getCurrentPageType).orElse(PageType.viewImage);
            return getUrl(pageType);
        }

        /**
         *
         * @param pageType the page type used for URL building
         * @return Constructed URL
         */
        public String getUrl(PageType pageType) {
            StringBuilder sbUrl = new StringBuilder();
            sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                    .append('/')
                    .append(DataManager.getInstance().getUrlBuilder().buildPageUrl(structPi, pageNo, logId, pageType, false));
            return sbUrl.toString();
        }

        
        public String getLogId() {

            return logId;
        }

        
        public int getPageNo() {
            return pageNo;
        }
    }

    /**
     * Creates a StructElement from a Solr document.
     *
     * @param solrDoc the Solr document to wrap
     * @return the corresponding StructElement, or null if the document cannot be loaded
     */
    public static StructElement create(SolrDocument solrDoc) {
        try {
            return new StructElement(solrDoc);
        } catch (IndexUnreachableException e) {
            logger.error(e.toString());
            return null;
        }
    }

    @Override
    public boolean isHasImages() {
        return this.hasImages;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

}
