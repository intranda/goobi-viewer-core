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
package de.intranda.digiverso.presentation.model.glossary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.ws.rs.PathParam;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * @author Florian Alpers
 *
 */
public class GlossaryManager {

    private static final Logger logger = LoggerFactory.getLogger(GlossaryManager.class);
    private static final String VOCABULARY_TITLE_REGEX = "\"title\":\"(.*?)\"";
    private static final String VOCABULARY_DESCRIPTION_REGEX = "\"description\":\"(.*?)\"";

    public List<Glossary> getGlossaries() throws IOException {
        java.nio.file.Path viewerHome = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        java.nio.file.Path vocabulariesPath = viewerHome.resolve(DataManager.getInstance().getConfiguration().getVocabulariesFolder());

        try (Stream<java.nio.file.Path> allVocabFiles = Files.list(vocabulariesPath)) {
            List<java.nio.file.Path> vocabFiles =
                    allVocabFiles.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json")).collect(Collectors.toList());

            List<Glossary> glossaries = new ArrayList<>();
            for (java.nio.file.Path vocabPath : vocabFiles) {
                Optional<String> title = readFromFile(vocabPath, VOCABULARY_TITLE_REGEX, 1);
                Optional<String> description = readFromFile(vocabPath, VOCABULARY_DESCRIPTION_REGEX, 1);

                title.map(t -> new Glossary(t, vocabPath.getFileName().toString(), description.orElse(""))).ifPresent(g -> glossaries.add(g));
            }
            return glossaries;
        }
    }

    public String getGlossaryAsJson(String filename) throws IOException, ContentNotFoundException {
        java.nio.file.Path viewerHome = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        java.nio.file.Path vocabulariesPath = viewerHome.resolve(DataManager.getInstance().getConfiguration().getVocabulariesFolder());
        java.nio.file.Path vocabularyPath = vocabulariesPath.resolve(filename);

        if (Files.exists(vocabularyPath)) {
            List<String> lines = Files.readAllLines(vocabularyPath);
            return StringUtils.join(lines, "\n");
        }
        throw new ContentNotFoundException("No vocabulary found at " + vocabularyPath);
    }

    public Glossary getGlossary(String filename) throws IOException, ContentNotFoundException, ParseException {
        java.nio.file.Path viewerHome = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        java.nio.file.Path vocabulariesPath = viewerHome.resolve(DataManager.getInstance().getConfiguration().getVocabulariesFolder());
        java.nio.file.Path vocabularyPath = vocabulariesPath.resolve(filename);

        if (Files.exists(vocabularyPath)) {
            List<String> lines = Files.readAllLines(vocabularyPath);
            String jsonString = StringUtils.join(lines, "\n");
            JSONObject json = new JSONObject(jsonString);

            String title = json.getString("title");
            String description = json.getString("description");

            Glossary glossary = new Glossary(title, filename, description);

            List<GlossaryRecord> glossaryRecords = new ArrayList<>();
            JSONArray records = json.getJSONArray("records");

            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                GlossaryRecord glossaryRecord = new GlossaryRecord();
                JSONArray fields = record.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject field = fields.getJSONObject(j);
                    String label = field.getString("label");
                    String value = field.getString("value");
                    switch (label) {
                        case "Title":
                            glossaryRecord.setTitle(value);
                            break;
                        case "Keywords":
                            glossaryRecord.setKeywords(value);
                            break;
                        case "Description":
                            glossaryRecord.setDescription(value);
                            break;
                        case "Source":
                            glossaryRecord.setSource(value);
                            break;
                    }
                }
                glossaryRecords.add(glossaryRecord);
            }
            glossary.setRecords(glossaryRecords);
            return glossary;
        } else {
            throw new ContentNotFoundException("No vocabulary found at " + vocabularyPath);
        }

    }

    /**
     * @param vocabPath
     * @return
     */
    private Optional<String> readFromFile(java.nio.file.Path vocabPath, String regex, int group) {
        Optional<String> title = Optional.of(vocabPath).map(path -> {
            try {
                return Files.readAllLines(path);
            } catch (IOException e) {
                logger.error("Unable to read file " + path, e);
                return new ArrayList<String>();
            }
        }).map(lines -> lines.size() > 0 ? lines.get(0) : "").map(line -> Pattern.compile(regex).matcher(line)).filter(matcher -> matcher.find()).map(
                matcher -> matcher.group(group));
        return title;
    }

}
