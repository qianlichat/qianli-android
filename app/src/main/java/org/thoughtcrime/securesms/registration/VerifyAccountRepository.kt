/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration

import android.app.Application
import android.util.Base64
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.signal.core.util.logging.Log
import org.signal.libsignal.protocol.IdentityKeyPair
import org.thoughtcrime.securesms.AppCapabilities
import org.thoughtcrime.securesms.gcm.FcmUtil
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.push.AccountManagerFactory
import org.thoughtcrime.securesms.registration.PushChallengeRequest.PushChallengeEvent
import org.thoughtcrime.securesms.util.RSAUtils
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.whispersystems.signalservice.api.SignalServiceAccountManager
import org.whispersystems.signalservice.api.SvrNoDataException
import org.whispersystems.signalservice.api.account.AccountAttributes
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccess
import org.whispersystems.signalservice.api.kbs.MasterKey
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.api.push.exceptions.NoSuchSessionException
import org.whispersystems.signalservice.internal.ServiceResponse
import org.whispersystems.signalservice.internal.push.RegistrationSessionMetadataResponse
import org.whispersystems.signalservice.internal.push.RegistrationSessionState
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * Request SMS/Phone verification codes to help prove ownership of a phone number.
 */
class VerifyAccountRepository(private val context: Application) {

  fun validateSession(
    sessionId: String?,
    e164: String,
    password: String
  ): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    return if (sessionId.isNullOrBlank()) {
      Single.just(ServiceResponse.forApplicationError(NoSuchSessionException(), 409, null))
    } else {
      val accountManager: SignalServiceAccountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, SignalServiceAddress.DEFAULT_DEVICE_ID, password)
      Single.fromCallable { accountManager.getRegistrationSession(sessionId) }.subscribeOn(Schedulers.io())
    }
  }

  fun requestValidSession(
    e164: String,
    password: String,
    mcc: String?,
    mnc: String?
  ): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    Log.d(TAG, "Initializing registration session.")
    return Single.fromCallable {
//      val fcmToken: String? = FcmUtil.getToken(context).orElse(null)
      val accountManager: SignalServiceAccountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, SignalServiceAddress.DEFAULT_DEVICE_ID, password)
//      if (fcmToken == null) {
        return@fromCallable accountManager.createRegistrationSession(null, mcc, mnc)
