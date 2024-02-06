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
package io.goobi.viewer.model.cms.media;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;

public class CMSMediaLister {

    private final IDAO dao;

    public CMSMediaLister(IDAO dao) {
        this.dao = dao;
    }

    public List<CMSMediaItem> getMediaItems(List<String> tags, Integer maxItems, Integer prioritySlots, boolean random) throws DAOException {
        List<CMSMediaItem> allItems = dao.getAllCMSMediaItems();
        List<String> cleanedTags = Optional.ofNullable(tags)
                .orElse(Collections.emptyList())
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        return allItems
                .stream()
                .filter(item -> cleanedTags.isEmpty()
                        || item.getCategories().stream().map(CMSCategory::getName).map(String::toLowerCase).anyMatch(cleanedTags::contains))
                .sorted(new PriorityComparator(prioritySlots, Boolean.TRUE.equals(random)))
                .limit(maxItems != null ? maxItems : Integer.MAX_VALUE)
                .sorted(new PriorityComparator(0, Boolean.TRUE.equals(random)))
                .collect(Collectors.toList());
    }

    public MediaList getMediaItems(List<String> tags, Integer maxItems, Integer prioritySlots, boolean random, HttpServletRequest request)
            throws DAOException {
        return new MediaList(getMediaItems(tags, maxItems, prioritySlots, random), request);
    }
}
