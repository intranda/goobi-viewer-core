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
package io.goobi.viewer.model.cms.pages.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import io.goobi.viewer.model.cms.pages.CMSPage;

/**
 * Loads {@link CMSComponent components} to include in a {@link CMSPage}
 * 
 * @author florian
 *
 */
public class CMSPageContentManager implements Serializable {

    private static final long serialVersionUID = 2016712096257871657L;

    private static final Logger logger = LogManager.getLogger(CMSPageContentManager.class);

    private final List<CMSComponent> components = new ArrayList<>();

    public CMSPageContentManager(Path... configFolders) throws IOException {
        for (Path path : configFolders) {
            if (path != null && Files.exists(path)) {
                this.components.addAll(loadComponents(path));
            }
        }
    }

    private static List<CMSComponent> loadComponents(Path folder) throws IOException {
        if (folder == null || !Files.isDirectory(folder)) {
            throw new FileNotFoundException(folder + " doesn't exist or is not a directory");
        }
        CMSComponentReader reader = new CMSComponentReader();
        try (Stream<Path> xmlFiles = Files.list(folder).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xml"))) {
            return xmlFiles.map(file -> {
                try {
                    return reader.read(file);
                } catch (IOException | JDOMException e) {
                    logger.error("Error reading CMSContent from file {}", file, e);
                    return null;
                }
            })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    public List<CMSComponent> getComponents() {
        return components;
    }

    public Optional<CMSComponent> getComponent(String filename) {
        return this.components.stream().filter(c -> c.getTemplateFilename().equals(filename)).findAny();
    }
}
