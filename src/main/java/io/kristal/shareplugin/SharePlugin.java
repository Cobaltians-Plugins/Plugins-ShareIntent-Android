/**
 * SharePlugin
 * The MIT License (MIT)
 * Copyright (c) 2016 Cobaltians
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
**/

package io.kristal.shareplugin;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import org.cobaltians.cobalt.Cobalt;
import org.cobaltians.cobalt.fragments.CobaltFragment;
import org.cobaltians.cobalt.plugin.CobaltAbstractPlugin;
import org.cobaltians.cobalt.plugin.CobaltPluginWebContainer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.kristal.shareplugin.shareDataClass.ShareContactData;
import io.kristal.shareplugin.shareDataClass.ShareLocalFile;
import io.kristal.shareplugin.shareDataClass.ShareRemoteFile;
import io.kristal.shareplugin.shareDataClass.ShareSimpleShareData;
import io.kristal.shareplugin.utils.FileSystemTools;
import io.kristal.shareplugin.utils.ParsingShareData;

/**
 * Created by Roxane P. on 4/18/16.
 * SharePlugin
 * Start an intent for sharing a file from data given by a Json Object Message
 */
public class SharePlugin extends CobaltAbstractPlugin {

    protected final static String TAG = SharePlugin.class.getSimpleName();
    private static CobaltPluginWebContainer mWebContainer;
    private static final String SHARE_ME_APP = "share";

    /**************************************************************************************
     * MEMBERS
     **************************************************************************************/

    protected static SharePlugin sInstance;
    // fragment handler
    public static CobaltFragment currentFragment;
    public static Context currentContext;
    private static String mType;

    /**************************************************************************************
     * CONSTANTS MEMBERS
     **************************************************************************************/

    // Data extension types - string
    public static final String TYPE_IMAGE_KEY = "image";
    public static final String TYPE_TEXT_KEY = "text";
    public static final String TYPE_VIDEO_KEY = "video";
    public static final String TYPE_AUDIO_KEY = "audio";
    public static final String TYPE_DOCUMENT_KEY = "document";
    public static final String TYPE_CONTACT_KEY = "contact";
    public static final String TYPE_DATA_KEY = "data";

    // Data source types
    public static final int dataFromAssets = 1;
    public static final int dataFromSDCard = 2;
    public static final int dataFromContentProvider = 3;
    public static final int dataFromUrl = 5;

    // File path of the downloaded remote files
    public static String pathFileStorage;

    // Content provider path
    public static final String AUTHORITY = "io.kristal.shareplugin.SharePlugin";
    public static final String SCHEME = "content://";

    /**************************************************************************************
     * CONFIGURATION
     * *************************************************************************************/

    private final String StorageDirectoryName = "CobaltiansStorage";
    private static Boolean forceChooser = true;

    /**************************************************************************************
     * CONSTRUCTORS
     **************************************************************************************/

    public static CobaltAbstractPlugin getInstance(CobaltPluginWebContainer webContainer) {
        if (sInstance == null) sInstance = new SharePlugin();
        sInstance.addWebContainer(webContainer);
        return sInstance;
    }

    @Override
    public void onMessage(CobaltPluginWebContainer webContainer, JSONObject message) throws JSONException {
        Log.d(TAG, "onMessage called with message: " + message.toString());
        mWebContainer = webContainer;
        currentFragment = webContainer.getFragment();
        currentContext = currentFragment.getContext();
        // Create or set the path of the storage directory in the phone
        pathFileStorage = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Android/data/" +
                currentFragment.getContext().getPackageName() +
                "/" + StorageDirectoryName + "/";
        if (!FileSystemTools.makeDirs(pathFileStorage)) {
            if (Cobalt.DEBUG) Log.e(TAG, "Can't create directory at " + pathFileStorage);
        }
        // will parse message, create and launching intent
        try {
            String action = message.getString(Cobalt.kJSAction);
            if (action.equals(SHARE_ME_APP)) {
                // setting up share
                CobaltFragment fragment = webContainer.getFragment();
                // parse JSON, put into an hashMap
                ParsingShareData psd = new ParsingShareData(message);
                Map data = psd.returnDataFromWeb();
                // mType is used for intent title
                mType = data.get("type").toString();
                // web side return data file to get from a source
                if (data.containsKey("source")) {
                    String source = data.get("source").toString();
                    // send intents for items with sources
                    switch (setSourceFromType(source)) {
                        case SharePlugin.dataFromAssets:
                            doShare(new ShareLocalFile(data).returnShareIntent());
                            break;
                        case SharePlugin.dataFromUrl:
                            // intent called asynchronously
                            new ShareRemoteFile(data);
                            break;
                        default:
                            Log.e(TAG, "onMessage: invalid action " + action + " in message " + message.toString() + ".");
                            break;
                    }
                } else {
                    switch (mType) {
                        case "contact":
                            doShare(new ShareContactData(data).returnShareIntent());
                            break;
                        case "text":
                            doShare(new ShareSimpleShareData(data).returnShareIntent());
                            break;
                        default:
                            Log.e(TAG, "onMessage: invalid action " + action + " in message " + message.toString() + ".");
                            break;
                    }
                }
                // send callback
                JSONObject callback = new JSONObject();
                callback.put("cobalt.share", "share completed");
                // send callback
                fragment.sendCallback(message.getString(Cobalt.kJSCallback), callback);
            } else if (Cobalt.DEBUG)
                Log.e(TAG, "onMessage: invalid action " + action + " in message " + message.toString() + ".");
        } catch (JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.d(TAG, "onMessage: missing action key in message " + message.toString() + ".");
                exception.printStackTrace();
            }
        }
    }

    public static void doShare(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "File data not found: intent is null");
            return;
        }
        if (Cobalt.DEBUG) {
            Log.d(TAG, "cobalt.share will share intent " + intent.toString() + " " + intent.getData() + " " + intent.getPackage() + " " + intent.getScheme());
        }
        for (String key : intent.getExtras().keySet()) {
            Object value = intent.getExtras().get(key);
            if (value != null) {
                Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }
        }
        // launch share activity
        if (forceChooser) {
            // with chooser
            mWebContainer.getFragment().getContext().startActivity(Intent.createChooser(intent, "Share " + mType + " with..."));
        } else {
            // without chooser
            mWebContainer.getFragment().getContext().startActivity(intent);
        }
    }

    private int setSourceFromType(String type) {
        int source;
        switch (type) {
            case "resource":
                source = SharePlugin.dataFromAssets;
                break;
            case "url":
                source = SharePlugin.dataFromUrl;
                break;
            case "sdcard":
                source = SharePlugin.dataFromSDCard;
                break;
            default:
                source = SharePlugin.dataFromContentProvider;
                break;
        }
        return source;
    }
}
