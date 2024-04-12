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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProgressInputStream extends FilterInputStream {

    private AtomicLong bytesRead = new AtomicLong(0);
    private final long totalBytes;
    private final Consumer<Progress> callback;
    private final Supplier<Progress> monitor;

    public ProgressInputStream(InputStream in, long streamLength, Optional<Consumer<Progress>> callback) {
        super(in);
        this.totalBytes = streamLength;
        this.callback = callback.orElse(l -> {
        }); //noop consumer as fallback
        this.monitor = () -> new Progress(this.bytesRead.get(), this.totalBytes);
    }

    @Override
    public int read() throws IOException {
        this.callback.accept(new Progress(bytesRead.incrementAndGet(), this.totalBytes));
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        this.callback.accept(new Progress(bytesRead.addAndGet(b.length), this.totalBytes));
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.callback.accept(new Progress(bytesRead.addAndGet(b.length), this.totalBytes));
        return super.read(b, off, len);
    }

    public Supplier<Progress> getMonitor() {
        return this.monitor;
    }

}
