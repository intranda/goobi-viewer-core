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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.wa.ImageResource;
import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.image.v3.ImageInformation3;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.Metadata;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * CollectionBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class CollectionBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CollectionBuilder.class);

    /**
     * Required field to create manifest stubs for works in collection
     */
    public static final String[] CONTAINED_WORKS_QUERY_FIELDS =
            { SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT,
                    SolrConstants.IDDOC };
    public final static String RSS_FEED_LABEL = "Rss feed";
    public final static String RSS_FEED_FORMAT = "Rss feed";

    private final int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
    private final int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();

    public CollectionBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    public Collection3 build(String collectionField) throws IndexUnreachableException {

        URI baseId = urls.path(COLLECTIONS).params(collectionField).buildURI();
        Collection3 baseCollection = new Collection3(baseId, null);
        baseCollection.setRequiredStatement(getRequiredStatement());

        List<CollectionResult> collections = dataRetriever.getTopLevelCollections(collectionField);
        for (CollectionResult collectionResult : collections) {
            Collection3 collection = createCollection(collectionField, collectionResult.getName());
            baseCollection.addItem(collection);
        }

        return baseCollection;

    }

    public Collection3 build(String collectionField, String collectionName) throws IndexUnreachableException, PresentationException {

        Collection3 baseCollection = createCollection(collectionField, collectionName);
        baseCollection.setRequiredStatement(getRequiredStatement());

        List<CollectionResult> collections = dataRetriever.getChildCollections(collectionField, collectionName);
        for (CollectionResult collectionResult : collections) {
            Collection3 collection = createCollection(collectionField, collectionResult.getName());
            baseCollection.addItem(collection);
        }

        List<StructElement> records = dataRetriever.getContainedRecords(collectionField, collectionName);
        for (StructElement record : records) {
            if(record.isAnchor()) {
                Collection3 manifest = createAnchorLink(collectionField, collectionName, record);
                baseCollection.addItem(manifest);
            } else {                
                Manifest3 manifest = createRecordLink(collectionField, collectionName, record);
                baseCollection.addItem(manifest);
            }
        }

        return baseCollection;

    }

    private Manifest3 createRecordLink(String collectionField, String collectionName, StructElement record) {
        URI id = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(collectionField, collectionName).buildURI();
        Manifest3 manifest = new Manifest3(id);
        try {
            manifest.addThumbnail(getThumbnail(record.getPi()));
        } catch (IndexUnreachableException | PresentationException | ViewerConfigurationException e) {
            logger.error("Error creating thumbnail for record " + record.getPi());
        }
        manifest.setLabel(record.getMultiLanguageDisplayLabel());
        return manifest;
    }
    
    private Collection3 createAnchorLink(String collectionField, String collectionName, StructElement record) {
        URI id = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(collectionField, collectionName).buildURI();
        Collection3 manifest = new Collection3(id, null);
        manifest.addViewingHint(ViewingHint.multipart);
        manifest.addThumbnail(getThumbnail(record));
        manifest.setLabel(record.getMultiLanguageDisplayLabel());
        return manifest;
    }

    private Metadata getRequiredStatement() {
        Metadata requiredStatement = null;
        List<String> attributions = DataManager.getInstance().getConfiguration().getIIIFAttribution();
        if (!attributions.isEmpty()) {
            IMetadataValue attributionLabel = ViewerResourceBundle.getTranslations("attribution", false);
            IMetadataValue attributionValue = new SimpleMetadataValue(attributions.stream().collect(Collectors.joining("\n")));
            requiredStatement = new Metadata(attributionLabel, attributionValue);
        }
        return requiredStatement;
    }

    private Collection3 createCollection(String collectionField, String collectionName) {
        URI id = urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, collectionName).buildURI();
        Collection3 collection = new Collection3(id, collectionName);
        CMSCollection cmsCollection = null;
        try {
            cmsCollection = DataManager.getInstance().getDao().getCMSCollection(collectionField, collectionName);
        } catch (DAOException e) {
            logger.error("Error getting CMS collections data from DAO for " + collectionName, e);
        }
        if (cmsCollection != null) {
            collection.setLabel(cmsCollection.getTranslationsForName());
            collection.setDescription(cmsCollection.getTranslationsForDescription());

            ImageResource thumb = createThumbnail(collectionField, collectionName, cmsCollection);
            collection.addThumbnail(thumb);

        } else {
            collection.setLabel(ViewerResourceBundle.getTranslations(collectionName, false));
            ImageResource thumb = new ImageResource(CMSCollection.getDefaultIcon(collectionField));
            collection.addThumbnail(thumb);
        }
        return collection;
    }

    private ImageResource createThumbnail(String collectionField, String collectionName, CMSCollection cmsCollection) {
        ImageResource thumb;
        if (cmsCollection.hasMediaItem()) {

            AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getCMSMediaImageApiManager(Version.v2);
            if (urls != null) {
                String thumbURI = urls.path(CMS_MEDIA, CMS_MEDIA_FILES_FILE).params(cmsCollection.getMediaItem().getFileName()).build();
                thumb = new ImageResource(thumbURI, thumbWidth, thumbHeight);
            } else {
                thumb = new ImageResource(cmsCollection.getIconURI(thumbWidth, thumbHeight));
            }

        } else if (cmsCollection.hasRepresentativeWork()) {
            String pi = cmsCollection.getRepresentativeWorkPI();
            try {
                thumb = getThumbnail(pi);
            } catch (IndexUnreachableException | PresentationException | ViewerConfigurationException e) {
                logger.error("Error creating thumbnail for record " + collectionName, e);
                thumb = new ImageResource(CMSCollection.getDefaultIcon(collectionField));
            }
        } else {
            thumb = new ImageResource(CMSCollection.getDefaultIcon(collectionField));
        }
        return thumb;
    }

    private ImageResource getThumbnail(String pi) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        ImageResource thumb;
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getContentApiManager(Version.v2).orElse(null);
        if (urls != null) {
            thumb = new ImageResource(urls.path(RECORDS_RECORD, RECORDS_IMAGE).params(pi).build(), thumbWidth, thumbHeight);
        } else {
            thumb = new ImageResource(URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(pi)));
        }
        return thumb;

    }
    
    private ImageResource getThumbnail(StructElement ele) {
            try {
                String thumbUrl = BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(ele);
                if (StringUtils.isNotBlank(thumbUrl)) {
                    ImageResource thumb = new ImageResource(new URI(thumbUrl));
                    if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                        String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl);
                        thumb.setService(new ImageInformation3(imageInfoURI));
                    }
                    return thumb;
                }
            } catch (URISyntaxException e) {
                logger.warn("Unable to retrieve thumbnail url", e);
            }
            return null;
    }

}
