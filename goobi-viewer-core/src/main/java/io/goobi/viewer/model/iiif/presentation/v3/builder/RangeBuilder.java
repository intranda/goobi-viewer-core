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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Range3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author florian
 *
 */
public class RangeBuilder extends AbstractBuilder {

    private static final String BASE_RANGE_ID = "base";
    private static final Logger logger = LogManager.getLogger(RangeBuilder.class);

    /**
     * @param apiUrlManager
     */
    public RangeBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    public Range3 build(String pi, String logId) throws PresentationException, IndexUnreachableException, ContentNotFoundException {
        List<StructElement> documents = this.dataRetriever.getDocumentWithChildren(pi);

        StructElement mainDocument = documents.get(0);
        List<StructElement> childDocuments = documents.subList(1, documents.size());

        return build(mainDocument, childDocuments, logId);
    }

    public Range3 build(StructElement topElement, List<StructElement> structures, String logId) throws ContentNotFoundException {

        structures.sort((s1, s2) -> Integer.compare(getFirstPageNo(s1), getFirstPageNo(s2)));
        StructElement structElement = (logId == null || BASE_RANGE_ID.equals(logId)) ? topElement
                : structures.stream()
                        .filter(s -> s.getLogid().equals(logId))
                        .findAny()
                        .orElseThrow(() -> new ContentNotFoundException("Range not found"));
        String rangeId = Optional.ofNullable(structElement.getLogid()).orElse(BASE_RANGE_ID);
        URI id = urls.path(ApiUrls.RECORDS_SECTIONS, ApiUrls.RECORDS_SECTIONS_RANGE).params(topElement.getPi(), rangeId).buildURI();
        Range3 range = new Range3(id);
        range.setLabel(structElement.getMultiLanguageDisplayLabel());

        addMetadata(range, structElement);

        List<StructElement> children = getChildStructs(structures, structElement);

        if (structElement.getImageNumber() > 0 && structElement.getNumPages() > 0) {

            int firstPageNo = getFirstPageNo(structElement);
            int lastPageNo = getLastPageNumber(structElement, children);

            //Add a start canvas to always have an image for the range
            //If the first subrange starts at the same page as the range itself, it gets no canvas items on its own (they are all in the sub ranges)
            URI startCanvasId = urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_CANVAS).params(topElement.getPi(), firstPageNo).buildURI();
            range.setStart(new Canvas3(startCanvasId));

            for (int pageNo = firstPageNo; pageNo <= lastPageNo; pageNo++) {
                URI canvasId = urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_CANVAS).params(topElement.getPi(), pageNo).buildURI();
                Canvas3 canvas = new Canvas3(canvasId);
                range.addItem(canvas);
            }
        }

        for (StructElement child : children) {
            Range3 childRange = build(topElement, structures, child.getLogid());
            range.addItem(childRange);
        }

        return range;
    }

    private static int getFirstPageNo(StructElement structElement) {
        return structElement.getImageNumber();
    }

    private static int getLastPageNumber(StructElement structElement, List<StructElement> children) {
        int firstPageNo = getFirstPageNo(structElement);
        int lastPageNo = firstPageNo + structElement.getNumPages() - 1;
        if (!children.isEmpty()) {
            lastPageNo = children.get(0).getImageNumber() - 1;
        }
        return lastPageNo;
    }

    private static void getNextSibling(List<StructElement> structures, StructElement structElement) {
        StructElement nextSibling = null;
        if (structures.indexOf(structElement) < structures.size() - 1) {
            nextSibling = structures.get(structures.indexOf(structElement) + 1);
        }
    }

    private static List<StructElement> getChildStructs(List<StructElement> structures, StructElement structElement) {
        return structures.stream()
                .filter(s -> structElement.getLuceneId() == s.getParentLuceneId())
                .collect(Collectors.toList());
    }
}
