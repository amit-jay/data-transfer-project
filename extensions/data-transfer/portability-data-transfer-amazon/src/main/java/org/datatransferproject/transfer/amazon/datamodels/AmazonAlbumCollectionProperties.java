package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class AmazonAlbumCollectionProperties {

    @JsonProperty
    private boolean areCoversDesired;

    @JsonProperty
    private List<AmazonAlbumCoverPhoto> covers;

    @JsonProperty
    private AmazonAlbumQuery query;

    public boolean isAreCoversDesired() {
        return areCoversDesired;
    }

    public void setAreCoversDesired(boolean areCoversDesired) {
        this.areCoversDesired = areCoversDesired;
    }

    public List<AmazonAlbumCoverPhoto> getCovers() {
        return covers;
    }

    public void setCovers(List<AmazonAlbumCoverPhoto> covers) {
        this.covers = covers;
    }

    public AmazonAlbumQuery getQuery() {
        return query;
    }

    public void setQuery(AmazonAlbumQuery query) {
        this.query = query;
    }


}
