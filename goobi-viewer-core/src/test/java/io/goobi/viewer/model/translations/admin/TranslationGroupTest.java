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
package io.goobi.viewer.model.translations.admin;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;

public class TranslationGroupTest extends AbstractSolrEnabledTest {

    /**
     * @see TranslationGroup#getEntryCount()
     * @verifies return correct count
     */
    @Test
    public void getEntryCount_shouldReturnCorrectCount() throws Exception {
        TranslationGroup group = TranslationGroup.create(0, TranslationGroupType.SOLR_FIELD_NAMES, "group", null, 2);
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "DC", false));
        group.getItems().add(TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "PI", false));
        Assert.assertEquals(2, group.getEntryCount());
    }
}