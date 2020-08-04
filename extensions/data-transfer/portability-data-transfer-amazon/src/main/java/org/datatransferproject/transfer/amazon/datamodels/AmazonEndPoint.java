package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AmazonEndPoint {

    @JsonProperty
    private boolean customerExists;

    @JsonProperty
    private String contentUrl;

    @JsonProperty
    private String metadataUrl;

    public boolean isCustomerExists() {
        return customerExists;
    }

    public void setCustomerExists(boolean customerExists) {
        this.customerExists = customerExists;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }
}
