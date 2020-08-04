package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AmazonAlbumQueryInclude {
    @JsonProperty
    private List<String> folderIds;

    public List<String> getFolderIds() {
        return folderIds;
    }

    public void setFolderIds(List<String> folderIds) {
        this.folderIds = folderIds;
    }
}
