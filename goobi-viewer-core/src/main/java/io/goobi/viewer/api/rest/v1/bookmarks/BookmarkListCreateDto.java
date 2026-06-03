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
package io.goobi.viewer.api.rest.v1.bookmarks;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Transfer object for {@code POST /users/bookmarks}. Intentionally exposes only
 * {@code name} — neither {@code shareKey}, {@code description}, nor any of the
 * JPA-managed fields (id, owner, dateCreated, items) is accepted from the client,
 * preventing mass-assignment of those properties.
 *
 * <p>{@code description} is not part of the create surface because the underlying
 * builder ({@code UserBookmarkResourceBuilder.addBookmarkList(String name)}) does
 * not accept one today. Adding the field here without wiring it through would
 * silently drop client-supplied descriptions — worse UX than not accepting them
 * at all. If a description-on-create use case appears, extend the builder API
 * and re-add the field together.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookmarkListCreateDto {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
