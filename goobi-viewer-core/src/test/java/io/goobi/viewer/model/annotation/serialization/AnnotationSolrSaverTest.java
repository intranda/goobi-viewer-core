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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.serialization.AnnotationSolrSaver.Target;

/**
 * @author florian
 *
 */
public class AnnotationSolrSaverTest {

    private AnnotationConverter converter = new AnnotationConverter();
    
    @Test
    public void testSaveAnnotationsShouldCorrectlyCallReindexTarget() throws IOException {
        String pi1 = "PI1";
        String pi2 = "PI2";
        Integer noPage = null;
        Integer page1 = 5;
        Integer page2 = 11;
        
        PersistentAnnotation anno1 = new PersistentAnnotation(new WebAnnotation(), 1l, pi1, page1);
        PersistentAnnotation anno2 = new PersistentAnnotation(new WebAnnotation(), 2l, pi1, page1);
        PersistentAnnotation anno3 = new PersistentAnnotation(new WebAnnotation(), 3l, pi1, page2);
        PersistentAnnotation anno4 = new PersistentAnnotation(new WebAnnotation(), 4l, pi2, page2);
        
        AnnotationSolrSaver saver = Mockito.spy(AnnotationSolrSaver.class);
        saver.save(anno1, anno2);
        ArgumentCaptor<Target> targetArgument = ArgumentCaptor.forClass(Target.class);
        ArgumentCaptor<AnnotationIndexAugmenter> augmenterArgument = ArgumentCaptor.forClass(AnnotationIndexAugmenter.class);
        Mockito.verify(saver, Mockito.times(1)).reindexTarget(targetArgument.capture(), augmenterArgument.capture());
        assertEquals(new Target(pi1, page1), targetArgument.getValue());
        assertEquals(new AnnotationIndexAugmenter(Arrays.asList(anno1, anno2)), augmenterArgument.getValue());
    }

}
