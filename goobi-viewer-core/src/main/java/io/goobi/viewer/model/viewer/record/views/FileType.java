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
package io.goobi.viewer.model.viewer.record.views;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.ViewManager;

/**
 * An enum of possible file types available for a record. Used to check if a record or page contains a specific filetype
 */
public enum FileType {

    IMAGE,
    AUDIO,
    VIDEO,
    MODEL,
    ALTO,
    TEXT,
    TEI,
    PDF,
    EPUB;

    private static final Logger logger = LogManager.getLogger(FileType.class);
    private static final FileNameMap filenameMap = URLConnection.getFileNameMap();

    public static Collection<FileType> containedFiletypes(ViewManager viewManager) throws IndexUnreachableException, PresentationException {
        Set<FileType> types = new HashSet<>();

        Map<String, List<String>> filenames = viewManager.getFilenamesByMimeType();

        List<BaseMimeType> baseTypes = filenames.keySet().stream().map(BaseMimeType::getByName).collect(Collectors.toList());

        if (baseTypes.contains(BaseMimeType.AUDIO)) {
            types.add(FileType.AUDIO);
        }
        if (baseTypes.contains(BaseMimeType.VIDEO)) {
            types.add(FileType.VIDEO);
        }
        if (baseTypes.contains(BaseMimeType.IMAGE)) {
            types.add(FileType.IMAGE);
        }
        if (baseTypes.contains(BaseMimeType.MODEL)) {
            types.add(FileType.MODEL);
        }
        if (filenames.keySet().contains("application/pdf")) {
            types.add(FileType.PDF);
        }
        if (filenames.keySet().contains("application/epub+zip")) {
            types.add(FileType.EPUB);
        }
        if (viewManager.getTopStructElement().isHasTei()) {
            types.add(FileType.TEI);
        }

        try {
            if (viewManager.getPageCountWithAlto() > 1) {
                types.add(FileType.ALTO);
            }
            if (viewManager.getPageCountWithFulltext() > 1) {
                types.add(FileType.TEXT);
            }
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Error counting text files for {}", viewManager.getTopStructElement().getPi(), e);
        }

        return types;
    }

    public static Map<FileType, String> sortByFileType(Collection<String> filenames) {
        Map<FileType, String> types = new HashMap<>();

        for (String filename : filenames) {
            String mimeType = getContentTypeFor(filename);
            FileType type = FileType.fromMimeType(Optional.ofNullable(mimeType).orElse(""));
            if (type != null) {
                types.put(type, filename);
            }
        }
        return types;
    }

    public static String getContentTypeFor(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        String suffix = FileNameUtils.getExtension(Path.of(filename));
        if (StringUtils.isBlank(suffix)) {
            return "";
        }
        if (suffix.matches("(?i)obj|gltf|glb|ply|stl|fbx")) {
            return "model/" + suffix.toLowerCase();
        }
        return filenameMap.getContentTypeFor(filename);
    }

    public static Collection<FileType> containedFiletypes(PhysicalElement page)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {

        Set<FileType> types = new HashSet<>();
        Map<String, String> filenames = page.getFileNames();
        filenames.put(page.getMimeType(), page.getFileName());
        for (Entry<String, String> entry : filenames.entrySet()) {
            String fileType = entry.getKey();
            String filename = entry.getValue();
            String mimeType = getContentTypeFor(filename);
            if ("application/pdf".equals(mimeType)) {
                types.add(FileType.PDF);
            } else if ("application/epub+zip".equals(mimeType)) {
                types.add(FileType.EPUB);
            } else if ("text/xml".equals(mimeType) || "application/xml".equals(mimeType)) {
                if ("alto".equalsIgnoreCase(fileType)) {
                    types.add(FileType.ALTO);
                } else {
                    types.add(FileType.TEI);
                }
            } else if ("text/plain".equals(mimeType)) {
                types.add(FileType.TEXT);
            } else if (fileType.startsWith("object") || fileType.startsWith("model")) {
                types.add(FileType.MODEL);
            } else if ("jpeg".equalsIgnoreCase(fileType)) {
                //pages with external urls also get a "jpeg" filename, even though there is not actual jpeg file
                //to ignore these, only add jpeg file if FILENAME_JPEG == FILENAME in the PAGE document in solr
                boolean actualFile = filename.equals(page.getFileName());
                if (actualFile) {
                    types.add(IMAGE);
                }
            } else {
                FileType type = FileType.fromMimeType(mimeType);
                if (type != null) {
                    types.add(type);
                }
            }
        }

        return types;
    }

    public static FileType fromMimeType(String mimeType) {
        BaseMimeType baseType = BaseMimeType.getByName(mimeType);
        try {
            return FileType.valueOf(baseType.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
