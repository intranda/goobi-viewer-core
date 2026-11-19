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

import java.time.Instant;
import java.time.LocalDateTime;
import java.io.StringReader;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.resourcebuilders.PodcastFeedResourceBuilder.Channel;
import io.goobi.viewer.api.rest.resourcebuilders.PodcastFeedResourceBuilder.Item;
import io.goobi.viewer.solr.SolrConstants;

class PodcastFeedResourceBuilderTest extends AbstractTest {

    private static final Namespace ITUNES = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");
    private static final Namespace ATOM = Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace PODCAST = Namespace.getNamespace("podcast", "https://podcastindex.org/namespace/1.0");

    private static Channel channel() {
        return new Channel("Show", "https://viewer/show", "https://viewer/api/v1/records/SHOW/podcast/feed.rss", "Wir sprechen über Bücher",
                "Aus der Stabi", "de", Arrays.asList("A", "B"), "info@example.org", "A, B", Arrays.asList("Arts", "Books", "Bücher"),
                "https://viewer/poster/SHOW.png", false);
    }

    private static Item item(String pi, String title, LocalDateTime pubDate) {
        return new Item(pi, title, "https://viewer/" + pi, "Summary " + pi, "Sub " + pi, pubDate, "https://viewer/audio/" + pi + ".mp3", 1234L,
                "audio/mpeg", "00:45:12", "https://viewer/poster/" + pi + ".png", false);
    }

    private static Element render(Channel channel, List<Item> items) throws Exception {
        String xml = PodcastFeedResourceBuilder.toXml(PodcastFeedResourceBuilder.buildDocument(channel, items));
        Document doc = new SAXBuilder().build(new StringReader(xml));
        return doc.getRootElement();
    }

    /**
     * @see PodcastFeedResourceBuilder#toFeedLanguage(String)
     * @verifies convert iso 639-2 to 639-1
     */
    @Test
    void toFeedLanguage_shouldConvertIso6392To6391() {
        Assertions.assertEquals("de", PodcastFeedResourceBuilder.toFeedLanguage("ger"));
        Assertions.assertEquals("en", PodcastFeedResourceBuilder.toFeedLanguage("eng"));
    }

    /**
     * @see PodcastFeedResourceBuilder#toFeedLanguage(String)
     * @verifies keep unknown or two letter codes
     */
    @Test
    void toFeedLanguage_shouldKeepUnknownOrTwoLetterCodes() {
        Assertions.assertEquals("de", PodcastFeedResourceBuilder.toFeedLanguage("de"));
        Assertions.assertEquals("xyz", PodcastFeedResourceBuilder.toFeedLanguage("xyz"));
    }

    /**
     * @see PodcastFeedResourceBuilder#toFeedLanguage(String)
     * @verifies return null for blank
     */
    @Test
    void toFeedLanguage_shouldReturnNullForBlank() {
        Assertions.assertNull(PodcastFeedResourceBuilder.toFeedLanguage(null));
        Assertions.assertNull(PodcastFeedResourceBuilder.toFeedLanguage(""));
    }

    /**
     * @see PodcastFeedResourceBuilder#formatDuration(String)
     * @verifies normalize to hours minutes seconds
     */
    @Test
    void formatDuration_shouldNormalizeToHoursMinutesSeconds() {
        Assertions.assertEquals("00:45:12", PodcastFeedResourceBuilder.formatDuration("45:12"));
        Assertions.assertEquals("01:02:03", PodcastFeedResourceBuilder.formatDuration("1:02:03"));
        Assertions.assertEquals("01:02:03", PodcastFeedResourceBuilder.formatDuration("3723"));
        Assertions.assertEquals("01:02:03", PodcastFeedResourceBuilder.formatDuration("01:02:03.500"));
    }

    /**
     * @see PodcastFeedResourceBuilder#formatDuration(String)
     * @verifies return null for invalid input
     */
    @Test
    void formatDuration_shouldReturnNullForInvalidInput() {
        Assertions.assertNull(PodcastFeedResourceBuilder.formatDuration("abc"));
        Assertions.assertNull(PodcastFeedResourceBuilder.formatDuration(null));
    }

