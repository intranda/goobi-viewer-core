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
package de.intranda.digiverso.presentation.servlets.rest.cms;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.ImageContentLinkSerializer;
import de.intranda.digiverso.presentation.servlets.rest.iiif.presentation.MetadataSerializer;

/**
 * @author Florian Alpers
 *
 */
@Path("/cms/media")
@ViewerRestServiceBinding
public class CMSMediaResource {

        private static final Logger logger = LoggerFactory.getLogger(CMSContentResource.class);
        @Context
        protected HttpServletRequest servletRequest;
        @Context
        protected HttpServletResponse servletResponse;

        @GET
        @Path("/get/{tag}")
        @Produces({ MediaType.APPLICATION_JSON })
        public MediaList getMediaByTag(@PathParam("tag") String tag) throws DAOException  {
            
            List<CMSMediaItem> items = DataManager.getInstance().getDao().getAllCMSMediaItems().stream().filter(item -> item.getTags().contains(tag)).collect(Collectors.toList());
            return new MediaList(items);
        }
        
        @GET
        @Path("/get")
        @Produces({ MediaType.APPLICATION_JSON })
        public MediaList getAllMedia(@PathParam("tag") String tag) throws DAOException  {
            
            List<CMSMediaItem> items = DataManager.getInstance().getDao().getAllCMSMediaItems();
            return new MediaList(items);
        }

    public class MediaList {
        
        private final List<MediaItem> mediaItems;

        public MediaList(List<CMSMediaItem> items) {
            this.mediaItems = items.stream().map(MediaItem::new).collect(Collectors.toList());
        }
        
        /**
         * @return the mediaItems
         */
        public List<MediaItem> getMediaItems() {
            return mediaItems;
        };
        
    }
    
    public class MediaItem {
        
        @JsonSerialize(using=MetadataSerializer.class)
        private final IMetadataValue label;
        @JsonSerialize(using=MetadataSerializer.class)
        private final IMetadataValue description;
        private final String link;
        private final ImageContent image;
        private final List<String> tags;
        
        public MediaItem(CMSMediaItem source) {
            this.label = source.getTranslationsForName();
            this.description = source.getTranslationsForDescription();
            this.image = new ImageContent(source.getIconURI(), true);
            this.link = Optional.ofNullable(source.getLinkURI(servletRequest)).map(URI::toString).orElse("#");
            this.tags = source.getTags();
        }

        /**
         * @return the label
         */
        public IMetadataValue getLabel() {
            return label;
        }

        /**
         * @return the description
         */
        public IMetadataValue getDescription() {
            return description;
        }

        /**
         * @return the link
         */
        public String getLink() {
            return link;
        }
        
        /**
         * @return the image
         */
        public ImageContent getImage() {
            return image;
        }

        /**
         * @return the tags
         */
        public List<String> getTags() {
            return tags;
        }
        
        
    }
}
