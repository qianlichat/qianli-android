package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON POJO that represents the returned ACI from a call to
 * /v1/account/username/[username]
 */
class GetAccountTotpGenResponse {
  @JsonProperty
  private String totp;

  GetAccountTotpGenResponse() {}

  public String getTotp() {
    return totp;
  }

  public void setTotp(String totp) {
    this.totp = totp;
  }
}
