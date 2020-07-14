package org.datatransferproject.transfer.amazon.videos;

import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

public class AmazonVideosImporter implements Importer<TokensAndUrlAuthData, VideosContainerResource> {
  public AmazonVideosImporter(AppCredentials appCredentials) {}


  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentExecutor, TokensAndUrlAuthData authData, VideosContainerResource data) throws Exception {
    return null;
  }
}
