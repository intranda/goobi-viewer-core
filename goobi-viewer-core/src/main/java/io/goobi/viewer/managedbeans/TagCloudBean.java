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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.Tag;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Bean for the tag cloud page.
 */
@Named
@SessionScoped
public class TagCloudBean implements Serializable {

    private static final long serialVersionUID = -8655268287774862843L;

    private static final Logger logger = LogManager.getLogger(TagCloudBean.class);

    private Map<String, List<Tag>> lists = new HashMap<>();

    /**
     * Empty constructor.
     */
    public TagCloudBean() {
        // the emptiness inside
    }

    /**
     * Returns up to 50 publication-year tags from the {@code YEARPUBLISH} Solr field, shuffled.
     *
     * @return a list of tags derived from the YEARPUBLISH field for use in a tag cloud
     */
    public List<Tag> getTagsAutor() {
        try {
            return getTagsForField(SolrConstants.YEARPUBLISH, true, 50, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * Returns up to 50 tags from the {@code DEFAULT} Solr field, shuffled.
     *
     * @return a list of tags derived from the DEFAULT field for use in a tag cloud
     */
    public List<Tag> getTags() {
        try {
            return getTagsForField(SolrConstants.DEFAULT, true, 50, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * Returns the number of tags in the DEFAULT field tag list.
     *
     * @return number of DEFAULT field tags
     */
    public int getTagsSize() {
        return getTags().size();
    }

    /**
     * Returns up to 100 title tags from the {@code TITLE} Solr field, shuffled.
     *
     * @return a list of tags derived from the TITLE field for use in a tag cloud
     */
    public List<Tag> getTagsTitles() {
        try {
            return getTagsForField(SolrConstants.TITLE, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * Returns up to 100 publication-place tags from the {@code PLACEPUBLISH} Solr field, shuffled.
     *
     * @return a list of tags derived from the PLACEPUBLISH field for use in a tag cloud
     */
    public List<Tag> getTagsPlaces() {
        try {
            return getTagsForField(SolrConstants.PLACEPUBLISH, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * Returns up to 100 publication-year tags from the {@code YEARPUBLISH} Solr field, shuffled.
     *
     * @return a list of tags derived from the YEARPUBLISH field for use in a year tag cloud
     */
    public List<Tag> getTagsYears() {
        try {
            return getTagsForField(SolrConstants.YEARPUBLISH, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * Returns up to 100 person-name tags from the {@code PERSON_ONEFIELD} Solr field, shuffled.
     *
     * @return a list of tags derived from the PERSON_ONEFIELD field for use in a persons tag cloud
     */
    public List<Tag> getTagsPersons() {
        try {
            return getTagsForField(SolrConstants.PERSON_ONEFIELD, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    /**
     * getTags.
     *
     * @param luceneField Solr field to collect tag values from.
     * @param shuffle if true, randomizes the returned tag list order.
     * @param topCount maximum number of tags to return; 0 means unlimited.
     * @param query optional Solr filter query; null or empty for all records.
     * @return a list of Tag objects for the given Solr field, optionally shuffled and limited in count
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<Tag> getTags(String luceneField, boolean shuffle, int topCount, String query) throws IndexUnreachableException {
        return this.getTagsForField(luceneField, shuffle, topCount, query);
    }

    private List<Tag> getTagsForField(String luceneField, boolean shuffle, int topCount, String query) throws IndexUnreachableException {
        if (!lists.containsKey(luceneField) || StringUtils.isNotEmpty(query)) {
            List<Tag> mytags = new ArrayList<>();

            // int byzero = 1/0;

            String suffix = SearchHelper.getAllSuffixes();
            if (StringUtils.isEmpty(query)) {
                mytags = DataManager.getInstance().getSearchIndex().generateFilteredTagCloud(luceneField, suffix);
            } else {
                mytags = DataManager.getInstance().getSearchIndex().generateFilteredTagCloud(luceneField, query + suffix);
            }

            if (topCount > 0) {
                Collections.sort(mytags);
                while (mytags.size() > topCount) {
                    mytags.remove(topCount);
                }
                int i = mytags.size();
                for (Tag tag : mytags) {
                    tag.setCss(String.valueOf(i));
                    i--;
                }
            }
            if (shuffle) {
                Collections.shuffle(mytags);
            }
            lists.put(luceneField, mytags);
        }

        return lists.get(luceneField);
    }
}
