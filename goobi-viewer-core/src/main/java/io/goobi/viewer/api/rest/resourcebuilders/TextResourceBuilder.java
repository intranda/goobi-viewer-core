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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.intranda.digiverso.ocr.tei.TEIBuilder;
import de.intranda.digiverso.ocr.tei.convert.AbstractTEIConvert;
import de.intranda.digiverso.ocr.tei.convert.HtmlToTEIConvert;
import de.intranda.digiverso.ocr.tei.header.Identifier;
import de.intranda.digiverso.ocr.tei.header.Person;
import de.intranda.digiverso.ocr.tei.header.TEIHeaderBuilder;
import de.intranda.digiverso.ocr.tei.header.Title;
import de.intranda.digiverso.ocr.xml.DocumentReader;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.UncheckedPresentationException;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
public class TextResourceBuilder {

    private static final Logger logger = LogManager.getLogger(TextResourceBuilder.class);

    private static final String RESOURCE_NOT_FOUND = "Resource not found";
    private static final String EXCEPTION_NO_DOCUMENT_FOUND = "No document found with pi ";

    /**
     * Zero-arg constructor.
     */
    public TextResourceBuilder() {
        //
    }

    public String getFulltext(String pi)
            throws IOException, PresentationException, IndexUnreachableException {
        Map<Path, String> map = this.getFulltextMap(pi);
        StringBuilder sb = new StringBuilder();
        for (String pageText : map.values()) {
            sb.append(pageText).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * 
     * @param pi
     * @return {@link StreamingOutput}
     * @throws IOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentLibException
     */
    public StreamingOutput getFulltextAsZip(String pi)
            throws IOException, PresentationException, IndexUnreachableException, ContentLibException {
        logger.trace("getFulltextAsZip: {}", pi);
        String filename = pi + "_plaintext.zip";
        //        String foldername = DataManager.getInstance().getConfiguration().getFulltextFolder();
        //        String crowdsourcingFolderName = DataManager.getInstance().getConfiguration().getFulltextCrowdsourcingFolder();
        //        List<Path> files = getFiles(pi, foldername, crowdsourcingFolderName, null);
        //        if (files.isEmpty()) {
        // Fallback if no plaintext files found
        File tempFolder = new File(DataManager.getInstance().getConfiguration().getTempFolder(), pi + "_fulltext_" + System.currentTimeMillis());
        tempFolder.mkdir();
        Map<Path, String> map = this.getFulltextMap(pi);
        List<Path> tempFiles = new ArrayList<>();
        for (Entry<Path, String> entry : map.entrySet()) {
            String text = entry.getValue();
            File tempFile = new File(tempFolder, FilenameUtils.getBaseName(entry.getKey().getFileName().toString()) + ".txt");
            FileUtils.write(tempFile, text, StandardCharsets.UTF_8.name());
            tempFiles.add(tempFile.toPath());
        }
        tempFiles.sort((f1, f2) -> f1.getFileName().toString().compareTo(f2.getFileName().toString()));
        return writeZipFile(tempFiles, filename);
        //        }
        //        return writeZipFile(files, filename);

    }

    public StreamingOutput getAltoAsZip(String pi)
            throws IOException, PresentationException, IndexUnreachableException, ContentLibException {
        String filename = pi + "_alto.zip";
        String foldername = DataManager.getInstance().getConfiguration().getAltoFolder();
        String crowdsourcingFolderName = DataManager.getInstance().getConfiguration().getAltoCrowdsourcingFolder();
        List<Path> files = getFiles(pi, foldername, crowdsourcingFolderName, null);
        return writeZipFile(files, filename);
    }

    public String getAltoDocument(String pi)
            throws IOException, PresentationException, IndexUnreachableException {
        String foldername = DataManager.getInstance().getConfiguration().getAltoFolder();
        String crowdsourcingFolderName = DataManager.getInstance().getConfiguration().getAltoCrowdsourcingFolder();
        List<Path> files = getFiles(pi, foldername, crowdsourcingFolderName, null);

        StringBuilder sb = new StringBuilder();
        for (Path path : files) {
            String xmlString = FileTools.getStringFromFile(path.toFile(), StringTools.DEFAULT_ENCODING);
            sb.append(xmlString).append("\n");
        }
        return sb.toString().trim();

    }

    /**
     *
     * @param pi
     * @param fileName
     * @return StringPair(ALTO,charset)
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     */
    public StringPair getAltoDocument(String pi, String fileName) throws PresentationException,
            IndexUnreachableException, ContentNotFoundException {
        // logger.trace("getAltoDocument: {}/{}", pi, fileName);
        java.nio.file.Path file = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getAltoCrowdsourcingFolder(),
                DataManager.getInstance().getConfiguration().getAltoFolder(), fileName);

        if (file == null || !Files.isRegularFile(file)) {
            throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
        }

        try {
            String charset = FileTools.getCharset(file);
            // logger.trace(file.toAbsolutePath().toString());
            String alto = FileTools.getStringFromFile(file.toFile(), charset != null ? charset : StringTools.DEFAULT_ENCODING);
            return new StringPair(alto, charset);
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage());
            throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new PresentationException("Error reading resource");
        }
    }

