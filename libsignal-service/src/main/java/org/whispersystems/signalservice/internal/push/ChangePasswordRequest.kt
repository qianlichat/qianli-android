/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.whispersystems.signalservice.internal.push

import com.fasterxml.jackson.annotation.JsonProperty

data class ChangePasswordRequest(
  @JsonProperty val sessionId: String,
  @JsonProperty val oldPwd: String,
  @JsonProperty val newPwd: String,
)
