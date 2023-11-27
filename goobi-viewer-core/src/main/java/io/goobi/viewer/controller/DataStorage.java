package io.goobi.viewer.controller;

public interface DataStorage {
    
    public void put(String key, Object value);
    public Object get(String key);

}