    /**
     * 
     * @param pi
     * @param filename
     * @return Plain text extracted from TEI
     * @throws PresentationException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     */
    public String getFulltextAsTEI(String pi, String filename)
            throws PresentationException, ContentLibException, IndexUnreachableException {

        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (solrDoc != null) {

            try {
                String text = getFulltext(pi, filename);
                HtmlToTEIConvert textConverter = new HtmlToTEIConvert();
                text = convert(textConverter, text, filename);

                TEIHeaderBuilder header = createTEIHeader(solrDoc);

                TEIBuilder builder = new TEIBuilder();
                Document xmlDoc = builder.build(header, text);
                return DocumentReader.getAsString(xmlDoc, Format.getPrettyFormat());
            } catch (JDOMException e) {
                throw new ContentLibException("Unable to parse xml from alto file " + pi + ", " + filename, e);
            } catch (UncheckedPresentationException e) {
                throw new ContentLibException(e);
            }

        }

        throw new ContentNotFoundException(EXCEPTION_NO_DOCUMENT_FOUND + pi);
    }

    /**
     * 
     * @param pi
     * @param langCode
     * @return TEI document as {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws ContentLibException
     */
    public String getTeiDocument(String pi, String langCode)
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {

        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getTeiFolder(), null, null);
        java.nio.file.Path filePath = getDocumentLanguageVersion(teiPath, language);

        if (filePath != null && Files.isRegularFile(filePath)) {
            // TEI-based records
            try {
                Document doc = XmlTools.readXmlFile(filePath);
                return new XMLOutputter().outputString(doc);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException | JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            // All full-text pages as TEI
            SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            if (solrDoc == null) {
                throw new ContentNotFoundException(EXCEPTION_NO_DOCUMENT_FOUND + pi);
            }

            Map<java.nio.file.Path, String> fulltexts = getFulltextMap(pi);
            if (fulltexts.isEmpty()) {
                throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
            }

            TEIBuilder builder = new TEIBuilder();
            TEIHeaderBuilder header = createTEIHeader(solrDoc);
            HtmlToTEIConvert textConverter = new HtmlToTEIConvert();
            try {
                List<String> pages = fulltexts.entrySet()
                        .stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .map(entry -> convert(textConverter, entry.getValue(), entry.getKey().toString()))
                        .collect(Collectors.toList());

                Document xmlDoc = builder.build(header, pages);
                return DocumentReader.getAsString(xmlDoc, Format.getPrettyFormat());
            } catch (JDOMException e) {
                throw new ContentLibException("Unable to parse xml from alto file in " + pi, e);
            } catch (UncheckedPresentationException e) {
                throw new ContentLibException(e);
            }

        }

        throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
    }

    /**
     * 
     * @param pi
     * @param langCode
     * @return {@link StreamingOutput}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws ContentLibException
     */
    public StreamingOutput getTeiAsZip(String pi, String langCode)
            throws PresentationException, IndexUnreachableException, IOException, ContentLibException {

        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path teiPath = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getTeiFolder(), null, null);
        java.nio.file.Path filePath = getDocumentLanguageVersion(teiPath, language);

