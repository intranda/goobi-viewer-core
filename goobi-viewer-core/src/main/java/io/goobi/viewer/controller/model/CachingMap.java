package io.goobi.viewer.controller.model;

import java.util.LinkedHashMap;
import java.util.function.Function;

public class CachingMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1882556825980074260L;
    private final long maxSize;

    public CachingMap(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    synchronized public V get(Object key) {
        return super.get(key);
    }

    @Override
    synchronized public V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    synchronized public V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    synchronized public V computeIfAbsent(K arg0, Function<? super K, ? extends V> arg1) {
        return super.computeIfAbsent(arg0, arg1);
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > this.maxSize;
    }
}
