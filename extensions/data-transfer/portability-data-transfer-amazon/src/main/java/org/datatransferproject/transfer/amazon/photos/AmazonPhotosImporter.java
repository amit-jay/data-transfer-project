package org.datatransferproject.transfer.amazon.photos;

import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

public class AmazonPhotosImporter implements Importer<TokensAndUrlAuthData, PhotosContainerResource>  {
  public AmazonPhotosImporter(AppCredentials appCredentials) {}


  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentExecutor, TokensAndUrlAuthData authData, PhotosContainerResource data) throws Exception {
    return null;
  }
}
