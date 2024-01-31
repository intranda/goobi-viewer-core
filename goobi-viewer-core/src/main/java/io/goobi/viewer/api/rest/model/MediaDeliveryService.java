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
package io.goobi.viewer.api.rest.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;

/**
 * <p>
 * MediaDeliveryService class.
 * </p>
 *
 * @author Florian Alpers
 */
public class MediaDeliveryService {

    private static final String CONTENT_RANGE_HEADER = "Content-Range";
    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

    /**
     * Process the actual request.
     *
     * @param request The request to be processed.
     * @param response The response to be created.
     * @param filePath a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void processRequest(HttpServletRequest request, HttpServletResponse response, String filePath, String mimeType) throws IOException {

        if (filePath == null || mimeType == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = Paths.get(filePath);

        // Check if file actually exists in filesystem.
        if (!Files.exists(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare some variables. The ETag is an unique identifier of the file.
        String fileName = file.getFileName().toString();
        long length = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        String eTag = fileName + "_" + length + "_" + lastModified;

        // Validate request headers for caching ---------------------------------------------------

        Optional<Integer> cachingResponseCode = getCachingResponse(request, lastModified, eTag);
        if (cachingResponseCode.isPresent()) {
            Integer code = cachingResponseCode.get();
            if (code.equals(HttpServletResponse.SC_NOT_MODIFIED)) {
                response.setHeader("ETag", eTag); // Required in 304.
            }
            response.sendError(code);
            return;
        }

        // Validate and process range -------------------------------------------------------------
        List<Section> sections;
        try {
            sections = getSections(request, length, lastModified, eTag);
        } catch (IllegalRequestException e) {
            response.setHeader(CONTENT_RANGE_HEADER, "bytes */" + length); // Required in 416.
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = StringUtils.isNotBlank(mimeType) ? mimeType : "application/octet-stream";
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the browser and expand content type with the one and right character encoding.
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        }
        // Else, expect for images, determine content disposition. If content type is supported by
        // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
        else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }
        if (acceptsGzip) {
            response.setHeader("Content-Encoding", "gzip");
        }

        // Initialize response.
        initResponse(response, fileName, lastModified, eTag, disposition);

        // Send requested file (part(s)) to client ------------------------------------------------

        try (RandomAccessFile raf = new RandomAccessFile(file.toString(), "r"); FileChannel input = raf.getChannel();
                OutputStream out = acceptsGzip ? new GZIPOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE) : response.getOutputStream();
                WritableByteChannel output = Channels.newChannel(response.getOutputStream())) {
            // Open streams.

            if (sections.isEmpty()) {

                // Return full file.
                Section sec = new Section(length);
                response.setContentType(contentType);
                response.setHeader(CONTENT_RANGE_HEADER, "bytes " + sec.start + "-" + sec.end + "/" + sec.total);

                // Content length is not directly predictable in case of GZIP.
                // So only add it if there is no means of GZIP, else browser will hang.
                if (!acceptsGzip) {
                    response.setHeader("Content-Length", String.valueOf(sec.length));
                }

                // Copy full range.
                copy(input, output, sec);

            } else if (sections.size() == 1) {

                // Return single part of file.
                Section sec = sections.get(0);
                response.setContentType(contentType);
                response.setHeader(CONTENT_RANGE_HEADER, "bytes " + sec.start + "-" + sec.end + "/" + sec.total);
                response.setHeader("Content-Length", String.valueOf(sec.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
                copy(input, output, sec);

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = response.getOutputStream();

                // Copy multi part range.
                for (Section sec : sections) {
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println("Content-Type: " + contentType);
                    sos.println("Content-Range: bytes " + sec.start + "-" + sec.end + "/" + sec.total);

                    // Copy single part range of multi part range.
                    copy(input, output, sec);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        }
    }

    /**
     * @param input
     * @param output
     * @param sec
     * @throws IOException
     */
    private static void copy(FileChannel input, WritableByteChannel output, Section sec) throws IOException {
        input.transferTo(sec.start, sec.length, output);
    }

    /**
     * @param response
     * @param fileName
     * @param lastModified
     * @param eTag
     * @param disposition
     */
    private static void initResponse(HttpServletResponse response, String fileName, long lastModified, String eTag, String disposition) {
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);
    }

    /**
     * @param request
     * @param length
     * @param lastModified
     * @param eTag
     * @return List<Section>
     * @throws IOException
     * @throws IllegalRequestException
     */
    private static List<Section> getSections(HttpServletRequest request, long length, long lastModified, String eTag)
            throws IllegalRequestException {
        // Prepare some variables. The full Section represents the complete file.
        List<Section> sections = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n..." or "bytes=n-". If not, then return 416.
            if (!matchesRangeHeaderPattern(range)) {
                throw new IllegalRequestException("Range has wrong syntax: " + range);
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        return sections;
                    }
                } catch (IllegalArgumentException ignore) {
                    return sections;
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (sections.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        throw new IllegalRequestException("End of range lies before start");
                    }

                    // Add range.
                    sections.add(new Section(start, end, length));
                }
            }
        }
        return sections;
    }

    /**
     * 
     * @param range
     * @return true if range matches pattern; false otherwise
     */
    protected static boolean matchesRangeHeaderPattern(String range) {
        if (range.matches("bytes=.+")) {
            String rangeParts = range.substring(6);
            String[] parts = rangeParts.split(",\\s*");
            if (parts.length > 0) {
                for (String part : parts) {
                    if (!part.matches("\\d+-|-\\d+|\\d+-\\d+")) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Returns a status code for a response indicating cached content If the return value is empty, the no caching can be achieved and the request
     * needs to continue
     *
     * @param request
     * @param lastModified
     * @param eTag
     * @return Optional<Integer>
     * @throws IOException
     */
    private static Optional<Integer> getCachingResponse(HttpServletRequest request, long lastModified, String eTag) {
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            return Optional.of(HttpServletResponse.SC_NOT_MODIFIED);
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            return Optional.of(HttpServletResponse.SC_NOT_MODIFIED);
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            return Optional.of(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            return Optional.of(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        return Optional.empty();
    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split(",");
        //trim surrounding spaces now. If included into split regex, they could potentially lead to 
        for (int i = 0; i < matchValues.length; i++) {
            matchValues[i] = matchValues[i].trim();
        }
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end index as a long. If the substring is empty, then -1
     * will be returned
     *
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("[,;]");
        //trim surrounding spaces now. If included into split regex, they could potentially lead to 
        for (int i = 0; i < acceptValues.length; i++) {
            acceptValues[i] = acceptValues[i].trim();
        }
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1 || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * A section within a byte array
     */
    private static class Section {
        private long start;
        private long end;
        private long length;
        private long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Section(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        /**
         * Creates a "full" section spanning the entire content
         *
         * @param total
         */
        public Section(long total) {
            this.start = 0;
            this.end = total - 1;
            this.length = total;
            this.total = total;
        }

        public boolean isFull() {
            return this.length == this.total;
        }

        /**
         * @return the start
         */
        public long getStart() {
            return start;
        }

        /**
         * @param start the start to set
         */
        public void setStart(long start) {
            this.start = start;
        }

        /**
         * @return the end
         */
        public long getEnd() {
            return end;
        }

        /**
         * @param end the end to set
         */
        public void setEnd(long end) {
            this.end = end;
        }

        /**
         * @return the length
         */
        public long getLength() {
            return length;
        }

        /**
         * @param length the length to set
         */
        public void setLength(long length) {
            this.length = length;
        }

        /**
         * @return the total
         */
        public long getTotal() {
            return total;
        }

        /**
         * @param total the total to set
         */
        public void setTotal(long total) {
            this.total = total;
        }

        
    }

}
