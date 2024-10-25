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
package io.goobi.viewer.exceptions;

public class ContentTypeException extends PresentationException {

    public ContentTypeException(String string, Object... args) {
        super(string, args);
        // TODO Auto-generated constructor stub
    }

    public ContentTypeException(String string, Throwable e) {
        super(string, e);
        // TODO Auto-generated constructor stub
    }

    public ContentTypeException(String string) {
        super(string);
        // TODO Auto-generated constructor stub
    }

    public ContentTypeException(Throwable e, String string, Object... args) {
        super(e, string, args);
        // TODO Auto-generated constructor stub
    }

}
