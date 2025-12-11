package io.goobi.viewer.model.security;

import java.util.Locale;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;

/**
 * Classes implementing this interface must provide a method returning the appropriate access denied replacement thumnnail URL.
 */
public interface IAccessDeniedThumbnailOutput {

    /**
     * 
     * @param locale
     * @return Configured image URI for the given language; null if none found
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getAccessDeniedThumbnailUrl(Locale locale) throws IndexUnreachableException, DAOException;
}
