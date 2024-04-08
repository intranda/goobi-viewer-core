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
package io.goobi.viewer.model.files.external;

public class Progress {

    private final long totalSize;
    private final long progress;

    public Progress(long progress, long totalSize) {
        this.totalSize = totalSize;
        this.progress = progress;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getProgressAbsolute() {
        return this.progress;
    }

    public double getProgressRelative() {
        return this.progress / (double) this.totalSize;
    }

    public int getProgressPercentage() {
        return (int) (100 * progress / this.totalSize);
    }

    public boolean started() {
        return this.progress > 0;
    }

    public boolean complete() {
        return this.progress >= this.totalSize;
    }

    @Override
    public String toString() {
        return String.format("Progress: %s/%s", this.progress, this.totalSize);
    }

}
