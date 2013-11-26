package net.qldarch.ingest.archive;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArchiveFiles implements Iterable<ArchiveFile> {
    private List<ArchiveFile> afs;

    public ArchiveFiles() {
        afs = new ArrayList<ArchiveFile>();
    }

    public ArchiveFile getByMimeType(String mimetype) throws ArchiveFileNotFoundException {
        for (ArchiveFile af : afs) {
            if (af.mimetype.equals(mimetype)) {
                return af;
            }
        }
        throw new ArchiveFileNotFoundException("ArchiveFile matching " + mimetype + " not found");
    } 

    public Optional<ArchiveFile> firstByMimeType(String mimetype) throws ArchiveFileNotFoundException {
        for (ArchiveFile af : afs) {
            if (af.mimetype.equals(mimetype)) {
                return Optional.of(af);
            }
        }
        return Optional.absent();
    } 

    public void add(ArchiveFile af) {
        afs.add(af);
    }

    public Iterator<ArchiveFile> iterator() {
        return afs.iterator();
    }

    public ArchiveFile getFirst() throws ArchiveFileNotFoundException {
        if (afs.size() == 0) {
            throw new ArchiveFileNotFoundException("No ArchiveFile available");
        }
        return afs.get(0);
    }

    public String toString() {
        return afs.toString();
    }
}
