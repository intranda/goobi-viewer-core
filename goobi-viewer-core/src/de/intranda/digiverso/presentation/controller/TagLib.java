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
package de.intranda.digiverso.presentation.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.TagCloudBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.rss.RSSFeed;
import de.intranda.digiverso.presentation.model.search.FacetItem;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.Tag;

public class TagLib {

    private static final Logger logger = LoggerFactory.getLogger(TagLib.class);
    private static TagCloudBean tagCloudBean = new TagCloudBean();

    public static List<Tag> getTagsLuceneField(String luceneField, String count, String partnerId) throws IndexUnreachableException {
        Integer countInt = Integer.valueOf(count);
        List<Tag> ret = tagCloudBean.getTags(luceneField, true, countInt, getDiscriminatorQuery());

        return ret;
    }

    /**
     * Returns a list of <code>number</code> last import titles.
     *
     * @param number
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static List<String> getLastImports(Integer number) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder("(").append(SolrConstants.ISWORK)
                .append(":true")
                .append(getDiscriminatorQuery())
                .append(')')
                .append(SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()))
                .toString();
        logger.debug("getLastImports query: {}", query);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, number, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null,
                        Arrays.asList(RSSFeed.FIELDS))
                .getResults();
        List<String> ret = new ArrayList<>(docList.size());
        for (SolrDocument doc : docList) {
            Object o = doc.getFirstValue(SolrConstants.TITLE);
            if (o != null) {
                // String title = (String) doc.getFieldValue(LuceneConstants.TITLE);
                String title = (String) o;
                ret.add(title);
            }

        }
        return ret;
    }

    /**
     * Returns a list of <code>number</code> last import thumbnails.
     *
     * @param number
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static List<String> getLastImportImages(Integer number) throws PresentationException, IndexUnreachableException {
        List<String> ret = new ArrayList<>();

        String query = new StringBuilder("(").append(SolrConstants.ISWORK)
                .append(":true")
                .append(getDiscriminatorQuery())
                .append(')')
                .append(SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery()))
                .toString();
        logger.trace("getLastImportTitles query: {}", query);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, number, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null,
                        Arrays.asList(RSSFeed.FIELDS))
                .getResults();
        List<SolrDocument> docs = docList;
        for (SolrDocument doc : docs) {
            ret.add(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc));
            //            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            //            String thumbnailFile = (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
            //            ret.add(new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=")
            //                    .append(pi).append('/').append(thumbnailFile).append("&width=150&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true")
            //                    .append(DataManager.getInstance().getConfiguration().isForceJpegConversion() ? "&format=jpg" : "").toString());
        }

        return ret;
    }

    /**
     * Returns the value of the field <code>field</code> from the document with the given identifier.
     *
     * @param pi The record identifier.
     * @param field The field to return.
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static String getFeaturedVolumeField(String pi, String field) throws PresentationException, IndexUnreachableException {
        // TODO so geht es nicht
        logger.trace("field: {}", field);
        String query = new StringBuilder(SolrConstants.PI).append(':').append(pi).toString();
        String ret = "";
        logger.debug("query: {}", query);
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(field));
        if (doc != null) {
            // logger.info("field Value: " + doc.getFieldValue(field).toString());
            Object o = doc.getFirstValue(field);
            if (o != null) {
                return (String) o;
                // return ret = (String) doc.getFieldValue(field);
            }
            // return (String) doc.getFieldValue(field);
        }

        return ret;
    }

    /**
     * Returns the value of the field <code>field</code> from the document with the given identifier.
     *
     * @param pi The record identifier.
     * @param width Image width.
     * @param height Image height.
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static String getFeaturedVolumeImage(String pi, Integer width, Integer height) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder(SolrConstants.PI).append(':').append(pi).toString();
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(SolrConstants.THUMBNAIL));
        if (doc != null) {
            Object o = doc.getFieldValue(SolrConstants.THUMBNAIL);
            if (o != null) {
                String thumbnailFile = (String) o;
                return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, width, height);
                //                return new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                //                        "?action=image&sourcepath=").append(pi).append('/').append(thumbnailFile).append("&width=").append(width).append("&height=")
                //                        .append(height).append("&rotate=0&format=jpg&resolution=72&thumbnail=true&ignoreWatermark=true").toString();
            }
            // String thumbnailFile = (String) doc.getFieldValue(LuceneConstants.THUMBNAIL);
        }

        return "";
    }

    /**
     * Returns the CS url for an current PI and image number
     *
     * @param pi
     * @param imageNumber
     * @param imageWidth
     * @param imageHeight
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static String getFeaturedImage(String pi, Integer imageNumber, Integer imageWidth, Integer imageHeight)
            throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':')
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(imageNumber - 1)
                .append(" AND ")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE.name())
                .toString();
        logger.debug("query: {}", query);
        String width = String.valueOf(imageWidth);
        String height = String.valueOf(imageHeight);

        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(SolrConstants.FILENAME));
        if (doc == null) {
            query = query.replace(" AND DOCTYPE:PAGE", " AND FILENAME:*");
            doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(SolrConstants.FILENAME));
        }
        logger.debug("doc: {}", doc);
        if (doc != null) {
            Object o = doc.getFieldValue(SolrConstants.FILENAME);
            if (o != null) {
                logger.debug(o.toString());
                String fileName = (String) o;
                return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, imageWidth, imageHeight);
                //                return new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append(
                //                        "?action=image&sourcepath=").append(pi).append('/').append(fileName).append("&width=").append(width).append("&height=")
                //                        .append(height).append("&rotate=0&format=jpg&resolution=72&ignoreWatermark=true").toString();
            }
        }

        return "";
    }

    /**
     * TODO is not necessary, because to take it over the path in collcetionsPartner is better.
     *
     * @param collectionName
     * @return
     */
    public static String getCollectionText(String collectionName) {
        String collectionText = collectionName + "Text";
        String ret = Helper.getTranslation(collectionText, null);
        if (ret.equalsIgnoreCase(collectionText)) {
            return "";
        }

        return ret;
    }

