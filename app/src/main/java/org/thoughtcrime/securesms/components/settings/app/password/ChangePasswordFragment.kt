/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.password

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.registration.VerificationPinKeyboard
import org.thoughtcrime.securesms.registration.VerifyAccountRepository
import org.thoughtcrime.securesms.registration.fragments.PasswordCreationFragmentDirections
import org.thoughtcrime.securesms.util.RSAUtils
import org.thoughtcrime.securesms.util.ViewUtil
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.whispersystems.signalservice.api.push.exceptions.AuthorizationFailedException
import org.whispersystems.signalservice.api.push.exceptions.RateLimitException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Objects

class ChangePasswordFragment : LoggingFragment(R.layout.fragment_change_password) {
  private lateinit var continueBtn: MaterialButton
  private lateinit var editTextCurrent: TextInputEditText
  private lateinit var editTextPassword: TextInputEditText
  private lateinit var editTextPasswordConfirm: TextInputEditText
  private lateinit var keyboard: VerificationPinKeyboard

  private lateinit var viewModel: ChangePasswordViewModel
  private var viewModelJob: Job? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel = ViewModelProvider(requireActivity())[ChangePasswordViewModel::class.java]
    continueBtn = view.findViewById(R.id.continue_button)
    editTextCurrent = view.findViewById(R.id.password_current)
    editTextPassword = view.findViewById(R.id.password)
    editTextPasswordConfirm = view.findViewById(R.id.password_confirm)
    keyboard = view.findViewById(R.id.keyboard)

    continueBtn.setOnClickListener { v: View? -> onNext() }
  }

  private fun onNext() {
    ViewUtil.hideKeyboard(requireContext(), editTextCurrent)
    val passCurrent = Objects.requireNonNull(editTextCurrent.text).toString()
    if (passCurrent.length < 9 || passCurrent.length > 18) {
      showErrorDialog(getString(R.string.ChangePasswordFragment_wrong_pass_current))
      return
    }
    val pass = Objects.requireNonNull(editTextPassword.text).toString()
    if (pass.length < 9 || pass.length > 18) {
      showErrorDialog(getString(R.string.ChangePasswordFragment_wrong_pass_new))
      return
    }
    val passConfirm = Objects.requireNonNull(editTextPasswordConfirm.text).toString()
    if (pass != passConfirm) {
      showErrorDialog(getString(R.string.ChangePasswordFragment_wrong_pass_new_confirm))
      return
    }
    displayProgress()

    viewModelJob = lifecycleScope.launch(Dispatchers.IO) {
      try {
        val signResp = viewModel.fetchPasswordSign()
        if (!signResp.result.isPresent) {
          withContext(Dispatchers.Main){
            handleGeneralError(null)
          }
          return@launch
        }

        val pk = signResp.result.get().body.publicKey

        val passEncCurrent: String = getPassEncrypted(passCurrent,pk)
        val passEncNew: String = getPassEncrypted(pass,pk)

        viewModel.changePassword(signResp.result.get().body.id ,passEncCurrent,passEncNew)

        withContext(Dispatchers.Main){
          handleSuccess()
        }
      } catch (t: Throwable) {
        if(t is AuthorizationFailedException){
          withContext(Dispatchers.Main){
            handleGeneralError(R.string.ChangePasswordFragment_wrong_pass_current_server)
          }
        }else if( t is RateLimitException){
          withContext(Dispatchers.Main){
            handleGeneralError(R.string.ChangePasswordFragment_wrong_pass_current_server)
          }
        }else{
          t.printStackTrace()
          withContext(Dispatchers.Main){
            handleGeneralError(null)
          }
        }
      }
    }

  }

  private fun getPassEncrypted(passCurrent: String,publicKey:String) = try {
    val digest = MessageDigest.getInstance("SHA-256")
    val encodedHash = digest.digest(passCurrent.toByteArray(StandardCharsets.UTF_8))
    val hasHex = bytesToHex(encodedHash)
    RSAUtils.encrypt(hasHex, RSAUtils.getPublicKeyFromBase64(publicKey))
  } catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(e)
  }

  private fun bytesToHex(hash: ByteArray): String {
    val hexString = StringBuilder(2 * hash.size)
    for (i in hash.indices) {
      val hex = Integer.toHexString(0xff and hash[i].toInt())
      if (hex.length == 1) {
        hexString.append('0')
      }
      hexString.append(hex)
    }
    return hexString.toString()
  }

  private fun handleSuccess() {
    Toast.makeText(requireContext(), R.string.ChangePasswordFragment_success, Toast.LENGTH_LONG).show()
    displaySuccess {
      requireActivity().finish()
    }
  }

  private fun displaySuccess(runAfterAnimation: Runnable) {
    continueBtn.visibility = View.GONE
    keyboard.visibility = View.VISIBLE
    keyboard.displaySuccess().addListener(object : AssertedSuccessListener<Boolean?>() {
      override fun onSuccess(result: Boolean?) {
        runAfterAnimation.run()
        hideProgress()
      }
    })
  }

  private fun handleGeneralError(toastRes: Int?) {
    Toast.makeText(requireContext(), toastRes ?: R.string.RegistrationActivity_error_connecting_to_service, Toast.LENGTH_LONG).show()
    keyboard.visibility = View.VISIBLE
    continueBtn.visibility = View.GONE
    keyboard.displayFailure().addListener(object : AssertedSuccessListener<Boolean?>() {
      override fun onSuccess(result: Boolean?) {
        hideProgress()
      }
    })
  }

  private fun hideProgress() {
    continueBtn.visibility = View.VISIBLE
    keyboard.visibility = View.GONE
//    keyboard.displayProgress();
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModelJob?.cancel()
  }

  private fun displayProgress() {
    continueBtn.visibility = View.GONE
    keyboard.visibility = View.VISIBLE
    keyboard.displayProgress()
  }

  fun showErrorDialog(msg: String?) {
    showErrorDialog(msg, null)
  }

  fun showErrorDialog(msg: String?, positiveButtonListener: DialogInterface.OnClickListener?) {
    MaterialAlertDialogBuilder(requireContext()).setMessage(msg).setPositiveButton(R.string.ok, positiveButtonListener).show()
  }

}