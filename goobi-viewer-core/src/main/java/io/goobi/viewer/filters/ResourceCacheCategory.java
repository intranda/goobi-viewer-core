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
package io.goobi.viewer.filters;

import java.util.Locale;
import java.util.Set;

/**
 * Cache policy category of a single HTTP request, derived from its servlet path.
 *
 * <p>The category decides which {@code Cache-Control} header the viewer emits. Classification is
 * path based rather than extension based: the same file extension can appear below paths with
 * entirely different protection needs, and {@code /resources} carries markup as well as assets.
 */
public enum ResourceCacheCategory {

    /** Content addressable assets below the whitelisted directories. */
    STATIC,
    /** Ordinary viewer pages. */
    DYNAMIC,
    /** Pages bound to a user account; these must never be written to disk. */
    ACCOUNT,
    /** REST API; the JAX-RS response filter owns these headers. */
    API,
    /** JSF resources; Mojarra owns these headers. */
    SKIP;

    private static final String JSF_RESOURCE_PREFIX = "/jakarta.faces.resource";
    private static final String RESOURCES_PREFIX = "/resources/";
    private static final String THEMES_PREFIX = "/resources/themes/";
    private static final String PARENT_DIRECTORY_SEGMENT = "..";

    /** Directories below /resources that hold assets only; verified to contain no markup. */
    private static final Set<String> ASSET_DIRECTORIES =
            Set.of("css", "javascript", "images", "icons", "fonts", "xsl", "opensearch");

    /** URL prefixes whose pages are bound to a user account. */
    private static final Set<String> ACCOUNT_PREFIXES = Set.of("/admin", "/user", "/campaigns", "/bookmarks", "/myactivity");

    /**
     * Additional account bound prefix that, unlike {@link #ACCOUNT_PREFIXES}, is matched without a
     * path boundary, mirroring {@code LoginFilter#isRestrictedUri(String)}: any path starting with
     * this prefix, not just one followed by a slash, is account bound.
     */
    private static final String ACCOUNT_PREFIX_CROWD = "/crowd";

    /** View directories below /resources that back the account bound prefixes. */
    private static final Set<String> ACCOUNT_RESOURCE_DIRECTORIES = Set.of("admin", "crowdsourcing");

    /**
     * Returns the cache category for the given servlet path.
     *
     * <p>The argument is the path of the current dispatch target without the context path, i.e.
     * {@code /resources/css/x.css}, not {@code /viewer/resources/css/x.css}.
     *
     * @param servletPath path to classify; may be null
     * @return matching category, never null
     * @should classify asset directories as static
     * @should classify view templates below resources as account or dynamic
     * @should never classify markup inside the asset whitelist as static
     * @should classify jsf resources as skip
     * @should classify api paths as api regardless of file extension
     * @should not classify lookalike paths as api
     * @should classify account bound prefixes as account
     * @should ignore path parameters and tolerate percent encoded characters
     * @should treat null and empty path as dynamic
     * @should classify ordinary viewer pages as dynamic
     * @should not throw on malformed percent encoded characters
     * @should treat plus signs as literal characters
     * @should never classify paths containing a parent directory segment as static
     * @should treat markup extensions case insensitively
     * @should not classify lookalike paths as account
     * @should classify incomplete theme paths as dynamic
     */
    public static ResourceCacheCategory classify(String servletPath) {
        if (servletPath == null || servletPath.isEmpty()) {
            return DYNAMIC;
        }
        String path = normalize(servletPath);

        if (path.startsWith(JSF_RESOURCE_PREFIX)) {
            return SKIP;
        }
        if (isPrefix(path, "/api") || isPrefix(path, "/rest")) {
            return API;
        }
        if (isAccountPath(path)) {
            return ACCOUNT;
        }
        if (isWhitelistedAsset(path)) {
            return STATIC;
        }
        return DYNAMIC;
    }

