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
package io.goobi.viewer.model.annotation.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class AnnotationSolrSaver implements AnnotationSaver {

    private final static Logger logger = LoggerFactory.getLogger(AnnotationSolrSaver.class);
    
    private final AnnotationConverter converter;

    public AnnotationSolrSaver() {
        this.converter = new AnnotationConverter();
    }
    
    @Override
    public void save(WebAnnotation... annotations) throws IOException {
        
        Collection<PersistentAnnotation> pas = Arrays.stream(annotations).map(converter::getAsPersistentAnnotation).collect(Collectors.toMap(anno -> anno.get, null));
        
        for (PersistentAnnotation pa : pas) {            
            if(pa.getTargetPageOrder() != null) {
                try {
                    IndexerTools.reIndexPage(pa.getTargetPI(), pa.getTargetPageOrder(), Arrays.asList(null));
                } catch (DAOException | PresentationException | IndexUnreachableException | IOException e) {
                    logger.warn("Error reindexing single page. Try reindexing entire record");
                    IndexerTools.triggerReIndexRecord(pa.getTargetPI());
                }
            } else {            
                IndexerTools.triggerReIndexRecord(pa.getTargetPI());
            }
        }
        
    }

}
