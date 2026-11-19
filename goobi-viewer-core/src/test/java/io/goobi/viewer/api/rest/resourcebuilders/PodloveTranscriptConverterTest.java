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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PodloveTranscriptConverterTest {

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies convert cues
     */
    @Test
    void convertVtt_shouldConvertCues() {
        String vtt = "﻿WEBVTT\r\n\r\n00:00:06.620 --> 00:00:09.300\r\nBlau-Schweiz Berlin\r\n\r\n01:02:03.004 --> 01:02:04.500 align:start\r\n"
                + "aus der Stabi.\r\n";

        JSONArray result = PodloveTranscriptConverter.convertVtt(vtt);

        Assertions.assertEquals(2, result.length());
        JSONObject first = result.getJSONObject(0);
        Assertions.assertEquals("00:00:06.620", first.getString("start"));
        Assertions.assertEquals(6620, first.getLong("start_ms"));
        Assertions.assertEquals("00:00:09.300", first.getString("end"));
        Assertions.assertEquals(9300, first.getLong("end_ms"));
        Assertions.assertEquals("Blau-Schweiz Berlin", first.getString("text"));
        Assertions.assertFalse(first.has("speaker"));
        JSONObject second = result.getJSONObject(1);
        Assertions.assertEquals("01:02:03.004", second.getString("start"));
        Assertions.assertEquals(3723004, second.getLong("start_ms"));
        Assertions.assertEquals("aus der Stabi.", second.getString("text"));
    }

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies read speaker from speaker label
     */
    @Test
    void convertVtt_shouldReadSpeakerFromSpeakerLabel() {
        JSONArray result = PodloveTranscriptConverter.convertVtt("WEBVTT\n\n00:00:09.400 --> 00:00:10.040\nSpeaker 1: aus der Stabi.\n");

        JSONObject entry = result.getJSONObject(0);
        Assertions.assertEquals("1", entry.getString("speaker"));
        Assertions.assertEquals("Speaker 1", entry.getString("voice"));
        Assertions.assertEquals("aus der Stabi.", entry.getString("text"));
    }

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies read speaker from voice tag
     */
    @Test
    void convertVtt_shouldReadSpeakerFromVoiceTag() {
        JSONArray result = PodloveTranscriptConverter.convertVtt("WEBVTT\n\n00:01.000 --> 00:02.000\n<v.loud Ludwig Lohmann>Hallo, guten Abend!</v>\n");

        JSONObject entry = result.getJSONObject(0);
        Assertions.assertEquals("00:00:01.000", entry.getString("start"));
        Assertions.assertEquals("Ludwig Lohmann", entry.getString("speaker"));
        Assertions.assertEquals("Ludwig Lohmann", entry.getString("voice"));
        Assertions.assertEquals("Hallo, guten Abend!", entry.getString("text"));
    }

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies skip notes styles and invalid cues
     */
    @Test
    void convertVtt_shouldSkipNotesStylesAndInvalidCues() {
        String vtt = "WEBVTT - Episode 86\nKind: captions\n\nNOTE contains --> in a comment\nsecond line\n\nSTYLE\n::cue { color: red; }\n\n"
                + "cue-1\n00:00:01.000 --> 00:00:02.000\nvalid\n\n00:00:61.000 --> 00:00:62.000\ninvalid time\n\n00:00:03.000 --> 00:00:04.000\n\n";

        JSONArray result = PodloveTranscriptConverter.convertVtt(vtt);

        Assertions.assertEquals(1, result.length());
        Assertions.assertEquals("valid", result.getJSONObject(0).getString("text"));
    }

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies join multi line cue text and strip tags
     */
    @Test
    void convertVtt_shouldJoinMultiLineCueTextAndStripTags() {
        JSONArray result =
                PodloveTranscriptConverter.convertVtt("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\n<i>erste</i> Zeile\n<c.yellow>zweite</c> &amp; mehr\n");

        Assertions.assertEquals("erste Zeile zweite & mehr", result.getJSONObject(0).getString("text"));
    }

    /**
     * @see PodloveTranscriptConverter#convertVtt(String)
     * @verifies throw IllegalArgumentException if signature is missing
     */
    @Test
    void convertVtt_shouldThrowIllegalArgumentExceptionIfSignatureIsMissing() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PodloveTranscriptConverter.convertVtt("[{\"start\": 1}]"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PodloveTranscriptConverter.convertVtt(null));
    }

    /**
     * @see PodloveTranscriptConverter#parseTimestamp(String)
     * @verifies parse timestamps with and without hours
     */
    @Test
    void parseTimestamp_shouldParseTimestampsWithAndWithoutHours() {
        Assertions.assertEquals(6620L, PodloveTranscriptConverter.parseTimestamp("00:00:06.620"));
        Assertions.assertEquals(6620L, PodloveTranscriptConverter.parseTimestamp("00:06.620"));
        Assertions.assertEquals(360_000_000L, PodloveTranscriptConverter.parseTimestamp("100:00:00.000"));
    }

    /**
     * @see PodloveTranscriptConverter#parseTimestamp(String)
     * @verifies return null for invalid timestamps
     */
    @Test
    void parseTimestamp_shouldReturnNullForInvalidTimestamps() {
        Assertions.assertNull(PodloveTranscriptConverter.parseTimestamp(null));
        Assertions.assertNull(PodloveTranscriptConverter.parseTimestamp("00:00:06,620"));
        Assertions.assertNull(PodloveTranscriptConverter.parseTimestamp("00:60:00.000"));
        Assertions.assertNull(PodloveTranscriptConverter.parseTimestamp("6.62"));
    }
}
