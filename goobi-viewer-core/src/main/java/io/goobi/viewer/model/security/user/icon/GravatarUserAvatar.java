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
package io.goobi.viewer.model.security.user.icon;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
public class GravatarUserAvatar implements UserAvatar {

    public static final String DEFAULT_GRAVATAR_ICON = "";
    public static final String DEFAULT_HTTP_404 = "404";
    public static final String DEFAULT_MYSTERY_MAN = "mm";
    public static final String DEFAULT_IDENTICON = "identicon";
    public static final String DEFAULT_MONSTERID = "monsterid";
    public static final String DEFAULT_WAVATAR = "wavatar";
    public static final String DEFAULT_RETRO = "retro";
    public static final String DEFAULT_BLANK = "blank";

    private final String email;

    public GravatarUserAvatar(String email) {
        this.email = email;
    }

    @Override
    public String getIconUrl(int size, HttpServletRequest request) {
        return getGravatarUrl(size);
    }

    private String getGravatarUrl(int size) {
        if (StringUtils.isNotEmpty(email)) {
            return "//www.gravatar.com/avatar/" + md5Hex(email) + "?rating=g&size=" + size + "&default=" + DEFAULT_IDENTICON;
        }

        return "//www.gravatar.com/avatar/";
    }

    static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i]
                    & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    static String md5Hex(String message) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }
}
