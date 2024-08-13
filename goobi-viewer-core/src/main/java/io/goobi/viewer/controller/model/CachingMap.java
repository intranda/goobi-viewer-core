package io.goobi.viewer.controller.model;

import java.util.LinkedHashMap;

public class CachingMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1882556825980074260L;
    private final long maxSize;

    public CachingMap(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > this.maxSize;
    }
}
