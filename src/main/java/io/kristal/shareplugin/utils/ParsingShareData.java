package io.kristal.shareplugin.utils;

import android.util.Log;

import org.cobaltians.cobalt.Cobalt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roxane P. on 4/18/16.
 * ParsingShareData
 * Take a Json Object Message from web side
 * Parse it, and return an HashMap containing the data
 */
public class ParsingShareData {

    public static final String TAG = "ParsingShareData";
    // Data store all the key-value for the file in data
    private Map<String, String> data = new HashMap<String, String>();
    private JSONObject shareData;

    public ParsingShareData(JSONObject shareData) {
        this.shareData = shareData;
    }

    /**
     * returnDataFromWeb get hash map with data from any resource type
     *
     * @return - a Map<String, String> data containing the file data
     * @throws JSONException if the given JSON message is malformed.
     */
    public Map<String, String> returnDataFromWeb() throws JSONException {
        // Type is a mandatory value
        if (!shareData.has(Tokens.JS_TOKEN_TYPE)) return null;
        String typeFile = shareData.getString(Tokens.JS_TOKEN_TYPE);
        // Set type of file
        putInData(Tokens.JS_TOKEN_TYPE);

        // Set data of this file in map
        switch (typeFile) {
            /**
             * Return share data for a bloc of text
             * mandatory params: type, content
             * optional params: title
             */
            case Tokens.JS_TOKEN_TEXT_TYPE:
                // mandatory data:
                putInData(Tokens.JS_TOKEN_TEXT_CONTENT, true);
                // others data:
                putInData(Tokens.JS_TOKEN_TITLE);
                if (Cobalt.DEBUG)
                    Log.d(TAG, Tokens.JS_TOKEN_TEXT_TYPE + " Json parsed: " + data.toString());
                return data;
            /**
             * Return share data for a contact
             * No mandatory params but need a least one tokens
             * params: type, name, mobile, email, company, postal, job, detail
             */
            case Tokens.JS_TOKEN_CONTACT_TYPE:
                boolean tokenExist = putInData(Tokens.JS_TOKEN_CONTACT_NAME);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_CONTACT_MOBILE);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_CONTACT_EMAIL);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_CONTACT_COMPANY);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_CONTACT_POSTAL);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_CONTACT_JOB);
                tokenExist = tokenExist || putInData(Tokens.JS_TOKEN_DETAIL);
                if (!tokenExist) {
                    Log.e(TAG, "No params found for sharing contact: need a least one token.");
                    return null;
                }
                if (Cobalt.DEBUG)
                    Log.d(TAG, Tokens.JS_TOKEN_CONTACT_TYPE + " Json parsed: " + data.toString());
                return data;
            /**
             * Return share data for type of file (image/data/audio/video/document)
             * mandatory params: type, source, path / local => assets todo
             * optional params: title, detail
             */
            case Tokens.JS_TOKEN_IMAGE_TYPE:
            case Tokens.JS_TOKEN_DATA_TYPE:
            case Tokens.JS_TOKEN_AUDIO_TYPE:
            case Tokens.JS_TOKEN_VIDEO_TYPE:
            case Tokens.JS_TOKEN_DOCUMENT_TYPE:
                // mandatory data:
                if (shareData.has(Tokens.JS_TOKEN_SOURCE) && shareData.has(Tokens.JS_TOKEN_PATH)) {
                    putInData(Tokens.JS_TOKEN_SOURCE, true);
                    putInData(Tokens.JS_TOKEN_PATH, true);
                } else if (shareData.has(Tokens.JS_TOKEN_SOURCE) && shareData.has(Tokens.JS_TOKEN_LOCAL)) {
                    putInData(Tokens.JS_TOKEN_SOURCE, true);
                    putInData(Tokens.JS_TOKEN_LOCAL, true);
                } else {
                    Log.e(TAG, "Error while parsing: data must have an assets or local path");
                    return null;
                }
                // others data:
                putInData(Tokens.JS_TOKEN_TITLE);
                putInData(Tokens.JS_TOKEN_DETAIL);
                if (Cobalt.DEBUG)
                    Log.d(TAG, Tokens.JS_TOKEN_IMAGE_TYPE + " Json parsed: " + data.toString());
                return data;
        }
        return null;
    }

    /**
     * Set item 'token' in file data
     * @param token a immutable string to localise value in JSON object
     */
    private boolean putInData(String token) {
        if (!shareData.has(token)) return false;
        try {
            data.put(token, shareData.getString(token));
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing data from web.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Set item 'token' in file data
     * Throws an exception if i can't set it
     * @param token a immutable string to localise value in JSON object
     * @param mandatoriness is true when value is mandatory
     */
    private void putInData(String token, boolean mandatoriness) {
        if (mandatoriness) {
            if (!shareData.has(token)) {
                Log.e(TAG, "Error while parsing: " + token + " is a mandatory fields.");
                return;
            }
        }
        try {
            data.put(token, shareData.getString(token));
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing data from web.");
            e.printStackTrace();
        }
    }
}
