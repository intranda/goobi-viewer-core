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
package io.goobi.viewer.model.annotation.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class SolrAnnotationSaver implements AnnotationSaver {

    private static final Logger logger = LogManager.getLogger(SolrAnnotationSaver.class);

    @Override
    public void save(PersistentAnnotation... annotations) throws IOException {

        List<Target> targets = Arrays.stream(annotations)
                .map(anno -> new Target(anno.getTargetPI(), anno.getTargetPageOrder()))
                .distinct()
                .toList();

        for (Target target : targets) {
            reindexTarget(target);
        }

    }

    protected void reindexTarget(Target target) {
        if (target.page != null) {
            try {
                IndexerTools.reIndexPage(target.pi, target.page);
            } catch (DAOException | PresentationException | IndexUnreachableException | IOException e) {
                logger.warn("Error reindexing single page. Try reindexing entire record");
                IndexerTools.triggerReIndexRecord(target.pi);
            }
        } else {
            IndexerTools.triggerReIndexRecord(target.pi);
        }
    }

    static class Target {
        private final String pi;
        private final Integer page;

        static final Map<String, Target> TARGET_STORE = new ConcurrentHashMap<>();

        Target(String pi, Integer page) {
            if (StringUtils.isBlank(pi)) {
                throw new IllegalArgumentException("Target pi must not be empty");
            }
            this.pi = pi;
            this.page = page;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.pi + (this.page != null ? (" / " + this.page) : "");
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.pi, this.page);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj.getClass().equals(Target.class)) {
                Target other = (Target) obj;
                return Objects.equals(this.pi, (other.pi)) && Objects.equals(this.page, (other.page));
            }
            return false;
        }
    }

}
