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
