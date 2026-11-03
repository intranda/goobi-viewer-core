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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Converts WebVTT subtitle files into the transcript format of the Podlove Web Player (version 5).
 *
 * <p>
 * Each cue becomes one transcript entry with start and end time (as normal play time and in milliseconds), the text and, if available, the speaker.
 * Speakers are read from WebVTT voice tags ({@code <v Name>text}) or from a leading speaker label such as {@code Speaker 0: text}. The speaker id is
 * written to {@code speaker}, the speaker name to {@code voice}.
 */
public final class PodloveTranscriptConverter {

    private static final Pattern PATTERN_TIMING = Pattern.compile("^(\\S+)\\s+-->\\s+(\\S+)");
    private static final Pattern PATTERN_TIMESTAMP = Pattern.compile("^(?:(\\d+):)?(\\d{2}):(\\d{2})\\.(\\d{3})$");
    private static final Pattern PATTERN_VOICE_TAG = Pattern.compile("<v(?:\\.[^\\s>]*)?\\s+([^>]+)>");
    private static final Pattern PATTERN_SPEAKER_LABEL = Pattern.compile("^((?:Speaker|SPEAKER|Sprecher|SPRECHER)[ _]?(\\w+)):\\s*");
    private static final Pattern PATTERN_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern PATTERN_BLOCK_SEPARATOR = Pattern.compile("\\n\\s*\\n");

    private PodloveTranscriptConverter() {
        // static utility
    }

    /**
     * Converts the given WebVTT content into Podlove transcript entries. Cues with invalid timings or without text are skipped.
     *
     * @param vtt content of a WebVTT file
     * @return Podlove transcript entries in the order of the cues
     * @throws IllegalArgumentException if the content does not start with the WEBVTT signature
     * @should convert cues
     * @should read speaker from speaker label
     * @should read speaker from voice tag
     * @should skip notes styles and invalid cues
     * @should join multi line cue text and strip tags
     * @should throw IllegalArgumentException if signature is missing
     */
    public static JSONArray convertVtt(String vtt) {
        String content = StringUtils.defaultString(vtt).replace("﻿", "").replace("\r\n", "\n").replace('\r', '\n');
        if (!content.startsWith("WEBVTT")) {
            throw new IllegalArgumentException("Not a WebVTT file");
        }

        JSONArray ret = new JSONArray();
        String[] blocks = PATTERN_BLOCK_SEPARATOR.split(content);
        // The first block is the header (signature and optional metadata)
        for (int i = 1; i < blocks.length; i++) {
            JSONObject entry = convertCue(blocks[i]);
            if (entry != null) {
                ret.put(entry);
            }
        }

        return ret;
    }

    /**
     * @param block lines of a single WebVTT block
     * @return Podlove transcript entry; null if the block is no valid cue
     */
    private static JSONObject convertCue(String block) {
        String[] lines = block.strip().split("\n");
        if (lines[0].startsWith("NOTE") || lines[0].startsWith("STYLE") || lines[0].startsWith("REGION")) {
            return null;
        }
        // The timing line is either the first line or follows the optional cue identifier
        int timingIndex = -1;
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            if (lines[i].contains("-->")) {
                timingIndex = i;
                break;
            }
        }
        if (timingIndex < 0) {
            return null;
        }
        Matcher timing = PATTERN_TIMING.matcher(lines[timingIndex].strip());
        if (!timing.find()) {
            return null;
        }
        Long startMs = parseTimestamp(timing.group(1));
        Long endMs = parseTimestamp(timing.group(2));
        if (startMs == null || endMs == null) {
            return null;
        }

        StringBuilder sbText = new StringBuilder();
        for (int i = timingIndex + 1; i < lines.length; i++) {
            if (sbText.length() > 0) {
                sbText.append(' ');
            }
            sbText.append(lines[i].strip());
        }
        String text = sbText.toString();

        String speakerId = null;
        String speakerName = null;
        Matcher voice = PATTERN_VOICE_TAG.matcher(text);
        if (voice.find()) {
            speakerName = voice.group(1).strip();
            speakerId = speakerName;
        }
        text = decodeEntities(PATTERN_TAG.matcher(text).replaceAll("")).strip();
        if (speakerName == null) {
            Matcher label = PATTERN_SPEAKER_LABEL.matcher(text);
            if (label.find()) {
                speakerName = label.group(1);
                speakerId = label.group(2);
                text = text.substring(label.end());
            }
        }
        text = StringUtils.normalizeSpace(text);
        if (text.isEmpty()) {
            return null;
        }

        JSONObject entry = new JSONObject();
        entry.put("start", formatTimestamp(startMs));
        entry.put("start_ms", startMs);
        entry.put("end", formatTimestamp(endMs));
        entry.put("end_ms", endMs);
        if (speakerId != null) {
            entry.put("speaker", speakerId);
            entry.put("voice", speakerName);
        }
        entry.put("text", text);
        return entry;
    }

    /**
     * @param timestamp WebVTT timestamp ("hh:mm:ss.ttt" or "mm:ss.ttt")
     * @return time in milliseconds; null if the timestamp is invalid
     * @should parse timestamps with and without hours
     * @should return null for invalid timestamps
     */
    static Long parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        Matcher matcher = PATTERN_TIMESTAMP.matcher(timestamp.strip());
        if (!matcher.matches()) {
            return null;
        }
        long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
        long minutes = Long.parseLong(matcher.group(2));
        long seconds = Long.parseLong(matcher.group(3));
        if (minutes > 59 || seconds > 59) {
            return null;
        }
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + Long.parseLong(matcher.group(4));
    }

    private static String formatTimestamp(long millis) {
        return String.format("%02d:%02d:%02d.%03d", millis / 3_600_000, (millis / 60_000) % 60, (millis / 1000) % 60, millis % 1000);
    }

    private static String decodeEntities(String text) {
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&lrm;", "")
                .replace("&rlm;", "")
                .replace("&amp;", "&");
    }
}