    /**
     * Returns the cache category for the given dispatch, checking the original request uri for an
     * account bound prefix before falling back to {@link #classify(String)}.
     *
     * <p>The pretty url rewrite filter forwards a request such as {@code /user/searches/} to its
     * backing view id, {@code /userBackendSearches.xhtml}, before this class ever sees the servlet
     * path. None of the account bound prefixes match that view id, so {@link #classify(String)}
     * alone would misclassify the page as {@code DYNAMIC}. The original, pre-forward uri still
     * carries the pretty url and is checked first; only a path that is not account bound there
     * falls through to the servlet path check, exactly as {@link #classify(String)} performs it on
     * its own.
     *
     * @param servletPath path of the current dispatch target; may be null
     * @param originalPath request uri before any forward rewrote it to the servlet path, without
     *            the context path; may be null
     * @return matching category, never null
     * @should classify account bound original paths as account regardless of the servlet path
     * @should fall back to the servlet path when the original path is not account bound
     */
    public static ResourceCacheCategory classify(String servletPath, String originalPath) {
        if (originalPath != null && !originalPath.isEmpty() && isAccountPath(normalize(originalPath))) {
            return ACCOUNT;
        }
        return classify(servletPath);
    }

    /**
     * Strips path parameters such as {@code ;jsessionid}.
     *
     * <p>The servlet container has already percent-decoded the path by the time it reaches a
     * filter; decoding it a second time here would turn a literal {@code +} into a space and could
     * turn an encoded path separator into a real one, both of which would undermine the
     * classification below.
     */
    private static String normalize(String servletPath) {
        String path = servletPath;
        int semicolon = path.indexOf(';');
        if (semicolon >= 0) {
            path = path.substring(0, semicolon);
        }
        return path;
    }

    /** Matches a prefix only at a path boundary, so that /apidocs does not count as /api. */
    private static boolean isPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static boolean isAccountPath(String path) {
        for (String prefix : ACCOUNT_PREFIXES) {
            if (isPrefix(path, prefix)) {
                return true;
            }
        }
        if (path.startsWith(ACCOUNT_PREFIX_CROWD)) {
            return true;
        }
        return ACCOUNT_RESOURCE_DIRECTORIES.contains(firstSegmentBelowResources(path));
    }

    /**
     * Returns true for paths below a whitelisted asset directory.
     *
     * <p>Markup is excluded even inside those directories: a facelet served from an asset path is
     * still a page, and caching it publicly would hand it to shared caches. A path carrying a
     * parent directory segment is excluded as well; a container is expected to resolve those
     * before dispatch, but a path that reaches this method unresolved must not be treated as a
     * fixed, whitelisted asset path.
     */
    private static boolean isWhitelistedAsset(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);
        if (lowerCasePath.endsWith(".xhtml") || lowerCasePath.endsWith(".html")) {
            return false;
        }
        if (containsParentDirectorySegment(path)) {
            return false;
        }
        return ASSET_DIRECTORIES.contains(firstSegmentBelowResources(path));
    }

    /** Returns true if any path segment is a parent directory reference. */
    private static boolean containsParentDirectorySegment(String path) {
        for (String segment : path.split("/")) {
            if (PARENT_DIRECTORY_SEGMENT.equals(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the directory segment that decides the category, or an empty string.
     *
     * <p>For theme paths the deciding segment sits one level deeper, because the theme name comes
     * first: {@code /resources/themes/<theme>/css/x.css}.
     */
    private static String firstSegmentBelowResources(String path) {
        String remainder;
        if (path.startsWith(THEMES_PREFIX)) {
            String afterThemes = path.substring(THEMES_PREFIX.length());
            int themeEnd = afterThemes.indexOf('/');
            if (themeEnd < 0) {
                return "";
            }
            remainder = afterThemes.substring(themeEnd + 1);
        } else if (path.startsWith(RESOURCES_PREFIX)) {
            remainder = path.substring(RESOURCES_PREFIX.length());
        } else {
            return "";
        }
        int end = remainder.indexOf('/');
        return end < 0 ? "" : remainder.substring(0, end);
    }
}
