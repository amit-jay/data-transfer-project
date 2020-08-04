/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.transfer.amazon;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.amazon.common.AmazonCredentialFactory;
import org.datatransferproject.transfer.amazon.photos.AmazonPhotosImporter;
import org.datatransferproject.transfer.amazon.videos.AmazonVideosImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

public class AmazonTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "Amazon";
  private static final ImmutableList<String> SUPPORTED_SERVICES =
      ImmutableList.of("PHOTOS", "VIDEOS");
  private boolean initialized = false;
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);

    AppCredentials appCredentials;
    final Monitor monitor = context.getMonitor();
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("AMAZON_KEY", "AMAZON_SECRET");
    } catch (IOException e) {
      monitor.info(
          () ->
              "Unable to retrieve Amazon AppCredentials. Did you set AMAZON_KEY and AMAZON_SECRET?",
          e);
      return;
    }

    AmazonCredentialFactory amazonCredentialFactory =
        new AmazonCredentialFactory(httpTransport, jsonFactory, appCredentials, monitor);

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put(
        "PHOTOS",
        new AmazonPhotosImporter(amazonCredentialFactory, jobStore, jsonFactory, monitor));
    importerBuilder.put("VIDEOS", new AmazonVideosImporter(appCredentials));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
