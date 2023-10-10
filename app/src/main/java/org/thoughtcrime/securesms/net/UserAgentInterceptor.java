package org.thoughtcrime.securesms.net;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {

  private final String userAgent;

  public UserAgentInterceptor(@NonNull String userAgent) {
    this.userAgent = userAgent;
  }

  private static final String TAG = Log.tag(UserAgentInterceptor.class);

  @Override
  public Response intercept(@NonNull Chain chain) throws IOException {
    final Request request = chain.request();
    Log.d(TAG,"intercept request : " + request.url());
    return chain.proceed(request.newBuilder()
                                .header("User-Agent", userAgent)
                                .build());
  }
}
