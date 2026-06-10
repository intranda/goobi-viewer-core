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
package io.goobi.viewer.api.rest.model.webarchives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response model for replaywebpage json")
public class ReplayJson {

    private final String id;

    private final String name;

    private final List<WebArchiveResource> resources;

    private final List<WebArchivePage> initialPages;

    private String description;
    private String caption;
    private String homUrl;
    private final List<String> tags = new ArrayList<>();

    public ReplayJson(String id, String name, List<WebArchiveResource> resources) {
        this(id, name, resources, Collections.emptyList());
    }

    public ReplayJson(String id, String name, List<WebArchiveResource> resources, List<WebArchivePage> initialPages) {
        super();
        this.id = id;
        this.name = name;
        this.resources = resources;
        this.initialPages = initialPages;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<WebArchiveResource> getResources() {
        return resources;
    }

    public List<WebArchivePage> getInitialPages() {
        return initialPages;
    }

    public String getDownloadUrl() {
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getHomUrl() {
        return homUrl;
    }

    public void setHomUrl(String homUrl) {
        this.homUrl = homUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);

    }

}
