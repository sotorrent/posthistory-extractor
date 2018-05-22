package org.sotorrent.posthistoryextractor.version;

public interface VersionList {
    int getPostId();
    byte getPostTypeId();
    boolean isSorted();
}
