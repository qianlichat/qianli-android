/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationDismissedReceiver: BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    context?.stopService(Intent(context,IncomingMessageObserver.ForegroundService::class.java))
  }
}