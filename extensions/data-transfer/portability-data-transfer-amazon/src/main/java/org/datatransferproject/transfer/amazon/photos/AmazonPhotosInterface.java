package org.datatransferproject.transfer.amazon.photos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.amazon.common.AmazonCredentialFactory;
import org.datatransferproject.transfer.amazon.datamodels.AmazonEndPoint;
import org.datatransferproject.transfer.amazon.datamodels.AmazonMedia;
import org.datatransferproject.transfer.amazon.datamodels.AmazonNode;
import org.datatransferproject.transfer.amazon.datamodels.AmazonNodeData;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AmazonPhotosInterface {
  private static final String ACCOUNT_URL = "https://drive.amazonaws.com/drive/v1/account/endpoint";
  private static final String NODE_RESOURCE = "nodes";
  private static final String UPLOAD_RESOURCE = "v2/upload";
  private static final String ROOT_NODE_FILTERS = "kind:FOLDER and isRoot:true";

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final HttpTransport httpTransport = new NetHttpTransport();
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private final AmazonCredentialFactory credentialFactory;
  private AmazonEndPoint amazonEndPoint;
  private Credential credential;

  public AmazonPhotosInterface(
      AmazonCredentialFactory credentialFactory,
      Credential credential,
      JsonFactory jsonFactory,
      AmazonEndPoint amazonEndPoint,
      Monitor monitor) {
    this.credential = credential;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.amazonEndPoint = amazonEndPoint;
  }

  AmazonNode createNode(AmazonNode album)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Map<String, Object> albumMap = createJsonMap(album);
    HttpContent content = new JsonHttpContent(jsonFactory, albumMap);

    return makePostRequest(
        amazonEndPoint.getMetadataUrl() + NODE_RESOURCE,
        Optional.empty(),
        content,
        "",
        AmazonNode.class);
  }

  AmazonEndPoint getEndPoints()
      throws PermissionDeniedException, InvalidTokenException, IOException {
    return makeGetRequest(ACCOUNT_URL, Optional.empty(), AmazonEndPoint.class);
  }

  AmazonNodeData getAmazonRootNodeData()
      throws PermissionDeniedException, InvalidTokenException, IOException {
    Map<String, String> nodeFilters = new LinkedHashMap<>();
    nodeFilters.put("filters", ROOT_NODE_FILTERS);
    return makeGetRequest(
        amazonEndPoint.getMetadataUrl(), Optional.of(nodeFilters), AmazonNodeData.class);
  }

  AmazonMedia uploadMedia(InputStream inputStream, String title, String albumId)
      throws IOException, InvalidTokenException, PermissionDeniedException,
          NoSuchAlgorithmException {

    InputStreamContent content = new InputStreamContent(null, inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    byte[] contentBytes = outputStream.toByteArray();
    if (contentBytes.length == 0) {
      return null;
    }
    HttpContent httpContent = new ByteArrayContent(null, contentBytes);
    byte[] digest = MessageDigest.getInstance("MD5").digest(contentBytes);
    String contentMD5 = DatatypeConverter.printHexBinary(digest);

    Map<String, String> PHOTO_UPLOAD_PARAMS =
        ImmutableMap.of(
            "name",
            title,
            "fileSize",
            Integer.toString(contentBytes.length),
            "parentNodeId",
            albumId);

    return makePostRequest(
        amazonEndPoint.getContentUrl() + UPLOAD_RESOURCE,
        Optional.of(PHOTO_UPLOAD_PARAMS),
        httpContent,
        contentMD5,
        AmazonMedia.class);
  }

  private <T> T makeGetRequest(String url, Optional<Map<String, String>> parameters, Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)));
    HttpHeaders httpHeaders = getCommonHeaderParameters();
    getRequest.setHeaders(httpHeaders);
    HttpResponse response;
    try {
      response = getRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildGetRequest(
                      new GenericUrl(url + "?" + generateParamsString(parameters))),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }

  <T> T makePostRequest(
      String url,
      Optional<Map<String, String>> parameters,
      HttpContent httpContent,
      String contentMD5,
      Class<T> clazz)
      throws IOException, InvalidTokenException, PermissionDeniedException {

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpHeaders httpHeaders = getCommonHeaderParameters();

    httpHeaders.set("x-amz-access-token", credential.getAccessToken());
    httpHeaders.set("x-amz-file-md5", contentMD5);
    HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent);
    postRequest.setReadTimeout(2 * 60000); // 2 minutes read timeout
    postRequest.setHeaders(httpHeaders);
    HttpResponse response;

    try {
      response = postRequest.execute();
    } catch (HttpResponseException e) {
      response =
          handleHttpResponseException(
              () ->
                  requestFactory.buildPostRequest(
                      new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent),
              e);
    }

    Preconditions.checkState(response.getStatusCode() == 200);
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  private HttpHeaders getCommonHeaderParameters() {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setAuthorization("Bearer " + credential.getAccessToken());
    //httpHeaders.setContentType("application/json");
    return httpHeaders;
  }

  private HttpResponse handleHttpResponseException(
      SupplierWithIO<HttpRequest> httpRequest, HttpResponseException e)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // if the response is "unauthorized", refresh the token and try the request again
    final int statusCode = e.getStatusCode();

    if (statusCode == 401) {
      monitor.info(() -> "Attempting to refresh authorization token");
      // if the credential refresh failed, let the error bubble up via the IOException that gets
      // thrown
      credential = credentialFactory.refreshCredential(credential);
      monitor.info(() -> "Refreshed authorization token successfully");

      // if the second attempt throws an error, then something else is wrong, and we bubble up the
      // response errors
      return httpRequest.getWithIO().execute();
    }
    // "The caller does not have permission" is potential error for albums.
    if (statusCode == 403
        && (e.getContent().contains("File size exceeds account allowance"))) {
      throw new PermissionDeniedException("User permission to Amazon photos was denied", e);
    } else {
      // something else is wrong, bubble up the error
      throw new IOException(
          "Bad status code: "
              + e.getStatusCode()
              + " Error: '"
              + e.getStatusMessage()
              + "' Content: "
              + e.getContent());
    }
  }

  private String generateParamsString(Optional<Map<String, String>> params) {
    Map<String, String> updatedParams = new ArrayMap<>();
    params.ifPresent(updatedParams::putAll);

    List<String> orderedKeys = new ArrayList<>(updatedParams.keySet());
    Collections.sort(orderedKeys);

    List<String> paramStrings = new ArrayList<>();
    for (String key : orderedKeys) {
      String k = key.trim();
      String v = updatedParams.get(key).trim();
      paramStrings.add(k + "=" + v);
    }

    return String.join("&", paramStrings);
  }

  private HashMap<String, Object> createJsonMap(Object object) throws IOException {
    // JacksonFactory expects to receive a Map, not a JSON-annotated POJO, so we have to convert the
    // NewMediaItemUpload to a Map before making the HttpContent.
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    return objectMapper.readValue(objectMapper.writeValueAsString(object), typeRef);
  }

  private interface SupplierWithIO<T> {
    T getWithIO() throws IOException;
  }
}
