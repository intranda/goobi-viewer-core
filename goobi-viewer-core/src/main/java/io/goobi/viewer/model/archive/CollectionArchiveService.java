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
package io.goobi.viewer.model.archive;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.export.ExportFormat;
import io.goobi.viewer.model.export.XsltSearchExport;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Builds and serves the per-collection BagIt archives described by {@code <collectionArchives>} in {@code config_viewer.xml}.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>build the open-access Solr filter (only {@code OPENACCESS} and backend-configured open-access license types) and the
 * collection-selector query, with all indexed values Solr-escaped to prevent query injection;</li>
 * <li>decide whether a collection's archive needs (re)generation by comparing the current record count and maximum
 * {@link SolrConstants#DATEINDEXED} against the values encoded in the existing archive's file name;</li>
 * <li>assemble the enabled content types into a bag's {@code data/} directory, write it via {@link BagItWriter}, and atomically move it
 * into the storage folder;</li>
 * <li>locate and describe existing archives for the download endpoint / collection page, and prune orphaned archives.</li>
 * </ul>
 *
 * <p>
 * Archive files are named {@code <slug>__n<recordCount>__u<maxIndexedMillis>.zip}; this file name is the sole persisted state (no sidecar
 * manifest, no database table).
 */
public class CollectionArchiveService {

    private static final Logger logger = LogManager.getLogger(CollectionArchiveService.class);

    /** Upper bound on the number of records processed per collection, to bound memory and generation time. */
    static final int MAX_RECORDS = 100_000;

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^(.+)__n(\\d+)__u(\\d+)\\.zip$");
    private static final String DEFAULT_RIS_XSLT = "solr2ris.xsl";
    private static final String DEFAULT_BIBTEX_XSLT = "solr2bibtex.xsl";

    /**
     * Immutable description of an existing archive on disk, as recovered from its file name.
     *
     * @param path the archive file
     * @param recordCount the record count encoded in the file name
     * @param maxIndexedMillis the maximum DATEINDEXED (epoch millis) encoded in the file name
     * @param sizeBytes the archive file size in bytes
     */
    public record ArchiveInfo(Path path, long recordCount, long maxIndexedMillis, long sizeBytes) {
    }

    /**
     * Outcome of a {@link #generateIfChanged(String, String, CollectionArchiveConfig)} call.
     *
     * @param generated true if an archive was (re)written, false if the run was skipped
     * @param detail human-readable detail — a content summary when generated, or the skip reason otherwise
     */
    public record GenerationResult(boolean generated, String detail) {
        static GenerationResult generated(String detail) {
            return new GenerationResult(true, detail);
        }

        static GenerationResult skipped(String reason) {
            return new GenerationResult(false, reason);
        }
    }

    /**
     * Accumulates, per content type, how many records contributed at least one file ("present") versus how many had none ("missing"), so
     * generation can report a summary instead of silently skipping absent files.
     */
    private static final class PopulationStats {
        private final Map<ArchiveContentType, int[]> counts = new EnumMap<>(ArchiveContentType.class);

        void record(ArchiveContentType type, boolean present) {
            int[] c = counts.computeIfAbsent(type, t -> new int[2]);
            c[present ? 0 : 1]++;
        }

        int missing() {
            return counts.values().stream().mapToInt(c -> c[1]).sum();
        }

        /**
         * @return a compact per-type summary such as {@code "metadata_source 50/50, fulltext_alto 30/50"} (present/total)
         */
        String summary() {
            if (counts.isEmpty()) {
                return "no content";
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<ArchiveContentType, int[]> e : counts.entrySet()) {
                int present = e.getValue()[0];
                int total = present + e.getValue()[1];
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(e.getKey().name().toLowerCase()).append(' ').append(present).append('/').append(total);
            }
            return sb.toString();
        }
    }

    private final Configuration config;

    /**
     * Creates a service using the global {@link DataManager} configuration.
     */
    public CollectionArchiveService() {
        this(DataManager.getInstance().getConfiguration());
    }

    /**
     * @param config the configuration to read archive settings and data-folder names from
     */
    public CollectionArchiveService(Configuration config) {
        this.config = config;
    }

    // ----- storage layout / file name encoding -----

    /**
     * @return the base storage folder for all collection archives ({@code <viewerHome>/<collectionArchivesFolder>})
     */
    public Path getStorageBaseFolder() {
        return Paths.get(config.getViewerHome(), config.getCollectionArchivesFolder());
    }

    /**
     * @param field the collection Solr field (validated by the caller against the configured allow-list)
     * @return the storage folder holding archives for the given field
     */
    public Path getFieldFolder(String field) {
        return getStorageBaseFolder().resolve(slugify(field));
    }

    /**
     * Builds a filesystem-safe slug from a collection (or field) name. All characters outside {@code [A-Za-z0-9.-]} are replaced with an
     * underscore, so the slug can never contain a path separator or traversal sequence.
     *
     * @param name the raw name
     * @return a slug safe for use as a single path segment
     */
    public static String slugify(String name) {
        if (StringUtils.isBlank(name)) {
            return "_";
        }
        String slug = name.replaceAll("[^A-Za-z0-9.-]", "_");
        // Guard against "." / ".." and leading dots which are unsafe path segments.
        slug = slug.replaceAll("^\\.+", "_");
        return slug.isEmpty() ? "_" : slug;
    }

    static String buildFileName(String slug, long recordCount, long maxIndexedMillis) {
        return slug + "__n" + recordCount + "__u" + maxIndexedMillis + ".zip";
    }

    /**
     * Parses the record count and max-DATEINDEXED state encoded in an archive file name.
     *
     * @param fileName the archive file name
     * @return a {@code long[]{recordCount, maxIndexedMillis}}, or {@code null} if the name does not match the expected pattern
     */
    static long[] parseFileName(String fileName) {
        Matcher m = FILE_NAME_PATTERN.matcher(fileName);
        if (!m.matches()) {
            return null;
        }
        try {
            return new long[] { Long.parseLong(m.group(2)), Long.parseLong(m.group(3)) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Finds the current archive file for a collection, if one exists.
     *
     * @param field the collection Solr field
     * @param collectionName the collection name
     * @return the archive description, or empty if none exists
     * @throws IOException on directory read error
     */
    public Optional<ArchiveInfo> findArchive(String field, String collectionName) throws IOException {
        String slug = slugify(collectionName);
        Path folder = getFieldFolder(field);
        if (!Files.isDirectory(folder)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, slug + "__n*__u*.zip")) {
            for (Path path : stream) {
                long[] state = parseFileName(path.getFileName().toString());
                if (state != null && matchesSlug(path.getFileName().toString(), slug)) {
                    return Optional.of(new ArchiveInfo(path, state[0], state[1], Files.size(path)));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesSlug(String fileName, String slug) {
        Matcher m = FILE_NAME_PATTERN.matcher(fileName);
        return m.matches() && m.group(1).equals(slug);
    }

    // ----- Solr queries -----

    /**
     * Builds the open-access filter query fragment: only records whose access condition is {@code OPENACCESS} or a backend-configured
     * open-access {@link LicenseType}. License-type names are Solr-escaped to prevent query injection.
     *
     * @return a Solr filter fragment of the form {@code +(ACCESSCONDITION:"OPENACCESS" ACCESSCONDITION:"…")}
     * @throws DAOException if the license types cannot be loaded
     */
    public String buildOpenAccessFilter() throws DAOException {
        StringBuilder sb = new StringBuilder();
        sb.append("+(").append(SolrConstants.ACCESSCONDITION).append(":\"").append(SolrConstants.OPEN_ACCESS_VALUE).append('"');
        for (LicenseType licenseType : DataManager.getInstance().getDao().getAllLicenseTypes()) {
            if (licenseType.isOpenAccess() && StringUtils.isNotBlank(licenseType.getName())) {
                sb.append(' ').append(SolrConstants.ACCESSCONDITION).append(":\"").append(escapePhrase(licenseType.getName())).append('"');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Builds the full Solr query selecting the freely-accessible top-level records of a collection (and its sub-collections). The
     * collection name is escaped both as a phrase and as a wildcard prefix to prevent query injection.
     *
     * @param field the collection Solr field
     * @param collectionName the collection name
     * @return the combined Solr query string
     * @throws DAOException if the license types cannot be loaded for the access filter
     */
    public String buildCollectionQuery(String field, String collectionName) throws DAOException {
        String phrase = escapePhrase(collectionName);
        String prefix = ClientUtils.escapeQueryChars(collectionName);
        return "+(" + field + ":\"" + phrase + "\" " + field + ":" + prefix + ".*)"
                + " +(" + SolrConstants.ISWORK + ":* " + SolrConstants.ISANCHOR + ":*) "
                + buildOpenAccessFilter();
    }

    /**
     * Escapes a value for use inside a Solr phrase query (double-quoted). Only backslash and double-quote need escaping there; escaping the
     * full set of query-syntax characters (as {@link ClientUtils#escapeQueryChars(String)} does) would corrupt the phrase.
     *
     * @param value the raw value
     * @return the value with {@code \} and {@code "} escaped
     */
    static String escapePhrase(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    long getRecordCount(String query) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance().getSearchIndex().getHitCount(query);
    }

    long getMaxIndexedMillis(String query) throws IndexUnreachableException, PresentationException {
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(query, 1,
                Collections.singletonList(new StringPair(SolrConstants.DATEINDEXED, "desc")),
                Collections.singletonList(SolrConstants.DATEINDEXED));
        if (docs == null || docs.isEmpty()) {
            return 0;
        }
        return coerceMillis(docs.get(0).getFieldValue(SolrConstants.DATEINDEXED));
    }

    private static long coerceMillis(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Date date) {
            return date.getTime();
        }
        if (value instanceof Iterable<?> iterable) {
            long max = 0;
            for (Object o : iterable) {
                max = Math.max(max, coerceMillis(o));
            }
            return max;
        }
        return 0;
    }

    // ----- generation -----

    /**
     * Generates the archive for a single collection if (and only if) it does not yet exist or its content has changed since the last
     * generation, as determined by comparing the current record count and maximum DATEINDEXED against the state encoded in the existing
     * archive's file name.
     *
     * @param field the collection Solr field
     * @param collectionName the collection name
     * @param archiveConfig the resolved set of content types to include
     * @return a {@link GenerationResult} indicating whether an archive was (re)generated and a human-readable detail (content summary when
     *         generated, or the reason when skipped)
     * @throws PresentationException on Solr query errors
     * @throws IndexUnreachableException if the Solr index is unreachable
     * @throws DAOException if license types cannot be loaded
     * @throws IOException on filesystem errors
     * @throws ViewerConfigurationException on export configuration errors
     */
    public GenerationResult generateIfChanged(String field, String collectionName, CollectionArchiveConfig archiveConfig)
            throws PresentationException, IndexUnreachableException, DAOException, IOException, ViewerConfigurationException {
        if (archiveConfig == null || !archiveConfig.hasEnabledTypes()) {
            deleteExistingArchives(field, collectionName);
            return GenerationResult.skipped("no content types enabled");
        }

        String query = buildCollectionQuery(field, collectionName);
        long recordCount = getRecordCount(query);
        if (recordCount == 0) {
            // Empty collection: remove any stale archive so the page never offers an empty download.
            deleteExistingArchives(field, collectionName);
            return GenerationResult.skipped("collection is empty (no freely accessible records)");
        }
        long maxIndexed = getMaxIndexedMillis(query);

        Optional<ArchiveInfo> existing = findArchive(field, collectionName);
        if (existing.isPresent() && existing.get().recordCount() == recordCount && existing.get().maxIndexedMillis() == maxIndexed
                && Files.isRegularFile(existing.get().path())) {
            logger.debug("Collection archive for {}:{} is up to date ({} records), skipping.", field, collectionName, recordCount);
            return GenerationResult.skipped("unchanged (" + recordCount + " records)");
        }

        String summary = buildBag(field, collectionName, archiveConfig, query, recordCount, maxIndexed);
        return GenerationResult.generated(summary);
    }

    private String buildBag(String field, String collectionName, CollectionArchiveConfig archiveConfig, String query, long recordCount,
            long maxIndexed) throws PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException, DAOException {
        String slug = slugify(collectionName);
        Path tempRoot = Paths.get(config.getTempFolder(), "collection_archives", slug + "_" + maxIndexed);
        Path bagRoot = tempRoot.resolve(slug);
        Path dataDir = bagRoot.resolve("data");
        Files.createDirectories(dataDir);
        Path tempZip = tempRoot.resolve(buildFileName(slug, recordCount, maxIndexed));

        try {
            List<SolrDocument> records = fetchRecords(query);
            PopulationStats stats = new PopulationStats();
            for (ArchiveContentType type : archiveConfig.getEnabledTypes()) {
                populate(type, dataDir, field, collectionName, query, records, stats);
            }

            Map<String, String> bagInfo = new LinkedHashMap<>();
            bagInfo.put("Source-Organization", config.getName());
            bagInfo.put("Bagging-Date", LocalDate.now().toString());
            bagInfo.put("External-Identifier", field + ":" + collectionName);
            bagInfo.put("External-Description", "Collection " + collectionName + " (" + field + ")");
            bagInfo.put("Bag-Count", String.valueOf(recordCount));

            // Media payloads are already compressed; use a low level to save CPU when images are included.
            Integer level = archiveConfig.isEnabled(ArchiveContentType.IMAGES) ? 1 : 9;
            BagItWriter.writeZippedBag(bagRoot, tempZip, bagInfo, level);

            Path targetFolder = getFieldFolder(field);
            Files.createDirectories(targetFolder);
            Path targetFile = targetFolder.resolve(buildFileName(slug, recordCount, maxIndexed));
            moveInto(tempZip, targetFile);

            // Remove any previous archive(s) for this collection that carry a different state suffix.
            deleteExistingArchives(field, collectionName, targetFile);

            String summary = stats.summary();
            logger.info("Generated collection archive {} ({} records). Content: {}", targetFile, recordCount, summary);
            if (stats.missing() > 0) {
                logger.info("Collection {}:{}: {} record(s) had no file for one or more enabled content types "
                        + "(per-record detail at DEBUG). This is expected when records legitimately lack that content.",
                        field, collectionName, stats.missing());
            }
            return summary;
        } finally {
            FileUtils.deleteQuietly(tempRoot.toFile());
        }
    }

    private List<SolrDocument> fetchRecords(String query) throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(query, MAX_RECORDS, null,
                List.of(SolrConstants.PI, SolrConstants.SOURCEDOCFORMAT, SolrConstants.DATAREPOSITORY,
                        SolrConstants.BOOL_IMAGEAVAILABLE, SolrConstants.FULLTEXTAVAILABLE));
        if (docs != null && docs.getNumFound() > MAX_RECORDS) {
            logger.warn("Collection has {} records; only the first {} are included in the archive.", docs.getNumFound(), MAX_RECORDS);
        }
        return docs != null ? docs : new SolrDocumentList();
    }

    private void populate(ArchiveContentType type, Path dataDir, String field, String collectionName, String query,
            List<SolrDocument> records, PopulationStats stats)
            throws PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException, DAOException {
        Path targetDir = dataDir.resolve(type.getDataSubfolder());
        switch (type) {
            case METADATA_SOURCE -> populateSourceFiles(targetDir, records, stats);
            case FULLTEXT_ALTO -> populateRecordFolders(type, targetDir, records, config.getAltoFolder(), stats);
            case FULLTEXT_TEXT -> populateRecordFolders(type, targetDir, records, config.getFulltextFolder(), stats);
            case FULLTEXT_TEI -> populateRecordFolders(type, targetDir, records, config.getTeiFolder(), stats);
            case IMAGES -> populateRecordFolders(type, targetDir, records, config.getMediaFolder(), stats);
            case EXPORT_CSV -> populateCsv(targetDir, collectionName, query, stats);
            case EXPORT_RIS -> populateXslt(type, targetDir, collectionName, query, "ris", DEFAULT_RIS_XSLT, "ris", stats);
            case EXPORT_BIBTEX -> populateXslt(type, targetDir, collectionName, query, "bibtex", DEFAULT_BIBTEX_XSLT, "bib", stats);
            default -> logger.warn("Unhandled archive content type: {}", type);
        }
    }

    private void populateSourceFiles(Path targetDir, List<SolrDocument> records, PopulationStats stats) throws IOException {
        for (SolrDocument doc : records) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (StringUtils.isBlank(pi)) {
                continue;
            }
            String format = (String) doc.getFieldValue(SolrConstants.SOURCEDOCFORMAT);
            String dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
            String filePath = DataFileTools.getSourceFilePath(FileTools.sanitizeFileName(pi) + ".xml", dataRepository,
                    format != null ? format.toUpperCase() : SolrConstants.SOURCEDOCFORMAT_METS);
            Path source = Paths.get(filePath);
            if (Files.isRegularFile(source)) {
                Files.createDirectories(targetDir);
                Files.copy(source, targetDir.resolve(FileTools.sanitizeFileName(pi) + ".xml"), StandardCopyOption.REPLACE_EXISTING);
                stats.record(ArchiveContentType.METADATA_SOURCE, true);
            } else {
                stats.record(ArchiveContentType.METADATA_SOURCE, false);
                // A record present in the index should always have a metadata source file; its absence is a data-integrity problem.
                logger.warn("Record {} is indexed but its metadata source file is missing (expected at {}).", pi, source);
            }
        }
    }

    private void populateRecordFolders(ArchiveContentType type, Path targetDir, List<SolrDocument> records, String dataFolderName,
            PopulationStats stats) throws IOException {
        for (SolrDocument doc : records) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (StringUtils.isBlank(pi)) {
                continue;
            }
            String dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
            Path recordFolder = DataFileTools.getDataFolder(pi, dataFolderName, dataRepository);
            int copied = 0;
            if (recordFolder != null && Files.isDirectory(recordFolder)) {
                copied = copyFolderContents(recordFolder, targetDir.resolve(FileTools.sanitizeFileName(pi)));
            }
            if (copied > 0) {
                stats.record(type, true);
            } else {
                stats.record(type, false);
                if (contentExpected(type, doc)) {
                    // The index advertises this content, so its absence on disk is a data-integrity problem worth a warning.
                    logger.warn("Record {} is flagged as having {} content, but no '{}' files were found (folder {}).",
                            pi, availabilityLabel(type), dataFolderName, recordFolder);
                } else {
                    logger.debug("No '{}' files for record {} (folder {}).", dataFolderName, pi, recordFolder);
                }
            }
        }
    }

    /**
     * Whether the index advertises that the given content type should be present for a record — {@code BOOL_IMAGEAVAILABLE} for images,
     * {@code FULLTEXTAVAILABLE} for any fulltext flavour. Used to decide whether a missing folder is a warning (index says it exists) or
     * merely a debug note (record legitimately has none).
     */
    private static boolean contentExpected(ArchiveContentType type, SolrDocument doc) {
        return switch (type) {
            case IMAGES -> isTrue(doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE));
            case FULLTEXT_ALTO, FULLTEXT_TEXT, FULLTEXT_TEI -> isTrue(doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE));
            default -> false;
        };
    }

    private static String availabilityLabel(ArchiveContentType type) {
        return type == ArchiveContentType.IMAGES ? "image" : "fulltext";
    }

    /**
     * Coerces a Solr field value to a boolean, tolerating both native {@link Boolean} values and the string {@code "true"}.
     */
    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int copyFolderContents(Path sourceDir, Path targetDir) throws IOException {
        int copied = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (copied == 0) {
                        // Create the record's payload sub-directory lazily so records without files leave no empty folder in the bag.
                        Files.createDirectories(targetDir);
                    }
                    Files.copy(entry, targetDir.resolve(entry.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }
        }
        return copied;
    }

    private void populateCsv(Path targetDir, String collectionName, String query, PopulationStats stats)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, IOException {
        Files.createDirectories(targetDir);
        try (Writer writer = Files.newBufferedWriter(targetDir.resolve(slugify(collectionName) + ".csv"), StandardCharsets.UTF_8)) {
            SearchHelper.exportSearchAsCsv(writer, query, query, null, null, null, null, Locale.ENGLISH, 0);
        }
        stats.record(ArchiveContentType.EXPORT_CSV, true);
    }

    private void populateXslt(ArchiveContentType type, Path targetDir, String collectionName, String query, String formatName,
            String defaultXslt, String extension, PopulationStats stats)
            throws PresentationException, IndexUnreachableException, IOException {
        Optional<ExportFormat> format = config.getSearchExportFormat(formatName);
        String xslt = format.map(ExportFormat::getXslt).filter(StringUtils::isNotBlank).orElse(defaultXslt);
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(query, MAX_RECORDS, null, null);
        try {
            String result = XsltSearchExport.transform(docs, xslt);
            Files.createDirectories(targetDir);
            Files.writeString(targetDir.resolve(slugify(collectionName) + "." + extension), result, StandardCharsets.UTF_8);
            stats.record(type, true);
        } catch (TransformerException | ParserConfigurationException e) {
            // Wrap the XSLT machinery's checked exceptions as IOException so the worker treats any export failure uniformly as a
            // retriable generation error.
            throw new IOException("Failed to generate " + formatName + " export for collection " + collectionName, e);
        }
    }

    // ----- pruning / cleanup -----

    /**
     * Deletes all archive files for a collection.
     *
     * @param field the collection Solr field
     * @param collectionName the collection name
     * @throws IOException on filesystem error
     */
    public void deleteExistingArchives(String field, String collectionName) throws IOException {
        deleteExistingArchives(field, collectionName, null);
    }

    private void deleteExistingArchives(String field, String collectionName, Path keep) throws IOException {
        String slug = slugify(collectionName);
        Path folder = getFieldFolder(field);
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, slug + "__n*__u*.zip")) {
            for (Path path : stream) {
                if (matchesSlug(path.getFileName().toString(), slug) && (keep == null || !path.equals(keep))) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    /**
     * Removes archive files under a field's folder whose collection slug is not present in {@code liveSlugs} (i.e. the collection no longer
     * exists in the index or is no longer configured for archiving).
     *
     * @param field the collection Solr field
     * @param liveSlugs the set of slugs for collections that should retain their archives
     * @throws IOException on filesystem error
     */
    public void pruneOrphans(String field, Set<String> liveSlugs) throws IOException {
        Path folder = getFieldFolder(field);
        if (!Files.isDirectory(folder)) {
            return;
        }
        List<Path> toDelete = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.zip")) {
            for (Path path : stream) {
                Matcher m = FILE_NAME_PATTERN.matcher(path.getFileName().toString());
                if (m.matches() && !liveSlugs.contains(m.group(1))) {
                    toDelete.add(path);
                }
            }
        }
        for (Path path : toDelete) {
            Files.deleteIfExists(path);
            logger.info("Pruned orphaned collection archive {}", path);
        }
    }

    private static void moveInto(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Temp and storage folders may be on different filesystems; fall back to a non-atomic replace.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