//      } else {
//        return@fromCallable createSessionAndBlockForPushChallenge(accountManager, fcmToken, mcc, mnc)
//      }
    }
      .subscribeOn(Schedulers.io())
  }

  private fun createSessionAndBlockForPushChallenge(accountManager: SignalServiceAccountManager, fcmToken: String, mcc: String?, mnc: String?): ServiceResponse<RegistrationSessionMetadataResponse> {
    val subscriber = PushTokenChallengeSubscriber()
    val eventBus = EventBus.getDefault()
    eventBus.register(subscriber)

    val response: ServiceResponse<RegistrationSessionMetadataResponse> = accountManager.createRegistrationSession(fcmToken, mcc, mnc)

    if (!response.result.isPresent) {
      return response
    }

    val receivedPush = subscriber.latch.await(PUSH_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
    eventBus.unregister(subscriber)

    if (receivedPush) {
      val challenge = subscriber.challenge
      if (challenge != null) {
        Log.w(TAG, "Push challenge token received.")
        return accountManager.submitPushChallengeToken(response.result.get().body.id, challenge)
      } else {
        Log.w(TAG, "Push received but challenge token was null.")
      }
    } else {
      Log.i(TAG, "Push challenge timed out.")
    }
    Log.i(TAG, "Push challenge unsuccessful. Updating registration state accordingly.")
    val registrationSessionState = RegistrationSessionState(pushChallengeTimedOut = true)
    val rawResponse: RegistrationSessionMetadataResponse = response.result.get()
    return ServiceResponse.forResult(rawResponse.copy(state = registrationSessionState), 200, null)
  }

  fun requestAndVerifyPushToken(
    sessionId: String,
    e164: String,
    password: String
  ): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    val fcmToken: Optional<String> = FcmUtil.getToken(context)
    val accountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, SignalServiceAddress.DEFAULT_DEVICE_ID, password)
    val pushChallenge = PushChallengeRequest.getPushChallengeBlocking(accountManager, sessionId, fcmToken, PUSH_REQUEST_TIMEOUT)
    return Single.fromCallable {
      return@fromCallable accountManager.submitPushChallengeToken(sessionId, pushChallenge.orElse(null))
    }.subscribeOn(Schedulers.io())
  }

  fun verifyCaptcha(
    sessionId: String,
    captcha: String,
    e164: String,
    password: String
  ): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    val accountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, SignalServiceAddress.DEFAULT_DEVICE_ID, password)
    return Single.fromCallable {
      return@fromCallable accountManager.submitCaptchaToken(sessionId, captcha)
    }.subscribeOn(Schedulers.io())
  }

  fun requestVerificationCode(
    sessionId: String,
    e164: String,
    password: String,
    mode: Mode
  ): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    Log.d(TAG, "SMS Verification requested")

    return Single.fromCallable {
      val accountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, SignalServiceAddress.DEFAULT_DEVICE_ID, password)
      if (mode == Mode.PHONE_CALL) {
        return@fromCallable accountManager.requestVoiceVerificationCode(sessionId, Locale.getDefault(), mode.isSmsRetrieverSupported)
      } else {
        return@fromCallable accountManager.requestSmsVerificationCode(sessionId, Locale.getDefault(), mode.isSmsRetrieverSupported)
      }
    }.subscribeOn(Schedulers.io())
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

  fun verifyAccount(sessionId: String, registrationData: RegistrationData, publicKey: String): Single<ServiceResponse<RegistrationSessionMetadataResponse>> {
    val accountManager: SignalServiceAccountManager = AccountManagerFactory.getInstance().createUnauthenticated(
      context,
      registrationData.e164,
      SignalServiceAddress.DEFAULT_DEVICE_ID,
      registrationData.password
    )

    return Single.fromCallable {
      accountManager.verifyAccount(
        registrationData.code,
        sessionId
      )
    }.subscribeOn(Schedulers.io())
  }

  fun registerAccount(sessionId: String?, pass : String,registrationData: RegistrationData, pin: String? = null, masterKeyProducer: Object? = null,publicKey: String): Single<ServiceResponse<VerifyResponse>> {
    val universalUnidentifiedAccess: Boolean = TextSecurePreferences.isUniversalUnidentifiedAccess(context)
    val unidentifiedAccessKey: ByteArray = UnidentifiedAccess.deriveAccessKeyFrom(registrationData.profileKey)

    val accountManager: SignalServiceAccountManager = AccountManagerFactory.getInstance().createUnauthenticated(
      context,
      registrationData.e164,
      SignalServiceAddress.DEFAULT_DEVICE_ID,
      registrationData.password
    )

//    val masterKey: MasterKey? = masterKeyProducer?.produceMasterKey()
//    val registrationLock: String? = masterKey?.deriveRegistrationLock()

    val accountAttributes = AccountAttributes(
      signalingKey = null,
      registrationId = registrationData.registrationId,
      fetchesMessages = registrationData.isNotFcm,
      registrationLock = null,
      unidentifiedAccessKey = unidentifiedAccessKey,
      unrestrictedUnidentifiedAccess = universalUnidentifiedAccess,
      capabilities = AppCapabilities.getCapabilities(true),
      discoverableByPhoneNumber = SignalStore.phoneNumberPrivacy().phoneNumberListingMode.isDiscoverable,
      name = null,
      pniRegistrationId = registrationData.pniRegistrationId,
      recoveryPassword = registrationData.recoveryPassword
    )

    SignalStore.account().generateAciIdentityKeyIfNecessary()
    val aciIdentity: IdentityKeyPair = SignalStore.account().aciIdentityKey

    SignalStore.account().generatePniIdentityKeyIfNecessary()
    val pniIdentity: IdentityKeyPair = SignalStore.account().pniIdentityKey

    val aciPreKeyCollection = RegistrationRepository.generateSignedAndLastResortPreKeys(aciIdentity, SignalStore.account().aciPreKeys)
    val pniPreKeyCollection = RegistrationRepository.generateSignedAndLastResortPreKeys(pniIdentity, SignalStore.account().pniPreKeys)

    return Single.fromCallable {
      val passHash: String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val encodedHash = digest.digest(pass.toByteArray(StandardCharsets.UTF_8))
        bytesToHex(encodedHash)
      } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
      }
      Log.d(TAG, "registerAccount before encrypt = $passHash")
      val passDecrypted = RSAUtils.encrypt(passHash,RSAUtils.getPublicKeyFromBase64(publicKey))
      Log.d(TAG, "registerAccount after encrypt = $passDecrypted")
      Log.d(TAG, "registerAccount session id = $sessionId")
      val response = accountManager.registerAccount(sessionId, passDecrypted, registrationData.recoveryPassword, accountAttributes, aciPreKeyCollection, pniPreKeyCollection, registrationData.fcmToken, true)
      VerifyResponse.from(response, null, pin, aciPreKeyCollection, pniPreKeyCollection)
    }.subscribeOn(Schedulers.io())
  }

  fun getFcmToken(): Single<String> {
    return Single.fromCallable {
      return@fromCallable FcmUtil.getToken(context).orElse("")
    }.subscribeOn(Schedulers.io())
  }

  enum class Mode(val isSmsRetrieverSupported: Boolean) {
    SMS_WITH_LISTENER(true),
    SMS_WITHOUT_LISTENER(false),
    PHONE_CALL(false)
  }

  private class PushTokenChallengeSubscriber {
    var challenge: String? = null
    val latch = CountDownLatch(1)

    @Subscribe
    fun onChallengeEvent(pushChallengeEvent: PushChallengeEvent) {
      challenge = pushChallengeEvent.challenge
      latch.countDown()
    }
  }

  companion object {
    private val TAG = Log.tag(VerifyAccountRepository::class.java)
    private val PUSH_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
