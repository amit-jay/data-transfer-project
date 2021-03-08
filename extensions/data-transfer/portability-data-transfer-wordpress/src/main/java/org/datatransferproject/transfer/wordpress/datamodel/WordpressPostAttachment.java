package org.datatransferproject.transfer.wordpress.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WordpressPostAttachment {
    @JsonProperty("ID")
    private int attachmentId;

    @JsonProperty("videopress_guid")
    private String videoAttachmentGuid;

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getVideoAttachmentGuid() {
        return videoAttachmentGuid;
    }

    public void setVideoAttachmentGuid(String videoAttachmentGuid) {
        this.videoAttachmentGuid = videoAttachmentGuid;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @JsonProperty("mime_type")
    private String attachmentType;

    @JsonProperty("extension")
    private String extension;
}
