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
     * @return Appropriate thumbnail URL
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getAccessDeniedThumbnailUrl(Locale locale) throws IndexUnreachableException, DAOException;
}
