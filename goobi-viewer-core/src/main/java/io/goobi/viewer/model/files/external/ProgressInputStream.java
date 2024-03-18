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
