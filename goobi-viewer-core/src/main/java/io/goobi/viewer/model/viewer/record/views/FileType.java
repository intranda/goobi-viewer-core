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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
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

    public static Collection<FileType> containedFiletypes(PhysicalElement page)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {
        Set<FileType> types = new HashSet<>();

        String mimeType = page.getMimeType();
        BaseMimeType baseMimeType = BaseMimeType.getByName(mimeType);

        if (BaseMimeType.AUDIO.equals(baseMimeType)) {
            types.add(FileType.AUDIO);
        }
        if (BaseMimeType.VIDEO.equals(baseMimeType)) {
            types.add(FileType.VIDEO);
        }
        if (BaseMimeType.IMAGE.equals(baseMimeType)) {
            types.add(FileType.IMAGE);
        }
        if (BaseMimeType.MODEL.equals(baseMimeType)) {
            types.add(FileType.MODEL);
        }
        if ("application/pdf".equals(mimeType)) {
            types.add(FileType.PDF);
        }
        if ("application/epub+zip".equals(mimeType)) {
            types.add(FileType.EPUB);
        }
        if (StringUtils.isNotBlank(page.getAltoFileName())) {
            types.add(FileType.ALTO);
        }
        if (StringUtils.isNotBlank(page.getFulltextFileName())) {
            types.add(FileType.TEXT);
        }
        if (!new TextResourceBuilder().getTEIFiles(page.getPi()).isEmpty()) {
            types.add(FileType.TEI);
        }

        return types;
    }

}
