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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.ISelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Manifest;
import io.goobi.viewer.api.rest.IApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

/**
 * @author florian
 *
 */
public class AnnotationsResourceBuilder {

    private final IApiUrlManager urls;

    private final static Logger logger = LoggerFactory.getLogger(AnnotationsResourceBuilder.class);

    public AnnotationsResourceBuilder(IApiUrlManager urls) {
        this.urls = urls;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public IResource getWebAnnotationCollectionForRecord(String pi, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForTarget(pi, null);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.buildCollection();
        return collection;
    }

    /**
     * @param pi
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForRecord(String pi, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(this::getAsOpenAnnotation).forEach(oa -> list.addResource(oa));
        return list;
    }

    /**
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationPage getWebAnnotationPageForRecord(String pi, URI uri) throws DAOException {
        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(this::getAsWebAnnotation).collect(Collectors.toList());
        AnnotationPage annoPage = builder.buildPage(annos, 1);
        return annoPage;
    }

    /**
     * @param format
     * @param uri
     * @return
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi, true);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.buildCollection();

        return collection;
    }

    public AnnotationPage getWebAnnotationPageForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi, true);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationPage annoPage = builder.buildPage(
                data.stream()
                        .map(c -> getAsOpenAnnotation(c))
                        .collect(Collectors.toList()),
                1);

        return annoPage;
    }

    public AnnotationList getOAnnotationListForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi, true);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(c -> getAsOpenAnnotation(c)).forEach(oa -> list.addResource(oa));
        return list;
    }

    public OpenAnnotation getAsOpenAnnotation(Comment comment) {
        OpenAnnotation anno = new OpenAnnotation(getOpenAnnotationCommentURI(comment.getPi(), comment.getPage(), comment.getId()));
        anno.setMotivation(Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas(URI.create(urls.path(RECORDS_RECORD, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    public WebAnnotation getAsWebAnnotation(Comment comment) {
        WebAnnotation anno = new WebAnnotation(getWebAnnotationCommentURI(comment.getPi(), comment.getPage(), comment.getId()));
        anno.setMotivation(Motivation.COMMENTING);
        if (comment.getPage() != null) {
            anno.setTarget(
                    new Canvas(URI.create(urls.path(RECORDS_RECORD, RECORDS_PAGES_CANVAS).params(comment.getPi(), comment.getPage()).build())));
        } else {
            anno.setTarget(new Manifest(URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(comment.getPi()).build())));
        }
        TextualResource body = new TextualResource(comment.getText());
        anno.setBody(body);
        return anno;
    }

    private URI getWebAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build());
    }

    private URI getOpenAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).query("format", "oa").build());
    }

    private URI getWebAnnotationCommentURI(String pi, Integer page, Long id) {
        String url;
        if (page != null) {
            url = urls.path(RECORDS_RECORD, RECORDS_PAGES_COMMENTS_COMMENT).params(pi, page, id).query("format", "oa").build();
        } else {
            url = urls.path(RECORDS_RECORD, RECORDS_COMMENTS_COMMENT).params(pi, id).query("format", "oa").build();
        }
        return URI.create(url);
    }

    private URI getOpenAnnotationCommentURI(String pi, Integer page, Long id) {
        String url;
        if (page != null) {
            url = urls.path(RECORDS_RECORD, RECORDS_PAGES_COMMENTS_COMMENT).params(pi, page, id).build();
        } else {
            url = urls.path(RECORDS_RECORD, RECORDS_COMMENTS_COMMENT).params(pi, id).build();
        }
        return URI.create(url);
    }

    /**
     * Get the annotation target as an WebAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        if (anno.getTarget() != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource;
            if (anno.getTarget().contains("SpecificResource")) {
                resource = mapper.readValue(anno.getTarget(), SpecificResource.class);
            } else {
                resource = mapper.readValue(anno.getTarget(), TypedResource.class);
            }
            return resource;
        }
        return null;
    }

    /**
     * Get the annotation target as an OpenAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsOAResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        IResource resource = getTargetAsResource(anno);
        if (resource != null) {
            if (resource instanceof SpecificResource && ((SpecificResource) resource).getSelector() instanceof FragmentSelector) {
                FragmentSelector selector = (FragmentSelector) ((SpecificResource) resource).getSelector();
                ISelector oaSelector = new de.intranda.api.annotation.oa.FragmentSelector(selector.getFragment());
                IResource oaResource = new de.intranda.api.annotation.oa.SpecificResource(resource.getId(), oaSelector);
                return oaResource;
            }
            return resource;
        }
        return null;

    }

    /**
     * Get the
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        if (anno.getBody() != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource = mapper.readValue(anno.getBody(), TextualResource.class);
            return resource;
        }
        return null;
    }

    /**
     * <p>
     * getBodyAsOAResource.
     * </p>
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsOAResource(PersistentAnnotation anno) throws JsonParseException, JsonMappingException, IOException {
        TextualResource resource = (TextualResource) getBodyAsResource(anno);
        if (resource != null) {
            IResource oaResource = new de.intranda.api.annotation.oa.TextualResource(resource.getText());
            return oaResource;
        }
        return null;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.wa.WebAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     * @throws DAOException
     */
    public WebAnnotation getAsWebAnnotation(PersistentAnnotation anno) {
        URI uri = getWebAnnotationURI(anno.getId());
        WebAnnotation annotation = new WebAnnotation(uri);
        try {
            annotation.setCreated(anno.getDateCreated());
            annotation.setModified(anno.getDateModified());
            try {
                if (anno.getCreator() != null) {
                    annotation.setCreator(new Agent(anno.getCreator().getIdAsURI(), AgentType.PERSON, anno.getCreator().getDisplayName()));
                }
                if (anno.getGenerator() != null) {
                    annotation
                            .setGenerator(new Agent(anno.getGenerator().getIdAsURI(), AgentType.SOFTWARE, anno.getGenerator().getOwner().getTitle()));
                }
            } catch (DAOException e) {
                logger.error("Error getting author of web annotation for " + anno, e);
            }
            annotation.setBody(getBodyAsResource(anno));
            annotation.setTarget(getTargetAsResource(anno));
            annotation.setMotivation(anno.getMotivation());
        } catch (IOException e) {
            logger.error("Error creating web annotation from " + anno, e);
        }
        return annotation;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.oa.OpenAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public OpenAnnotation getAsOpenAnnotation(PersistentAnnotation anno) {
        URI uri = getOpenAnnotationURI(anno.getId());
        OpenAnnotation annotation = new OpenAnnotation(uri);
        try {
            annotation.setBody(getBodyAsOAResource(anno));
            annotation.setTarget(getTargetAsOAResource(anno));
            annotation.setMotivation(anno.getMotivation());
        } catch (IOException e) {
            logger.error("Error creating open annotation from " + anno, e);
        }

        return annotation;
    }


}
