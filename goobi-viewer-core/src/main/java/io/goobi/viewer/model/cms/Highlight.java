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
package io.goobi.viewer.model.cms;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.HighlightData.TargetType;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

public class Highlight implements CMSMediaHolder, IPolyglott {
    
    private static final Logger logger = LogManager.getLogger(Highlight.class);

    private final HighlightData data;
    private final ThumbnailHandler thumbs;
    private final Configuration configuration;
    private Map<Locale, List<Metadata>> metadata = new HashMap<>();
    private MetadataElement metadataElement = null;

    public Highlight(HighlightData data, ThumbnailHandler thumbs, Configuration config) {
        if (data == null || thumbs == null) {
            throw new NullPointerException("Constructor arguments may not be null");
        }
        this.data = new HighlightData(data);
        this.thumbs = thumbs;
        this.configuration = config;
    }

    public Highlight(HighlightData data) {
        this(data, BeanUtils.getImageDeliveryBean().getThumbs(), DataManager.getInstance().getConfiguration());
    }

    @Override
    public boolean isComplete(Locale locale) {
        return this.data.getName().isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return this.data.getName().isValid(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.data.getName().isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.data.getName().getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.data.getName().setSelectedLocale(locale);
    }

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    @Override
    public boolean hasMediaItem() {
        return this.data.getMediaItem() != null;
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return this.data.getMediaItem();
    }

    @Override
    public void setMediaItem(CMSMediaItem item) {
        this.data.setMediaItem(item);
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(getMediaItem(), true,
                    getMediaItem().getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    public HighlightData getData() {
        return data;
    }

    /**
     * Check whether an image is set for this object
     * 
     * @return true if the image is taken from the record identifier or if it is taken from a media item and the media item is set
     */
    public boolean hasImageURI() {
        switch (this.data.getImageMode()) {
            case RECORD_REPRESENTATIVE:
                return true;
            case UPLOADED_IMAGE:
                return hasMediaItem();
            default:
                return false;
        }
    }

    /**
     * Get the URI to the image representing the object
     * 
     * @return true if {@link #hasImageURI()} returns true and, if the image is taken from the record identifier, the record identifier points to a
     *         records which has a representative image
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public URI getImageURI(int width, int height) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        switch (this.data.getImageMode()) {
            case UPLOADED_IMAGE:
                return Optional.ofNullable(this.getMediaItem())
                        .map(mediaItem -> this.thumbs.getThumbnailUrl(mediaItem, width, height))
                        .map(URI::create)
                        .orElse(null);
            case RECORD_REPRESENTATIVE:
                SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getFirstDoc("PI:"+this.data.getRecordIdentifier(), List.of(SolrConstants.MIMETYPE));
                if(solrDoc != null) {
                    String mimeType = SolrTools.getSingleFieldStringValue(solrDoc, SolrConstants.MIMETYPE);
                    if(StringUtils.isNotBlank(mimeType)) {
                        ImageFileFormat format = ImageFileFormat.getImageFileFormatFromMimeType(mimeType);
                        if(format != null) {                            
                            return Optional.ofNullable(this.thumbs.getImageUrl(this.data.getRecordIdentifier(), width, height, ImageFileFormat.getMatchingTargetFormat(format).getFileExtension())).map(URI::create).orElse(null);
                        }
                    }
                }                   
                return Optional.ofNullable(this.thumbs.getThumbnailUrl(this.data.getRecordIdentifier(), width, height)).map(URI::create).orElse(null);
            default:
                return null;

        }
    }

    public URI getImageURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getImageURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * alias for {@link #isCurrent()}
     * 
     * @return true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isPresent() {
        return isCurrent();
    }

    /**
     * Check if the object is active now. Mutually exclusive with {@link Highlight#isPast()} and {@link #isFuture()}
     * 
     * @return true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isCurrent() {
        return isCurrent(LocalDateTime.now());
    }

    /**
     * Check if the object is active at the given date
     * 
     * @return true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isCurrent(LocalDateTime date) {
        return (data.getTimeStart() == null || date.isAfter(data.getTimeStart())) && (data.getTimeEnd() == null || date.isBefore(data.getTimeEnd()));
    }

    /**
     * Check if this object is no longer active. Mutually exclusive with {@link #isCurrent()} and {@link #isFuture()}
     * 
     * @return true if timeEnd is not null and before now
     */
    public boolean isPast() {
        return isPast(LocalDateTime.now());
    }

    /**
     * Check if this object was only active before the given date.
     * 
     * @return true if timeEnd is not null and before now
     */
    public boolean isPast(LocalDateTime date) {
        return data.getTimeEnd() != null && date.isAfter(data.getTimeEnd());
    }

    /**
     * Check if this object is not currently active but will be active in the future. Mutually exclusive with {@link #isCurrent()} and
     * {@link #isPast()}
     * 
     * @return true if startTime is not null and after now and timeEnd is either null or after now
     */
    public boolean isFuture() {
        return isFuture(LocalDateTime.now());
    }

    /**
     * Check if this object is not currently active but will be active in the future of the given date.
     * 
     * @return true if startTime is not null and after now and timeEnd is either null or after now
     */
    public boolean isFuture(LocalDateTime date) {
        return data.getTimeStart() != null && date.isBefore(data.getTimeStart());
    }

    /**
     * Check if this object may be active at all
     * 
     * @return
     */
    public boolean isEnabled() {
        return this.data.isEnabled();
    }

    @Override
    public String toString() {
        return "Data: " + this.getData().toString();
    }

    public List<Metadata> getMetadataList() throws IndexUnreachableException, PresentationException {
        return getMetadataList(BeanUtils.getLocale());
    }

    /**
     * 
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<Metadata> getMetadataList(Locale locale) throws IndexUnreachableException, PresentationException {
        List<Metadata> md = this.metadata.get(locale);
        if (md == null) {
            md = initMetadataList(locale);
            this.metadata.put(locale, md);
        }
        return md;
    }

    /**
     * 
     * @param field
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<Metadata> getMetadataForField(String field, Locale locale) throws IndexUnreachableException, PresentationException {
        List<Metadata> ret = new ArrayList<>();
        String languageField = field + (locale != null ? SolrConstants.MIDFIX_LANG + locale.getLanguage().toUpperCase() : "");
        logger.trace(languageField);
        for (Metadata md : getMetadataList(locale)) {
            if (md.getLabel().equals(languageField)) {
                ret.add(md);
                logger.trace("added " + md.getLabel());
            }
        }

        return ret;
    }

    /**
     * 
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private List<Metadata> initMetadataList(Locale locale) throws IndexUnreachableException, PresentationException {
        if (TargetType.RECORD == this.data.getTargetType()) {
            SolrDocument doc = loadSolrDocument(this.data.getRecordIdentifier());
            if (doc != null) {
                StructElement se = new StructElement(doc);
                List<Metadata> metadataList = configuration.getHighlightMetadataForTemplate(se.getDocStructType());
                List<Metadata> ret = new ArrayList<>(metadataList.size());
                for (Metadata md : metadataList) {
                    // Skip fields that have a different language code than the given locale
                    if (locale == null || !SolrTools.isHasWrongLanguageCode(md.getLabel(), locale.getLanguage())) {
                        md.populate(se, Long.toString(se.getLuceneId()), null, locale);
                        ret.add(md);
                    }
                }
                return ret;
            }
        }
        return Collections.emptyList();
    }

    public MetadataElement getMetadataElement() throws IndexUnreachableException, PresentationException {
        if (this.metadataElement == null) {
            SolrDocument solrDoc = loadSolrDocument(this.getData().getRecordIdentifier());
            if (solrDoc != null) {
                this.metadataElement = loadMetadataElement(solrDoc, 0);
                if (this.getData().getName().isEmpty()) {
                    this.getData().setName(createRecordTitle(solrDoc));
                }
            }
        }
        return this.metadataElement;
    }

    /**
     * @param note2
     * @param metadataElement2
     */
    private static TranslatedText createRecordTitle(SolrDocument solrDoc) {
        IMetadataValue label = TocMaker.buildTocElementLabel(solrDoc);
        TranslatedText text = createRecordTitle(label);
        text.setSelectedLocale(IPolyglott.getDefaultLocale());
        return text;
    }

    /**
     * @param label
     * @return
     */
    private static TranslatedText createRecordTitle(IMetadataValue label) {
        if (label instanceof MultiLanguageMetadataValue) {
            MultiLanguageMetadataValue mLabel = (MultiLanguageMetadataValue) label;
            return new TranslatedText(mLabel);
        }
        TranslatedText title = new TranslatedText();
        title.setValue(label.getValue().orElse(""), IPolyglott.getDefaultLocale());
        return title;
    }

    /**
     * @param recordPi
     * @param index Metadata view index
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private MetadataElement loadMetadataElement(SolrDocument solrDoc, int index) throws PresentationException, IndexUnreachableException {
        StructElement structElement = new StructElement(solrDoc);
        return new MetadataElement().init(structElement, index, BeanUtils.getLocale())
                .setSelectedRecordLanguage(this.getSelectedLocale().getLanguage());

    }

    private static SolrDocument loadSolrDocument(String recordPi) throws IndexUnreachableException, PresentationException {
        if (StringUtils.isBlank(recordPi)) {
            return null;
        }

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(recordPi);
        if (solrDoc == null) {
            return null;
        }
        return solrDoc;
    }

}
