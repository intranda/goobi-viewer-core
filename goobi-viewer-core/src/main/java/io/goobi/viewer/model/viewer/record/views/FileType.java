package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PhysicalElement;

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
