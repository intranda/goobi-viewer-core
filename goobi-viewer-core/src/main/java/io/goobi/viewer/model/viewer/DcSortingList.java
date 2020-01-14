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
package io.goobi.viewer.model.viewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of collection names and an additional String "sortAfter" denoting the collection after which the listed collections are to be
 * inserted. If "sortAfter" is null, or doesn't denote an existing collection, the collections are to be inserted before other collections.
 */
public class DcSortingList {

    //    private static final Logger logger = LoggerFactory.getLogger(DcSortingList.class);

    private final String sortAfter;
    private final List<String> collections;

    /**
     * <p>Constructor for DcSortingList.</p>
     *
     * @param sortAfter the collection after which to insert the collectionlist
     * @param collectionList the sorted list of collections to insert
     */
    public DcSortingList(String sortAfter, List<String> collectionList) {
        super();
        this.sortAfter = sortAfter;
        this.collections = collectionList;
    }

    /**
     * <p>Constructor for DcSortingList.</p>
     *
     * @param sortAfter the collection after which to insert the collectionlist
     */
    public DcSortingList(String sortAfter) {
        super();
        this.sortAfter = sortAfter;
        this.collections = new ArrayList<>();
    }

    /**
     * <p>Constructor for DcSortingList.</p>
     *
     * @param collectionList the sorted list of collections to insert
     */
    public DcSortingList(List<String> collectionList) {
        super();
        this.sortAfter = null;
        this.collections = collectionList;
    }

    /**
     * <p>Constructor for DcSortingList.</p>
     */
    public DcSortingList() {
        super();
        this.sortAfter = null;
        this.collections = new ArrayList<>();
    }

    /**
     * <p>Getter for the field <code>sortAfter</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSortAfter() {
        return sortAfter;
    }

    /**
     * <p>Getter for the field <code>collections</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getCollections() {
        return collections;
    }

    /**
     * <p>addCollection.</p>
     *
     * @param collection a {@link java.lang.String} object.
     */
    public void addCollection(String collection) {
        collections.add(collection);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(this.getClass().getName());
        if (getSortAfter() != null) {
            stringBuilder.append("\nSort after = ").append(getSortAfter());
        }
        if (getCollections() != null) {
            stringBuilder.append("\nCollections: ").append(getCollections().toString());
        }
        stringBuilder.append('\n');
        return stringBuilder.toString();
    }
}
