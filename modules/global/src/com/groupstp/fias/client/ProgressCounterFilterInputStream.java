package com.groupstp.fias.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressCounterFilterInputStream extends FilterInputStream {
    private long progress = 0;

    public long getProgress() {
        return progress;
    }

    public ProgressCounterFilterInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int red = super.read();
        progress = progress + red;
        return red;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int red = super.read(b);
        progress = progress + red;
        return red;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int red = super.read(b, off, len);
        progress = progress + red;
        return red;
    }

    @Override
    public long skip(long n) throws IOException {
        return super.skip(n);
    }
}
