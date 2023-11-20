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
    private final Consumer<Long> callback;
    private final Supplier<Long> monitor;
     
    public ProgressInputStream(InputStream in, long streamLength, Optional<Consumer<Long>> callback) {
        super(in);
        this.totalBytes = streamLength;
        this.callback = callback.orElse(l -> {});   //noop consumer as fallback
        this.monitor = () -> this.bytesRead.get();
    }
    
    @Override
    public int read() throws IOException {
        this.callback.accept(bytesRead.incrementAndGet());
        return super.read();
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        this.callback.accept(bytesRead.addAndGet(b.length));
        return super.read(b);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.callback.accept(bytesRead.addAndGet(b.length));
        return super.read(b, off, len);
    }
    
    public Supplier<Long> getMonitor() {
        return this.monitor;
    }

}
