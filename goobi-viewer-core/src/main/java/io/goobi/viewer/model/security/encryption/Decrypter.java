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
package io.goobi.viewer.model.security.encryption;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enrich strings with sensitive data which is in some way encrypted or stored as environment variable by the system. Currently only replacing Strings
 * with environment variables is suppoerted. To include the value "VALUE" of a variable "VARIABLE" in a string, the input must be passed to the static
 * method {@link #decrypt(String)} as
 * <code>Some Text $SYS(VARIABLE) and some more</code>
 * which returns
 * <code>Some Text VALUE and some more</code>
 */
public final class Decrypter {

    private static final String REGEX_SYSENV = "\\$SYS\\((\\w+)\\)";

    private Decrypter() {

    }

    private static String replaceEnvironmentVariables(String string) {
        Matcher matcher = Pattern.compile(REGEX_SYSENV).matcher(string);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(getEnvironmentVariable(variable)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String getEnvironmentVariable(String variable) {
        String value = System.getenv(variable);
        if (value == null) {
            value = System.getProperty(variable);
        }
        return value == null ? "" : value;
    }

    /**
     * Returns the given string with all encryption variable expressions <code>$SYS(...)</code> replaced by the encrypted value.
     * 
     * @param string the input string
     * @return the input string with replacements
     */
    public static String decrypt(String string) {
        return Decrypter.replaceEnvironmentVariables(string);
    }

}
