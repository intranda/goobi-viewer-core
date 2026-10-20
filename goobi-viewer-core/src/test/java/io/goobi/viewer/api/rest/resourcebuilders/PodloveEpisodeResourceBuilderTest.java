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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.solr.SolrConstants;

class PodloveEpisodeResourceBuilderTest {

    /**
     * @see PodloveEpisodeResourceBuilder#formatNormalPlayTime(String)
     * @verifies normalize hours minutes seconds
     */
    @Test
    void formatNormalPlayTime_shouldNormalizeHoursMinutesSeconds() {
        Assertions.assertEquals("01:02:03.000", PodloveEpisodeResourceBuilder.formatNormalPlayTime("1:02:03"));
        Assertions.assertEquals("01:02:03.500", PodloveEpisodeResourceBuilder.formatNormalPlayTime("01:02:03.5"));
        Assertions.assertEquals("00:00:00.000", PodloveEpisodeResourceBuilder.formatNormalPlayTime("00:00:00.000"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#formatNormalPlayTime(String)
     * @verifies normalize minutes seconds
     */
    @Test
    void formatNormalPlayTime_shouldNormalizeMinutesSeconds() {
        Assertions.assertEquals("00:12:34.000", PodloveEpisodeResourceBuilder.formatNormalPlayTime("12:34"));
        Assertions.assertEquals("01:15:00.000", PodloveEpisodeResourceBuilder.formatNormalPlayTime("75:00"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#formatNormalPlayTime(String)
     * @verifies convert plain seconds
     */
    @Test
    void formatNormalPlayTime_shouldConvertPlainSeconds() {
        Assertions.assertEquals("00:02:05.250", PodloveEpisodeResourceBuilder.formatNormalPlayTime("125.25"));
        Assertions.assertEquals("01:00:00.000", PodloveEpisodeResourceBuilder.formatNormalPlayTime("3600"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#formatNormalPlayTime(String)
     * @verifies return null for invalid input
     */
    @Test
    void formatNormalPlayTime_shouldReturnNullForInvalidInput() {
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime(null));
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime(""));
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime("abc"));
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime("1:2:3:4"));
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime("00:61:00"));
        Assertions.assertNull(PodloveEpisodeResourceBuilder.formatNormalPlayTime("-5"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#buildChapters(List)
     * @verifies sort chapters by start time and skip invalid ones
     */
    @Test
    void buildChapters_shouldSortChaptersByStartTimeAndSkipInvalidOnes() {
        SolrDocument second = chapterDoc("Second", "10:00");
        SolrDocument first = chapterDoc("First", "0");
        SolrDocument invalid = chapterDoc("Invalid", "soon");
        SolrDocument third = chapterDoc("Third", "01:00:00");

        JSONArray chapters = PodloveEpisodeResourceBuilder.buildChapters(Arrays.asList(second, invalid, third, first));

        Assertions.assertEquals(3, chapters.length());
        Assertions.assertEquals("First", chapters.getJSONObject(0).getString("title"));
        Assertions.assertEquals("00:00:00.000", chapters.getJSONObject(0).getString("start"));
        Assertions.assertEquals("Second", chapters.getJSONObject(1).getString("title"));
        Assertions.assertEquals("00:10:00.000", chapters.getJSONObject(1).getString("start"));
        Assertions.assertEquals("Third", chapters.getJSONObject(2).getString("title"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#buildChapters(List)
     * @verifies fall back to label if title is missing
     */
    @Test
    void buildChapters_shouldFallBackToLabelIfTitleIsMissing() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.LABEL, "Label only");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_CHAPTER_START, "00:05:00");

        JSONArray chapters = PodloveEpisodeResourceBuilder.buildChapters(List.of(doc));

        Assertions.assertEquals(1, chapters.length());
        Assertions.assertEquals("Label only", chapters.getJSONObject(0).getString("title"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getAudioMimeType(String)
     * @verifies map formats to mime types
     */
    @Test
    void getAudioMimeType_shouldMapFormatsToMimeTypes() {
        Assertions.assertEquals("audio/mpeg", PodloveEpisodeResourceBuilder.getAudioMimeType("mp3"));
        Assertions.assertEquals("audio/mpeg", PodloveEpisodeResourceBuilder.getAudioMimeType("mpeg"));
        Assertions.assertEquals("audio/ogg", PodloveEpisodeResourceBuilder.getAudioMimeType("ogg"));
    }

    private static SolrDocument chapterDoc(String title, String start) {
        SolrDocument doc = new SolrDocument();
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_TITLE, title);
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_CHAPTER_START, start);
        return doc;
    }
}
