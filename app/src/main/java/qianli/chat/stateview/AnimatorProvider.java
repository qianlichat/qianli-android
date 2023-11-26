/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package qianli.chat.stateview;


import android.animation.Animator;
import android.view.View;

/**
 * @author Nukc.
 */

public interface AnimatorProvider {

  Animator showAnimation(View view);

  Animator hideAnimation(View view);
}
