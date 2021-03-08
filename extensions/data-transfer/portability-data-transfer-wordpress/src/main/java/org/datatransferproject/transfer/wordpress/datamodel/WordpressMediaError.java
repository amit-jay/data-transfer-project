
package org.datatransferproject.transfer.wordpress.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WordpressMediaError {
    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("file")
    private String file;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;
}
