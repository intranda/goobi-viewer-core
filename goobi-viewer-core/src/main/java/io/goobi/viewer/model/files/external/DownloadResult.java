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

import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class DownloadResult {

    private final Supplier<Long> progressMonitor;
    private final Future<Path> path;
    private final long size;

    public DownloadResult(Supplier<Long> progressMonitor, Future<Path> resultPath, long size) {
        super();
        this.progressMonitor = progressMonitor;
        this.path = resultPath;
        this.size = size;
    }

    public Supplier<Long> getProgressMonitor() {
        return progressMonitor;
    }

    public Future<Path> getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public long getProgressPercent() {
        return 100 * progressMonitor.get() / size;
    }

}
