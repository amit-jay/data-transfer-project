// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.transfer.wordpress;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.transfer.wordpress.common.WordPressClientFactory;
import org.datatransferproject.transfer.wordpress.common.WordPressCredentialFactory;
import org.datatransferproject.transfer.wordpress.posts.WordPressPostsImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;

public class WordPressTransferExtension implements TransferExtension {
  private static final List<String> SUPPORTED_TYPES = ImmutableList.of("SOCIAL-POSTS", "NOTES");
  private static final String WORDPRESS_KEY = "WORDPRESS_KEY";
  private static final String WORDPRESS_SECRET = "WORDPRESS_SECRET";

  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "WordPress";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
            initialized, "Trying to call getExporter before initializing WordPressTransferExtension");
    Preconditions.checkArgument(
            SUPPORTED_TYPES.contains(transferDataType),
            "Export of " + transferDataType + " not supported by WordPress");
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
            initialized, "Trying to call getImporter before initializing WordPressTransferExtension");
    Preconditions.checkArgument(
            SUPPORTED_TYPES.contains(transferDataType),
            "Import of " + transferDataType + " not supported by WordPress");
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);
    OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();
    ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    monitor.debug(() -> "Starting WordPress initialization");
    if (initialized) {
      monitor.severe(() -> "WordPressTransferExtension already initialized.");
      return;
    }

    AppCredentials appCredentials;
    try {
      appCredentials =
              context
                      .getService(AppCredentialStore.class)
                      .getAppCredentials(WORDPRESS_KEY, WORDPRESS_SECRET);
    } catch (IOException e) {
      monitor.info(
              () ->
                      String.format(
                              "Unable to retrieve WordPress AppCredentials. Did you set %s and %s?",
                              WORDPRESS_KEY, WORDPRESS_SECRET),
              e);
      return;
    }

    WordPressCredentialFactory wordPressCredentialFactory =
            new WordPressCredentialFactory(httpTransport, jsonFactory, appCredentials);

    WordPressClientFactory wordPressClientFactory =
            new WordPressClientFactory(
                    getServiceId(), client, mapper, monitor, wordPressCredentialFactory);

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put(
            "SOCIAL-POSTS", new WordPressPostsImporter(wordPressClientFactory, monitor));
    importerBuilder.put("NOTES", new WordPressPostsImporter(wordPressClientFactory, monitor));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
