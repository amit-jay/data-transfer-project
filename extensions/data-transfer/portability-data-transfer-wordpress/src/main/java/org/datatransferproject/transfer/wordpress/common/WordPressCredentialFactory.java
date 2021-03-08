// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.transfer.wordpress.common;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import java.io.IOException;
import org.datatransferproject.types.transfer.auth.*;

public class WordPressCredentialFactory {
  private static final long EXPIRE_TIME_IN_SECONDS = 0L;
  private final HttpTransport httpTransport;
  private final JsonFactory jsonFactory;
  private final AppCredentials appCredentials;

  public WordPressCredentialFactory(
          HttpTransport httpTransport, JsonFactory jsonFactory, AppCredentials appCredentials) {
    this.httpTransport = httpTransport;
    this.jsonFactory = jsonFactory;
    this.appCredentials = appCredentials;
  }

  public HttpTransport getHttpTransport() {
    return httpTransport;
  }

  public JsonFactory getJsonFactory() {
    return jsonFactory;
  }

  /**
   * Creates a {@link Credential} objects with the given {@link TokensAndUrlAuthData} which supports
   * refreshing tokens.
   */
  public Credential createCredential(TokensAndUrlAuthData authData, String tokenServerUrl) {

    // Wordpress OAuth response does not send refresh token. So set it to null.
    return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setClientAuthentication(
                    new ClientParametersAuthentication(appCredentials.getKey(), appCredentials.getSecret()))
            .setTokenServerEncodedUrl(tokenServerUrl)
            .build()
            .setAccessToken(authData.getAccessToken())
            .setRefreshToken(null)
            .setExpiresInSeconds(EXPIRE_TIME_IN_SECONDS);
  }

  /** Refreshes and updates the given credential */
  public Credential refreshCredential(Credential credential) throws IOException {
    TokenResponse tokenResponse =
            new RefreshTokenRequest(
                    httpTransport,
                    jsonFactory,
                    new GenericUrl(credential.getTokenServerEncodedUrl()),
                    credential.getRefreshToken())
                    .setClientAuthentication(credential.getClientAuthentication())
                    .setRequestInitializer(credential.getRequestInitializer())
                    .execute();

    return credential.setFromTokenResponse(tokenResponse);
  }
}
