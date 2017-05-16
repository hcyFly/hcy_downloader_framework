package com.stay4it.stay4it_downloader;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * Created by Stay on 31/7/15.
 * Powered by www.stay4it.com
 */
public class GsonRequest<T> extends Request<T> {

    private final Response.Listener<T> mListener;
    private final Type mPojoType;

    /**
     * Creates a new request with the given method.
     *
     * @param method        the request {@link Method} to use
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public GsonRequest(int method, String url,Type type, Response.Listener<T> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mPojoType = type;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, "utf-8");
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        try {
            JSONObject json = new JSONObject(parsed);
            parsed = json.opt("data").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        T t = gson.fromJson(parsed, mPojoType);
        return Response.success(t, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }
}
