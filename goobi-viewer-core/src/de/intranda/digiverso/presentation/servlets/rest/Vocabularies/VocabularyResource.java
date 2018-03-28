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
package de.intranda.digiverso.presentation.servlets.rest.Vocabularies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * @author Florian Alpers
 *
 */
@Path("/vocabularies")
@ViewerRestServiceBinding
public class VocabularyResource {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyResource.class);
    private static final String VOCABULARY_TITLE_REGEX = "\"title\":\"(.*?)\"";
    private static final String VOCABULARY_DESCRIPTION_REGEX = "\"description\":\"(.*?)\"";


    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Glossary> listVocabularies() throws IOException {
        java.nio.file.Path viewerHome = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        java.nio.file.Path vocabulariesPath = viewerHome.resolve(DataManager.getInstance().getConfiguration().getVocabulariesFolder());

        List<java.nio.file.Path> vocabFiles = Files.list(vocabulariesPath)
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                .collect(Collectors.toList());

        List<Glossary> glossaries = new ArrayList<>();
        for (java.nio.file.Path vocabPath : vocabFiles) {
            Optional<String> title = readFromFile(vocabPath, VOCABULARY_TITLE_REGEX, 1);
            Optional<String> description = readFromFile(vocabPath, VOCABULARY_DESCRIPTION_REGEX, 1);
            
            title.map(t -> new Glossary(vocabPath.getFileName().toString(), t, description.orElse(""))).ifPresent(g -> glossaries.add(g));
        }
        return glossaries;
        
        
    }

    @GET
    @Path("/{filename}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getVocabulary(@PathParam("filename") String filename) throws IOException, ContentNotFoundException {
        java.nio.file.Path viewerHome = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome());
        java.nio.file.Path vocabulariesPath = viewerHome.resolve(DataManager.getInstance().getConfiguration().getVocabulariesFolder());
        java.nio.file.Path vocabularyPath = vocabulariesPath.resolve(filename);
        
        if(Files.exists(vocabularyPath)) {            
            List<String> lines = Files.readAllLines(vocabularyPath);
            return StringUtils.join(lines, "\n");
        } else {
            throw new ContentNotFoundException("No vocabulary found at " + vocabularyPath);
        }
        
    }

    /**
     * @param vocabPath
     * @return
     */
    public Optional<String> readFromFile(java.nio.file.Path vocabPath, String regex, int group) {
        Optional<String> title = Optional.of(vocabPath).map(path -> {
            try {
                return Files.readAllLines(path);
            } catch (IOException e) {
                logger.error("Unable to read file " + path, e);
                return new ArrayList<String>();
            }
        }).map(lines -> lines.size() > 0 ? lines.get(0) : "")
        .map(line -> Pattern.compile(regex).matcher(line))
        .filter(matcher -> matcher.find())
        .map(matcher -> matcher.group(group));
        return title;
    }

}
