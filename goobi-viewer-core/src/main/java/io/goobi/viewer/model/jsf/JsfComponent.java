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
package io.goobi.viewer.model.jsf;

import java.io.Serializable;

public class JsfComponent implements Serializable {

    private static final long serialVersionUID = 2205896851743823811L;

    private final String library;
    private final String name;

    public JsfComponent(String library, String name) {
        this.library = library;
        this.name = name;
    }

    public String getLibrary() {
        return library;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        if (name.matches("(i?)[^.]+\\.x?html")) {
            return name;
        }
        return name + ".xhtml";
    }

    @Override
    public String toString() {
        return this.library + "/" + this.getFilename();
    }
}
