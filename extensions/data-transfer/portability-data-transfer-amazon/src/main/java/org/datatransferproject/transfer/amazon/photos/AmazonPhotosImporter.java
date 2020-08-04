package org.datatransferproject.transfer.amazon.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.transfer.amazon.common.AmazonCredentialFactory;
import org.datatransferproject.transfer.amazon.datamodels.*;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.*;

public class AmazonPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {
  private static final String COPY_PREFIX = "Copy of ";
  private final AmazonCredentialFactory credentialFactory;
  private final JsonFactory jsonFactory;
  private final TemporaryPerJobDataStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  private final Monitor monitor;
  private volatile AmazonPhotosInterface photosInterface;
  private AmazonEndPoint amazonEndPoint;

  public AmazonPhotosImporter(
      AmazonCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor) {
    this(credentialFactory, jobStore, jsonFactory, null, new ImageStreamProvider(), monitor);
  }

  @VisibleForTesting
  AmazonPhotosImporter(
      AmazonCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      JsonFactory jsonFactory,
      AmazonPhotosInterface photosInterface,
      ImageStreamProvider imageStreamProvider,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.imageStreamProvider = imageStreamProvider;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    // Get the Amazon endpoint url for the current transfer
    amazonEndPoint = getOrCreatePhotosInterface(authData).getEndPoints();
    if (!amazonEndPoint.isCustomerExists()) {
      throw new Exception("Register for an Amazon account and try again");
    }

    // Get the Amazon root node
    AmazonNodeData amazonNodeData = getOrCreatePhotosInterface(authData).getAmazonRootNodeData();
    if (amazonNodeData.getCount() == 0) {
      throw new Exception("Register for an Amazon account and try again");
    }
    AmazonNode amazonRootNode = amazonNodeData.getData().get(0);

    // Uploads album metadata
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {

        // Setup a Amazon Parent node with rootnode as the parent

        String amazonAlbumFolderId =
            idempotentImportExecutor.executeAndSwallowIOExceptions(
                album.getId(),
                album.getName(),
                () -> createAlbumFolder(authData, album, amazonRootNode.getId()));

        // Setup Amazon dynamic album using the parent node created above as the folder to be
        // included in the album

        idempotentImportExecutor.executeAndSwallowIOExceptions(
            album.getId() + "-" + amazonAlbumFolderId,
            album.getName(),
            () -> createAlbum(authData, album, amazonAlbumFolderId));
      }
    }

    // Uploads photos
    if (data.getPhotos() != null && data.getPhotos().size() > 0) {
      for (PhotoModel photo : data.getPhotos()) {
        idempotentImportExecutor.executeAndSwallowIOExceptions(
            photo.getAlbumId() + "-" + photo.getDataId(),
            photo.getTitle(),
            () -> importSinglePhoto(jobId, authData, photo, idempotentImportExecutor));
      }
    }

    return ImportResult.OK;
  }

  @VisibleForTesting
  String createAlbum(
      TokensAndUrlAuthData authData, PhotoAlbum inputAlbum, String amazonAlbumFolderId)
      throws IOException, InvalidTokenException, PermissionDeniedException {

    AmazonNode amazonAlbum = new AmazonNode();
    amazonAlbum.setName(COPY_PREFIX + inputAlbum.getName());
    amazonAlbum.setResourceVersion("V2");
    amazonAlbum.setKind(AmazonNodeType.VISUAL_COLLECTION);
    amazonAlbum.setCollectionProperties(setAlbumProperties(amazonAlbumFolderId));

    AmazonNode responseAlbum = getOrCreatePhotosInterface(authData).createNode(amazonAlbum);
    return responseAlbum.getId();
  }

  @VisibleForTesting
  String createAlbumFolder(
      TokensAndUrlAuthData authData, PhotoAlbum inputAlbum, String amazonRootNodeId)
      throws IOException, InvalidTokenException, PermissionDeniedException {

    AmazonNode amazonAlbumFolder = new AmazonNode();
    amazonAlbumFolder.setKind(AmazonNodeType.FOLDER);
    amazonAlbumFolder.setName(COPY_PREFIX + inputAlbum.getName());
    List<String> albumParentIds = new ArrayList<>();
    albumParentIds.add(amazonRootNodeId);
    amazonAlbumFolder.setParents(albumParentIds);

    AmazonNode amazonDriveAlbumFolder =
        getOrCreatePhotosInterface(authData).createNode(amazonAlbumFolder);
    return amazonDriveAlbumFolder.getId();
  }

  private AmazonAlbumCollectionProperties setAlbumProperties(String albumFolderId) {

    AmazonAlbumQueryInclude amazonAlbumQueryInclude = new AmazonAlbumQueryInclude();
    amazonAlbumQueryInclude.setFolderIds(Collections.singletonList(albumFolderId));

    AmazonAlbumQuery amazonAlbumQuery = new AmazonAlbumQuery();
    amazonAlbumQuery.setInclude(Collections.singletonList(amazonAlbumQueryInclude));

    AmazonAlbumCollectionProperties amazonAlbumCollectionProperties =
        new AmazonAlbumCollectionProperties();
    amazonAlbumCollectionProperties.setQuery(amazonAlbumQuery);

    return amazonAlbumCollectionProperties;
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      TokensAndUrlAuthData authData,
      PhotoModel inputPhoto,
      IdempotentImportExecutor idempotentImportExecutor)
      throws IOException {

    // Upload photo
    InputStream inputStream;
    AmazonMedia amazonPhoto = null;

    if (inputPhoto.isInTempStore()) {
      final TemporaryPerJobDataStore.InputStreamWrapper streamWrapper =
          jobStore.getStream(jobId, inputPhoto.getFetchableUrl());

      inputStream = streamWrapper.getStream();
    } else {
      HttpURLConnection conn = imageStreamProvider.getConnection(inputPhoto.getFetchableUrl());
      inputStream = conn.getInputStream();
    }

    String albumId;
    if (Strings.isNullOrEmpty(inputPhoto.getAlbumId())) {
      // This is ok, since NewMediaItemUpload will ignore all null values and it's possible to
      // upload a NewMediaItem without a corresponding album id.
      albumId = null;
    } else {
      // Note this will throw if creating the album failed, which is what we want
      // because that will also mark this photo as being failed.
      albumId = idempotentImportExecutor.getCachedValue(inputPhoto.getAlbumId());
    }

    try {
      amazonPhoto =
          getOrCreatePhotosInterface(authData)
              .uploadMedia(inputStream, inputPhoto.getTitle(), albumId);
    } catch (Exception e) {
      if (e.getMessage() != null) {
        return e.getMessage();
      }
    }
    return amazonPhoto != null ? amazonPhoto.getId() : "";
  }

  private synchronized AmazonPhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized AmazonPhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new AmazonPhotosInterface(
        credentialFactory, credential, jsonFactory, amazonEndPoint, monitor);
  }
}