    /**
     * Returns a list of FilterLink elements for the given field over all documents in the index (optionally filtered by a subquery).
     *
     * @param field
     * @param subQuery
     * @param resultLimit
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static List<FacetItem> getDrillDown(String field, String subQuery, Integer resultLimit)
            throws PresentationException, IndexUnreachableException {
        return getDrillDown(field, subQuery, resultLimit, false);
    }

    /**
     * Returns a list of FilterLink elements for the given field over all documents in the index (optionally filtered by a subquery).
     *
     * @param field
     * @param subQuery
     * @param resultLimit
     * @param sortDescending If true, the facet items are sorted in a descending order
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static List<FacetItem> getDrillDown(String field, String subQuery, Integer resultLimit, final Boolean reverseOrder)
            throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder(100);
        sbQuery.append('(').append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true)").append(
                getDiscriminatorQuery());

        if (StringUtils.isNotEmpty(subQuery)) {
            if (subQuery.startsWith(" AND ")) {
                subQuery = subQuery.substring(5);
            }
            sbQuery.append(" AND (").append(subQuery).append(')');
        }
        // logger.debug("getDrillDown query: " + query);
        field = SearchHelper.facetifyField(field);
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, 0, null, Collections.singletonList(field),
                Collections.singletonList(SolrConstants.IDDOC));
        // TODO Filter with the docstruct whitelist?
        if (resp != null && resp.getFacetField(field) != null && resp.getFacetField(field).getValues() != null) {
            Map<String, Long> result =
                    resp.getFacetField(field).getValues().stream().filter(count -> count.getName().charAt(0) != 1).sorted((count1, count2) -> {
                        int compValue;
                        if (count1.getName().matches("\\d+") && count2.getName().matches("\\d+")) {
                            compValue = Long.compare(Long.parseLong(count1.getName()), Long.parseLong(count2.getName()));
                        } else {
                            compValue = count1.getName().compareToIgnoreCase(count2.getName());
                        }
                        if (Boolean.TRUE.equals(reverseOrder)) {
                            compValue *= -1;
                        }
                        return compValue;
                    }).limit(resultLimit > 0 ? resultLimit : resp.getFacetField(field).getValues().size()).collect(
                            Collectors.toMap(Count::getName, Count::getCount));
            List<String> hierarchicalFields = DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields();
            Locale locale = null;
            NavigationHelper nh = BeanUtils.getNavigationHelper();
            if (nh != null) {
                locale = nh.getLocale();
            }

            return FacetItem.generateFacetItems(field, result, true, reverseOrder, hierarchicalFields.contains(field) ? true : false, locale);
        }

        return Collections.emptyList();
    }

    public static String teaserList(String teaserList) {
        logger.trace("teaserList: {}", teaserList);
        return "";
    }

    private static String getDiscriminatorQuery() throws IndexUnreachableException {
        return SearchHelper.getDiscriminatorFieldFilterSuffix(BeanUtils.getNavigationHelper(),
                DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }
}
