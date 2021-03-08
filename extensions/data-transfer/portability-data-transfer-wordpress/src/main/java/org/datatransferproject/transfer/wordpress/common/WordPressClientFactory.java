// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.transfer.wordpress.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** A factory for WordPressClient instances. */
public class WordPressClientFactory {
  private final String serviceId;
  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final Monitor monitor;
  private final WordPressCredentialFactory credentialFactory;

  public WordPressClientFactory(
          String serviceId,
          OkHttpClient client,
          ObjectMapper objectMapper,
          Monitor monitor,
          WordPressCredentialFactory credentialFactory) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.serviceId = serviceId;
  }

  public WordPressClient create(TokensAndUrlAuthData authData) {
    WordPressClient wordPressClient =
            new WordPressClient(serviceId, client, objectMapper, monitor, credentialFactory);

    // Ensure credential is populated
    wordPressClient.getOrCreateCredential(authData);

    return wordPressClient;
  }
}
