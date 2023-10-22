package org.thoughtcrime.securesms.push

import android.content.Context
import okhttp3.Dns
import okhttp3.Interceptor
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.net.CustomDns
import org.thoughtcrime.securesms.net.DeprecatedClientPreventionInterceptor
import org.thoughtcrime.securesms.net.DeviceTransferBlockingInterceptor
import org.thoughtcrime.securesms.net.RemoteDeprecationDetectorInterceptor
import org.thoughtcrime.securesms.net.SequentialDns
import org.thoughtcrime.securesms.net.StandardUserAgentInterceptor
import org.thoughtcrime.securesms.net.StaticDns
import org.thoughtcrime.securesms.util.Base64
import org.whispersystems.signalservice.api.push.TrustStore
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl
import org.whispersystems.signalservice.internal.configuration.SignalStorageUrl
import java.io.IOException
import java.util.Optional

/**
 * Provides a [SignalServiceConfiguration] to be used with our service layer.
 * If you're looking for a place to start, look at [getConfiguration].
 */
open class SignalServiceNetworkAccess(context: Context) {
  companion object {
    @JvmField
    val DNS: Dns = SequentialDns(
      Dns.SYSTEM,
      CustomDns("1.1.1.1"),
      StaticDns(
        mapOf(
          BuildConfig.SIGNAL_URL.stripProtocol() to BuildConfig.SIGNAL_SERVICE_IPS.toSet(),
//          BuildConfig.STORAGE_URL.stripProtocol() to BuildConfig.SIGNAL_STORAGE_IPS.toSet(),
          BuildConfig.SIGNAL_SFU_URL.stripProtocol() to BuildConfig.SIGNAL_SFU_IPS.toSet(),
          BuildConfig.CONTENT_PROXY_HOST.stripProtocol() to BuildConfig.SIGNAL_CONTENT_PROXY_IPS.toSet(),
        )
      )
    )

    private fun String.stripProtocol(): String {
      return this.removePrefix("https://")
    }
  }

  private val serviceTrustStore: TrustStore = SignalServiceTrustStore(context)
  private val cdnTrustStore: TrustStore = CdnServiceTrustStore(context)
  private val cdn2TrustStore: TrustStore = Cdn2ServiceTrustStore(context)

  private val interceptors: List<Interceptor> = listOf(
    StandardUserAgentInterceptor(),
    RemoteDeprecationDetectorInterceptor(),
    DeprecatedClientPreventionInterceptor(),
    DeviceTransferBlockingInterceptor.getInstance()
  )

  private val zkGroupServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.ZKGROUP_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  private val genericServerPublicParams: ByteArray = try {
    Base64.decode(BuildConfig.GENERIC_SERVER_PUBLIC_PARAMS)
  } catch (e: IOException) {
    throw AssertionError(e)
  }

  open val uncensoredConfiguration: SignalServiceConfiguration = SignalServiceConfiguration(
    signalServiceUrls = arrayOf(SignalServiceUrl(BuildConfig.SIGNAL_URL, serviceTrustStore)),
    signalCdnUrlMap = mapOf(
      0 to arrayOf(SignalCdnUrl(BuildConfig.SIGNAL_CDN_URL2, cdn2TrustStore)),
      2 to arrayOf(SignalCdnUrl(BuildConfig.SIGNAL_CDN_URL, cdnTrustStore)),
      3 to arrayOf(SignalCdnUrl(BuildConfig.SIGNAL_CDN_URL, cdnTrustStore))
    ),
//    signalStorageUrls = arrayOf(SignalStorageUrl(BuildConfig.STORAGE_URL, serviceTrustStore)),
    networkInterceptors = interceptors,
    dns = Optional.of(DNS),
    signalProxy = if (SignalStore.proxy().isProxyEnabled) Optional.ofNullable(SignalStore.proxy().proxy) else Optional.empty(),
    zkGroupServerPublicParams = zkGroupServerPublicParams,
    genericServerPublicParams = genericServerPublicParams
  )

  open fun getConfiguration(): SignalServiceConfiguration {
    return getConfiguration(SignalStore.account().e164)
  }

  open fun getConfiguration(e164: String?): SignalServiceConfiguration {
      return uncensoredConfiguration
  }

  fun isCensored(): Boolean {
    return isCensored(SignalStore.account().e164)
  }

  fun isCensored(number: String?): Boolean {
    return false
  }

  fun isCountryCodeCensoredByDefault(countryCode: Int): Boolean {
    return false
  }
}
