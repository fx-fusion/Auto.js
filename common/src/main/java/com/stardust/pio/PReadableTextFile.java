package com.stardust.pio;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stardust on 2017/4/1.
 */

public class PReadableTextFile implements Closeable, PFileInterface {

    @NonNull
    private final FileInputStream mFileInputStream;
    private final int mBufferingSize;
    private final String mEncoding;
    private final String mPath;
    private BufferedReader mBufferedReader;

    public PReadableTextFile(String path) {
        this(path, PFiles.DEFAULT_ENCODING);
    }

    public PReadableTextFile(String path, String encoding) {
        this(path, encoding, -1);
    }

    public PReadableTextFile(String path, String encoding, int bufferingSize) {
        mEncoding = encoding;
        mBufferingSize = bufferingSize;
        mPath = path;
        try {
            mFileInputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }


    private void ensureBufferReader() {
        if (mBufferedReader == null) {
            try {
                if (mBufferingSize == -1)
                    mBufferedReader = new BufferedReader(new InputStreamReader(mFileInputStream, mEncoding));
                else
                    mBufferedReader = new BufferedReader(new InputStreamReader(mFileInputStream, mEncoding), mBufferingSize);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

        }
    }

    @NonNull
    public String read() {
        try {
            byte[] data = new byte[mFileInputStream.available()];
            mFileInputStream.read(data);
            return new String(data, mEncoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NonNull
    public String read(int size) {
        ensureBufferReader();
        try {
            char[] chars = new char[size];
            int len = mBufferedReader.read(chars);
            return new String(chars, 0, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String readline() {
        ensureBufferReader();
        try {
            return mBufferedReader.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NonNull
    public String[] readlines() {
        ensureBufferReader();
        List<String> lines = new ArrayList<>();
        try {
            while (mBufferedReader.ready()) {
                lines.add(mBufferedReader.readLine());
            }
            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (mBufferedReader != null) {
                mBufferedReader.close();
            } else {
                mFileInputStream.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getPath() {
        return mPath;
    }
}
