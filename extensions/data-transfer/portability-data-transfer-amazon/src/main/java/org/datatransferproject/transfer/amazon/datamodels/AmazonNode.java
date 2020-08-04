package org.datatransferproject.transfer.amazon.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AmazonNode {

    @JsonProperty
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private AmazonNodeType kind;

    @JsonProperty
    private List<String> parents;

    @JsonProperty
    private boolean isRoot;

    @JsonProperty
    private String resourceVersion;

    @JsonProperty
    private AmazonAlbumCollectionProperties collectionProperties;

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public AmazonAlbumCollectionProperties getCollectionProperties() {
        return collectionProperties;
    }

    public void setCollectionProperties(AmazonAlbumCollectionProperties collectionProperties) {
        this.collectionProperties = collectionProperties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AmazonNodeType getKind() {
        return kind;
    }

    public void setKind(AmazonNodeType kind) {
        this.kind = kind;
    }

    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

}
