package org.datatransferproject.transfer.wordpress.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WordpressPostStatusType {
    @JsonProperty("publish")
    PUBLISH("publish"),
    @JsonProperty("draft")
    DRAFT("draft"),
    @JsonProperty("private")
    PRIVATE("private"),
    @JsonProperty("pending")
    PENDING("pending"),
    @JsonProperty("future")
    FUTURE("future"),
    @JsonProperty("auto-draft")
    AUTODRAFT("auto-draft");

    public String getStatus() {
        return status;
    }

    private String status;

    WordpressPostStatusType(String status) {
        this.status = status;
    }
}
