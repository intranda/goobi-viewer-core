/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.ServletContext;

/**
 * @author florian
 */
public class FileResourceManager {

    private final Path coreResourcesPath;
    private final Path themResourcesPath;
    private final ServletContext context;
    private final String theme;

    /**
     *
     * @param servletContex The servletContext of the application
     * @param theme A file system path containing the root path of a theme repository. May be null to use the internal resource path for theme
     *            Resources
     */
    public FileResourceManager(ServletContext servletContex, String theme) {
        this.coreResourcesPath = Paths.get(servletContex.getRealPath("resources"));
        this.themResourcesPath = Paths.get(servletContex.getRealPath("resources/themes/" + theme));
        this.context = servletContex;
        this.theme = theme;
    }

    public FileResourceManager(String theme) {
        this.coreResourcesPath = Path.of("resources");
        this.themResourcesPath = Path.of("resources/themes/" + theme);
        this.context = null;
        this.theme = theme;
    }

    public Path getCoreResourcePath(String resource) {
        if (this.context != null) {
            String path = context.getRealPath("resources/" + resource);
            if (StringUtils.isNotBlank(path)) {
                return Paths.get(path);
            }
        }
        return Path.of("");
    }

    public Path getThemeResourcePath(String resource) {
        return this.themResourcesPath.resolve(resource);
    }

    public URI getCoreResourceURI(String resource) {
        return URI.create("/resources/" + resource);
    }

    public URI getThemeResourceURI(String resource) {
        return URI.create("/resources/themes/" + this.theme + "/" + resource);
    }
}
