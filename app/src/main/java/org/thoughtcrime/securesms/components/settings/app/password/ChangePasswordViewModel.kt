/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.password

import androidx.lifecycle.ViewModel
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.whispersystems.signalservice.internal.ServiceResponse
import org.whispersystems.signalservice.internal.push.RegistrationSessionMetadataResponse

class ChangePasswordViewModel : ViewModel() {
  fun fetchPasswordSign(): ServiceResponse<RegistrationSessionMetadataResponse> = ApplicationDependencies.getSignalServiceAccountManager().passwordSign
  fun changePassword(sessionId: String, passEncCurrent: String, passEncNew: String) = ApplicationDependencies.getSignalServiceAccountManager().changePassword(sessionId, passEncCurrent, passEncNew)
}