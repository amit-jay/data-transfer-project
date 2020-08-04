package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AmazonAlbumCoverPhoto {
    @JsonProperty
    private String id;

    @JsonProperty
    private String ownerId;

    @JsonProperty
    private boolean isDefault;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

}
