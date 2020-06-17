/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest;

import java.util.Map;

/**
 * @author florian
 *
 */
public interface IApiUrlManager {

    /**
     * Get the full url for the given api path. Replace all text in '{...}' with the given pathParams in the given order
     * 
     * @param path
     * @param pathParams
     * @return  The full url to the given api endpoint
     */
    public String getUrl(String path, String...pathParams );
    
    /**
     *  Get the full url for the given api path. Replace all text in '{...}' with the given pathParams in the given order.
     *  Add given query params to url
     * 
     * @param path
     * @param queryParams
     * @param pathParams
     * @return  The full url to the given api endpoint
     */
    public String getUrl(String path, Map<String, String> queryParams, String...pathParams );
    
    /**
     * @return The base url to the api without trailing slashes
     */
    public String getApiUrl();
    
    /**
     * @return  The base url of the viewer application
     */
    public String getApplicationUrl();
}
