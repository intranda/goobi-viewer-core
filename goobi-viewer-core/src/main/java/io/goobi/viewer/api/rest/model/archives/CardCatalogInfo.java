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
package io.goobi.viewer.api.rest.model.archives;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap response of the card catalog API: everything the card catalog view needs to render its
 * entry points and configure its navigation.
 */
public class CardCatalogInfo {

    /** Sidebar mode: letter entry points only. */
    public static final String MODE_LETTERS = "LETTERS";
    /** Sidebar mode: computed from-to register available. */
    public static final String MODE_REGISTER = "REGISTER";

    private String pi;
    private String label;
    private int totalEntries;
    private String mode;
    private Config config = new Config();
    private List<Drawer> drawers = new ArrayList<>();
    private List<Link> links = new ArrayList<>();

    public static class Config {

        private int zoomFactor;
        private int listPageSize;
        private int fullViewThreshold;
        private int registerThreshold;

        public int getZoomFactor() {
            return zoomFactor;
        }

        public void setZoomFactor(int zoomFactor) {
            this.zoomFactor = zoomFactor;
        }

        public int getListPageSize() {
            return listPageSize;
        }

        public void setListPageSize(int listPageSize) {
            this.listPageSize = listPageSize;
        }

        public int getFullViewThreshold() {
            return fullViewThreshold;
        }

        public void setFullViewThreshold(int fullViewThreshold) {
            this.fullViewThreshold = fullViewThreshold;
        }

        public int getRegisterThreshold() {
            return registerThreshold;
        }

        public void setRegisterThreshold(int registerThreshold) {
            this.registerThreshold = registerThreshold;
        }
    }

    public static class Drawer {

        private String id;
        private String label;
        private int entryCount;

        public Drawer() {
        }

        public Drawer(String id, String label, int entryCount) {
            this.id = id;
            this.label = label;
            this.entryCount = entryCount;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public int getEntryCount() {
            return entryCount;
        }
    }

    public static class Link {

        private String label;
        private String url;

        public Link() {
        }

        public Link(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() {
            return label;
        }

        public String getUrl() {
            return url;
        }
    }

    public String getPi() {
        return pi;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Config getConfig() {
        return config;
    }

    public List<Drawer> getDrawers() {
        return drawers;
    }

    public List<Link> getLinks() {
        return links;
    }
}