        if (filePath != null && Files.isRegularFile(filePath)) {

            String filename = pi + "_tei.zip";
            return writeZipFile(Collections.singletonList(filePath), filename);

        }

        // All full-text pages as TEI
        SolrDocument solrDoc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (solrDoc == null) {
            throw new ContentNotFoundException(EXCEPTION_NO_DOCUMENT_FOUND + pi);
        }

        Map<java.nio.file.Path, String> fulltexts = getFulltextMap(pi);
        if (fulltexts.isEmpty()) {
            throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
        }

        TEIBuilder builder = new TEIBuilder();
        TEIHeaderBuilder header = createTEIHeader(solrDoc);
        HtmlToTEIConvert textConverter = new HtmlToTEIConvert();
        Map<java.nio.file.Path, String> teis = new LinkedHashMap<>();

        try {
            for (Entry<Path, String> entry : fulltexts.entrySet()) {
                String filename = entry.getKey().getFileName().toString();
                filename = FilenameUtils.removeExtension(filename) + ".xml";
                String content = entry.getValue();
                content = convert(textConverter, content, filename);
                Document xmlDoc;
                xmlDoc = builder.build(header, content);
                String tei = DocumentReader.getAsString(xmlDoc, Format.getPrettyFormat());
                teis.put(Paths.get(filename), tei);
            }
            String filename = pi + "_tei.zip";
            return writeZipFile(teis, filename);
        } catch (JDOMException e) {
            throw new ContentLibException("Unable to parse xml from tei content in " + pi, e);
        } catch (UncheckedPresentationException e) {
            throw new ContentLibException(e);
        }

    }

    /**
     *
     * @param pi
     * @param langCode
     * @return CMDI document as {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     * @throws IOException
     */
    public String getCmdiDocument(String pi, String langCode)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, IOException {
        final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
        java.nio.file.Path cmdiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getCmdiFolder());
        java.nio.file.Path filePath = getDocumentLanguageVersion(cmdiPath, language);
        if (filePath != null && Files.isRegularFile(filePath)) {
            try {
                Document doc = XmlTools.readXmlFile(filePath);
                return new XMLOutputter().outputString(doc);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException | JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
    }

    public String getContentAsText(String contentFolder, String pi, String fileName)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException {

        java.nio.file.Path file = DataFileTools.getDataFilePath(pi, contentFolder, null, fileName);
        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), StringTools.DEFAULT_ENCODING);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        throw new ContentNotFoundException(RESOURCE_NOT_FOUND);
    }

    /**
     * <p>
     * getFulltext.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param fileName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws DAOException
     * @throws ServiceNotAllowedException
     */
    public String getFulltext(String pi, String fileName) throws PresentationException, IndexUnreachableException, ContentNotFoundException {

        java.nio.file.Path file = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getFulltextCrowdsourcingFolder(),
                DataManager.getInstance().getConfiguration().getFulltextFolder(), fileName);
        if (file != null && Files.isRegularFile(file)) {
            try {
                return FileTools.getStringFromFile(file.toFile(), StringTools.DEFAULT_ENCODING);
            } catch (FileNotFoundException e) {
                logger.debug(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            file = DataFileTools.getDataFilePath(pi, DataManager.getInstance().getConfiguration().getAltoFolder(),
                    DataManager.getInstance().getConfiguration().getAltoFolder(), fileName.replaceAll("(i?)\\.txt", ".xml"));
            if (file != null && Files.isRegularFile(file)) {
                try {
                    return ALTOTools.getFulltext(file, StringTools.DEFAULT_ENCODING);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        throw new ContentNotFoundException(RESOURCE_NOT_FOUND);

    }

    /**
     * Collects full-text file paths and content in a map. Priority is given to files from plaintext resources, with missing files being stuffed with
     * converted ALTO.
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should prioritize plaintext files over alto
     */
    public Map<java.nio.file.Path, String> getFulltextMap(String pi) throws IOException, PresentationException, IndexUnreachableException {
        Map<java.nio.file.Path, String> ret = new HashMap<>();
        List<java.nio.file.Path> fulltextFiles = getFiles(pi, DataManager.getInstance().getConfiguration().getFulltextCrowdsourcingFolder(),
                DataManager.getInstance().getConfiguration().getFulltextFolder(), "(i?).*\\.txt");

        Map<java.nio.file.Path, String> fileMapFromPlaintext = null;
        if (!fulltextFiles.isEmpty()) {
            logger.debug("Collecting plaintext files from {}", fulltextFiles.get(0).getParent().toAbsolutePath());
            fileMapFromPlaintext = fulltextFiles.stream().collect(Collectors.toMap(p -> p, p -> {
                try {
                    return FileTools.getStringFromFile(p.toFile(), StringTools.DEFAULT_ENCODING);
                } catch (IOException e) {
                    logger.error("Error reading file {}", p, e);
                    return "";
                }
            }));
        }

        Map<java.nio.file.Path, String> fileMapFromAlto = null;
        List<java.nio.file.Path> altoFiles = getFiles(pi, DataManager.getInstance().getConfiguration().getAltoFolder(),
                DataManager.getInstance().getConfiguration().getAltoFolder(), "(i?).*\\.(alto|xml)");
        if (!altoFiles.isEmpty()) {
            logger.debug("Converting ALTO files from {}", altoFiles.get(0).getParent().toAbsolutePath());
            fileMapFromAlto = altoFiles.stream()
                    .collect(Collectors.toMap(
                            p -> Paths.get(p.toString().replaceAll("(i?)\\.(alto|xml)", ".txt")),
                            p -> {
                                try {
                                    return ALTOTools.getFulltext(p, StringTools.DEFAULT_ENCODING);
                                } catch (IOException e) {
                                    logger.error("Error reading file {}", p, e);
                                    return "";
                                }
                            }));
        }

        // Add collected plaintext files
        final Set<String> fileNames = new HashSet<>();
        if (fileMapFromPlaintext != null && !fileMapFromPlaintext.isEmpty()) {
            for (Entry<Path, String> entry : fileMapFromPlaintext.entrySet()) {
                ret.put(entry.getKey(), entry.getValue());
                fileNames.add(entry.getKey().getFileName().toString());
                logger.trace("Added {} from plain text", entry.getKey());
            }
        }

        // Add text files converted from ALTO. Only add files whose name wasn't already collected from plain text resources.
        if (fileMapFromAlto != null && !fileMapFromAlto.isEmpty()) {
            for (Entry<Path, String> entry : fileMapFromAlto.entrySet()) {
                if (!fileNames.contains(entry.getKey().getFileName().toString())) {
                    ret.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return ret;
    }

    /**
     * <p>
     * getFiles.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param foldername a {@link java.lang.String} object.
     * @param altFoldername a {@link java.lang.String} object.
     * @param filter a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    private static List<java.nio.file.Path> getFiles(String pi, String foldername, String altFoldername, String filter)
            throws IOException, PresentationException, IndexUnreachableException {

        java.nio.file.Path folder1 = DataFileTools.getDataFilePath(pi, foldername, null, null);
        java.nio.file.Path folder2 = DataFileTools.getDataFilePath(pi, altFoldername, null, null);

        return getFiles(folder1, folder2, filter);
    }

    /**
     * <p>
     * getFiles.
     * </p>
     *
     * @param folder a {@link java.nio.file.Path} object.
     * @param altFolder a {@link java.nio.file.Path} object.
     * @param filter a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    private static List<java.nio.file.Path> getFiles(java.nio.file.Path folder, java.nio.file.Path altFolder, String filter) throws IOException {

        List<java.nio.file.Path> files = new ArrayList<>();

        if (folder != null && Files.isDirectory(folder)) {
            try (Stream<java.nio.file.Path> paths = Files.list(folder)
                    .filter(p -> p.getFileName().toString().toLowerCase().matches(StringUtils.isBlank(filter) ? ".*" : filter))
                    .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))) {
                files = paths.collect(Collectors.toList());
            }
        }

        if (altFolder != null && Files.isDirectory(altFolder)) {
            try (Stream<java.nio.file.Path> paths = Files.list(altFolder)
                    .filter(p -> p.getFileName().toString().toLowerCase().matches(StringUtils.isBlank(filter) ? ".*" : filter))
                    .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))) {
                List<java.nio.file.Path> altFiles = paths.collect(Collectors.toList());

                files = new ArrayList<>(Stream.of(files, altFiles)
                        .flatMap(List::stream)
                        .collect(Collectors.toMap(java.nio.file.Path::getFileName, path -> path,
                                (java.nio.file.Path path1, java.nio.file.Path path2) -> path1 == null ? path2 : path1))
                        .values());
            }

        }
        return files;
    }

    /**
     * <p>
     * createTEIHeader.
     * </p>
     *
     * @param solrDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link de.intranda.digiverso.ocr.tei.header.TEIHeaderBuilder} object.
     */
    private static TEIHeaderBuilder createTEIHeader(SolrDocument solrDoc) {
        TEIHeaderBuilder header = new TEIHeaderBuilder();

        Optional.ofNullable(solrDoc.getFieldValue(SolrConstants.LABEL))
                .map(SolrTools::getAsString)
                .map(Title::new)
                .ifPresent(header::setTitle);

        List<String> authors = Optional.ofNullable(solrDoc.getFieldValues("MD_AUTHOR"))
                .orElse(Collections.emptyList())
                .stream()
                .map(SolrTools::getAsString)
                .collect(Collectors.toList());
        for (String name : authors) {
            if (name.contains(",")) {
                String[] parts = name.split(",");
                header.addAuthor(new Person(parts[1], parts[0]));
            } else {
                header.addAuthor(new Person(name));
            }
        }

        Optional.ofNullable(solrDoc.getFieldValue(SolrConstants.PI))
                .map(SolrTools::getAsString)
                .map(Identifier::new)
                .ifPresent(header::addIdentifier);
        return header;
    }

    /**
     * Returns the first file on the given folder path that contains the requested language code in its name. ISO-3 files are preferred, with a
     * fallback to ISO-2.
     *
     * @param folder
     * @param language
     * @return Path of the requested file; null if not found
     * @throws IOException
     */
    private static java.nio.file.Path getDocumentLanguageVersion(java.nio.file.Path folder, Language language) throws IOException {
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }
        if (folder == null || !Files.isDirectory(folder)) {
            return null;
        }

        java.nio.file.Path ret;
        // This will return the file with the requested language or alternatively the first file in the TEI folder
        try (Stream<java.nio.file.Path> teiFiles = Files.list(folder)) {
            ret = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml")).findFirst().orElse(null);
        }
        // Fallback to ISO-2
        if (ret == null) {
            try (Stream<java.nio.file.Path> teiFiles = Files.list(folder)) {
                ret = teiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCodeOld() + ".xml"))
                        .findFirst()
                        .orElse(null);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getTEIFiles.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<java.nio.file.Path> getTEIFiles(String pi) {
        try {
            java.nio.file.Path teiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getTeiFolder());
            List<java.nio.file.Path> filePaths = new ArrayList<>();
            if (Files.exists(teiPath)) {
                // This will return the file with the requested language or alternatively the first file in the TEI folder
                try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                    filePaths = teiFiles.filter(path -> path.getFileName().toString().matches(".*_[a-z]{1,3}\\.xml")).collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
            }

            return filePaths;
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getTEIFiles.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param langCode
     * @return a {@link java.util.List} object.
     */
    public List<java.nio.file.Path> getTEIFiles(String pi, String langCode) {
        try {
            java.nio.file.Path teiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getTeiFolder());
            List<java.nio.file.Path> filePaths = new ArrayList<>();
            if (Files.exists(teiPath)) {
                Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
                if (language == null) {
                    return Collections.emptyList();
                }
                // This will return the file with the requested language or alternatively the first file in the TEI folder
                try (Stream<java.nio.file.Path> teiFiles = Files.list(teiPath)) {
                    filePaths = teiFiles
                            .filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml"))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
            }

            return filePaths;
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getCMDIFile.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param langCode a {@link java.lang.String} object.
     * @return a {@link java.nio.file.Path} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public java.nio.file.Path getCMDIFile(String pi, String langCode) throws IOException, PresentationException, IndexUnreachableException {
        java.nio.file.Path cmdiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getCmdiFolder());
        java.nio.file.Path filePath = null;
        logger.trace("CMDI: {}", cmdiPath.toAbsolutePath());
        if (Files.exists(cmdiPath)) {
            // This will return the file with the requested language or alternatively the first file in the CMDI folder
            try (Stream<java.nio.file.Path> cmdiFiles = Files.list(cmdiPath)) {
                final Language language = DataManager.getInstance().getLanguageHelper().getLanguage(langCode);
                if (language == null) {
                    return null;
                }
                filePath = cmdiFiles.filter(path -> path.getFileName().toString().endsWith("_" + language.getIsoCode() + ".xml"))
                        .findFirst()
                        .orElse(null);
            }
        }

        return filePath;
    }

    /**
     * <p>
     * getCMDIFiles.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<java.nio.file.Path> getCMDIFiles(String pi) {

        try {
            java.nio.file.Path cdmiPath = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getCmdiFolder());
            List<java.nio.file.Path> filePaths = new ArrayList<>();
            if (Files.exists(cdmiPath)) {
                // This will return the file with the requested language or alternatively the first file in the CMDI folder
                try (Stream<java.nio.file.Path> teiFiles = Files.list(cdmiPath)) {
                    filePaths = teiFiles.filter(path -> path.getFileName().toString().matches(".*_[a-z]{1,3}\\.xml")).collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error(e.toString(), e);
                }
            }
            return filePaths;
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * 
     * @param contentMap
     * @param filename
     * @return {@link StreamingOutput}
     * @throws ContentLibException
     */
    private static StreamingOutput writeZipFile(Map<Path, String> contentMap, String filename) throws ContentLibException {
        File tempFile = new File(DataManager.getInstance().getConfiguration().getTempFolder(), filename);
        try {
            if (!tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs()) {
                throw new ContentLibException("Not allowed to create temp file directory " + tempFile.getParentFile());
            }

            FileTools.compressZipFile(contentMap, tempFile, 9);
            return out -> {
                try (FileInputStream in = new FileInputStream(tempFile)) {
                    FileTools.copyStream(out, in);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    logger.trace(e.getMessage(), e);
                } finally {
                    if (tempFile.exists()) {
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (IOException e) {
            if (tempFile.exists()) {
                FileUtils.deleteQuietly(tempFile);
            }
            throw new ContentNotFoundException("Resource not found or not accessible", e);
        }
    }

    /**
     * 
     * @param files
     * @param filename
     * @return {@link StreamingOutput}
     * @throws ContentLibException
     */
    private static StreamingOutput writeZipFile(List<Path> files, String filename) throws ContentLibException {
        File tempFile = new File(DataManager.getInstance().getConfiguration().getTempFolder(), filename);
        try {
            if (!tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs()) {
                throw new ContentLibException("Not allowed to create temp file directory " + tempFile.getParentFile());
            }

            FileTools.compressZipFile(files.stream().map(Path::toFile).collect(Collectors.toList()), tempFile, 9);
            return out -> {
                try (FileInputStream in = new FileInputStream(tempFile)) {
                    FileTools.copyStream(out, in);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    logger.trace(e.getMessage(), e);
                } finally {
                    if (tempFile.exists()) {
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (IOException e) {
            if (tempFile.exists()) {
                FileUtils.deleteQuietly(tempFile);
            }
            throw new ContentNotFoundException("Resource not found or not accessible", e);
        }
    }

    /**
     * 
     * @param converter
     * @param input
     * @param identifier
     * @return Converted input
     * @throws UncheckedPresentationException
     */
    private static String convert(AbstractTEIConvert converter, String input, String identifier) throws UncheckedPresentationException {
        try {
            return converter.convert(input);
        } catch (Exception e) {
            throw new UncheckedPresentationException("Error converting the input from " + identifier, e);
        }
    }
}
