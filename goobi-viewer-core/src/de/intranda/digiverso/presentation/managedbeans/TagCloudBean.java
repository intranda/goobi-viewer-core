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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.Tag;

/**
 * Bean for the tag cloud page.
 */
@Named
@SessionScoped
public class TagCloudBean implements Serializable {

    private static final long serialVersionUID = -8655268287774862843L;

    private static final Logger logger = LoggerFactory.getLogger(TagCloudBean.class);

    private Map<String, List<Tag>> lists = new HashMap<>();

    /** Empty constructor. */
    public TagCloudBean() {
        // the emptiness inside
    }

    public List<Tag> getTagsAutor() {
        try {
            return getTagsForField(SolrConstants.YEARPUBLISH, true, 50, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public List<Tag> getTags() {
        try {
            return getTagsForField(SolrConstants.DEFAULT, true, 50, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public int getTagsSize() {
        return getTags().size();
    }

    public List<Tag> getTagsTitles() {
        try {
            return getTagsForField(SolrConstants.TITLE, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public List<Tag> getTagsPlaces() {
        try {
            return getTagsForField(SolrConstants.PLACEPUBLISH, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public List<Tag> getTagsYears() {
        try {
            return getTagsForField(SolrConstants.YEARPUBLISH, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public List<Tag> getTagsPersons() {
        try {
            return getTagsForField(SolrConstants.PERSON_ONEFIELD, true, 100, null);
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
            return Collections.emptyList();
        }
    }

    public List<Tag> getTags(String luceneField, boolean shuffle, int topCount, String query) throws IndexUnreachableException {
        return this.getTagsForField(luceneField, shuffle, topCount, query);
    }

    private List<Tag> getTagsForField(String luceneField, boolean shuffle, int topCount, String query) throws IndexUnreachableException {
        if (!lists.containsKey(luceneField) || StringUtils.isNotEmpty(query)) {
            List<Tag> mytags = new ArrayList<>();

            // int byzero = 1/0;

            String suffix = SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery());
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