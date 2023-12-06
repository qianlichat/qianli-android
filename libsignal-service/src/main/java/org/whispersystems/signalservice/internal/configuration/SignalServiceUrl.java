package org.whispersystems.signalservice.internal.configuration;


import org.whispersystems.signalservice.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class SignalServiceUrl extends SignalUrl {

  public SignalServiceUrl(String url) {
    super(url);
  }

  public SignalServiceUrl(String url, String hostHeader, ConnectionSpec connectionSpec) {
    super(url, hostHeader, connectionSpec);
  }
}
