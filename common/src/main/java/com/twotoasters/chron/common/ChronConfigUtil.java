package com.twotoasters.chron.common;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.NodeApi.GetLocalNodeResult;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public final class ChronConfigUtil {

    private static final String TAG = ChronConfigUtil.class.getSimpleName();

    /**
     * Asynchronously fetches the current config and passes it to the given callback.
     * <p>
     * If the current config doesn't exist, it isn't created and the callback receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client,
            final FetchConfigDataMapCallback callback) {
        Utils.logd("fetchConfigDataMap()");
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        Utils.logd("fetchConfigDataMap - onResult()");
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme(Constants.SCHEME_WEAR)
                                .path(Constants.PATH_WITH_FEATURE)
                                .authority(localNode)
                                .build();

                        Utils.logd("fetchConfigDataMap - onResult - getDataItem()");
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link com.google.android.gms.wearable.DataItem} with the
     * ones appearing in the given {@link com.google.android.gms.wearable.DataMap}. If the config DataItem doesn't exist, it's created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient, final DataMap configKeysToOverwrite) {
        Utils.logd("overwriteKeysInConfigDataMap()");
        ChronConfigUtil.fetchConfigDataMap(googleApiClient,
                new ChronConfigUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        Utils.logd("overwriteKeysInConfigDataMap - onConfigDataMapFetched()");
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        ChronConfigUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                }
        );
    }

    /**
     * Overwrites the current config {@link com.google.android.gms.wearable.DataItem}'s {@link com.google.android.gms.wearable.DataMap} with {@code newConfig}. If
     * the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        Utils.logd("putConfigDataItem()");
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.PATH_WITH_FEATURE);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Utils.logd("putConfigDataItem - putDataItem()");
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Utils.logd("putConfigDataItem - putDataItem - onResult()");
                        Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                    }
                });
    }

    private static class DataItemResultCallback implements ResultCallback<DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            Utils.logd("DataItemResultCallback - onResult()");
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    public interface FetchConfigDataMapCallback {
        void onConfigDataMapFetched(DataMap config);
    }

    private ChronConfigUtil() { }
}
