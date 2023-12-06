package org.whispersystems.signalservice.internal.configuration;


import org.whispersystems.signalservice.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class SignalStorageUrl extends SignalUrl {

  public SignalStorageUrl(String url) {
    super(url);
  }

  public SignalStorageUrl(String url, String hostHeader, ConnectionSpec connectionSpec) {
    super(url, hostHeader, connectionSpec);
  }
}
