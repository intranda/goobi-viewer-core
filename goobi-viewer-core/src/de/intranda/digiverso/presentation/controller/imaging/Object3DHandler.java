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
package de.intranda.digiverso.presentation.controller.imaging;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;

/**
 * Resolves urls audio or video files
 * 
 * @author Florian Alpers
 *
 */
public class Object3DHandler {
    
    private static final String URL_TEMPLATE = "view/object/{identifier}/{filename}/info.json";

    private final String restApiUrl;
    
    public Object3DHandler(Configuration config) throws ViewerConfigurationException {
        this.restApiUrl = config.getRestApiUrl();
    }
    
    /**
     * Returns the url to the media object for the given pi and filename
     * 
     * @param mimeType  The mime type to use
     * @param pi        The pi of the requested work
     * @param filename  The media filename
     * @return  the url to the media file of the given pi and filename
     */
    public String getObjectUrl(String pi, String filename) {
        return this.restApiUrl + URL_TEMPLATE.replace("{identifier}", pi).replace("{filename}", filename);
    }
}
