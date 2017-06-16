/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;

import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.user.User;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * Persistable search query.
 */
@Entity
@Table(name = "searches")
public class Search implements Serializable {

    private static final long serialVersionUID = -8968560376731964763L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long id;

    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_input")
    private String userInput;

    @Column(name = "query", nullable = false, columnDefinition = "LONGTEXT")
    private String query;

    @Column(name = "expand_query", nullable = false, columnDefinition = "LONGTEXT")
    private String expandQuery;

    @Column(name = "page", nullable = false)
    private int page;

    @Column(name = "collection")
    private String collection;

    @Column(name = "sort_field")
    private String sortField;

    @Column(name = "filter")
    private String filter;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated", nullable = false)
    private Date dateUpdated;

    @Transient
    private boolean saved = false;

    /** Total hits count for the current search. */
    @Transient
    private long hitsCount = 0;

    /** BrowseElement list for the current search result page. */
    @Transient
    private final List<SearchHit> hits = new ArrayList<>();

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Search other = (Search) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        return true;
    }

    /**
     * Constructs a search URL using the query parameters contained in this object.
     *
     * @return
     */
    public String getUrl() {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        sbUrl.append('/').append(PageType.search.getName());
        sbUrl.append('/').append((StringUtils.isNotEmpty(collection) ? collection : "-"));
        sbUrl.append('/').append(query).append('/').append(page);
        sbUrl.append('/').append((StringUtils.isNotEmpty(sortField) ? sortField : "-"));
        sbUrl.append('/').append((StringUtils.isNotEmpty(filter) ? filter : "-")).append('/');
        return sbUrl.toString();
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the userInput
     */
    public String getUserInput() {
        return userInput;
    }

    /**
     * @param userInput the userInput to set
     */
    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the expandQuery
     */
    public String getExpandQuery() {
        return expandQuery;
    }

    /**
     * @param expandQuery the expandQuery to set
     */
    public void setExpandQuery(String expandQuery) {
        this.expandQuery = expandQuery;
    }

    /**
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * @param saved the saved to set
     */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /**
     * @return the hitsCount
     */
    public long getHitsCount() {
        return hitsCount;
    }

    /**
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        this.hitsCount = hitsCount;
    }

    /**
     * @return the hits
     */
    public List<SearchHit> getHits() {
        return hits;
    }
}
