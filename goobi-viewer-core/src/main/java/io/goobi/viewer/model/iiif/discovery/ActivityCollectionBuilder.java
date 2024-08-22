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
package io.goobi.viewer.model.iiif.discovery;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES_PAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.ActivityType;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Builder for both {@link de.intranda.api.iiif.discovery.OrderedCollection} and {@link de.intranda.api.iiif.discovery.OrderedCollectionPage} of
 * {@link Activity Acvitities} for the IIIF Discovery API.
 *
 * @author Florian Alpers
 */
public class ActivityCollectionBuilder {

    private static final String[] SOLR_FIELDS = { SolrConstants.PI, SolrConstants.DATEUPDATED, SolrConstants.DATECREATED, SolrConstants.DATEDELETED };
    private static final String[] FACET_FIELDS = { SolrConstants.DATEUPDATED, SolrConstants.DATECREATED };

    private static final String QUERY_ISWORK = "+" + SolrConstants.ISWORK + ":true";

    private final int activitiesPerPage = DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage();
    private Integer numActivities = null;
    private LocalDateTime startDate = null;
    private String filterQuery = "";
    private final AbstractApiUrlManager urls;
    private final SolrSearchIndex searchIndex;

    public ActivityCollectionBuilder(AbstractApiUrlManager apiUrlManager, SolrSearchIndex searchIndex, int itemsPerPage) {
        this.urls = apiUrlManager;
        this.searchIndex = searchIndex;
    }

    /**
     * Creates a An {@link de.intranda.api.iiif.discovery.OrderedCollection} of {@link Activity Acvitities}, linking to the first and last contained
     * {@link de.intranda.api.iiif.discovery.OrderedCollectionPage} as well as counting the total number of Activities
     *
     * @return An {@link de.intranda.api.iiif.discovery.OrderedCollection}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public OrderedCollection<Activity> buildCollection() throws PresentationException, IndexUnreachableException {
        OrderedCollection<Activity> collection = new OrderedCollection<>(getCollectionURI());
        collection.setTotalItems(Long.valueOf(getNumActivities()));
        collection.setFirst(new OrderedCollectionPage<>(getPageURI(0)));
        collection.setLast(new OrderedCollectionPage<>(getPageURI(getLastPageNo())));
        return collection;
    }

    /**
     * Creates An {@link de.intranda.api.iiif.discovery.OrderedCollection} of {@link Activity Activities}, i.e. a partial list of Activities. Which
     * Activities are contained within the page depends on the given pageNo as well as the configured number of entries per page defined by
     * {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage() Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     *
     * @param pageNo The number of this page, beginning with 0
     * @return An {@link de.intranda.api.iiif.discovery.OrderedCollectionPage}
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
        Long start = dates.get(0);
        Long end = dates.get(dates.size() - 1);
        SolrDocumentList docs = getDocs(start, end);

        page.setOrderedItems(buildItems(docs, start, end));

        return page;

    }

    /**
     * Set the earliest date of Activities to be included in this collection.
     *
     * @param startDate the earliest date of Activities to be included in this collection. If null, Activities are not filtered by date which is the
     *            default
     * @return The Builder itself
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public ActivityCollectionBuilder setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
        //reset numActivities because it is affected  by startDate
        this.numActivities = null;
        return this;
    }

    /**
     * Set an additional filter query which must be met by all processes for which activities are counted
     * 
     * @param query The solr query which counted processes must meet
     * @return myself
     */
    public ActivityCollectionBuilder setFilterQuery(String query) {
        this.filterQuery = query;
        this.numActivities = null;
        return this;
    }

