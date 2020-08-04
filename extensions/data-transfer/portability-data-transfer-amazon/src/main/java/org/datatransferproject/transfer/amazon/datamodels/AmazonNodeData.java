package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AmazonNodeData {

    @JsonProperty
    private List<AmazonNode> data;

    @JsonProperty
    private int count;

    public List<AmazonNode> getData() {
        return data;
    }

    public void setData(List<AmazonNode> data) {
        this.data = data;
    }

    public int getCount() {
        return count;
    }


}
