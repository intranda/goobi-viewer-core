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
package io.goobi.viewer.model.annotation;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_USERID;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.ISelector;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.comments.Comment;

/**
 * @author florian
 *
 */
public class AnnotationConverter {

    private static final Logger logger = LogManager.getLogger(AnnotationConverter.class);
    private final AbstractApiUrlManager urls;

    public AnnotationConverter() {
        this(DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .orElseThrow(() -> new IllegalStateException("No api manager available")));
    }

    public AnnotationConverter(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    private URI getWebAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).build());
    }

    private URI getOpenAnnotationURI(Long id) {
        return URI.create(this.urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).params(id).query("format", "oa").build());
    }

    private URI getWebAnnotationCommentURI(Long id) {
        return URI.create(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).params(id).build());
    }

    /**
     * Get the annotation target as an WebAnnotation {@link de.intranda.api.annotation.IResource} java object
     * 
     * @param anno
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsResource(PersistentAnnotation anno) throws IOException {
        if (anno.getTarget() != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource;
            try {
                if (anno.getTarget().contains("SpecificResource")) {
                    resource = mapper.readValue(anno.getTarget(), SpecificResource.class);
                } else {
                    resource = mapper.readValue(anno.getTarget(), TypedResource.class);
                }
            } catch (JsonParseException e) {
                resource = new TextualResource(anno.getTarget());
            }
            return resource;
        }
        return null;
    }

    /**
     * Get the annotation target as an OpenAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @param anno
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsOAResource(PersistentAnnotation anno) throws IOException {
        IResource resource = getTargetAsResource(anno);
        if (resource != null) {
            if (resource instanceof SpecificResource && ((SpecificResource) resource).getSelector() instanceof FragmentSelector) {
                FragmentSelector selector = (FragmentSelector) ((SpecificResource) resource).getSelector();
                ISelector oaSelector = new de.intranda.api.annotation.oa.FragmentSelector(selector.getFragment());
                return new de.intranda.api.annotation.oa.SpecificResource(resource.getId(), oaSelector);
            }
            return resource;
        }
        return null;

    }

    /**
     * Get the
     *
     * @param anno
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsResource(PersistentAnnotation anno) throws IOException {
        if (anno.getBody() != null && anno.getBody().startsWith("{")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(anno.getBody(), TextualResource.class);
        } else if (StringUtils.isNotBlank(anno.getBody())) {
            return new TextualResource(anno.getBody());
        }
        return null;
    }

    /**
     * <p>
     * getBodyAsOAResource.
     * </p>
     * 
     * @param anno
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsOAResource(PersistentAnnotation anno) throws IOException {
        if (anno.getBody() != null && anno.getBody().startsWith("{")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(anno.getBody(), de.intranda.api.annotation.oa.TextualResource.class);
        } else if (StringUtils.isNotBlank(anno.getBody())) {
            return new TextualResource(anno.getBody());
        }
        return null;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.wa.WebAnnotation} java object
     *
     * @param anno
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     * @throws DAOException
     */
    public WebAnnotation getAsWebAnnotation(PersistentAnnotation anno) {
        URI uri;
        if (anno instanceof Comment) {
            uri = getWebAnnotationCommentURI(anno.getId());
        } else {
            uri = getWebAnnotationURI(anno.getId());
        }
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
                logger.error("Error getting author of web annotation for {}", anno, e);
            }
            annotation.setBody(getBodyAsResource(anno));
            annotation.setTarget(getTargetAsResource(anno));
            annotation.setMotivation(anno.getMotivation());
            annotation.setRights(anno.getAccessCondition());
        } catch (IOException e) {
            logger.error("Error creating web annotation from {}", anno, e);
        }
        return annotation;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.oa.OpenAnnotation} java object
     *
     * @param anno
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

    /**
     * 
     * @param anno
     * @return {@link PersistentAnnotation}
     */
    public PersistentAnnotation getAsPersistentAnnotation(WebAnnotation anno) {
        return new CrowdsourcingAnnotation(anno, getPersistenceId(anno), getPI(anno.getTarget()).orElse(null),
                getPageNo(anno.getTarget()).orElse(null));
    }

    private Optional<Long> getUserId(Agent creator) {
        String id = urls.parseParameter(urls.path(USERS, USERS_USERID).build(), creator.getId().toString(), "{userId}");
        if (StringUtils.isNotBlank(id) && id.matches("\\d")) {
            return Optional.of(Long.parseLong(id));
        }
        return Optional.empty();
    }

    /**
     * @param target
     * @return Optional<String>
     */
    private Optional<String> getPI(IResource target) {
        if (target.getId() != null) {
            String uri = target.getId().toString();

            String pi = urls.parseParameter(urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build(), uri, "{pi}");
            if (StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }

            pi = urls.parseParameter(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).build(), uri, "{pi}");
            if (StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }

            pi = urls.parseParameter(urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).build(), uri, "{pi}");
            if (StringUtils.isNotBlank(pi)) {
                return Optional.of(pi);
            }

        }
        return Optional.empty();
    }

    private Optional<String> getDivId(IResource target) {
        if (target.getId() != null) {
            String uri = target.getId().toString();

            String id = urls.parseParameter(urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).build(), uri, "{divId}");
            if (StringUtils.isNotBlank(id)) {
                return Optional.of(id);
            }

        }
        return Optional.empty();
    }

    /**
     * @param target
     * @return Optional<Integer>
     */
    private Optional<Integer> getPageNo(IResource target) {
        if (target.getId() != null) {
            String uri = target.getId().toString();
            String pageNo = urls.parseParameter(urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build(), uri, "{pageNo}");
            if (StringUtils.isNotBlank(pageNo) && pageNo.matches("\\d+")) {
                return Optional.of(Integer.parseInt(pageNo));
            }
        }
        return Optional.empty();
    }

    /**
     * @param anno
     * @return anno.id if exists; null otherwise
     */
    private Long getPersistenceId(WebAnnotation anno) {
        Long id = null;
        if (anno.getId() != null) {
            String uri = anno.getId().toString();
            String idString = urls.parseParameter(urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).build(), uri, "{id}");
            if (StringUtils.isNotBlank(idString)) {
                id = Long.parseLong(idString);
                //            } else {
                //                idString = urls.parseParameter(urls.path(ANNOTATIONS, ANNOTATIONS_COMMENT).build(), uri, "{id}");
                //                if (StringUtils.isNotBlank(idString)) {
                //                    id = Long.parseLong(idString);
                //                }
            }
        }
        if (id != null) {
            return id;
        }
        
        return null;
    }
}
