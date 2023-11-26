/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.databinding.ActivityBind2faBinding
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies


class Bind2FAActivity : BaseActivity() {

  val binding by lazy { ActivityBind2faBinding.inflate(LayoutInflater.from(this)) }
  private val lifecycleDisposable = LifecycleDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    binding.stateView.showLoading()
    lifecycleDisposable.bindTo(this)

    bind()
    load()
  }

  private fun bind() {
    binding.llKey.setOnClickListener {
      val cm: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val mClipData = ClipData.newPlainText("qianli.chat", binding.tvKey.text.toString())
      cm.setPrimaryClip(mClipData)
      Toast.makeText(this@Bind2FAActivity, getString(R.string.Bind2FAActivity__copied),Toast.LENGTH_LONG).show()
    }

    binding.btnSubmit.setOnClickListener {
      checkTotp(binding.etOtp.text.toString())
    }

    binding.toolbar.setNavigationOnClickListener{
      finish()
    }
  }

  private fun checkTotp(otp: String) {
    binding.btnSubmit.isEnabled = false
    binding.stateView.showLoading()
    val d = Single.fromCallable {
      ApplicationDependencies.getSignalServiceAccountManager().bind2FA(otp)
    }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          binding.btnSubmit.isEnabled = true
          binding.stateView.showContent()
          if(it){
            Toast.makeText(this@Bind2FAActivity, getString(R.string.Bind2FAActivity__success),Toast.LENGTH_LONG).show()
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            defaultSharedPreferences.edit().putInt(MainActivity.OTP_BIND_ALREADY,1).apply()
            finish()
          }else{
            Toast.makeText(this@Bind2FAActivity, getString(R.string.Bind2FAActivity__failed),Toast.LENGTH_LONG).show()
          }
        },
        {
          binding.btnSubmit.isEnabled = true
          binding.stateView.showContent()
          Toast.makeText(this@Bind2FAActivity,getString(R.string.Bind2FAActivity__failed),Toast.LENGTH_LONG).show()
        })

    lifecycleDisposable.add(d)
  }

  private fun load() {
    binding.llReal.visibility = View.GONE
    val d = Single.fromCallable {
      ApplicationDependencies.getSignalServiceAccountManager().genOtpToBind()
    }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          binding.stateView.showContent()
          binding.llReal.visibility = View.VISIBLE
          binding.tvKey.text = it
        },
        {
          binding.stateView.showRetry()
          binding.stateView.setOnRetryClickListener {
            load()
          }
        })

    lifecycleDisposable.add(d)
  }

}