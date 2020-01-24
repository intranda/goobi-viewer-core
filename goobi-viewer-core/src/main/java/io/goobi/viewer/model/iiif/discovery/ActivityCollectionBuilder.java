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
package io.goobi.viewer.model.iiif.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.ActivityType;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Manifest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Builder for both {@link de.intranda.api.iiif.discovery.OrderedCollection} and {@link de.intranda.api.iiif.discovery.OrderedCollectionPage} of {@link Activity Acvitities}
 * for the IIIF Discovery API.
 *
 * @author Florian Alpers
 */
public class ActivityCollectionBuilder {

    private final static String[] SOLR_FIELDS = { SolrConstants.PI, SolrConstants.DATEUPDATED, SolrConstants.DATECREATED, SolrConstants.DATEDELETED };
    private final static String[] FACET_FIELDS = { SolrConstants.DATEUPDATED, SolrConstants.DATECREATED };

    private final URI servletURI;
    private final URI requestURI;
    private final Optional<HttpServletRequest> request;
    private final int activitiesPerPage = DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage();
    private Integer numActivities = null;
    private Date startDate = null;

    /**
     * Constructs the builder from a {@link javax.servlet.http.HttpServletRequest}
     *
     * @param request   The request to which to respond (for URI creation)
     */
    public ActivityCollectionBuilder(HttpServletRequest request) {
        this.request = Optional.ofNullable(request);
        this.servletURI = URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        this.requestURI = URI.create(ServletUtils.getServletPathWithoutHostAsUrlFromRequest(request) + request.getRequestURI());
    }

    /**
     * Constructs the builder from specific URIs; used to testing
     *
     * @param servletUri    The URI of the containing server
     * @param requestURI    The URI called by the request
     */
    public ActivityCollectionBuilder(URI servletUri, URI requestURI) {
        this.request = Optional.empty();
        this.servletURI = servletUri;
        this.requestURI = requestURI;
    }

    /**
     * Creates a An {@link de.intranda.api.iiif.discovery.OrderedCollection} of {@link Activity Acvitities}, linking to the first and last contained
     * {@link de.intranda.api.iiif.discovery.OrderedCollectionPage} as well as counting the total number of Activities
     *
     * @return  An {@link de.intranda.api.iiif.discovery.OrderedCollection}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public OrderedCollection<Activity> buildCollection() throws PresentationException, IndexUnreachableException {
        OrderedCollection<Activity> collection = new OrderedCollection<>(getCollectionURI());
        collection.setTotalItems(getNumActivities());
        collection.setFirst(new OrderedCollectionPage<>(getPageURI(0)));
        collection.setLast(new OrderedCollectionPage<>(getPageURI(getLastPageNo())));
        return collection;
    }

    /**
     * Creates An {@link de.intranda.api.iiif.discovery.OrderedCollection} of {@link Activity Acvitities}, i.e. a partial list of Activities.
     * Which Activities are contained within the page depends on the given pageNo as well as the configured number of
     * entries per page defined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage() Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     *
     * @param pageNo    The number of this page, beginning with 0
     * @return  An {@link de.intranda.api.iiif.discovery.OrderedCollectionPage}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public OrderedCollectionPage<Activity> buildPage(int pageNo) throws PresentationException, IndexUnreachableException {

        int first = pageNo * getActivitiesPerPage();
        int last = first + getActivitiesPerPage() - 1;

        OrderedCollectionPage<Activity> page = new OrderedCollectionPage<>(getPageURI(pageNo));
        OrderedCollection<Activity> parent = new OrderedCollection<>(getCollectionURI());
        page.setPartOf(parent);
        if (pageNo > 0) {
            OrderedCollectionPage<Activity> prev = new OrderedCollectionPage<>(getPageURI(pageNo - 1));
            page.setPrev(prev);
        }
        if (pageNo < getLastPageNo()) {
            OrderedCollectionPage<Activity> next = new OrderedCollectionPage<>(getPageURI(pageNo + 1));
            page.setNext(next);
        }

        List<Long> dates = getActivities(startDate, first, last);
        Long startDate = dates.get(0);
        Long endDate = dates.get(dates.size() - 1);
        SolrDocumentList docs = getDocs(startDate, endDate);

        page.setOrderedItems(buildItems(docs, startDate, endDate));

        return page;

    }
    
    /**
     * Set the earliest date of Activities to be included in this collection.
     *
     * @param startDate the earliest date of Activities to be included in this collection. If null, Activities are not filtered by date which is the default
     * @return  The Builder itself
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public ActivityCollectionBuilder setStartDate(Date startDate) throws PresentationException, IndexUnreachableException {
        this.startDate = startDate;
        //reset numActivities because it is affected  by startDate
        this.numActivities = null;
        return this;
    }

    /**
     * Get the earliest date of Activities which may be contained in the collection
     *
     * @return the earliest date of Activities which may be contained in the collection. May return null if no startDate is specified
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Get the number of activities per page as defined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage() Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     *
     * @return the number of activities per page as defined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage() Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     */
    public int getActivitiesPerPage() {
        return activitiesPerPage;
    }

