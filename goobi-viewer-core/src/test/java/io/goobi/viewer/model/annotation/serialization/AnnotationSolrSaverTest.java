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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.serialization.SolrAnnotationSaver.Target;

/**
 * @author florian
 *
 */
public class AnnotationSolrSaverTest extends AbstractDatabaseAndSolrEnabledTest {


    @Test
    void callRedindexTargetWithCorrectArguments() throws IOException {
        String pi1 = "PI1";
        Integer page1 = 5;

        CrowdsourcingAnnotation anno1 = new CrowdsourcingAnnotation(new WebAnnotation(), 1l, pi1, page1);
        CrowdsourcingAnnotation anno2 = new CrowdsourcingAnnotation(new WebAnnotation(), 2l, pi1, page1);
        SolrAnnotationSaver saver = Mockito.spy(SolrAnnotationSaver.class);
        Mockito.doNothing().when(saver).reindexTarget(Mockito.any());
        saver.save(anno1, anno2);
        ArgumentCaptor<Target> targetArgument = ArgumentCaptor.forClass(Target.class);
        Mockito.verify(saver, Mockito.times(1)).reindexTarget(targetArgument.capture());
        assertEquals(new Target(pi1, page1), targetArgument.getValue());
    }

    @Test
    void callReindexTargetCorrectNumberOfTimes() throws IOException {
        String pi1 = "PI1";
        String pi2 = "PI2";
        Integer noPage = null;
        Integer page1 = 5;
        Integer page2 = 11;

        CrowdsourcingAnnotation anno1 = new CrowdsourcingAnnotation(new WebAnnotation(), 1l, pi1, page1);
        CrowdsourcingAnnotation anno2 = new CrowdsourcingAnnotation(new WebAnnotation(), 2l, pi1, page1);
        CrowdsourcingAnnotation anno3 = new CrowdsourcingAnnotation(new WebAnnotation(), 3l, pi1, page2);
        CrowdsourcingAnnotation anno4 = new CrowdsourcingAnnotation(new WebAnnotation(), 4l, pi2, page2);
        CrowdsourcingAnnotation anno5 = new CrowdsourcingAnnotation(new WebAnnotation(), 4l, pi2, noPage);


        SolrAnnotationSaver saver = Mockito.spy(SolrAnnotationSaver.class);
        Mockito.doNothing().when(saver).reindexTarget(Mockito.any());
        saver.save(anno1, anno2, anno3, anno4, anno5);
        Mockito.verify(saver, Mockito.times(4)).reindexTarget(Mockito.any());
    }

}
