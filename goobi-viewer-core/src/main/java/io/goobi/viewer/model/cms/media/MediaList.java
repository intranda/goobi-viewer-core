package io.goobi.viewer.model.cms.media;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

public class MediaList {

    private final List<MediaItem> mediaItems;

    public MediaList(List<CMSMediaItem> items, HttpServletRequest servletRequest) {
        this.mediaItems = items.stream().map(item -> new MediaItem(item, servletRequest)).collect(Collectors.toList());
    }

    /**
     * @return the mediaItems
     */
    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }
}