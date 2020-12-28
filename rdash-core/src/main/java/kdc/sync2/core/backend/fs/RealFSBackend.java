package kdc.sync2.core.backend.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import kdc.sync2.core.backend.FSBackend;
import kdc.sync2.core.backend.TimeRWFSBackend;

public class RealFSBackend extends TimeRWFSBackend {
    public String prefix;

    public RealFSBackend(String pfx) {
        prefix = pfx;
    }

    public File asFile(String fileName) {
        return new File(prefix + fileName);
    }

    @Override
    public XState getState(String fileName) throws IOException {
        File f = asFile(fileName);
        if (!f.exists())
            return null;
        return new FileTimeState(f.length(), f.lastModified());
    }

    @Override
    public InputStream openRead(String fileName) throws IOException {
        return new FileInputStream(asFile(fileName));
    }

    @Override
    public OutputStream openWrite(String fileName) throws IOException {
        return new FileOutputStream(asFile(fileName));
    }

    @Override
    public void delete(String fileName) throws IOException {
        asFile(fileName).delete();
    }

    @Override
    public void changeTime(String fileName, long time) throws IOException {
        asFile(fileName).setLastModified(time);
    }

    @Override
    public void copy(String fileNameSource, FSBackend target, String fileNameTarget) throws IOException {
        if (target instanceof RealFSBackend) {
            // Fast-path
            Files.copy(asFile(fileNameSource).toPath(), ((RealFSBackend) target).asFile(fileNameTarget).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        super.copy(fileNameSource, target, fileNameTarget);
    }
}
