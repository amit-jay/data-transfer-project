package org.datatransferproject.transfer.wordpress.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WordpressPost {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public WordpressPostStatusType getStatusType() {
        return statusType;
    }

    public void setStatusType(WordpressPostStatusType statusType) {
        this.statusType = statusType;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public HashMap<String, WordpressPostAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(HashMap<String, WordpressPostAttachment> attachments) {
        this.attachments = attachments;
    }

    public List<WordpressMediaError> getMediaErrors() {
        return mediaErrors;
    }

    public void setMediaErrors(List<WordpressMediaError> mediaErrors) {
        this.mediaErrors = mediaErrors;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    @JsonProperty("ID")
    private int id;

    @JsonProperty("site_ID")
    private int siteId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("date")
    private String date;

    @JsonProperty("status")
    private WordpressPostStatusType statusType;

    @JsonProperty("attachment_count")
    private int attachmentCount;

    @JsonProperty("attachments")
    private HashMap<String, WordpressPostAttachment> attachments;

    @JsonProperty("media_errors")
    private List<WordpressMediaError> mediaErrors = new ArrayList<>();

    @JsonProperty("media_urls")
    private List<String> mediaUrls;
}
