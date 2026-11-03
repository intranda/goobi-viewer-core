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
import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.model.metadata.ComplexMetadata;
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

    /**
     * @see PodloveEpisodeResourceBuilder#getTranscriptFileName(SolrDocument)
     * @verifies return file name without path
     */
    @Test
    void getTranscriptFileName_shouldReturnFileNameWithoutPath() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.FILENAME_JSON, "PPN123/episode.json");
        Assertions.assertEquals("episode.json", PodloveEpisodeResourceBuilder.getTranscriptFileName(doc));

        doc.setField(SolrConstants.FILENAME_JSON, "episode.json");
        Assertions.assertEquals("episode.json", PodloveEpisodeResourceBuilder.getTranscriptFileName(doc));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getTranscriptFileName(SolrDocument)
     * @verifies return null if page has no transcript
     */
    @Test
    void getTranscriptFileName_shouldReturnNullIfPageHasNoTranscript() {
        Assertions.assertNull(PodloveEpisodeResourceBuilder.getTranscriptFileName(null));
        SolrDocument doc = new SolrDocument();
        Assertions.assertNull(PodloveEpisodeResourceBuilder.getTranscriptFileName(doc));
        doc.setField(SolrConstants.FILENAME_JSON, " ");
        Assertions.assertNull(PodloveEpisodeResourceBuilder.getTranscriptFileName(doc));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getTranscriptFileName(SolrDocument)
     * @verifies return null if file is not a json file
     */
    @Test
    void getTranscriptFileName_shouldReturnNullIfFileIsNotAJsonFile() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.FILENAME_JSON, "episode.vtt");
        Assertions.assertNull(PodloveEpisodeResourceBuilder.getTranscriptFileName(doc));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#buildTranscriptUrl(io.goobi.viewer.api.rest.AbstractApiUrlManager, String, String)
     * @verifies build transcript endpoint url
     */
    @Test
    void buildTranscriptUrl_shouldBuildTranscriptEndpointUrl() {
        ApiUrls urls = new ApiUrls("https://viewer.goobi.io/api/v1");
        Assertions.assertEquals("https://viewer.goobi.io/api/v1/records/PPN123/files/transcript/episode.json",
                PodloveEpisodeResourceBuilder.buildTranscriptUrl(urls, "PPN123", "episode.json"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#buildContributor(ComplexMetadata, String, String, Locale)
     * @verifies build contributor with group
     */
    @Test
    void buildContributor_shouldBuildContributorWithGroup() {
        SolrDocument doc = personDoc("MD_AUTHOR");
        doc.setField(SolrConstants.IDDOC, "123");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_LAST_NAME, "Lohmann");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_FIRST_NAME, "Ludwig");

        JSONObject contributor =
                PodloveEpisodeResourceBuilder.buildContributor(ComplexMetadata.getFromSolrDoc(doc), "MD_AUTHOR", "Autor", Locale.GERMAN);

        Assertions.assertNotNull(contributor);
        Assertions.assertEquals("123", contributor.getString("id"));
        Assertions.assertEquals("Ludwig Lohmann", contributor.getString("name"));
        Assertions.assertFalse(contributor.has("avatar"));
        JSONObject group = contributor.getJSONObject("group");
        Assertions.assertEquals("MD_AUTHOR", group.getString("id"));
        Assertions.assertEquals("md_author", group.getString("slug"));
        Assertions.assertEquals("Autor", group.getString("title"));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#buildContributor(ComplexMetadata, String, String, Locale)
     * @verifies return null if name is missing
     */
    @Test
    void buildContributor_shouldReturnNullIfNameIsMissing() {
        Assertions.assertNull(
                PodloveEpisodeResourceBuilder.buildContributor(ComplexMetadata.getFromSolrDoc(personDoc("MD_AUTHOR")), "MD_AUTHOR", "Autor",
                        Locale.GERMAN));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getContributorName(ComplexMetadata, Locale)
     * @verifies prefer display form
     */
    @Test
    void getContributorName_shouldPreferDisplayForm() {
        SolrDocument doc = personDoc("MD_AUTHOR");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_DISPLAY_FORM, "Maria-Christina Piwowarski");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_LAST_NAME, "Piwowarski");
        doc.setField(SolrConstants.MD_VALUE, "Piwowarski, Maria-Christina");
        Assertions.assertEquals("Maria-Christina Piwowarski",
                PodloveEpisodeResourceBuilder.getContributorName(ComplexMetadata.getFromSolrDoc(doc), Locale.GERMAN));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getContributorName(ComplexMetadata, Locale)
     * @verifies combine first and last name
     */
    @Test
    void getContributorName_shouldCombineFirstAndLastName() {
        SolrDocument doc = personDoc("MD_AUTHOR");
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_LAST_NAME, "blauschwarzberlin");
        doc.setField(SolrConstants.MD_VALUE, "something else");
        Assertions.assertEquals("blauschwarzberlin",
                PodloveEpisodeResourceBuilder.getContributorName(ComplexMetadata.getFromSolrDoc(doc), Locale.GERMAN));
    }

    /**
     * @see PodloveEpisodeResourceBuilder#getContributorName(ComplexMetadata, Locale)
     * @verifies fall back to metadata value
     */
    @Test
    void getContributorName_shouldFallBackToMetadataValue() {
        SolrDocument doc = personDoc("MD_PUBLISHER");
        doc.setField(SolrConstants.MD_VALUE, "Staatsbibliothek zu Berlin");
        Assertions.assertEquals("Staatsbibliothek zu Berlin",
                PodloveEpisodeResourceBuilder.getContributorName(ComplexMetadata.getFromSolrDoc(doc), Locale.GERMAN));
    }

    private static SolrDocument personDoc(String field) {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCTYPE, "METADATA");
        doc.setField(SolrConstants.LABEL, field);
        doc.setField(SolrConstants.METADATATYPE, "PERSON");
        return doc;
    }

    private static SolrDocument chapterDoc(String title, String start) {
        SolrDocument doc = new SolrDocument();
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_TITLE, title);
        doc.setField(PodloveEpisodeResourceBuilder.FIELD_CHAPTER_START, start);
        return doc;
    }
}