    /**
     * @see PodcastFeedResourceBuilder#getPublicationDate(SolrDocument)
     * @verifies prefer iso year publish
     */
    @Test
    void getPublicationDate_shouldPreferIsoYearPublish() {
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_YEARPUBLISH", "2024-03-01");
        doc.addField("MD_DATEPUBLISH", "2020-01-01");
        Assertions.assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), PodcastFeedResourceBuilder.getPublicationDate(doc));
    }

    /**
     * @see PodcastFeedResourceBuilder#getPublicationDate(SolrDocument)
     * @verifies fall back to date publish
     */
    @Test
    void getPublicationDate_shouldFallBackToDatePublish() {
        SolrDocument doc = new SolrDocument();
        doc.addField("MD_YEARPUBLISH", "2024");
        doc.addField("MD_DATEPUBLISH", "2020-05-06");
        Assertions.assertEquals(LocalDateTime.of(2020, 5, 6, 0, 0), PodcastFeedResourceBuilder.getPublicationDate(doc));
    }

    /**
     * @see PodcastFeedResourceBuilder#getPublicationDate(SolrDocument)
     * @verifies fall back to date created
     */
    @Test
    void getPublicationDate_shouldFallBackToDateCreated() {
        SolrDocument doc = new SolrDocument();
        doc.addField(SolrConstants.DATECREATED, 1700000000000L);
        Assertions.assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC),
                PodcastFeedResourceBuilder.getPublicationDate(doc));
    }

    /**
     * @see PodcastFeedResourceBuilder#getPublicationDate(SolrDocument)
     * @verifies return null if no date
     */
    @Test
    void getPublicationDate_shouldReturnNullIfNoDate() {
        Assertions.assertNull(PodcastFeedResourceBuilder.getPublicationDate(new SolrDocument()));
    }

    /**
     * @see PodcastFeedResourceBuilder#formatRfc822(LocalDateTime)
     * @verifies format date
     */
    @Test
    void formatRfc822_shouldFormatDate() {
        Assertions.assertEquals("Fri, 1 Mar 2024 00:00:00 GMT", PodcastFeedResourceBuilder.formatRfc822(LocalDateTime.of(2024, 3, 1, 0, 0)));
    }

    /**
     * @see PodcastFeedResourceBuilder#isExplicit(String)
     * @verifies accept true and yes
     */
    @Test
    void isExplicit_shouldAcceptTrueAndYes() {
        Assertions.assertTrue(PodcastFeedResourceBuilder.isExplicit("true"));
        Assertions.assertTrue(PodcastFeedResourceBuilder.isExplicit("Yes"));
        Assertions.assertFalse(PodcastFeedResourceBuilder.isExplicit(null));
        Assertions.assertFalse(PodcastFeedResourceBuilder.isExplicit("no"));
        Assertions.assertFalse(PodcastFeedResourceBuilder.isExplicit("false"));
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies write required channel elements
     */
    @Test
    void buildDocument_shouldWriteRequiredChannelElements() throws Exception {
        Element rss = render(channel(), Collections.emptyList());
        Assertions.assertEquals("rss", rss.getName());
        Assertions.assertEquals("2.0", rss.getAttributeValue("version"));
        Assertions.assertEquals(ITUNES, rss.getNamespace("itunes"));
        Assertions.assertEquals(ATOM, rss.getNamespace("atom"));
        Assertions.assertEquals(PODCAST, rss.getNamespace("podcast"));
        Element ch = rss.getChild("channel");
        Assertions.assertEquals("Show", ch.getChildText("title"));
        Assertions.assertEquals("https://viewer/show", ch.getChildText("link"));
        Assertions.assertEquals("Wir sprechen über Bücher", ch.getChildText("description"));
        Assertions.assertEquals("de", ch.getChildText("language"));
        Element self = ch.getChild("link", ATOM);
        Assertions.assertEquals("self", self.getAttributeValue("rel"));
        Assertions.assertEquals("application/rss+xml", self.getAttributeValue("type"));
        Assertions.assertEquals("https://viewer/api/v1/records/SHOW/podcast/feed.rss", self.getAttributeValue("href"));
        Assertions.assertEquals("A, B", ch.getChildText("author", ITUNES));
        Assertions.assertEquals("info@example.org", ch.getChild("owner", ITUNES).getChildText("email", ITUNES));
        List<Element> categories = ch.getChildren("category", ITUNES);
        Assertions.assertEquals(List.of("Arts", "Books", "Bücher"), categories.stream().map(e -> e.getAttributeValue("text")).toList());
        Assertions.assertEquals("false", ch.getChildText("explicit", ITUNES));
        Assertions.assertEquals("Yes", ch.getChildText("block", ITUNES));
        Assertions.assertEquals("yes", ch.getChildText("block", PODCAST));
        Assertions.assertEquals("episodic", ch.getChildText("type", ITUNES));
        Assertions.assertEquals("https://viewer/poster/SHOW.png", ch.getChild("image", ITUNES).getAttributeValue("href"));
        Assertions.assertNull(ch.getChild("image"));
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies write item elements
     */
    @Test
    void buildDocument_shouldWriteItemElements() throws Exception {
        Element it = render(channel(), List.of(item("EP1", "Folge 1", LocalDateTime.of(2024, 3, 1, 0, 0)))).getChild("channel").getChild("item");
        Assertions.assertEquals("Folge 1", it.getChildText("title"));
        Assertions.assertEquals("https://viewer/EP1", it.getChildText("link"));
        Assertions.assertEquals("Summary EP1", it.getChildText("description"));
        Assertions.assertEquals("Summary EP1", it.getChildText("summary", ITUNES));
        Assertions.assertEquals("Sub EP1", it.getChildText("subtitle", ITUNES));
        Assertions.assertEquals("EP1", it.getChildText("guid"));
        Assertions.assertEquals("false", it.getChild("guid").getAttributeValue("isPermaLink"));
        Assertions.assertEquals("Fri, 1 Mar 2024 00:00:00 GMT", it.getChildText("pubDate"));
        Element enclosure = it.getChild("enclosure");
        Assertions.assertEquals("https://viewer/audio/EP1.mp3", enclosure.getAttributeValue("url"));
        Assertions.assertEquals("1234", enclosure.getAttributeValue("length"));
        Assertions.assertEquals("audio/mpeg", enclosure.getAttributeValue("type"));
        Assertions.assertEquals("00:45:12", it.getChildText("duration", ITUNES));
        Assertions.assertEquals("https://viewer/poster/EP1.png", it.getChild("image", ITUNES).getAttributeValue("href"));
        Assertions.assertEquals("false", it.getChildText("explicit", ITUNES));
        Assertions.assertEquals("full", it.getChildText("episodeType", ITUNES));
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies omit empty optional elements
     */
    @Test
    void buildDocument_shouldOmitEmptyOptionalElements() throws Exception {
        Channel channel = new Channel("Show", "https://viewer/show", "https://viewer/feed.rss", "Show", null, null, Collections.emptyList(), null,
                null, Collections.emptyList(), null, false);
        Item item = new Item("EP1", "Folge 1", "https://viewer/EP1", "Summary", null, null, "https://viewer/a.mp3", 1L, "audio/mpeg", null, null,
                false);
        Element ch = render(channel, List.of(item)).getChild("channel");
        Assertions.assertNull(ch.getChild("language"));
        Assertions.assertTrue(ch.getChildren("category", ITUNES).isEmpty());
        Assertions.assertNull(ch.getChild("author", ITUNES));
        Assertions.assertNull(ch.getChild("owner", ITUNES));
        Assertions.assertNull(ch.getChild("image", ITUNES));
        Element it = ch.getChild("item");
        Assertions.assertNull(it.getChild("subtitle", ITUNES));
        Assertions.assertNull(it.getChild("image", ITUNES));
        Assertions.assertNull(it.getChild("duration", ITUNES));
        Assertions.assertNull(it.getChild("pubDate"));
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies write channel without items
     */
    @Test
    void buildDocument_shouldWriteChannelWithoutItems() throws Exception {
        Element ch = render(channel(), Collections.emptyList()).getChild("channel");
        Assertions.assertNotNull(ch);
        Assertions.assertTrue(ch.getChildren("item").isEmpty());
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies sort items newest first with undated last
     */
    @Test
    void buildDocument_shouldSortItemsNewestFirstWithUndatedLast() throws Exception {
        List<Item> items = List.of(item("EP2023", "a", LocalDateTime.of(2023, 1, 1, 0, 0)), item("EPNONE", "b", null),
                item("EP2024", "c", LocalDateTime.of(2024, 1, 1, 0, 0)));
        List<String> guids = render(channel(), items).getChild("channel").getChildren("item").stream().map(e -> e.getChildText("guid")).toList();
        Assertions.assertEquals(List.of("EP2024", "EP2023", "EPNONE"), guids);
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies escape special characters
     */
    @Test
    void buildDocument_shouldEscapeSpecialCharacters() throws Exception {
        String title = "Tom & Jerry <live> \"Folge 1\"";
        Element it = render(channel(), List.of(item("EP1", title, null))).getChild("channel").getChild("item");
        Assertions.assertEquals(title, it.getChildText("title"));
    }

    /**
     * @see PodcastFeedResourceBuilder#selectEnclosure(JSONArray)
     * @verifies prefer mpeg over ogg
     */
    @Test
    void selectEnclosure_shouldPreferMpegOverOgg() {
        JSONArray audio = new JSONArray().put(new JSONObject().put("url", "a.ogg").put("mimeType", "audio/ogg"))
                .put(new JSONObject().put("url", "a.mp3").put("mimeType", "audio/mpeg"));
        Assertions.assertEquals("a.mp3", PodcastFeedResourceBuilder.selectEnclosure(audio).getString("url"));
    }

    /**
     * @see PodcastFeedResourceBuilder#selectEnclosure(JSONArray)
     * @verifies fall back to first source
     */
    @Test
    void selectEnclosure_shouldFallBackToFirstSource() {
        JSONArray audio = new JSONArray().put(new JSONObject().put("url", "a.ogg").put("mimeType", "audio/ogg"))
                .put(new JSONObject().put("url", "a.wav").put("mimeType", "audio/wav"));
        Assertions.assertEquals("a.ogg", PodcastFeedResourceBuilder.selectEnclosure(audio).getString("url"));
        Assertions.assertNull(PodcastFeedResourceBuilder.selectEnclosure(new JSONArray()));
    }

    /**
     * @see PodcastFeedResourceBuilder#buildDocument(Channel, List)
     * @verifies strip characters not allowed in xml
     */
    @Test
    void buildDocument_shouldStripCharactersNotAllowedInXml() throws Exception {
        Element it = render(channel(), List.of(item("EP1", "Folge\u000B 1\u0000", null))).getChild("channel").getChild("item");
        Assertions.assertEquals("Folge 1", it.getChildText("title"));
    }
}
