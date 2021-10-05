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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;

/**
 * @author florian
 *
 */
public class GravatarUserAvatar implements UserAvatar {

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
            Gravatar gravatar =
                    new Gravatar().setSize(size).setRating(GravatarRating.GENERAL_AUDIENCES).setDefaultImage(GravatarDefaultImage.IDENTICON);
            String url = gravatar.getUrl(email);
            return url.replace("http:", "");
        }

        return "//www.gravatar.com/avatar/";
    }

}
