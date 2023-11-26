package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON POJO that represents the returned ACI from a call to
 * /v1/account/username/[username]
 */
class GetAccountTotpBindResponse {
  @JsonProperty
  private boolean success;

  GetAccountTotpBindResponse() {}

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }
}
