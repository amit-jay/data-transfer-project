// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.transfer.wordpress.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.transfer.wordpress.common.WordPressClient;
import org.datatransferproject.transfer.wordpress.common.WordPressClientFactory;
import org.datatransferproject.transfer.wordpress.datamodel.WordpressMediaError;
import org.datatransferproject.transfer.wordpress.datamodel.WordpressPost;
import org.datatransferproject.transfer.wordpress.datamodel.WordpressPostAttachment;
import org.datatransferproject.transfer.wordpress.datamodel.WordpressPostStatusType;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class WordPressPostsImporter
        implements Importer<TokensAndUrlAuthData, SocialActivityContainerResource> {

    private final ObjectMapper mapper;
    private final WordPressClientFactory wordPressClientFactory;
    private final Monitor monitor;

    public WordPressPostsImporter(WordPressClientFactory wordPressClientFactory, Monitor monitor) {
        this.wordPressClientFactory = wordPressClientFactory;
        this.mapper = new ObjectMapper();
        this.monitor = monitor;
    }

    @Override
    public ImportResult importItem(
            UUID jobId,
            IdempotentImportExecutor idempotentExecutor,
            TokensAndUrlAuthData authData,
            SocialActivityContainerResource data)
            throws Exception {

        WordPressClient wordPressClient = wordPressClientFactory.create(authData);

        // The Authdata tokenServerEncodeUrl is set to WP blog id when the DTP Wordpress Oauth
        // provider parses the response from WP after user authenticates.
        String siteId = authData.getTokenServerEncodedUrl();
        monitor.debug(() -> String.format("Starting posts import to Wordpress blog %s", siteId));

        for (SocialActivityModel activityModel : data.getActivities()) {
            idempotentExecutor.executeAndSwallowIOExceptions(
                    String.format("%s-%s", jobId, activityModel.getId()),
                    activityModel.getTitle(),
                    () -> importPost(siteId, activityModel, wordPressClient));
        }

        return ImportResult.OK;
    }

    private String importPost(
            String siteId, SocialActivityModel activityModel, WordPressClient wordPressClient)
            throws IOException {

        // URLs which need to show up as links
        final String[] linkUrls =
                activityModel.getAttachments().stream()
                        .filter(attachment -> attachment.getType() == SocialActivityAttachmentType.LINK)
                        .map(SocialActivityAttachment::getUrl)
                        .toArray(String[]::new);

        // URLs of media (image and video) which user has uploaded
        final String[] mediaUrls =
                activityModel.getAttachments().stream()
                        .filter(
                                attachment ->
                                        attachment.getType() == SocialActivityAttachmentType.IMAGE
                                                || attachment.getType() == SocialActivityAttachmentType.VIDEO)
                        .map(SocialActivityAttachment::getUrl)
                        .toArray(String[]::new);

        WordpressPost wordPressPost = new WordpressPost();
        String content = activityModel.getContent() != null ? activityModel.getContent() : "";
        // Append the links in a new line to the existing content. This will enable WP to embed the
        // content onto the post.
        for (String url : linkUrls) {
            content = content.concat("\n").concat(url);
        }
        if (mediaUrls.length > 0) {
            wordPressPost.setMediaUrls(Arrays.asList(mediaUrls));
        }
        wordPressPost.setDate(activityModel.getPublished().toString());
        wordPressPost.setTitle(activityModel.getTitle());
        wordPressPost.setContent(content);
        wordPressPost.setStatusType(WordpressPostStatusType.DRAFT);

        // Create WP post and pass in all media objects (via media_urls) that needs to be copied over
        // to WP.
        // Note - the images will show up on the post embedded. Videos will be uploaded to the media
        // gallery but not embedded on the post.
        WordpressPost createdPost = wordPressClient.makeWordPressPostCall(siteId, null, wordPressPost);

        for (WordpressMediaError mediaError : createdPost.getMediaErrors()) {
            monitor.info(
                    () ->
                            String.format(
                                    "Wordpress post %s has media error - %s",
                                    activityModel.getTitle(), mediaError.getError() + "-" + mediaError.getMessage()));
        }

        String blogId = Integer.toString(createdPost.getId());
        int blogAttachmentCount = createdPost.getAttachmentCount();

        // For every video in the media_urls that was uploaded to WP during the create post call,
        // append the video embedcode to the existing post content and make an EDIT call on the post.
        // This will allow WP to show the post with the video embedded.
        if (mediaUrls.length > 0 && blogAttachmentCount > 0) {
            String videoGuid;
            String VIDEO_EMBED_SHORT_CODE = "[wpvideo %s]";
            String videoEmbedCode = "";
            HashMap<String, WordpressPostAttachment> attachmentDetails = createdPost.getAttachments();
            monitor.debug(
                    () -> String.format("Size of the post attachment = %s", attachmentDetails.size()));
            // get the embed code for all videos on the post
            for (WordpressPostAttachment wordpressPostAttachment : attachmentDetails.values()) {
                monitor.debug(
                        () ->
                                String.format(
                                        "Attachment video guid = %s",
                                        wordpressPostAttachment.getVideoAttachmentGuid()));
                if (wordpressPostAttachment.getVideoAttachmentGuid() != null) {
                    videoGuid = wordpressPostAttachment.getVideoAttachmentGuid();
                    videoEmbedCode = videoEmbedCode + "\n" + String.format(VIDEO_EMBED_SHORT_CODE, videoGuid);
                }
            }
            // Make the edit call if there is atleast one embed code
            if (videoEmbedCode.length() > 0) {
                String postContentWithEmbed = createdPost.getContent() + videoEmbedCode;
                wordPressPost = new WordpressPost();
                wordPressPost.setContent(postContentWithEmbed);
                createdPost = wordPressClient.makeWordPressPostCall(siteId, blogId, wordPressPost);
                blogId = Integer.toString(createdPost.getId());
            }
        }

        return blogId;
    }
}