    /**
     * Get the total number of {@link Activity Activities} in the collection
     *
     * @return the total number of {@link Activity Activities} in the collection
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getNumActivities() throws PresentationException, IndexUnreachableException {
        if (numActivities == null) {
            numActivities = getNumberOfActivities(getStartDate());
        }
        return numActivities;
    }

    /**
     * Get the URI for the collection request
     *
     * @return the URI for the collection request
     */
    public URI getCollectionURI() {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/discovery/activities");
        return URI.create(sb.toString());
    }

    /**
     * Get the URI to request a specific collection page
     *
     * @param no    the page number
     * @return  the URI to request a specific collection page
     */
    public URI getPageURI(int no) {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/discovery/activities/").append(no);
        return URI.create(sb.toString());
    }
    

    private int getLastPageNo() throws PresentationException, IndexUnreachableException {
        return getNumActivities() / getActivitiesPerPage();
    }

    private List<Activity> buildItems(SolrDocumentList docs, Long startDate, Long endDate) {
        List<Activity> activities = new ArrayList<>();
        for (SolrDocument doc : docs) {

            List<Long> updates = doc.getFieldValues(SolrConstants.DATEUPDATED).stream().map(o -> (Long) o).filter(date -> date >= startDate && date <= endDate).collect(Collectors.toList());
            Long created = (Long) doc.getFieldValue(SolrConstants.DATECREATED);
            
            Long deleted = null;
            if (doc.containsKey(SolrConstants.DATEDELETED)) {
                deleted = (Long) doc.getFieldValue(SolrConstants.DATEDELETED);
            }
            if(created >= startDate && created <= endDate) {                
                Activity createActivity = new Activity();
                createActivity.setEndTime(new Date(created));
                createActivity.setType(ActivityType.CREATE);
                createActivity.setObject(createObject(doc));
                activities.add(createActivity);
            }
            for (Long update : updates) {
                if(!update.equals(created)) {                    
                    Activity activity = new Activity();
                    activity.setEndTime(new Date(update));
                    activity.setType(update.equals(deleted) ? ActivityType.DELETE : ActivityType.UPDATE);
                    activity.setObject(createObject(doc));
                    activities.add(activity);
                }
            }
            
        }
        activities.sort((a1,a2) -> a1.getEndTime().compareTo(a2.getEndTime()));
        return activities;
    }

    private IPresentationModelElement createObject(SolrDocument doc) {
        String pi = (String) doc.getFieldValue(SolrConstants.PI);
        URI uri = new ManifestBuilder(servletURI, requestURI).getManifestURI(pi);
        Manifest manifest = new Manifest(uri);
        return manifest;
    }

    /**
     * @return The requested url before any presentation specific parts. Generally the rest api url. Includes a trailing slash
     */
    private URI getBaseUrl() {

        String request = requestURI.toString();
        if (!request.contains("/iiif/")) {
            return requestURI;
        }
        request = request.substring(0, request.indexOf("/iiif/") + 1);
        try {
            return new URI(request);
        } catch (URISyntaxException e) {
            return requestURI;
        }

    }

    private int getNumberOfActivities(Date startDate) throws PresentationException, IndexUnreachableException {
        String query = "ISWORK:true";
        query += " " + SearchHelper.getAllSuffixes(false);
        if (startDate != null) {
            query += " AND (DATEUPDATED:[" + startDate + " TO*] OR DATECREATED:[" + startDate + " TO *])";
        }
        QueryResponse qr = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(query, Arrays.asList(FACET_FIELDS), 1, false);
        if (qr != null) {
            Long count = qr.getFacetFields().stream().flatMap(field -> field.getValues().stream()).map(Count::getName).distinct().count();
            return count.intValue();
        } else {
            return 0;
        }
    }

    /**
     * Get all activity dates after startDate limited to the results between first and last; sorted chronologically ascending (earliest dates come
     * first)
     * 
     * @param startDate
     * @param first
     * @param last
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private List<Long> getActivities(Date startDate, int first, int last) throws PresentationException, IndexUnreachableException {
        return getActivities(startDate).stream().skip(first).limit(last - first + 1).collect(Collectors.toList());
    }

    private List<Long> getActivities(Date startDate) throws PresentationException, IndexUnreachableException {
        String query = "ISWORK:true";
        query += " " + SearchHelper.getAllSuffixes(false);
        if (startDate != null) {
            query += " AND (DATEUPDATED:[" + startDate + " TO *] OR DATECREATED:[" + startDate + " TO *])";
        }
        QueryResponse qr = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(query, Arrays.asList(FACET_FIELDS), 1, false);
        if (qr != null) {
            List<Long> list = qr.getFacetFields()
                    .stream()
                    .flatMap(field -> field.getValues().stream())
                    .map(Count::getName)
                    .distinct()
                    .map(Long::parseLong)
                    .sorted()
                    .collect(Collectors.toList());
            return list;
        } else {
            return new ArrayList<>();
        }
    }

    private SolrDocumentList getDocs(Long startDate, Long endDate) throws PresentationException, IndexUnreachableException {
        String query = "ISWORK:true";
        query += " " + SearchHelper.getAllSuffixes(false);
        if (startDate != null && endDate != null) {
            query = "(" + query + ") AND (DATEUPDATED:[" + startDate + " TO " + endDate + "] OR DATECREATED:[" + startDate + " TO " + endDate + "])";
        }
        SolrDocumentList list =
                DataManager.getInstance().getSearchIndex().search(query, getActivitiesPerPage(), null, Arrays.asList(SOLR_FIELDS));
        return list;
    }


}
