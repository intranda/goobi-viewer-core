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
package io.goobi.viewer.model.bookmark;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.RestApiManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * <p>
 * BookmarkList class.
 * </p>
 */
@Entity
@Table(name = "bookshelves")
@JsonInclude(Include.NON_NULL)
public class BookmarkList implements Serializable, Comparable<BookmarkList> {

    /**
     *
     */
    public static final String MIRADOR_LIB_PATH = "/resources/javascript/libs/mirador/";

    private static final long serialVersionUID = -3040539541804852903L;

    private static final Logger logger = LogManager.getLogger(BookmarkList.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "bookshelf_id")
    private Long id;

    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "public")
    private boolean isPublic = false;

    // Field length had to be limited to 64 chars (SHA-256 length) because InnoDB only supports 767 bytes per index,
    // and the unique index will require 255*n bytes (where n depends on the charset)
    @Column(name = "share_key", unique = true, columnDefinition = "VARCHAR(64)")
    private String shareKey;

    @Column(name = "date_updated", nullable = true)
    @JsonIgnore
    private LocalDateTime dateUpdated;

    @OneToMany(mappedBy = "bookmarkList", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @PrivateOwned
    private List<Bookmark> items = new ArrayList<>();

    /** UserGroups that may access this bookshelf. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "bookshelf_group_shares", joinColumns = @JoinColumn(name = "bookshelf_id"),
            inverseJoinColumns = @JoinColumn(name = "user_group_id"))
    @JsonIgnore
    private List<UserGroup> groupShares = new ArrayList<>();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (owner == null ? 0 : owner.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
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
        BookmarkList other = (BookmarkList) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else {
            return id.equals(other.id);
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (getOwner() == null) {
            if (other.getOwner() != null) {
                return false;
            }
        } else if (!getOwner().equals(other.getOwner())) {
            return false;
        }
        return true;
    }

    /**
     * Descending order by dateUpdated.
     */
    @Override
    public int compareTo(BookmarkList o) {
        if (dateUpdated != null) {
            if (o.getDateUpdated() == null) {
                return -1;
            }
            return dateUpdated.compareTo(o.getDateUpdated()) * -1;
        }

        return 1;
    }

    /**
     * add bookshelf to list and save
     *
     * @param item a {@link io.goobi.viewer.model.bookmark.Bookmark} object.
     * @return boolean if list changed
     */
    public boolean addItem(Bookmark item) {
        if (items == null) {
            items = new ArrayList<>();
        }

        if (item != null && !items.contains(item) && items.add(item)) {
            item.setBookmarkList(this);
            return true;
        }
        return false;
    }

    /**
     * remove bookshelf from list and save
     *
     * @param item a {@link io.goobi.viewer.model.bookmark.Bookmark} object.
     * @return boolean if list changed
     */
    public boolean removeItem(Bookmark item) {
        if (items != null) {
            return items.remove(item);
        }
        return false;
    }

    /**
     * add user group to list and save
     *
     * @param group a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return boolean if list changed
     */
    public boolean addGroupShare(UserGroup group) {
        return group != null && !groupShares.contains(group) && groupShares.add(group);
    }

    /**
     * remove user group from list and save
     *
     * @param group a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return boolean if list changed
     */
    public boolean removeGroupShare(UserGroup group) {
        return groupShares != null && groupShares.remove(group);
    }

    /**
     * Returns a Solr query that would retrieve the Solr documents representing the items listed on this bookshelf.
     *
     * @should return correct query
     * @return a {@link java.lang.String} object.
     */
    public String generateSolrQueryForItems() {
        StringBuilder sb = new StringBuilder();
        if (items.isEmpty()) {
            return "-*:*";
        }
        for (Bookmark item : items) {
            if (StringUtils.isNotEmpty(item.getPi())) {
                if (StringUtils.isNotEmpty(item.getLogId())) {
                    // with LOGID
                    sb.append('(')
                            .append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(item.getPi())
                            .append(SolrConstants.SOLR_QUERY_AND)
                            .append(SolrConstants.LOGID)
                            .append(':')
                            .append(item.getLogId())
                            .append(SolrConstants.SOLR_QUERY_AND)
                            .append(SolrConstants.DOCTYPE)
                            .append(':')
                            .append(DocType.DOCSTRCT.name())
                            .append(')');
                } else {
                    // just PI
                    sb.append('(').append(SolrConstants.PI).append(':').append(item.getPi()).append(')');
                }
                sb.append(" OR ");
            } else if (StringUtils.isNotEmpty(item.getUrn())) {
                sb.append('(')
                        .append(SolrConstants.URN)
                        .append(':')
                        .append(item.getUrn())
                        .append(SolrConstants.SOLR_QUERY_OR)
                        .append(SolrConstants.IMAGEURN)
                        .append(':')
                        .append(item.getUrn())
                        .append(") OR ");
            }
        }
        if (sb.length() >= 4) {
            sb.delete(sb.length() - 4, sb.length());
        }

        return sb.toString();
    }

    /**
     * <p>
     * isMayView.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isMayView(User user) {
        if (isPublic) {
            return true;
        }
        if (user == null) {
            return false;
        }
        if (user.equals(owner)) {
            return true;
        }
        // TODO This is expensive - cache shared lists somewhere
        try {
            if (BookmarkTools.getBookmarkListsSharedWithUser(user).contains(this)) {
                return true;
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return false;
        }

        return false;
    }

    /**
     * <p>
     * isMayEdit.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isMayEdit(User user) throws DAOException {
        if (user == null) {
            return false;
        }
        if (owner != null && owner.equals(user)) {
            return true;
        }

        for (UserGroup ug : groupShares) {
            if (ug.getOwner().equals(user) || ug.getMembers().contains(user)) {
                return true;
            }
        }

        return false;
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name the name to set
     */
    public void setName(String name) {
        if (name != null) {
            this.name = name.trim();
        } else {
            this.name = name;
        }
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * hasDescription.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasDescription() {
        return StringUtils.isNotBlank(getDescription());
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * isIsPublic.
     * </p>
     *
     * @return the isPublic
     */
    public boolean isIsPublic() {
        return isPublic;
    }

    /**
     * <p>
     * getPublicString.
     * </p>
     *
     * @return the isPublic Value as a String <br>
     *         surrounded with ()
     */
    @JsonIgnore
    public String getPublicString() {
        String publicString = "";

        if (this.isPublic) {
            publicString = "(" + ViewerResourceBundle.getTranslation("public", null) + ")";
        }

        return publicString;
    }

    /**
     * <p>
     * Setter for the field <code>isPublic</code>.
     * </p>
     *
     * @param isPublic the isPublic to set
     */
    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * <p>
     * Getter for the field <code>shareKey</code>.
     * </p>
     *
     * @return the shareKey
     */
    public String getShareKey() {
        return shareKey;
    }

    /**
     * <p>
     * Setter for the field <code>shareKey</code>.
     * </p>
     *
     * @param shareKey the shareKey to set
     */
    public void setShareKey(String shareKey) {
        this.shareKey = shareKey;
    }

    public boolean hasShareKey() {
        return this.shareKey != null;
    }

    /**
     * Generates a persistent share key for public sharing via link.
     */
    public void generateShareKey() {
        setShareKey(StringTools.generateHash(String.valueOf(System.currentTimeMillis())));
    }

    /**
     * Removes the share key.
     */
    public void removeShareKey() {
        setShareKey(null);
    }

    /**
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>
     * getNumItems.
     * </p>
     *
     * @return Number of items
     */
    public int getNumItems() {
        return items.size();
    }

    /**
     * <p>
     * Getter for the field <code>items</code>.
     * </p>
     *
     * @return the items
     */
    public List<Bookmark> getItems() {
        return items;
    }

    /**
     * <p>
     * Setter for the field <code>items</code>.
     * </p>
     *
     * @param items the items to set
     */
    public void setItems(List<Bookmark> items) {
        this.items = items;
    }

    /**
     * <p>
     * Getter for the field <code>groupShares</code>.
     * </p>
     *
     * @return the groupShares
     */
    public List<UserGroup> getGroupShares() {
        if (groupShares == null) {
            groupShares = new ArrayList<>();
        }
        return groupShares;
    }

    /**
     * <p>
     * Setter for the field <code>groupShares</code>.
     * </p>
     *
     * @param groupShares the groupShares to set
     */
    public void setGroupShares(List<UserGroup> groupShares) {
        this.groupShares = groupShares;
    }

    /**
     * <p>
     * getOwnerName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOwnerName() {
        if (getOwner() != null) {
            return getOwner().getDisplayName();
        }
        return null;
    }

    /**
     * <p>
     * getMiradorJsonObject.
     * </p>
     *
     * @param applicationRoot a {@link java.lang.String} object.
     * @param restApiUrl
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should generate JSON object correctly
     */
    public String getMiradorJsonObject(String applicationRoot, String restApiUrl)
            throws ViewerConfigurationException, IndexUnreachableException, PresentationException {
        // int cols = (int) Math.sqrt(items.size());
        int cols = (int) Math.ceil(Math.sqrt(items.size()));
        int rows = (int) Math.ceil(items.size() / (float) cols);

        JSONObject root = new JSONObject();
        root.put("id", "miradorViewer");
        root.put("layout", rows + "x" + cols);
        root.put("buildPath", applicationRoot + MIRADOR_LIB_PATH);

        JSONArray dataArray = new JSONArray();
        JSONArray windowObjectsArray = new JSONArray();
        String queryRoot = SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT + " AND " + SolrConstants.PI_TOPSTRUCT + ":";
        //        int row = 1;
        //        int col = 1;
        for (Bookmark bi : items) {
            String pi = bi.getPi();
            String manifestUrl;
            if (RestApiManager.isLegacyUrl(restApiUrl)) {
                manifestUrl = getLegacyManifestUrl(pi);
            } else {
                manifestUrl = new ApiUrls(restApiUrl)
                        .path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_MANIFEST)
                        .params(pi)
                        .build();
            }
            boolean sidePanel = DataManager.getInstance().getSearchIndex().getHitCount(queryRoot + bi.getPi()) > 1;

            JSONObject dataItem = new JSONObject();
            dataItem.put("manifestUri", manifestUrl);
            dataItem.put("location", "Goobi viewer");
            dataArray.put(dataItem);

            JSONObject windowObjectItem = new JSONObject();
            windowObjectItem.put("loadedManifest", manifestUrl);
            //windowObjectItem.put("slotAddress", "row" + row + ".column" + col);
            windowObjectItem.put("sidePanel", sidePanel);
            windowObjectItem.put("sidePanelVisible", false);
            windowObjectItem.put("bottomPanel", false);
            windowObjectItem.put("viewType", "ImageView");
            windowObjectsArray.put(windowObjectItem);

            //            col++;
            //            if (col > cols) {
            //                col = 1;
            //                row++;
            //            }
        }
        root.put("data", dataArray);
        root.put("windowObjects", windowObjectsArray);

        return root.toString();
    }

    /**
     * @param pi
     * @return Generated URL
     */
    public String getLegacyManifestUrl(String pi) {
        return new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl()).append("iiif/manifests/")
                .append(pi)
                .append("/manifest")
                .toString();
    }

    /**
     * <p>
     * getFilterQuery.
     * </p>
     *
     * @should construct query correctly
     * @return a {@link java.lang.String} object.
     */
    public String getFilterQuery() {
        if (items.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("+(");
        for (Bookmark item : items) {
            sb.append(' ').append(SolrConstants.PI).append(':').append(item.getPi());
        }
        sb.append(')');

        return sb.toString();
    }

    /**
     * <p>
     * getIIIFCollectionURI.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFCollectionURI() {
        if (StringUtils.isBlank(getShareKey())) {
            return null;
        }

        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path(ApiUrls.USERS_BOOKMARKS, ApiUrls.USERS_BOOKMARKS_LIST_SHARED_IIIF).params(getShareKey()).build())
                .orElse(DataManager.getInstance().getConfiguration().getRestApiUrl() + "bookmarks/key/" + getShareKey() + "/");
    }

    /**
     *
     * @return the URL encoded name
     */
    public String getEscapedName() {
        if (name != null) {
            return StringEscapeUtils.escapeHtml4(name);
        }

        return "";
    }

    public boolean isOwnedBy(User user) {
        return user != null && user.equals(this.owner);
    }

    public long numItemsWithoutImages() {
        return this.getItems().stream().filter(bm -> !bm.isHasImages()).count();
    }

    /**
     *
     * @param bookmarkLists
     * @should sort lists correctly
     */
    public static void sortBookmarkLists(List<BookmarkList> bookmarkLists) {
        if (bookmarkLists == null || bookmarkLists.isEmpty()) {
            return;
        }

        // If a list has no dateUpdated value, add the latest item date
        boolean sort = false;
        for (BookmarkList list : bookmarkLists) {
            if (list.getDateUpdated() != null || list.getItems() == null || list.getItems().isEmpty()) {
                continue;
            }
            LocalDateTime latest = null;
            for (Bookmark bookmark : list.getItems()) {
                if (bookmark.getDateAdded() == null) {
                    continue;
                }
                if (latest == null || bookmark.getDateAdded().isAfter(latest)) {
                    latest = bookmark.getDateAdded();
                }
            }
            if (latest != null) {
                list.setDateUpdated(latest);
                sort = true;
            }
        }
        if (sort) {
            Collections.sort(bookmarkLists);
        }
    }
}
