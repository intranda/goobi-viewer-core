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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.COLLECTIONS_COLLECTION;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.wa.ImageResource;
import de.intranda.api.iiif.presentation.CollectionExtent;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.metadata.multilanguage.Metadata;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * CollectionBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class CollectionBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(CollectionBuilder.class);

    /**
     * Required field to create manifest stubs for works in collection
     */
    public static final String[] CONTAINED_WORKS_QUERY_FIELDS =
            { SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT,
                    SolrConstants.IDDOC };
    public static final String RSS_FEED_LABEL = "Rss feed";
    public static final String RSS_FEED_FORMAT = "Rss feed";

    /**
     * 
     * @param apiUrlManager
     */
    public CollectionBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * 
     * @param collectionField
     * @return {@link Collection3}
     * @throws IndexUnreachableException
     */
    public Collection3 build(String collectionField) throws IndexUnreachableException {

        URI baseId = urls.path(COLLECTIONS).params(collectionField).buildURI();
        Collection3 baseCollection = new Collection3(baseId, null);
        baseCollection.setLabel(ViewerResourceBundle.getTranslations("browse", false));
        addRequiredStatement(baseCollection);

        List<CollectionResult> collections = dataRetriever.getTopLevelCollections(collectionField);
        for (CollectionResult collectionResult : collections) {
            Collection3 collection = createCollection(collectionField, collectionResult.getName());
            baseCollection.addItem(collection);
        }

        return baseCollection;

    }

    /**
     * 
     * @param collectionField
     * @param collectionName
     * @return {@link Collection3}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Collection3 build(String collectionField, String collectionName) throws IndexUnreachableException, PresentationException {

        Collection3 baseCollection = createCollection(collectionField, collectionName);
        addRequiredStatement(baseCollection);

        List<CollectionResult> collections = dataRetriever.getChildCollections(collectionField, collectionName);
        for (CollectionResult collectionResult : collections) {
            Collection3 collection = createCollection(collectionField, collectionResult.getName());
            CollectionExtent extent = new CollectionExtent(collectionResult.getChildCount().intValue(), (int) collectionResult.getCount().intValue());
            collection.addService(extent);
            baseCollection.addItem(collection);
        }
        //        CollectionExtent extent = new CollectionExtent(collectionResult.getChildCount().intValue(), (int) collectionResult.getCount().intValue());
        //        baseCollection.addService(extent);

        List<StructElement> records = dataRetriever.getContainedRecords(collectionField, collectionName);
        for (StructElement rec : records) {
            if (rec.isAnchor()) {
                Collection3 manifest = createAnchorLink(rec);
                baseCollection.addItem(manifest);
            } else {
                Manifest3 manifest = createRecordLink(rec);
                baseCollection.addItem(manifest);
            }
        }

        return baseCollection;

    }

    protected void addRequiredStatement(Collection3 baseCollection) {
        VariableReplacer variableReplacer = new VariableReplacer(DataManager.getInstance().getConfiguration());
        Metadata requiredStatement = variableReplacer.replace(getRequiredStatement());
        if (!requiredStatement.getValue().isEmpty()) {
            baseCollection.setRequiredStatement(variableReplacer.replace(getRequiredStatement()));
        }
    }

    /**
     * 
     * @param collectionField
     * @param collectionName
     * @return {@link Collection3}
     */
    private Collection3 createCollection(String collectionField, String collectionName) {
        URI id = urls.path(COLLECTIONS, COLLECTIONS_COLLECTION).params(collectionField, collectionName).buildURI();
        Collection3 collection = new Collection3(id, collectionName);
        CMSCollection cmsCollection = null;
        try {
            cmsCollection = DataManager.getInstance().getDao().getCMSCollection(collectionField, collectionName);
        } catch (DAOException e) {
            logger.error("Error getting CMS collections data from DAO for {}", collectionName, e);
        }
        if (cmsCollection != null) {
            collection.setLabel(cmsCollection.getTranslationsForName());
            collection.setDescription(cmsCollection.getTranslationsForDescription());

            ImageResource thumb = createThumbnail(collectionName, cmsCollection);
            if (thumb != null) {
                collection.addThumbnail(thumb);
            }

        } else {
            collection.setLabel(ViewerResourceBundle.getTranslations(collectionName, false));
            ImageResource thumb = new ImageResource(null);
            collection.addThumbnail(thumb);
        }
        return collection;
    }

    /**
     * 
     * @param collectionName
     * @param cmsCollection
     * @return {@link ImageResource}
     */
    private ImageResource createThumbnail(String collectionName, CMSCollection cmsCollection) {
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
                logger.error("Error creating thumbnail for record {}", collectionName, e);
                thumb = new ImageResource(null);
            }
        } else {
            thumb = new ImageResource(null);
        }
        return thumb;
    }

}
