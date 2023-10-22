package org.thoughtcrime.securesms.push;

import android.content.Context;

import org.thoughtcrime.securesms.R;
import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.InputStream;

public class Cdn2ServiceTrustStore implements TrustStore {

  private final Context context;

  public Cdn2ServiceTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.cdn2);
  }

  @Override
  public String getKeyStorePassword() {
    return "whisper";
  }
}
