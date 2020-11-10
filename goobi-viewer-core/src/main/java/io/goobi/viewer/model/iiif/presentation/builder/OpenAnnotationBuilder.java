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
package io.goobi.viewer.model.iiif.presentation.builder;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_UGC;

import java.awt.Rectangle;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;
import org.json.JSONObject;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.JSONResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.oa.FragmentSelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResource;
import de.intranda.api.annotation.oa.SpecificResourceURI;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class OpenAnnotationBuilder extends AbstractBuilder {

    /**
     * @param apiUrlManager
     */
    public OpenAnnotationBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }
    
    /**
     * Get all annotations for the given PI from the SOLR index, sorted by page number. The annotations are stored as DOCTYPE:UGC in the SOLR and are
     * converted to OpenAnnotations here
     *
     * @param pi The persistent identifier of the work to query
     * @return A map of page numbers (1-based) mapped to a list of associated annotations
     * @param urlOnlyTarget a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Map<Integer, List<OpenAnnotation>> getCrowdsourcingAnnotations(String pi, boolean urlOnlyTarget)
            throws PresentationException, IndexUnreachableException {
        String query = "DOCTYPE:UGC AND PI_TOPSTRUCT:" + pi;
        List<String> displayFields = addLanguageFields(Arrays.asList(UGC_SOLR_FIELDS), ViewerResourceBundle.getAllLocales());
        SolrDocumentList ugcDocs = DataManager.getInstance().getSearchIndex().getDocs(query, displayFields);
        Map<Integer, List<OpenAnnotation>> annoMap = new HashMap<>();
        if (ugcDocs != null && !ugcDocs.isEmpty()) {
            for (SolrDocument doc : ugcDocs) {
                OpenAnnotation anno = createUGCOpenAnnotation(pi, doc, urlOnlyTarget);
                Integer page = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
                List<OpenAnnotation> annoList = annoMap.get(page);
                if (annoList == null) {
                    annoList = new ArrayList<>();
                    annoMap.put(page, annoList);
                }
                annoList.add(anno);
            }
        }
        return annoMap;
    }

    public IAnnotation getCrowdsourcingAnnotation(String id) throws PresentationException, IndexUnreachableException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +IDDOC:").append(id);

        SolrDocumentList docList = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString());
        if (docList != null && !docList.isEmpty()) {
            SolrDocument doc = docList.get(0);
            IAnnotation anno = createUGCOpenAnnotation(doc, false);
            return anno;
        }

        return null;
    }

    /**
     * <p>
     * createOpenAnnotation.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     */
    public OpenAnnotation createUGCOpenAnnotation(SolrDocument doc, boolean urlOnlyTarget) {
        String pi = Optional.ofNullable(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).map(Object::toString).orElse("");
        return createUGCOpenAnnotation(pi, doc, urlOnlyTarget);

    }

    /**
     * <p>
     * createOpenAnnotation.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param urlOnlyTarget a boolean.
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     */
    public OpenAnnotation createUGCOpenAnnotation(String pi, SolrDocument doc, boolean urlOnlyTarget) {
        String iddoc = Optional.ofNullable(doc.getFieldValue(SolrConstants.IDDOC)).map(Object::toString).orElse("");
        Integer pageOrder = Optional.ofNullable(doc.getFieldValue(SolrConstants.ORDER)).map(o -> (Integer) o).orElse(null);
        String coordString = Optional.ofNullable(doc.getFieldValue(SolrConstants.UGCCOORDS)).map(Object::toString).orElse(null);
        URI annoURI = URI.create(urls.path(ApiUrls.ANNOTATIONS, ANNOTATIONS_UGC).params(iddoc).build());
        
        OpenAnnotation anno = new OpenAnnotation(annoURI);

        IResource body = createAnnnotationBodyFromUGCDocument(doc);
        anno.setBody(body);

        if(pageOrder != null && coordString != null) {
            anno.setTarget(createFragmentTarget(pi, pageOrder, coordString, urlOnlyTarget));
        } else if(pageOrder != null) {
            anno.setTarget(new SimpleResource(getCanvasURI(pi, pageOrder)));
        } else {
            anno.setTarget(new SimpleResource(getManifestURI(pi)));
        }

        anno.setMotivation(Motivation.DESCRIBING);
        return anno;
    }

    /**
     * @param pi
     * @param urlOnlyTarget
     * @param anno
     * @param coordString
     * @param pageOrder
     */
    public IResource createFragmentTarget(String pi, int pageOrder, String coordString,  boolean urlOnlyTarget) {
        try {
            FragmentSelector selector = new FragmentSelector(coordString);
            if (urlOnlyTarget) {
                return new SpecificResourceURI(this.getCanvasURI(pi, pageOrder), selector);
            } else {
                return new SpecificResource(this.getCanvasURI(pi, pageOrder), selector);
            }
        } catch (IllegalArgumentException e) {
            //old UGC coords format
            String regex = "([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+),\\s*([\\d\\.]+)";
            Matcher matcher = Pattern.compile(regex).matcher(coordString);
            if (matcher.find()) {
                int x1 = Math.round(Float.parseFloat(matcher.group(1)));
                int y1 = Math.round(Float.parseFloat(matcher.group(2)));
                int x2 = Math.round(Float.parseFloat(matcher.group(3)));
                int y2 = Math.round(Float.parseFloat(matcher.group(4)));
                FragmentSelector selector = new FragmentSelector(new Rectangle(x1, y1, x2 - x1, y2 - y1));
                if (urlOnlyTarget) {
                    return new SpecificResourceURI(this.getCanvasURI(pi, pageOrder), selector);
                } else {
                    return new SpecificResource(this.getCanvasURI(pi, pageOrder), selector);
                }
            } else {
                //failed to decipher selector
                return new SimpleResource(getCanvasURI(pi, pageOrder));
            }
        }
    }

    /**
     * @param doc
     * @return
     */
    public IResource createAnnnotationBodyFromUGCDocument(SolrDocument doc) {
        IResource body = null;
        if (doc.containsKey(SolrConstants.MD_BODY)) {
            String bodyString = readSolrField(doc, doc.getFieldValue(SolrConstants.MD_BODY));
            try {                
                JSONObject json = new JSONObject(bodyString);
                body = new JSONResource(json);
            } catch(JSONException e)  {
                body = new TextualResource(bodyString);
            }
        } else if (doc.containsKey(SolrConstants.MD_TEXT)) {
            String text = readSolrField(doc, doc.getFieldValue(SolrConstants.MD_TEXT));
            body = new TextualResource(text);
        }
        return body;
    }

    /**
     * @param pi
     * @param uri 
     * @param b
     * @return
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     */
    public IAnnotationCollection getCrowdsourcingAnnotationCollection(URI uri, String pi, boolean urlsOnly) throws PresentationException, IndexUnreachableException {
        AnnotationList list = new AnnotationList(uri);
        getCrowdsourcingAnnotations(pi, urlsOnly).values().stream().flatMap(List::stream).forEach(anno -> list.addResource(anno));
        return list;
    }
    
    public IAnnotationCollection getCrowdsourcingAnnotationCollection(URI uri, String pi, Integer page, boolean urlsOnly) throws PresentationException, IndexUnreachableException {
        AnnotationList list = new AnnotationList(uri);
        getCrowdsourcingAnnotations(pi, urlsOnly).entrySet().stream()
        .filter(entry -> ObjectUtils.equals(page, entry.getKey()))
        .map(Entry::getValue)
        .flatMap(List::stream)
        .forEach(anno -> list.addResource(anno));
        return list;
    }




}
