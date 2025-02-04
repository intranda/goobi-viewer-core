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
package io.goobi.viewer.api.rest.resourcebuilders;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.InternalAnnotationPage;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
public class AnnotationsResourceBuilder {

    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;
    private final WebAnnotationBuilder waBuilder;
    private final OpenAnnotationBuilder oaBuilder;
    private final AnnotationConverter converter;

    private static final Logger logger = LogManager.getLogger(AnnotationsResourceBuilder.class);

    private static final int MAX_ANNOTATIONS_PER_PAGE = 100;

    /**
     * Default constructor.
     *
     * @param urls TheApiUrlManager handling the creation of annotation urls/ids
     * @param request Used to check access to restricted annotations. May be null, which prevents delivering any annotations with accessconditions
     *            other than OPENACCESS
     */
    public AnnotationsResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        if (urls == null) {
            throw new IllegalArgumentException("ApiUrlManager must not be null. Configure a rest api with a current ('/api/v1') endpoint");
        }
        this.urls = urls;
        this.request = request;
        this.waBuilder = new WebAnnotationBuilder(urls);
        this.oaBuilder = new OpenAnnotationBuilder(urls);
        converter = new AnnotationConverter(urls);
    }

    public AnnotationCollection getWebAnnotationCollection() throws PresentationException, IndexUnreachableException {
        long count = waBuilder.getAnnotationCount(request);
        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        return builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildCollection();
    }

    /**
     *
     * @param page
     * @return {@link AnnotationPage}
     * @throws IllegalRequestException
     * @throws DAOException
     */
    public AnnotationPage getWebAnnotationPage(Integer page) throws IllegalRequestException, DAOException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        int first = (page - 1) * MAX_ANNOTATIONS_PER_PAGE;
        String sortField = "id";

        List<IAnnotation> annotations = DataManager.getInstance()
                .getDao()
                .getAnnotations(0, Integer.MAX_VALUE, sortField, false, null)
                .stream()
                .filter(anno -> isAccessible(anno, request))
                .skip(first)
                .limit(MAX_ANNOTATIONS_PER_PAGE)
                .map(converter::getAsWebAnnotation)
                .collect(Collectors.toList());

        URI uri = URI.create(urls.path(ANNOTATIONS).build());
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, annotations.size());
        return builder.setItemsPerPage(MAX_ANNOTATIONS_PER_PAGE).buildPage(annotations, page);
    }

    /**
     * @param anno
     * @param request
     * @return true if session has access permission to given annotation; false otherwise
     */
    private boolean isAccessible(CrowdsourcingAnnotation anno, HttpServletRequest request) {

        if (StringUtils.isBlank(anno.getAccessCondition()) || anno.getAccessCondition().equals(SolrConstants.OPEN_ACCESS_VALUE)) {
            return true;
        }
        try {
            return AccessConditionUtils.checkAccessPermission(Collections.singleton(anno.getAccessCondition()),
                    IPrivilegeHolder.PRIV_VIEW_UGC, waBuilder.getAnnotationQuery(anno.getId()), request).isGranted();
        } catch (IndexUnreachableException | PresentationException | DAOException e) {
            logger.error("Error ckecking access conditions for annotation {}: {}", anno.getId(), e.toString());
            return false;
        }
    }

    /**
     * @param pi
     * @param uri
     * @return {@link AnnotationCollection}
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecord(String pi, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForWork(pi);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(new InternalAnnotationPage(getWebAnnotationPageForRecord(pi, uri, 1)));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     *
     * @param pi
     * @param pageNo
     * @param uri
     * @return {@link AnnotationCollection}
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        long count = DataManager.getInstance().getDao().getAnnotationCountForTarget(pi, pageNo);
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, count);
        AnnotationCollection collection = builder.setItemsPerPage((int) count).buildCollection();
        if (count > 0) {
            try {
                collection.setFirst(new InternalAnnotationPage(getWebAnnotationPageForPage(pi, pageNo, uri, 1)));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     * @param pi
     * @param uri
     * @return {@link AnnotationList}
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForRecord(String pi, URI uri) throws DAOException {
        List<CrowdsourcingAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(list::addResource);
        return list;
    }

    /**
     * @param pi
     * @param pageNo
     * @param uri
     * @return {@link IAnnotationCollection}
     * @throws DAOException
     */
    public IAnnotationCollection getOAnnotationListForPage(String pi, Integer pageNo, URI uri) throws DAOException {
        List<CrowdsourcingAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(list::addResource);
        return list;
    }

    /**
     * @param pi
     * @param uri
     * @param page
     * @return {@link AnnotationPage}
     * @throws DAOException
     * @throws IllegalRequestException
     */
    public AnnotationPage getWebAnnotationPageForRecord(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<CrowdsourcingAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(converter::getAsWebAnnotation).collect(Collectors.toList());
        return builder.setItemsPerPage(annos.size()).buildPage(annos, page);
    }

    /**
     *
     * @param pi
     * @param pageNo
     * @param uri
     * @param page
     * @return {@link AnnotationPage}
     * @throws DAOException
     * @throws IllegalRequestException
     */
    public AnnotationPage getWebAnnotationPageForPage(String pi, Integer pageNo, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<CrowdsourcingAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, pageNo);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        List<IAnnotation> annos = data.stream().map(converter::getAsWebAnnotation).collect(Collectors.toList());
        return builder.setItemsPerPage(annos.size()).buildPage(annos, page);
    }

    /**
     * @param pi
     * @param uri
     * @return {@link AnnotationCollection}
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.setItemsPerPage(data.size()).buildCollection();
        if (!data.isEmpty()) {
            try {
                collection.setFirst(getWebAnnotationPageForRecordComments(pi, uri, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     * @param pi
     * @param pageNo
     * @param uri
     * @return {@link AnnotationCollection}
     * @throws DAOException
     */
    public AnnotationCollection getWebAnnotationCollectionForPageComments(String pi, int pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        AnnotationCollection collection = builder.setItemsPerPage(data.size()).buildCollection();
        if (!data.isEmpty()) {
            try {
                collection.setFirst(getWebAnnotationPageForPageComments(pi, uri, pageNo, 1));
            } catch (IllegalRequestException e) {
                //no items
            }
        }
        return collection;
    }

    /**
     *
     * @param pi
     * @param uri
     * @param page
     * @return {@link AnnotationPage}
     * @throws DAOException
     * @throws IllegalRequestException
     */
    public AnnotationPage getWebAnnotationPageForRecordComments(String pi, URI uri, Integer page) throws DAOException, IllegalRequestException {
        if (page == null || page < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        return builder.setItemsPerPage(data.size())
                .buildPage(
                        data.stream()
                                .map(converter::getAsWebAnnotation)
                                .collect(Collectors.toList()),
                        page);
    }

    public AnnotationPage getWebAnnotationPageForPageComments(String pi, URI uri, Integer pageNo, Integer collectionPage)
            throws DAOException, IllegalRequestException {
        if (collectionPage == null || collectionPage < 1) {
            throw new IllegalRequestException("Page number must be at least 1");
        }
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);
        if (data.isEmpty()) {
            throw new IllegalRequestException("Page number is out of bounds");
        }
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(uri, data.size());
        return builder.setItemsPerPage(data.size())
                .buildPage(
                        data.stream()
                                .map(converter::getAsWebAnnotation)
                                .collect(Collectors.toList()),
                                collectionPage);
    }

    /**
     *
     * @param pi
     * @param uri
     * @return {@link AnnotationList}
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForRecordComments(String pi, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForWork(pi);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(list::addResource);
        return list;
    }

    /**
     *
     * @param pi
     * @param pageNo
     * @param uri
     * @return {@link AnnotationList}
     * @throws DAOException
     */
    public AnnotationList getOAnnotationListForPageComments(String pi, Integer pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);

        AnnotationList list = new AnnotationList(uri);
        data.stream().map(converter::getAsOpenAnnotation).forEach(list::addResource);
        return list;
    }

    /**
     *
     * @param pi
     * @param pageNo
     * @param uri
     * @return {@link AnnotationPage}
     * @throws DAOException
     */
    public AnnotationPage getWebAnnotationPageForPageComments(String pi, Integer pageNo, URI uri) throws DAOException {
        List<Comment> data = DataManager.getInstance().getDao().getCommentsForPage(pi, pageNo);
        AnnotationPage page = new AnnotationPage(uri);
        data.stream()
                .map(converter::getAsWebAnnotation)
                .collect(Collectors.toList())
                .forEach(c -> page.addItem(c));

        return page;
    }

    /**
     * @param id
     * @return Optional<WebAnnotation>
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Optional<WebAnnotation> getWebAnnotation(long id) throws DAOException {
        CrowdsourcingAnnotation annotation = DataManager.getInstance().getDao().getAnnotation(id);
        return Optional.ofNullable(annotation).map(converter::getAsWebAnnotation);
    }

    /**
     *
     * @param id
     * @return Optional<WebAnnotation>
     * @throws DAOException
     */
    public Optional<WebAnnotation> getCommentWebAnnotation(long id) throws DAOException {
        Comment comment = DataManager.getInstance().getDao().getComment(id);
        return Optional.ofNullable(comment).map(converter::getAsWebAnnotation);
    }

    /**
     * @param id
     * @return Optional<OpenAnnotation>
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Optional<OpenAnnotation> getOpenAnnotation(Long id) throws DAOException {
        CrowdsourcingAnnotation annotation = DataManager.getInstance().getDao().getAnnotation(id);
        return Optional.ofNullable(annotation).map(converter::getAsOpenAnnotation);
    }

}
