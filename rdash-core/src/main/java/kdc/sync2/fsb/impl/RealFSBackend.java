package kdc.sync2.fsb.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import kdc.sync2.fsb.FSBackend;
import kdc.sync2.fsb.TimeRWFSBackend;

public class RealFSBackend extends TimeRWFSBackend {
    public String prefix;

    public RealFSBackend(String pfx) {
        prefix = pfx;
    }

    @Override
    public String toString() {
        return "real:" + prefix;
    }

    public File asFile(String fileName) {
        return new File(prefix + fileName);
    }

    @Override
    public XState getState(String fileName) {
        File f = asFile(fileName);
        if (!f.exists())
            return null;
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            String[] ents = new String[list.length];
            for (int i = 0; i < ents.length; i++)
                ents[i] = list[i].getName();
            return new DirectoryState(ents);
        }
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
    public void delete(String fileName) {
        File fn = asFile(fileName);
        fn.delete();
        if (fn.exists())
            throw new RuntimeException("Failed to delete file.");
    }

    @Override
    public void mkdir(String fileName) {
        File fn = asFile(fileName);
        fn.mkdir();
        if (!fn.isDirectory())
            throw new RuntimeException("Failed to create directory.");
    }

    @Override
    public void changeTime(String fileName, long time) {
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
