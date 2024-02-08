package io.goobi.viewer.model.search;

import java.util.Locale;

import io.goobi.viewer.messages.ViewerResourceBundle;

public enum HitType {
    ACCESSDENIED,
    ARCHIVE, // EAD archive
    DOCSTRCT,
    PAGE,
    METADATA, // grouped metadata
    UGC, // user-generated content
    LOCATION,   //metadata type location
    SHAPE,      //metadata type shape
    SUBJECT,    //metadata type subject
    PERSON, // UGC/metadata person
    CORPORATION, // UGC/meadata corporation
    ADDRESS, // UGC address
    COMMENT, // UGC comment
    EVENT, // LIDO event
    GROUP, // convolute/series
    CMS; // CMS page type for search hits

    /**
     * 
     * @param name
     * @return {@link HitType} matching given name; null if none found
     * @should return all known types correctly
     * @should return null if name unknown
     */
    public static HitType getByName(String name) {
        if (name != null) {
            if ("OVERVIEWPAGE".equals(name)) {
                return HitType.CMS;
            }
            for (HitType type : HitType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
        }

        return null;
    }

    public String getLabel(Locale locale) {
        return ViewerResourceBundle.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
    }
}

