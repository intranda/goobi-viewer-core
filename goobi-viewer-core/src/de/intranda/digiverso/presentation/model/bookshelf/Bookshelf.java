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
package de.intranda.digiverso.presentation.model.bookshelf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;

@Entity
@Table(name = "bookshelves")
@XStreamAlias("bookshelf")
public class Bookshelf implements Serializable {

    private static final long serialVersionUID = -3040539541804852903L;

    private static final Logger logger = LoggerFactory.getLogger(Bookshelf.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @OneToMany(mappedBy = "bookshelf", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @PrivateOwned
    private List<BookshelfItem> items = new ArrayList<>();

    /** UserGroups that may access this bookshelf. */
    // TODO
    @Transient
    @JsonIgnore
    private List<UserGroup> groupShares = new ArrayList<>();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
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
        Bookshelf other = (Bookshelf) obj;
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
     * add bookshelf to list and save
     *
     * @param item
     * @return boolean if list changed
     */
    public boolean addItem(BookshelfItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }

        if (item != null && !items.contains(item) && items.add(item)) {
            item.setBookshelf(this);
            return true;
        }
        return false;
    }

    /**
     * remove bookshelf from list and save
     *
     * @param item
     * @return boolean if list changed
     */
    public boolean removeItem(BookshelfItem item) {
        if (items != null) {
            return items.remove(item);
        }
        return false;
    }

    /**
     * add user group to list and save
     *
     * @param group
     * @return boolean if list changed
     */
    public boolean addGroupShare(UserGroup group) {
        return group != null && !groupShares.contains(group) && groupShares.add(group);
    }

    /**
     * remove user group from list and save
     *
     * @param group
     * @return boolean if list changed
     */
    public boolean removeGroupShare(UserGroup group) {
        return groupShares != null && groupShares.remove(group);
    }

    /**
     * Returns a Solr query that would retrieve the Solr documents representing the items listed on this bookshelf.
     *
     * @return
     * @should return correct query
     */
    public String generateSolrQueryForItems() {
        StringBuilder sb = new StringBuilder();

        for (BookshelfItem item : items) {
            if (StringUtils.isNotEmpty(item.getPi())) {
                if (StringUtils.isNotEmpty(item.getLogId())) {
                    // with LOGID
                    sb.append('(')
                            .append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(item.getPi())
                            .append(" AND ")
                            .append(SolrConstants.LOGID)
                            .append(':')
                            .append(item.getLogId())
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
                        .append(" OR ")
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

    /*********************************** Getter and Setter ***************************************/

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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
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
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasDescription() {
        return StringUtils.isNotBlank(getDescription());
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
     * @return the isPublic
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * @return the isPublic Value as a String <br>
     *         surrounded with ()
     */
    @JsonIgnore
    public String getPublicString() {
        String publicString = "";

        if (this.isPublic) {
            publicString = "(" + Helper.getTranslation("public", null) + ")";
        }

        return publicString;
    }

    /**
     * @param isPublic the isPublic to set
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * @return the items
     */
    public List<BookshelfItem> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(List<BookshelfItem> items) {
        this.items = items;
    }

    /**
     * @return the groupShares
     */
    public List<UserGroup> getGroupShares() {
        if (groupShares == null) {
            groupShares = new ArrayList<>();
        }
        return groupShares;
    }

    /**
     * @param groupShares the groupShares to set
     */
    public void setGroupShares(List<UserGroup> groupShares) {
        this.groupShares = groupShares;
    }

    public String getOwnerName() {
        if (getOwner() != null) {
            return getOwner().getDisplayNameObfuscated();
        }
        return null;
    }

    /**
     * 
     * @param applicationRoot
     * @return
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should generate JSON object correctly
     */
    @SuppressWarnings("unchecked")
    public String getMiradorJsonObject(String applicationRoot) throws ViewerConfigurationException, IndexUnreachableException, PresentationException {
        // int cols = (int) Math.sqrt(items.size());
        int cols = 2;
        int rows = (int) Math.ceil(items.size() / (float) cols);

        JSONObject root = new JSONObject();
        root.put("id", "miradorViewer");
        root.put("layout", rows + "x" + cols);
        root.put("buildPath", applicationRoot + "/resources/javascript/libs/mirador/");

        JSONArray dataArray = new JSONArray();
        JSONArray windowObjectsArray = new JSONArray();
        String queryRoot = SolrConstants.DOCTYPE + ":" + DocType.DOCSTRCT + " AND " + SolrConstants.PI_TOPSTRUCT + ":";
        //        int row = 1;
        //        int col = 1;
        for (BookshelfItem bi : items) {
            String manifestUrl = new StringBuilder(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("iiif/manifests/")
                    .append(bi.getPi())
                    .append("/manifest")
                    .toString();
            boolean sidePanel = DataManager.getInstance().getSearchIndex().getHitCount(queryRoot + bi.getPi()) > 1;

            JSONObject dataItem = new JSONObject();
            dataItem.put("manifestUri", manifestUrl);
            dataItem.put("location", "Goobi viewer");
            dataArray.add(dataItem);

            JSONObject windowObjectItem = new JSONObject();
            windowObjectItem.put("loadedManifest", manifestUrl);
            //windowObjectItem.put("slotAddress", "row" + row + ".column" + col);
            windowObjectItem.put("sidePanel", sidePanel);
            windowObjectItem.put("sidePanelVisible", false);
            windowObjectItem.put("bottomPanel", false);
            windowObjectItem.put("viewType", "ImageView");
            windowObjectsArray.add(windowObjectItem);

            //            col++;
            //            if (col > cols) {
            //                col = 1;
            //                row++;
            //            }
        }
        root.put("data", dataArray);
        root.put("windowObjects", windowObjectsArray);

        return root.toJSONString();
    }
}
