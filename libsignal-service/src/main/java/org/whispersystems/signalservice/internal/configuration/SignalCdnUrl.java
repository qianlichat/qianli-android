package org.whispersystems.signalservice.internal.configuration;


import org.whispersystems.signalservice.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class SignalCdnUrl extends SignalUrl {
  public SignalCdnUrl(String url) {
    super(url);
  }

  public SignalCdnUrl(String url, String hostHeader, ConnectionSpec connectionSpec) {
    super(url, hostHeader, connectionSpec);
  }
}
