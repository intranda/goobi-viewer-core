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
package de.intranda.digiverso.presentation.servlets.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Florian Alpers
 *
 */
public class CombinedPath {

    private String hostUrl;
    private Path pagePath;
    private Path parameterPath;
    
    /**
     * 
     */
    public CombinedPath() {
        hostUrl = "";
        pagePath = Paths.get("");
        parameterPath = Paths.get("");
    }
    
    
    
    /**
     * @param hostPath
     * @param pagePath
     * @param parameterPath
     */
    public CombinedPath(String hostUrl, Path pagePath, Path parameterPath) {
        super();
        this.hostUrl = hostUrl;
        this.pagePath = pagePath;
        this.parameterPath = parameterPath;
    }



    /**
     * @return the hostUrl
     */
    public String getHostUrl() {
        return hostUrl;
    }
    /**
     * @param hostUrl the hostUrl to set
     */
    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }
    /**
     * @return the pagePath
     */
    public Path getPagePath() {
        return pagePath;
    }
    /**
     * @param pagePath the pagePath to set
     */
    public void setPagePath(Path pagePath) {
        this.pagePath = pagePath;
    }
    /**
     * @return the parameterPath
     */
    public Path getParameterPath() {
        return parameterPath;
    }
    /**
     * @param parameterPath the parameterPath to set
     */
    public void setParameterPath(Path parameterPath) {
        this.parameterPath = parameterPath;
    }
    
    public Path getCombinedPath() {
        return pagePath.resolve(parameterPath);
    }
    
    public String getCombinedUrl() {
        
        String url = ("/" + getCombinedPath().toString() + "/").replaceAll("\\/+", "/");
        return url;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getCombinedUrl();
    }



    /**
     * @return true if the last url path part does not contain a dot ('.')
     */
    public boolean isPage() {
        return !getCombinedPath().getFileName().toString().contains(".");
    }
}
