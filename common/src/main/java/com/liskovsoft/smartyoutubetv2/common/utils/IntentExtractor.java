package com.liskovsoft.smartyoutubetv2.common.utils;

import android.content.Intent;
import android.net.Uri;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.querystringparser.UrlQueryString;
import com.liskovsoft.sharedutils.querystringparser.UrlQueryStringFactory;

public class IntentExtractor {
    private static final String TAG = IntentExtractor.class.getSimpleName();
    /**
     * Browser: https://www.youtube.com/results?search_query=twice<br/>
     * Amazon: youtube://search?query=linkin+park&isVoice=true
     */
    private static final String[] SEARCH_KEYS = {"search_query", "query"};
    private static final String VIDEO_ID_KEY = "v";
    private static final String VIDEO_ID_LIST_KEY = "video_ids";
    /**
     * https://youtube.com/channel/BLABLA/video
     */
    private static final String CHANNEL_KEY = "channel";
    private static final String CHANNEL_URL_ALT = "/c/";
    private static final String USER_URL = "/user/";
    private static final String SUBSCRIPTIONS_URL = "https://www.youtube.com/tv#/zylon-surface?c=FEsubscriptions"; // last 'resume' param isn't parsed by intent and should be removed
    private static final String HISTORY_URL = "https://www.youtube.com/tv#/zylon-surface?c=FEmy_youtube"; // last 'resume' param isn't parsed by intent and should be removed
    private static final String RECOMMENDED_URL = "https://www.youtube.com/tv#/zylon-surface?c=default"; // last 'resume' param isn't parsed by intent and should be removed
    private static final String PLAYLIST_KEY = "list";

    public static String extractVideoId(Intent intent) {
        if (isEmptyIntent(intent)) {
            return null;
        }

        if (extractVoiceQuery(extractUri(intent)) != null) {
            return null;
        }

        // Don't Uri directly or you might get UnsupportedOperationException on some urls.
        UrlQueryString parser = UrlQueryStringFactory.parse(extractUri(intent));
        String videoId = parser.get(VIDEO_ID_KEY);

        if (videoId == null) {
            // https://youtube.com/watch_videos?video_ids=xdq_sYfmN6c,xdq_sYfmN6c
            String idList = parser.get(VIDEO_ID_LIST_KEY);

            if (idList != null) {
                // temp solution: use one video from the list
                videoId = idList.split(",")[0];
            } else {
                // Suppose that link type is https://youtu.be/lBeMDqcWTG8
                videoId = extractUri(intent).getLastPathSegment();
            }
        }

        if (!isValid(videoId)) {
            return null;
        }

        return videoId;
    }

    /**
     * Browser: https://www.youtube.com/results?search_query=twice<br/>
     * Amazon: youtube://search?query=linkin+park&isVoice=true
     */
    public static String extractSearchText(Intent intent) {
        if (isEmptyIntent(intent)) {
            return null;
        }

        String voiceQuery = extractVoiceQuery(extractUri(intent));

        if (voiceQuery != null) {
            return voiceQuery;
        }

        // Don't Uri directly or you might get UnsupportedOperationException on some urls.
        UrlQueryString parser = UrlQueryStringFactory.parse(extractUri(intent));

        for (String searchKey : SEARCH_KEYS) {
            String searchText = parser.get(searchKey);

            if (searchText != null) {
                return searchText;
            }
        }

        return null;
    }

    /**
     * Data: https://www.youtube.com/channel/UCtDjOV5nk982w35AIdVDuNw
     */
    public static String extractChannelId(Intent intent) {
        if (isEmptyIntent(intent)) {
            return null;
        }

        // https://youtube.com/channel/BLABLA/video
        // Don't Uri directly or you might get UnsupportedOperationException on some urls.
        UrlQueryString parser = UrlQueryStringFactory.parse(extractUri(intent));

        return parser.get(CHANNEL_KEY);
    }

    /**
     * Data: https://www.youtube.com/playlist?list=RDCLAK5uy_mk6AmqcHgCRhyJuYsQz5CCVdCF4SRGivs
     */
    public static String extractPlaylistId(Intent intent) {
        if (isEmptyIntent(intent)) {
            return null;
        }

        UrlQueryString parser = UrlQueryStringFactory.parse(extractUri(intent));

        return parser.get(PLAYLIST_KEY);
    }

    private static boolean isValid(String videoId) {
        return videoId != null && videoId.length() == 11;
    }

    public static boolean hasData(Intent intent) {
        return intent != null && extractUri(intent) != null;
    }

    /**
     * ATV: Channel icon url
     */
    public static boolean isChannelUrl(Intent intent) {
        return intent != null
                && extractUri(intent) != null
                && Helpers.startsWithAny(extractUri(intent).toString(), SUBSCRIPTIONS_URL, HISTORY_URL, RECOMMENDED_URL);
    }

    /**
     * ATV: Subscriptions icon url
     */
    public static boolean isSubscriptionsUrl(Intent intent) {
        return intent != null
                && extractUri(intent) != null
                && Helpers.startsWith(extractUri(intent).toString(), SUBSCRIPTIONS_URL);
    }

    /**
     * ATV: History icon url
     */
    public static boolean isHistoryUrl(Intent intent) {
        return intent != null
                && extractUri(intent) != null
                && Helpers.startsWith(extractUri(intent).toString(), HISTORY_URL);
    }

    /**
     * ATV: Recommended icon url
     */
    public static boolean isRecommendedUrl(Intent intent) {
        return intent != null
                && extractUri(intent) != null
                && Helpers.startsWith(extractUri(intent).toString(), RECOMMENDED_URL);
    }

    public static boolean isRootUrl(Intent intent) {
        return intent != null
                && extractUri(intent) != null
                && (extractUri(intent).toString().endsWith(".com/") || extractUri(intent).toString().endsWith(".com"));
    }

    public static boolean isStartVoiceCommand(Intent intent) {
        return intent != null && extractUri(intent) != null && extractUri(intent).toString().contains("launch=voice");
    }

    public static boolean hasFinishOnEndedFlag(Intent intent) {
        return intent != null && intent.getBooleanExtra("finish_on_ended", false);
    }

    /**
     * Example: https://www.youtube.com/tv?voice={"youtubeAssistantRequest":{"query":"Russian YouTube","queryIntent":"CgxTZWFyY2hJbnRlbnQSFAoFcXVlcnkSCxoJCgdSdXNzaWFuEiYKCGRvY190eXBlEhoaGAoWWU9VVFVCRV9ET0NfVFlQRV9WSURFTw==","youtubeAssistantParams":{"personalDataParams":{"showPersonalData":false}},"enablePrefetchLogging":true},"updateYoutubeSettings":{"enableSafetyMode":false,"enablePersonalResults":false},"hasEntityBar":false}&command_id=CWGIYL6nN8Gi3AP_5Y6wAQ&launch=voice&vq=Russian%20YouTube
     */
    private static String extractVoiceQuery(Uri data) {
        return Helpers.runMultiMatcher(data.toString(), ":\\{\"query\":\"([^\"]*)\"");
    }

    private static boolean isEmptyIntent(Intent intent) {
        return intent == null || (!Intent.ACTION_VIEW.equals(intent.getAction()) && !Intent.ACTION_SEND.equals(intent.getAction())) || extractUri(intent) == null;
    }

    private static Uri extractUri(Intent intent) {
        if (intent == null) {
            return null;
        }

        return intent.getData() != null ? intent.getData() :
                intent.getStringExtra(Intent.EXTRA_TEXT) != null ?
                        Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)) : null;
    }
}
