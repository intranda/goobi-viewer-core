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
package io.goobi.viewer.model.job.download;

import java.awt.Dimension;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import io.goobi.viewer.controller.DataManager;

/**
 * Download option configuration item.
 */
public class DownloadOption {

    /**
     * 
     */
    public static final String TIMES_SYMBOL = "\u00D7";
    /**
     * Dimension symbolizing the maximal image size
     */
    public static final Dimension MAX = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    /**
     * Dimension symbolizing that no image size has been set
     */
    public static final Dimension NONE = new Dimension(0, 0);

    private String label;
    private String format;
    private Dimension boxSize = NONE;

    /**
     * 
     */
    public DownloadOption() {
    }

    public DownloadOption(String label, String format, String boxSize) {
        this.label = label;
        this.format = format;
        setBoxSizeInPixel(boxSize);
    }

    public DownloadOption(String label, String format, Dimension boxSize) {
        this.label = label;
        this.format = format;
        this.boxSize = boxSize != null ? boxSize : NONE;
    }

    /**
     * 
     * @return true if all properties are set; false otherwise
     */
    public boolean isValid() {
        return StringUtils.isNotEmpty(label) && StringUtils.isNotEmpty(format) && boxSize != NONE;
    }

    /**
     * Retrieves the <code>DownloadOption</code> with the given label from configuration.
     * 
     * @param label Label of the <code>DownloadOption</code> to find
     * @return <code>DownloadOption</code> that matches label; null if none found
     */
    public static DownloadOption getByLabel(String label) {
        if (label == null) {
            return null;
        }

        List<DownloadOption> options = DataManager.getInstance().getConfiguration().getSidebarWidgetUsagePageDownloadOptions();
        if (options == null) {
            return null;
        }
        for (DownloadOption option : options) {
            if (label.equals(option.getLabel())) {
                return option;
            }
        }

        return null;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     * @return this
     */
    public DownloadOption setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     * @return this
     */
    public DownloadOption setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * @return the boxSizeInPixel
     */
    public Dimension getBoxSizeInPixel() {
        return boxSize;
    }

    public String getExtension() {
        return ImageFileFormat.getImageFileFormatFromFileExtension(getFormat()).getFileExtension();
    }

    /**
     * @param boxSizeInPixel the boxSizeInPixel to set
     * @return this
     */
    public DownloadOption setBoxSizeInPixel(String boxSizeInPixel) {
        if (StringUtils.isBlank(boxSizeInPixel)) {
            this.boxSize = NONE;
        } else if ("max".equalsIgnoreCase(boxSizeInPixel)) {
            this.boxSize = MAX;
        } else if (boxSizeInPixel.matches("\\d+")) {
            int size = Integer.parseInt(boxSizeInPixel);
            this.boxSize = new Dimension(size, size);
        } else {
            throw new IllegalArgumentException("Not a valid box size: " + boxSizeInPixel);
        }
        return this;
    }

    public String getBoxSizeLabel() {
        if (boxSize == MAX) {
            return "max";
        } else if (boxSize != NONE) {
            return boxSize.width + TIMES_SYMBOL + boxSize.height;
        }
        return "";
    }

    @Override
    public String toString() {
        return label + " (" + format + ")";
    }
}
