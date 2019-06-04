package org.acestream.engine.client.example;

import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.acestream.engine.controller.Callback;
import org.acestream.engine.service.v0.IAceStreamEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public class EngineApi {

    private final static String TAG = "AS/EngineApi";

    public static int STATUS_OK = 0;
    public static int STATUS_NON_FATAL_ERROR = 1;
    public static int STATUS_FATAL_ERROR = 2;

    protected IAceStreamEngine mService;
    protected String mHost;
    protected int mPort;

    private static ConnectionPool mHttpConnectionPool = new ConnectionPool(7, 2, TimeUnit.MINUTES);
    private Retrofit mRetrofit;

    public EngineApi(IAceStreamEngine service) {
        mService = service;
        mHost  = "127.0.0.1";

        try {
            mPort = service.getHttpApiPort();
        }
        catch(RemoteException e) {
            Log.e(TAG, "Failed to get HTTP API port", e);
            // Fallback to default port
            mPort = 6878;
        }

        mRetrofit = createRetrofit();
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public IAceStreamEngine getService() {
        return mService;
    }

    public void startSession(String contentId, @Nullable final Callback<EngineSessionResponse> callback) {
        String url = "/ace/getstream";
        Map<String,String> params = new HashMap<>();
        params.put("sid", "test-client");
        params.put("product_key", "test-client");
        params.put("format", "json");
        params.put("id", contentId);
        apiCall(url, params, null, callback, new TypeToken<EngineSessionResponse>(){}, true);
    }

    public void stopSession(@NonNull EngineSession session, @Nullable final Callback<String> callback) {
        if(session.command_url == null) {
            throw new IllegalStateException("missing command url");
        }
        String url = session.command_url + "?method=stop&api_version=2";
        // Returns string "ok" on success
        apiCall(url, null, callback, new TypeToken<EngineApiResponse<String>>(){});
    }

    protected <T> void apiCall(String url,
                               Map<String, String> params,
                               @Nullable final Callback<T> callback,
                               final TypeToken tt) {
        apiCall(url, params, null, callback, tt, false);
    }

    private <T> void apiCall(final String url,
                             Map<String, String> params,
                             RequestBody postPayload,
                             @Nullable final Callback<T> callback,
                             final TypeToken tt,
                             final boolean useRawResponse) {

        if(params == null) {
            // retrofit requires non-null params
            params = new HashMap<>();
        }
        EngineApiService api = getRetrofit().create(EngineApiService.class);
        final Call<ResponseBody> call;
        if(postPayload != null) {
            call = api.apiCallPost(url, params, postPayload);
        }
        else {
            call = api.apiCall(url, params);
        }

        call.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "call: empty body");
                    if(callback != null) {
                        callback.onError("missing body");
                    }
                    return;
                }

                EngineApiResponse<T> apiResponse = null;
                T rawResponse = null;
                Gson gson = new Gson();
                String rawBody = null;
                try {
                    rawBody = body.string();
                    body.close();

                    // for testing
                    Log.v(TAG, "url=" + url);
                    Log.v(TAG, "body=" + rawBody);

                    if(tt == null) {
                        apiResponse = new EngineApiResponse<>();
                        apiResponse.error = null;
                        apiResponse.result = (T) rawBody;
                    }
                    else if(useRawResponse) {
                        rawResponse = gson.fromJson(rawBody, tt.getType());
                    }
                    else {
                        apiResponse = gson.fromJson(rawBody, tt.getType());
                    }
                }
                catch(IOException|JsonSyntaxException e) {
                    Log.v(TAG, "failed response body=" + rawBody);
                    Log.e(TAG, "failed to deserialize response", e);
                    if(callback != null) {
                        callback.onError("Internal error");
                    }
                    return;
                }

                if (apiResponse != null && apiResponse.error != null) {
                    Log.e(TAG, "call: got error: " + apiResponse.error.message);
                    if(callback != null) {
                        callback.onError(apiResponse.error.message);
                    }
                    return;
                }

                if (apiResponse != null && apiResponse.result == null) {
                    Log.e(TAG, "call: got error: null result");
                    if(callback != null) {
                        callback.onError("Internal error");
                    }
                    return;
                }

                if(callback != null) {
                    if(rawResponse != null) {
                        callback.onSuccess(rawResponse);
                    }
                    else if(apiResponse != null) {
                        callback.onSuccess(apiResponse.result);
                    }
                    else {
                        throw new IllegalStateException("missing response");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if(callback != null) {
                    callback.onError(t.toString());
                }
            }
        });
    }

    private Retrofit createRetrofit() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectionPool(mHttpConnectionPool )
                .connectTimeout(240, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl("http://" + mHost + ":" + mPort)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private Retrofit getRetrofit() {
        return mRetrofit;
    }

    public interface EngineApiService {
        @GET
        Call<ResponseBody> apiCall(@Url String url, @QueryMap Map<String, String> params);

        @POST
        Call<ResponseBody> apiCallPost(@Url String url, @QueryMap Map<String, String> params, @Body RequestBody payload);
    }
}
