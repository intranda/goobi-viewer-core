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
package de.intranda.digiverso.presentation.model.iiif.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.OrderedCollection;
import de.intranda.digiverso.presentation.model.iiif.OrderedCollectionPage;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.ManifestBuilder;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
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

    public ActivityCollectionBuilder(HttpServletRequest request) {
        this.request = Optional.ofNullable(request);
        this.servletURI = URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
        this.requestURI = URI.create(ServletUtils.getServletPathWithoutHostAsUrlFromRequest(request) + request.getRequestURI());
    }

    public ActivityCollectionBuilder(URI servletUri, URI requestURI) {
        this.request = Optional.empty();
        this.servletURI = servletUri;
        this.requestURI = requestURI;
    }

    public OrderedCollection<Activity> buildCollection() throws PresentationException, IndexUnreachableException {
        OrderedCollection<Activity> collection = new OrderedCollection<>(getCollectionURI());
        collection.setTotalItems(getNumActivities());
        collection.setFirst(new OrderedCollectionPage<>(getPageURI(0)));
        collection.setLast(new OrderedCollectionPage<>(getPageURI(getLastPageNo())));
        return collection;
    }

    /**
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public int getLastPageNo() throws PresentationException, IndexUnreachableException {
        return getNumActivities() / getActivitiesPerPage();
    }

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
     * @param docs
     * @return
     */
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

    /**
     * @param doc
     * @return
     */
    private IPresentationModelElement createObject(SolrDocument doc) {
        String pi = (String) doc.getFieldValue(SolrConstants.PI);
        URI uri = new ManifestBuilder(servletURI, requestURI).getManifestURI(pi);
        Manifest manifest = new Manifest(uri);
        return manifest;
    }

    /**
     * @return The requested url before any presentation specific parts. Generally the rest api url. Includes a trailing slash
     */
    protected URI getBaseUrl() {

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

    /**
     * @param earliestDate
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
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

    /**
     * @param last
     * @param first
     * @param earliestDate
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private SolrDocumentList getDocs(Long startDate, Long endDate) throws PresentationException, IndexUnreachableException {
        String query = "ISWORK:true";
        query += " " + SearchHelper.getAllSuffixes(false);
        if (startDate != null) {
            query += " AND (DATEUPDATED:[" + startDate + " TO " + endDate + "] OR DATECREATED:[" + startDate + " TO " + endDate + "])";
        }
        SolrDocumentList list =
                DataManager.getInstance().getSearchIndex().search(query, getActivitiesPerPage(), null, Arrays.asList(SOLR_FIELDS));
        return list;
    }

    public ActivityCollectionBuilder setStartDate(Date startDate) throws PresentationException, IndexUnreachableException {
        this.startDate = startDate;
        //reset numActivities because it is affected  by startDate
        this.numActivities = null;
        return this;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @return the activitiesPerPage
     */
    public int getActivitiesPerPage() {
        return activitiesPerPage;
    }

    /**
     * @return the numActivities
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public int getNumActivities() throws PresentationException, IndexUnreachableException {
        if (numActivities == null) {
            numActivities = getNumberOfActivities(getStartDate());
        }
        return numActivities;
    }

    public URI getCollectionURI() {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/discovery/activities");
        return URI.create(sb.toString());
    }

    public URI getPageURI(int no) {
        StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/discovery/activities/").append(no);
        return URI.create(sb.toString());
    }

}
