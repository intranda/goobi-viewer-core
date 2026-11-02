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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Presentation data for the CMS search hero. Index facts are loaded at most once per request.
 */
@Named
@RequestScoped
public class SearchHeroBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(SearchHeroBean.class);

    private static final List<Slot> SLOTS = List.of(
            new Slot("a", 1200),
            new Slot("b", 900),
            new Slot("c", 900));

    @Inject
    private CollectionBrowseBean collectionBrowseBean;

    private Integer collectionCount;
    private int[] years;

    /**
     * Links emphasized portions of trusted CMS heading markup to the collections. Existing links are preserved,
     * including emphasis inside a link or containing a link; none are nested or overwritten.
     *
     * @param heading trusted CMS heading HTML, or null
     * @param collectionsUrl destination for emphasized text that is not already linked
     * @return the heading fragment with collection links
     */
    public String getHeading(String heading, String collectionsUrl) {
        if (heading == null) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(heading);
        document.outputSettings().prettyPrint(false);
        if (StringUtils.isNotBlank(collectionsUrl)) {
            for (Element emphasis : document.select("em")) {
                if (emphasis.closest("a") != null || emphasis.selectFirst("a") != null) {
                    continue;
                }
                List<Node> content = new ArrayList<>(emphasis.childNodes());
                Element link = emphasis.appendElement("a").attr("href", collectionsUrl);
                content.forEach(link::appendChild);
            }
        }
        return document.body().html();
    }

    /**
     * @return the three immutable image-slot definitions in display order
     */
    public List<Slot> getSlots() {
        return SLOTS;
    }

    /**
     * @return the number of top-level DC collections, or zero if the index cannot be reached
     */
    public int getCollectionCount() {
        if (collectionCount == null) {
            collectionCount = 0;
            try {
                collectionBrowseBean.getList(SolrConstants.DC, 0);
                CollectionView collection = collectionBrowseBean.getCollection(SolrConstants.DC);
                if (collection != null) {
                    collectionCount = (int) collection.getCompleteList().stream().filter(element -> element.getLevel() == 0).count();
                }
            } catch (IndexUnreachableException e) {
                logger.warn("Could not load search hero collections: {}", e.getMessage());
            }
        }
        return collectionCount;
    }

    /**
     * @return the earliest indexed year, or -1 if unavailable
     */
    public int getFirstYear() {
        loadYears();
        return years[0];
    }

    /**
     * @return the latest indexed year, or -1 if unavailable
     */
    public int getLastYear() {
        loadYears();
        return years[1];
    }

    private void loadYears() {
        if (years == null) {
            years = new int[] { -1, -1 };
            try {
                years = SearchHelper.getMinMaxYears(null);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.warn("Could not load search hero years: {}", e.getMessage());
            }
        }
    }

    /**
     * An image slot with JavaBean accessors for Facelets expressions.
     */
    public static final class Slot implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String area;
        private final int imageSize;

        private Slot(String area, int imageSize) {
            this.area = area;
            this.imageSize = imageSize;
        }

        public String getArea() {
            return area;
        }

        public int getImageSize() {
            return imageSize;
        }
    }
}
