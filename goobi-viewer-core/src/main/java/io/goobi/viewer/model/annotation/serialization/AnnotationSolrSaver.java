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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.modules.interfaces.IndexAugmenter;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

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
    public void save(PersistentAnnotation... annotations) throws IOException {
        
        Map<Target, Iterable<PersistentAnnotation>> pas = Arrays.stream(annotations)
                .collect(Collectors.toMap(anno -> new Target(anno.getTargetPI(), anno.getTargetPageOrder()), anno -> new ArrayList<>(Arrays.asList(anno)), (a1,a2) -> CollectionUtils.union(a1, a2)));
        
        for (Target target : pas.keySet()) {
            IndexAugmenter augmenter = new AnnotationIndexAugmenter(IterableUtils.toList(pas.get(target)));
            reindexTarget(target, augmenter);
        }
        
    }

    protected void reindexTarget(Target target, IndexAugmenter augmenter) {
        if(target.page != null) {
            try {
                IndexerTools.reIndexPage(target.pi, target.page, Arrays.asList(augmenter));
            } catch (DAOException | PresentationException | IndexUnreachableException | IOException e) {
                logger.warn("Error reindexing single page. Try reindexing entire record");
                IndexerTools.triggerReIndexRecord(target.pi, Arrays.asList(augmenter));
            }
        } else {            
            IndexerTools.triggerReIndexRecord(target.pi, Arrays.asList(augmenter));
        }
    }
    
    static class Target {
        final String pi;
        final Integer page;
        
        final static Map<String, Target> targetStore = new ConcurrentHashMap<>();
        
        Target(String pi, Integer page) {
            if(StringUtils.isBlank(pi)) {
                throw new IllegalArgumentException("Target pi must not be empty");
            }
            this.pi = pi;
            this.page = page;
        }
        
        static Target getOrCreate(String pi, Integer page) {
            String id = pi + (page != null ? ("$$$" + page) : "");
            targetStore.putIfAbsent(id, new Target(pi, page));
            return targetStore.get(id);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.pi + (this.page != null ? ("$$$" + this.page) : "");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            int hash = Objects.hash(this.pi, this.page);
            return hash;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj.getClass().equals(Target.class)) {
                Target other = (Target)obj;
                return Objects.equals(this.pi, (other.pi)) && Objects.equals(this.page, (other.page));
            } else {
                return false;
            }
        }
    }

}
