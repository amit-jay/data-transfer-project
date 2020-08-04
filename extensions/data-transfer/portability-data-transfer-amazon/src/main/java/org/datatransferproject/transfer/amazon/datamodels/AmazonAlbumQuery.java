package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class AmazonAlbumQuery {

    @JsonProperty
    private List<AmazonAlbumQueryInclude> include;

    public List<AmazonAlbumQueryInclude> getInclude() {
        return include;
    }

    public void setInclude(List<AmazonAlbumQueryInclude> include) {
        this.include = include;
    }

}
