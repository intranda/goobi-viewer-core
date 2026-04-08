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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.awt.Rectangle;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.JSONResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.SpecificResourceURI;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author Florian Alpers
 */
public class WebAnnotationBuilder extends AbstractAnnotationBuilder {

    /**
     * @param apiUrlManager API URL manager for building IIIF resource URIs
     */
    public WebAnnotationBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * Gets all annotations for the given PI from the SOLR index, sorted by page number. The annotations are stored as DOCTYPE:UGC in the SOLR and are
     * converted to WebAnnotations here
     *
     * @param pi The persistent identifier of the work to query
     * @param urlOnlyTarget if true, use URI-only annotation targets
     * @param request current HTTP servlet request
     * @return A map of page numbers (1-based) mapped to a list of associated annotations
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Map<Integer, List<WebAnnotation>> getCrowdsourcingAnnotationsFromSolr(String pi, boolean urlOnlyTarget, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException {
        List<SolrDocument> ugcDocs = getAnnotationDocuments(getAnnotationQuery(pi), request);
        Map<Integer, List<WebAnnotation>> annoMap = new HashMap<>();
        if (ugcDocs != null && !ugcDocs.isEmpty()) {
            for (SolrDocument doc : ugcDocs) {
                WebAnnotation anno = createUGCWebAnnotation(pi, doc, urlOnlyTarget);
                Integer page = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
                List<WebAnnotation> annoList = annoMap.computeIfAbsent(page, k -> new ArrayList<>());
                annoList.add(anno);
            }
        }
        return annoMap;
    }

    /**
     * Gets all annotations for the given PI from the the DAO, sorted by page number. The annotations are stored as DOCTYPE:UGC in the SOLR and are
     * converted to WebAnnotations here
     *
     * @param pi The persistent identifier of the work to query
     * @param urlOnlyTarget if true, use URI-only annotation targets
     * @return A map of page numbers (1-based) mapped to a list of associated annotations
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws DAOException
     */
    public Map<Integer, List<WebAnnotation>> getCrowdsourcingAnnotations(String pi, boolean urlOnlyTarget)
            throws DAOException {
        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForWork(pi);
        Map<Integer, List<WebAnnotation>> annoMap = new HashMap<>();
        if (annotations != null) {
            for (CrowdsourcingAnnotation persAnno : annotations) {
                WebAnnotation anno = new AnnotationConverter().getAsWebAnnotation(persAnno);
                Integer page = Optional.ofNullable(persAnno).map(p -> p.getTargetPageOrder()).orElse(null);
                List<WebAnnotation> annoList = annoMap.computeIfAbsent(page, k -> new ArrayList<>());

                annoList.add(anno);
            }
        }
        return annoMap;
    }

    /**
     *
     * @param id the IDDOC of the UGC Solr document to retrieve
     * @return {@link IAnnotation}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public IAnnotation getCrowdsourcingAnnotation(String id) throws PresentationException, IndexUnreachableException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +IDDOC:").append(id);

        SolrDocumentList docList = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString());
        if (docList != null && !docList.isEmpty()) {
            SolrDocument doc = docList.get(0);
            return createUGCWebAnnotation(doc, false);
        }

        return null;
    }

    /**
     * createWebAnnotation.
     *
     * @param doc Solr document of type UGC containing the annotation data
     * @param urlOnlyTarget if true, use URI-only annotation targets
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     */
    public WebAnnotation createUGCWebAnnotation(SolrDocument doc, boolean urlOnlyTarget) {
        String pi = Optional.ofNullable(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).map(SolrTools::getAsString).orElse("");
        return createUGCWebAnnotation(pi, doc, urlOnlyTarget);
    }

    /**
     * createWebAnnotation.
     *
     * @param pi persistent identifier of the annotated record
     * @param doc Solr document of type UGC containing the annotation data
     * @param urlOnlyTarget if true, use URI-only annotation targets
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     */
    public WebAnnotation createUGCWebAnnotation(String pi, SolrDocument doc, boolean urlOnlyTarget) {
        String id = Optional.ofNullable(doc.getFieldValue(SolrConstants.MD_ANNOTATION_ID))
                .map(SolrTools::getAsString)
                .map(i -> i.replace("annotation_", ""))
                .orElse((String) doc.getFieldValue(SolrConstants.IDDOC));
        Integer pageOrder = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
        String coordString = Optional.ofNullable(doc.getFieldValue(SolrConstants.UGCCOORDS)).map(SolrTools::getAsString).orElse(null);
        URI annoURI = getRestBuilder().getAnnotationURI(id);

        WebAnnotation anno = new WebAnnotation(annoURI);

        IResource body = createAnnnotationBodyFromUGCDocument(doc);
        anno.setBody(body);

        if (pageOrder != null && coordString != null) {
            anno.setTarget(createFragmentTarget(pi, pageOrder, coordString, urlOnlyTarget));
        } else if (pageOrder != null) {
            anno.setTarget(new SimpleResource(getRestBuilder().getCanvasURI(pi, pageOrder)));
        } else {
            anno.setTarget(new SimpleResource(getRestBuilder().getManifestURI(pi)));
        }

        anno.setMotivation(Motivation.DESCRIBING);
        return anno;
    }

    /**
     * @param pi persistent identifier of the record containing the page
     * @param pageOrder 1-based physical page order of the target canvas
     * @param coordString coordinate string describing the fragment region (IIIF xywh or legacy format)
     * @param urlOnlyTarget if true, returns a {@link SpecificResourceURI} instead of a full {@link SpecificResource}
     * @return {@link IResource}
     */
    public IResource createFragmentTarget(String pi, int pageOrder, String coordString, boolean urlOnlyTarget) {
        try {
            FragmentSelector selector = new FragmentSelector(coordString);
            if (urlOnlyTarget) {
                return new SpecificResourceURI(getRestBuilder().getCanvasURI(pi, pageOrder), selector);
            }
            return new SpecificResource(getRestBuilder().getCanvasURI(pi, pageOrder), selector);
        } catch (IllegalArgumentException e) {
            //old UGC coords format
            String regex = "([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+)";
            Matcher matcher = Pattern.compile(regex).matcher(coordString); //NOSONAR  no catastrophic backtracking detected
            if (matcher.find()) {
                int x1 = Math.round(Float.parseFloat(matcher.group(1)));
                int y1 = Math.round(Float.parseFloat(matcher.group(2)));
                int x2 = Math.round(Float.parseFloat(matcher.group(3)));
                int y2 = Math.round(Float.parseFloat(matcher.group(4)));
                FragmentSelector selector = new FragmentSelector(new Rectangle(x1, y1, x2 - x1, y2 - y1));
                if (urlOnlyTarget) {
                    return new SpecificResourceURI(getRestBuilder().getCanvasURI(pi, pageOrder), selector);
                }
                return new SpecificResource(getRestBuilder().getCanvasURI(pi, pageOrder), selector);
            }
            //failed to decipher selector
            return new SimpleResource(getRestBuilder().getCanvasURI(pi, pageOrder));
        }
    }

    /**
     * @param doc the UGC Solr document from which to extract the annotation body
     * @return {@link IResource}
     */
    public IResource createAnnnotationBodyFromUGCDocument(SolrDocument doc) {
        IResource body = null;
        if (doc.containsKey(SolrConstants.MD_BODY)) {
            String bodyString = SolrTools.getSingleFieldStringValue(doc, SolrConstants.MD_BODY);
            try {
                JSONObject json = new JSONObject(bodyString);
                body = new JSONResource(json);
            } catch (JSONException e) {
                body = new TextualResource(bodyString);
            }
        } else if (doc.containsKey(SolrConstants.MD_TEXT)) {
            String text = SolrTools.getSingleFieldStringValue(doc, SolrConstants.MD_TEXT);
            body = new TextualResource(text);
        }
        return body;
    }

    /**
     * @param uri the URI to use as the ID of the returned annotation page
     * @param pi persistent identifier of the work to collect annotations for
     * @param urlsOnly if true, annotation targets are represented as URL-only resources
     * @return {@link AnnotationPage}
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public AnnotationPage getCrowdsourcingAnnotationCollection(URI uri, String pi, boolean urlsOnly)
            throws DAOException {
        List<IAnnotation> annos =
                getCrowdsourcingAnnotations(pi, urlsOnly).values().stream().flatMap(List::stream).collect(Collectors.toList());
        AnnotationPage page = new AnnotationPage(uri);
        page.setItems(annos);
        return page;
    }

    /**
     *
     * @param uri the URI to use as the ID of the returned annotation page
     * @param pi persistent identifier of the work to collect annotations for
     * @param pageNo 1-based page order to filter annotations by; null returns all pages
     * @param urlsOnly if true, annotation targets are represented as URL-only resources
     * @return {@link AnnotationPage}
     * @throws DAOException
     */
    public AnnotationPage getCrowdsourcingAnnotationCollection(URI uri, String pi, Integer pageNo, boolean urlsOnly)
            throws DAOException {
        List<IAnnotation> annos = getCrowdsourcingAnnotations(pi, urlsOnly).entrySet()
                .stream()
                .filter(entry -> Objects.equals(pageNo, entry.getKey()))
                .map(Entry::getValue)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        AnnotationPage page = new AnnotationPage(uri);
        page.setItems(annos);
        return page;
    }

}
