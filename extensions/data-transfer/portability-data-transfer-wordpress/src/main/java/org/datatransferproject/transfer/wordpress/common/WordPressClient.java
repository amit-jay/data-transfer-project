// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.transfer.wordpress.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import java.io.IOException;
import java.net.URISyntaxException;
import okhttp3.*;
import org.apache.http.client.utils.URIBuilder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.transfer.wordpress.datamodel.WordpressPost;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class WordPressClient {
    private static final String WORDPRESS_BASE_URL = "https://public-api.wordpress.com/rest/v1.2";
    private static final String TOKEN_SERVER_URL = "https://public-api.wordpress.com/oauth2/token";
    private static final String SITE_NEW_POST_ENDPOINT = "/sites/%s/posts/new/";
    private static final String SITE_EDIT_POST_ENDPOINT = "/sites/%s/posts/%s";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;
    private final WordPressCredentialFactory credentialFactory;
    private Credential credential;
    private final String serviceId;

    public WordPressClient(
            final String serviceId,
            final OkHttpClient client,
            final ObjectMapper objectMapper,
            final Monitor monitor,
            final WordPressCredentialFactory credentialFactory) {
        this.serviceId = serviceId;
        this.client = client;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
        this.credentialFactory = credentialFactory;
        this.credential = null;
    }

    private Request.Builder getRequestBuilder(final String url) {
        final Request.Builder requestBuilder = new Request.Builder().url(url);
        requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
        requestBuilder.header("Content-Type", "application/json");

        return requestBuilder;
    }

    private URIBuilder getUriBuilder(final String endpoint) {
        String url = WORDPRESS_BASE_URL;
        try {
            if (!endpoint.isEmpty()) {
                url = url + endpoint;
            }
            return new URIBuilder(url);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Could not produce url.", e);
        }
    }

    public WordpressPost makeWordPressPostCall(
            String siteId, String blogId, WordpressPost wordpressPost) throws IOException {
        String url;
        String formattedUrl = String.format(SITE_NEW_POST_ENDPOINT, siteId);
        if (blogId != null) {
            formattedUrl = String.format(SITE_EDIT_POST_ENDPOINT, siteId, blogId);
        }
        try {
            url = getUriBuilder(formattedUrl).build().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not produce url.", e);
        }

        String wordpressPostString = objectMapper.writeValueAsString(wordpressPost);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), wordpressPostString);
        Request.Builder requestBuilder = getRequestBuilder(url).post(body);
        Response response = getResponse(requestBuilder);
        ResponseBody responseBody = response.body();
        return responseBody != null
                ? objectMapper.readValue(responseBody.string(), WordpressPost.class)
                : null;
    }

    private Response getResponse(final Request.Builder requestBuilder) throws IOException {
        Response response = client.newCall(requestBuilder.build()).execute();
        return response;
    }


    public Credential getOrCreateCredential(TokensAndUrlAuthData authData) {
        if (this.credential == null) {
            // The token url in authdata is set to the blog id after user authentication.
            // Hence fetch the WordPress token server url via local variable and pass it over to the
            // credential factory to ensure token url is set correctly and is refreshing works as
            // expected.
            this.credential = this.credentialFactory.createCredential(authData, TOKEN_SERVER_URL);
        }
        return this.credential;
    }
}
