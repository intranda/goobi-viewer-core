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
package de.intranda.digiverso.presentation.servlets.rest.ner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.ALTOTools;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/ner")
@ViewerRestServiceBinding
public class NERTagResource {

    private static final Logger logger = LoggerFactory.getLogger(NERTagResource.class);

    @Context
    private ResourceContext resourceContext;

    @GET
    @Path("/tags/{pi}.json")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagsForPageJson(@Context ContainerRequestContext request, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getNERTags(pi, null, null, null, 1);
    }

    @GET
    @Path("/tags/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTagsForPage(@Context ContainerRequestContext request, @PathParam("pi") String pi) throws PresentationException {
        try {
            return Response.seeOther(new URI(request.getUriInfo().getRequestUri().toString().replaceAll("/$", "") + ".json")).build();
        } catch (URISyntaxException e) {
            throw new PresentationException(e.getMessage());
        }
    }

    @GET
    @Path("/tags/{pi}.xml")
    @Produces({ MediaType.APPLICATION_XML })
    public DocumentReference getTagsForPageXml(@Context ContainerRequestContext request, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getNERTags(pi, null, null, null, 1);
    }

    @GET
    @Path("/tags/{pi}/{pageOrder}.json")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagsForPageJson(@Context ContainerRequestContext request, @PathParam("pi") String pi,
            @PathParam("pageOrder") Integer order) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getNERTags(pi, null, order, order, 1);
    }

    @GET
    @Path("/tags/{pi}/{pageOrder}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTagsForPage(@Context ContainerRequestContext request, @PathParam("pi") String pi, @PathParam("pageOrder") Long order)
            throws PresentationException {
        try {
            return Response.seeOther(new URI(request.getUriInfo().getRequestUri().toString().replaceAll("/$", "") + ".json")).build();
        } catch (URISyntaxException e) {
            throw new PresentationException(e.getMessage());
        }
    }

    @GET
    @Path("/tags/{pi}/{pageOrder}.xml")
    @Produces({ MediaType.APPLICATION_XML })
    public DocumentReference getTagsForPageXml(@Context ContainerRequestContext request, @PathParam("pi") String pi,
            @PathParam("pageOrder") Integer order) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getNERTags(pi, null, order, order, 1);
    }

    private static DocumentReference getNERTags(String pi, String type, Integer start, Integer end, int rangeSize)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        StringBuilder query = new StringBuilder();
        query.append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi);

        if (start != null && end != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[").append(start).append(" TO ").append(end).append("]");
        } else if (start != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[").append(start).append(" TO *]");
        } else if (end != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[* TO ").append(end).append("]");
        } else {
            query.append(" AND ").append(SolrConstants.DOCTYPE).append(":PAGE");
        }

        return getNERTagsByQuery(query.toString(), type, rangeSize);
    }

    //    private DocumentTagCountReference getNERTagCount(String pi, String type, Long start, Long end) throws PresentationException, IndexUnreachableException {
    //        StringBuilder query = new StringBuilder();
    //        query.append("PI_TOPSTRUCT:").append(pi);
    //
    //        if (start != null && end != null) {
    //            query.append(" AND ").append("ORDER:[").append(start).append(" TO ").append(end).append("]");
    //        } else if(start != null) {
    //            query.append(" AND ").append("ORDER:[").append(start).append(" TO *]");
    //        } else if(end != null) {
    //            query.append(" AND ").append("ORDER:[* TO ").append(end).append("]");
    //        } else {
    //            query.append(" AND ").append("DOCTYPE:PAGE");
    //        }
    //        
    //        return getNERTagCountByQuery(query.toString(), type);
    //    }

    @GET
    @Path("/tags/type/{type}/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagsByType(@Context ContainerRequestContext request, @PathParam("pi") String pi, @PathParam("type") String type)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        return getNERTags(pi, type, null, null, 1);
    }

    //    @GET
    //    @Path("/tags/recurrence/{order}/{pi}/")
    //    @Produces({ MediaType.APPLICATION_JSON })
    //    public DocumentTagCountReference getTagCount(@Context ContainerRequestContext request, @PathParam("pi") String pi,
    //            @PathParam("order") String order) throws PresentationException, IndexUnreachableException {
    //        DocumentTagCountReference ref = getNERTagCount(pi, null, null, null);
    //        for (PageTagCount page : ref.getPages()) {
    //            if (order.equals("asc")) {
    //            	List<TagCount> tags = page.getTags();
    //                Collections.sort(tags);
    //                page.setTags(tags);
    //            } else {
    //            	List<TagCount> tags = page.getTags();
    //                Collections.sort(tags);
    //                Collections.reverse(tags);
    //                page.setTags(tags);
    //            }
    //        }
    //        return ref;
    //    }

    //    @GET
    //    @Path("/tags/recurrence/{type}/{order}/{pi}/")
    //    @Produces({ MediaType.APPLICATION_JSON })
    //    public DocumentTagCountReference getTagCount(@Context ContainerRequestContext request, @PathParam("pi") String pi,
    //            @PathParam("order") String order, @PathParam("type") String type) throws PresentationException, IndexUnreachableException {
    //        if (type.equals("-")) {
    //            type = null;
    //        }
    //        DocumentTagCountReference ref = getNERTagCount(pi, type, null, null);
    //        for (PageTagCount page : ref.getPages()) {
    //            if (order.equals("asc")) {
    //                List<TagCount> tags = page.getTags();
    //                Collections.sort(tags);
    //                page.setTags(tags);
    //            } else {
    //                List<TagCount> tags = page.getTags();
    //                Collections.sort(tags);
    //                Collections.reverse(tags);
    //                page.setTags(tags);
    //            }
    //        }
    //        return ref;
    //    }

    @GET
    @Path("/tags/{startpage}/{endpage}/{pi}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagsForPageArea(@Context ContainerRequestContext request, @PathParam("startpage") int startpage,
            @PathParam("endpage") int endpage, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (startpage > endpage) {
            throw new PresentationException("start page must not be greater than end page");
        }
        DocumentReference ref = getNERTags(pi, null, startpage, endpage, 1);
        return ref;
    }

    @GET
    @Path("/tags/{startpage}/{endpage}/{type}/{pi}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagsForPageArea(@Context ContainerRequestContext request, @PathParam("startpage") int startpage,
            @PathParam("type") String type, @PathParam("endpage") int endpage, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (startpage > endpage) {
            throw new PresentationException("start page must not be greater than end page");
        }
        if (type.equals("-")) {
            type = null;
        }
        DocumentReference ref = getNERTags(pi, type, startpage, endpage, endpage - startpage + 1);
        return ref;
    }

    @GET
    @Path("/tags/ranges/{range}/{type}/{pi}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagRangesByType(@Context ContainerRequestContext request, @PathParam("range") int rangeSize,
            @PathParam("type") String type, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        DocumentReference ref = getNERTags(pi, type, null, null, rangeSize);
        return ref;
    }

    @GET
    @Path("/tags/ranges/{range}/{pi}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public DocumentReference getTagRanges(@Context ContainerRequestContext request, @PathParam("range") int rangeSize, @PathParam("pi") String pi)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        DocumentReference ref = getNERTags(pi, null, null, null, rangeSize);
        return ref;
    }

    /**
     * 
     * @param query must return a set of PAGE documents within a single topStruct
     * @param typeString
     * @param rangeSize
     * @return
     * @throws PresentationException if there is an error parsing the alto documents or if the search doesn't result in PAGE documents from a single
     *             topStruct
     * @throws IndexUnreachableException if the index cannot be reached
     * @throws ViewerConfigurationException
     */
    private static DocumentReference getNERTagsByQuery(String query, String typeString, int rangeSize)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        final List<String> fieldList = Arrays.asList(new String[] { SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.IDDOC,
                SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.FILENAME_ALTO });
        SolrDocumentList solrDocuments = DataManager.getInstance().getSearchIndex().search(query, fieldList);
        Collections.sort(solrDocuments, docOrderComparator);

        NERTag.Type type = NERTag.Type.getType(typeString);

        if (solrDocuments != null && !solrDocuments.isEmpty()) {
            String dataRepository = null;
            String topStructPi = null;
            if (solrDocuments.get(0).containsKey(SolrConstants.PI_TOPSTRUCT)) {
                topStructPi = SolrSearchIndex.getAsString(solrDocuments.get(0).getFieldValue(SolrConstants.PI_TOPSTRUCT));

                // Determine data repository name
                SolrDocument topSolrDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ':' + topStructPi,
                        Collections.singletonList(SolrConstants.DATAREPOSITORY));
                if (topSolrDoc != null && topSolrDoc.containsKey(SolrConstants.DATAREPOSITORY)) {
                    dataRepository = (String) topSolrDoc.get(SolrConstants.DATAREPOSITORY);
                }
            }
            DocumentReference doc = new DocumentReference(topStructPi);

            for (int index = 0; index < solrDocuments.size(); index += rangeSize) {
                List<SolrDocument> rangeList = solrDocuments.subList(index, Math.min(index + rangeSize, solrDocuments.size()));
                TagGroup range = createPageReference(rangeList);
                for (SolrDocument solrDoc : rangeList) {
                    String altoFileName = (String) solrDoc.getFieldValue(SolrConstants.FILENAME_ALTO);
                    if (altoFileName == null) {
                        logger.error("{}, page {} has no {} value.", topStructPi, solrDoc.getFieldValue(SolrConstants.ORDER),
                                SolrConstants.FILENAME_ALTO);
                        continue;
                    }
                    //TODO: Load directly from file if on same server?
                    // Load ALTO via the REST service
                    String url = Helper.buildFullTextUrl(dataRepository, altoFileName);
                    try {
                        String altoString = Helper.getWebContentGET(url);
                        Integer pageOrder = getPageOrder(solrDoc);
                        List<TagCount> tags = ALTOTools.getNERTags(altoString, type);
                        for (TagCount tagCount : tags) {
                            for (ElementReference reference : tagCount.getReferences()) {
                                reference.setPage(pageOrder);
                            }
                        }
                        range.addTags(tags);
                    } catch (FileNotFoundException e) {
                        logger.error(e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    } catch (HTTPException e) {
                        logger.error(e.getMessage());
                    }
                }
                Collections.sort(range.getTags());
                Collections.reverse(range.getTags());
                doc.addPageRange(range);
            }
            return doc;
        }
        return new DocumentReference();
    }

    //    private DocumentTagCountReference getNERTagCountByQuery(String query, String type) throws PresentationException, IndexUnreachableException {
    //        List<String> fieldList = Arrays.asList(new String[] { LuceneConstants.PI, LuceneConstants.PI_TOPSTRUCT, LuceneConstants.IDDOC,
    //                LuceneConstants.IDDOC_TOPSTRUCT, LuceneConstants.ORDER, LuceneConstants.ALTO });
    //        SolrDocumentList solrDocuments = DataManager.getInstance().getSolrHelper().search(query, fieldList);
    //        if (solrDocuments != null && !solrDocuments.isEmpty()) {
    //            List<PageTagCount> pages = new ArrayList<PageTagCount>();
    //            String topStructPi = null;
    //            for (SolrDocument solrDoc : solrDocuments) {
    //                if (solrDoc.containsKey(LuceneConstants.PI_TOPSTRUCT)) {
    //                    String localPi = SolrHelper.getAsString(solrDoc.getFieldValue(LuceneConstants.PI_TOPSTRUCT));
    //                    if (topStructPi != null && !topStructPi.equals(localPi)) {
    //                        throw new PresentationException("May not generate tags from more than one document in one query");
    //                    } else if (topStructPi == null) {
    //                        topStructPi = localPi;
    //                    }
    //                }
    //                String altoString = (String) solrDoc.getFieldValue(LuceneConstants.ALTO);
    //                try {
    //                    List<NERTag> tagList = ALTOTools.getNERTags(altoString, type);
    //
    //                    PageTagCount page = createPageTagCountReference(solrDoc);
    //
    //                    for (NERTag tag : tagList) {
    //                        page.addTagToList(tag.getValue(), tag.getType());
    //                    }
    //                    pages.add(page);
    //                } catch (IOException | JDOMException e) {
    //                    throw new PresentationException(e.getMessage());
    //                }
    //            }
    //            DocumentTagCountReference doc = new DocumentTagCountReference(topStructPi);
    //            doc.addPages(pages);
    //            return doc;
    //        }
    //        return new DocumentTagCountReference();
    //    }

    /**
     * @param solrDoc
     * @return
     */
    private static Integer getPageOrder(SolrDocument solrDoc) {
        Integer order = (Integer) solrDoc.getFieldValue(SolrConstants.ORDER);
        return order;
    }

    private static TagGroup createPageReference(List<SolrDocument> solrDocs) {
        Integer firstPage = null;
        Integer lastPage = null;
        for (SolrDocument solrDocument : solrDocs) {
            Integer order = getPageOrderFromSolrDoc(solrDocument);
            if (firstPage == null || (order != null && order < firstPage)) {
                firstPage = order;
            }
            if (lastPage == null || (order != null && order > lastPage)) {
                lastPage = order;
            }
        }
        TagGroup group;
        if (firstPage == lastPage) {
            group = new PageReference(firstPage);
        } else {
            group = new MultiPageReference(firstPage, lastPage);
        }
        return group;
    }

    /**
     * @param solrDocument
     * @return
     */
    private static Integer getPageOrderFromSolrDoc(SolrDocument solrDoc) {
        if (solrDoc != null && solrDoc.containsKey(SolrConstants.ORDER)) {
            String orderString = SolrSearchIndex.getAsString(solrDoc.getFieldValue(SolrConstants.ORDER));
            try {
                //                Integer.parseInt(orderString);
                return Integer.parseInt(orderString);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    private static Comparator<SolrDocument> docOrderComparator = new Comparator<SolrDocument>() {

        @Override
        public int compare(SolrDocument doc1, SolrDocument doc2) {
            Integer order1 = getPageOrderFromSolrDoc(doc1);
            Integer order2 = getPageOrderFromSolrDoc(doc2);
            if (order1 != null && order2 != null) {
                return order1.compareTo(order2);
            } else if (order1 != null) {
                return 1;
            } else if (order2 != null) {
                return -1;
            } else {
                return 1;
            }
        }
    };

}
