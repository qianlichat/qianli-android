package org.whispersystems.signalservice.internal.configuration;



import org.whispersystems.signalservice.api.push.TrustStore;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import okhttp3.ConnectionSpec;

public class SignalUrl {

  private final String                   url;
  private final Optional<String>         hostHeader;
  private final Optional<ConnectionSpec> connectionSpec;

  public SignalUrl(String url) {
    this(url, null, null);
  }

  public SignalUrl(String url, String hostHeader,
                   ConnectionSpec connectionSpec)
  {
    this.url            = url;
    this.hostHeader     = Optional.ofNullable(hostHeader);
    this.connectionSpec = Optional.ofNullable(connectionSpec);
  }


  public Optional<String> getHostHeader() {
    return hostHeader;
  }

  public String getUrl() {
    return url;
  }

  public Optional<List<ConnectionSpec>> getConnectionSpecs() {
    return connectionSpec.isPresent() ? Optional.of(Collections.singletonList(connectionSpec.get())) : Optional.empty();
  }

}
