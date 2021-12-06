package io.goobi.viewer.model.cms;

import java.util.Collections;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

public class SimpleMediaHolder implements CMSMediaHolder {

    private CMSMediaItem mediaItem = null;
    private CategorizableTranslatedSelectable<CMSMediaItem> mediaItemWrapper = null;
    private final String filter;
    
    public SimpleMediaHolder() {
        this.filter = "";
    }
    
    public SimpleMediaHolder(String filter) {
        this.filter = filter;
    }
    
    
    public SimpleMediaHolder(CMSMediaItem item) {
        this();
        setMediaItem(item);
    }
    
    public SimpleMediaHolder(CMSMediaItem item, String filter) {
        this(filter);
        setMediaItem(item);
    }

    @Override
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
        if (mediaItem != null) {
            this.mediaItemWrapper = new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
            ;
        } else {
            this.mediaItemWrapper = null;
        }
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override
    public String getMediaFilter() {
        return filter;
    }

    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        return this.mediaItemWrapper;
    }

}
