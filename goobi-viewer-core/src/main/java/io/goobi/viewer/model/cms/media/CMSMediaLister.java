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
        List<String> cleanedTags = Optional.ofNullable(tags).orElse(Collections.emptyList()).stream().filter(StringUtils::isNotBlank).map(String::toLowerCase).collect(Collectors.toList());
        return allItems
                .stream()
                .filter(item -> cleanedTags.isEmpty() ||
                        item.getCategories().stream().map(CMSCategory::getName).map(String::toLowerCase).anyMatch(cleanedTags::contains))
                .sorted(new PriorityComparator(prioritySlots, Boolean.TRUE.equals(random)))
                .limit(maxItems != null ? maxItems : Integer.MAX_VALUE)
                .sorted(new PriorityComparator(0, Boolean.TRUE.equals(random)))
                .collect(Collectors.toList());
    }
    
    public MediaList getMediaItems(List<String> tags, Integer maxItems, Integer prioritySlots, boolean random, HttpServletRequest request) throws DAOException {
        return new MediaList(getMediaItems(tags, maxItems, prioritySlots, random), request);
    }
}