    /**
     * Get the earliest date of Activities which may be contained in the collection
     *
     * @return the earliest date of Activities which may be contained in the collection. May return null if no startDate is specified
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Get the number of activities per page as defined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage()
     * Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
     *
     * @return the number of activities per page as defined by {@link io.goobi.viewer.controller.Configuration#getIIIFDiscoveryAvtivitiesPerPage()
     *         Configuration#getIIIFDiscoveryAvtivitiesPerPage()}
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
        String urlString = this.urls.path(ApiUrls.RECORDS_CHANGES).build();
        return URI.create(urlString);
    }

    /**
     * Get the URI to request a specific collection page
     *
     * @param no the page number
     * @return the URI to request a specific collection page
     */
    public URI getPageURI(int no) {
        String urlString = this.urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(no).build();
        UriBuilder uri = UriBuilder.fromUri(urlString);
        if (startDate != null) {
            uri.queryParam("start", startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (StringUtils.isNotBlank(filterQuery)) {
            uri.queryParam("filter", filterQuery);
        }
        return uri.build();
    }

    private int getLastPageNo() throws PresentationException, IndexUnreachableException {
        return getNumActivities() / getActivitiesPerPage();
    }

    /**
     * 
     * @param docs
     * @param startDate
     * @param endDate
     * @return List<Activity>
     */
    private List<Activity> buildItems(SolrDocumentList docs, Long startDate, Long endDate) {
        List<Activity> activities = new ArrayList<>();
        for (SolrDocument doc : docs) {
            List<Long> updates = doc.getFieldValues(SolrConstants.DATEUPDATED)
                    .stream()
                    .map(o -> (Long) o)
                    .filter(date -> date >= startDate && date <= endDate)
                    .toList();
            Long created = (Long) doc.getFieldValue(SolrConstants.DATECREATED);

            Long deleted = null;
            if (doc.containsKey(SolrConstants.DATEDELETED)) {
                deleted = (Long) doc.getFieldValue(SolrConstants.DATEDELETED);
            }
            if (created != null && created >= startDate && created <= endDate) {
                Activity createActivity = new Activity();
                createActivity.setEndTime(DateTools.getLocalDateTimeFromMillis(created, false));
                createActivity.setType(ActivityType.CREATE);
                createActivity.setObject(createObject(doc));
                activities.add(createActivity);
            }
            for (Long update : updates) {
                if (!update.equals(created)) {
                    Activity activity = new Activity();
                    activity.setEndTime(DateTools.getLocalDateTimeFromMillis(update, false));
                    activity.setType(update.equals(deleted) ? ActivityType.DELETE : ActivityType.UPDATE);
                    activity.setObject(createObject(doc));
                    activities.add(activity);
                }
            }

        }
        activities.sort((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime()));
        return activities;
    }

    private IPresentationModelElement createObject(SolrDocument doc) {
        String pi = (String) doc.getFieldValue(SolrConstants.PI);
        URI uri = URI.create(this.urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build());
        return new Manifest2(uri);
    }

    private int getNumberOfActivities(LocalDateTime startDate) throws PresentationException, IndexUnreachableException {
        Long millisStart = DateTools.getMillisFromLocalDateTime(startDate, false);
        String query = QUERY_ISWORK;
        query += SearchHelper.getAllSuffixes();
        if (startDate != null) {
            query += " +(DATEUPDATED:[" + startDate.toInstant(ZoneOffset.UTC).toEpochMilli() + " TO *] DATECREATED:["
                    + startDate.toInstant(ZoneOffset.UTC).toEpochMilli() + " TO *])";
        }
        QueryResponse qr = searchIndex.searchFacetsAndStatistics(query, List.of(filterQuery), Arrays.asList(FACET_FIELDS), 1, false);
        if (qr != null) {
            Long count = qr.getFacetFields()
                    .stream()
                    .flatMap(field -> field.getValues().stream())
                    .filter(c -> millisStart == null ? true : Long.parseLong(c.getName()) >= millisStart)
                    .mapToLong(Count::getCount)
                    .sum();
            return count.intValue();
        }

        return 0;
    }

    /**
     * Get all activity dates after startDate limited to the results between first and last; sorted chronologically ascending (earliest dates come
     * first)
     *
     * @param startDate
     * @param first
     * @param last
     * @return List<Long>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private List<Long> getActivities(LocalDateTime startDate, int first, int last) throws PresentationException, IndexUnreachableException {
        return getActivities(startDate).stream().skip(first).limit((long) last - first + 1).toList();
    }

    private List<Long> getActivities(LocalDateTime startDate) throws PresentationException, IndexUnreachableException {
        Long millisStart = DateTools.getMillisFromLocalDateTime(startDate, false);
        String query = QUERY_ISWORK;
        query += SearchHelper.getAllSuffixes();
        if (startDate != null) {
            query += " +(DATEUPDATED:[" + DateTools.getMillisFromLocalDateTime(startDate, false) + " TO *] DATECREATED:["
                    + DateTools.getMillisFromLocalDateTime(startDate, false) + " TO *])";
        }
        QueryResponse qr = searchIndex.searchFacetsAndStatistics(query, List.of(filterQuery), Arrays.asList(FACET_FIELDS), 1, false);
        if (qr != null) {
            return qr.getFacetFields()
                    .stream()
                    .flatMap(field -> field.getValues().stream())
                    .flatMap(count -> getAsList(count).stream())
                    .map(Long::parseLong)
                    .filter(millisDate -> millisStart == null ? true : millisDate >= millisStart)
                    .sorted()
                    .toList();
        }

        return new ArrayList<>();
    }

    private List<String> getAsList(Count count) {
        List<String> list = new ArrayList<>((int) count.getCount());
        for (int i = 0; i < count.getCount(); i++) {
            list.add(count.getName());
        }
        return list;
    }

    /**
     * 
     * @param startDate
     * @param endDate
     * @return {@link SolrDocumentList}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should only return topstructs
     */
    SolrDocumentList getDocs(Long startDate, Long endDate) throws PresentationException, IndexUnreachableException {
        String query = QUERY_ISWORK;
        query += SearchHelper.getAllSuffixes();
        if (startDate != null && endDate != null) {
            query = "+(" + query + ") +(DATEUPDATED:[" + startDate + " TO " + endDate + "] DATECREATED:[" + startDate + " TO " + endDate + "])";
        }

        return searchIndex.search(query, 0, Integer.MAX_VALUE, Collections.emptyList(), Collections.emptyList(), Arrays.asList(SOLR_FIELDS),
                List.of(filterQuery), Collections.emptyMap()).getResults();
    }
}
