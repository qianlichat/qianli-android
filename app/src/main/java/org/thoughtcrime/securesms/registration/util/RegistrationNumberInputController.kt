package org.thoughtcrime.securesms.registration.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.compose.ui.text.toLowerCase
import org.thoughtcrime.securesms.registration.viewmodel.NumberViewState
import java.util.Locale

/**
 * Handle the logic and formatting of phone number input specifically for registration number the flow.
 */
class RegistrationNumberInputController(
  val context: Context,
  val callbacks: Callbacks,
  private val phoneNumberInputLayout: EditText
) {
//  private val spinnerView: MaterialAutoCompleteTextView = countryCodeInputLayout.editText as MaterialAutoCompleteTextView
//  private val supportedCountryPrefixes: List<CountryPrefix> = PhoneNumberUtil.getInstance().supportedCallingCodes
//    .map { CountryPrefix(it, PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(it)) }
//    .sortedBy { it.digits.toString() }
//  private val spinnerAdapter: ArrayAdapter<CountryPrefix> = ArrayAdapter<CountryPrefix>(context, R.layout.registration_country_code_dropdown_item, supportedCountryPrefixes)
//  private val countryCodeEntryListener = CountryCodeEntryListener()

//  private var countryFormatter: AsYouTypeFormatter? = null
  private var isUpdating = false

  init {
    setUpNumberInput()

//    spinnerView.threshold = 100
//    spinnerView.setAdapter(spinnerAdapter)
//    spinnerView.addTextChangedListener(countryCodeEntryListener)
  }

//  fun prepopulateCountryCode() {
//    if (spinnerView.editableText.isBlank()) {
//      spinnerView.setText(supportedCountryPrefixes[0].toString())
//    }
//  }

//  private fun advanceToPhoneNumberInput() {
//    if (!isUpdating) {
//      phoneNumberInputLayout.requestFocus()
//    }
//    val numberLength: Int = phoneNumberInputLayout.text?.length ?: 0
//    phoneNumberInputLayout.setSelection(numberLength, numberLength)
//  }

  private fun setUpNumberInput() {
//    phoneNumberInputLayout.setText("@")
    phoneNumberInputLayout.addTextChangedListener(NumberChangedListener())
    phoneNumberInputLayout.onFocusChangeListener = OnFocusChangeListener { v: View?, hasFocus: Boolean ->
      if (hasFocus) {
        callbacks.onNumberFocused()
      }
    }
    phoneNumberInputLayout.imeOptions = EditorInfo.IME_ACTION_DONE
    phoneNumberInputLayout.setOnEditorActionListener { v: TextView?, actionId: Int, _: KeyEvent? ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        callbacks.onNumberInputDone(v!!)
        return@setOnEditorActionListener true
      }
      false
    }
  }

//  fun setNumberAndCountryCode(numberViewState: NumberViewState) {
////    val countryCode = numberViewState.countryCode
//
//    isUpdating = true
//    phoneNumberInputLayout.setText(numberViewState.nationalNumber)
////    if (numberViewState.countryCode != 0) {
////      spinnerView.setText(supportedCountryPrefixes.first { it.digits == numberViewState.countryCode }.toString())
////    }
////    val regionCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(countryCode)
////    setCountryFormatter(regionCode)
//
//    isUpdating = false
//  }

  fun updateNumberFormatter(numberViewState: NumberViewState) {
//    val countryCode = numberViewState.countryCode

//    isUpdating = true
//    val regionCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(countryCode)
//    setCountryFormatter(regionCode)

//    isUpdating = false
  }

//  private fun setCountryFormatter(regionCode: String?) {
//    val util = PhoneNumberUtil.getInstance()
//    countryFormatter = if (regionCode != null) util.getAsYouTypeFormatter(regionCode) else null
//    reformatText(phoneNumberInputLayout.text)
//  }

//  private fun reformatText(editable: Editable): String? {
//    if (TextUtils.isEmpty(editable)) {
//      return null
//    }
//    val countryFormatter: AsYouTypeFormatter = countryFormatter ?: return null
//    countryFormatter.clear()
//    var formattedNumber: String? = null
//    val justDigits = StringBuilder()
//    for (character in editable) {
//      if (Character.isDigit(character)) {
//        formattedNumber = countryFormatter.inputDigit(character)
//        justDigits.append(character)
//      }
//    }
//    if (formattedNumber != null && editable.toString() != formattedNumber) {
//      editable.replace(0, editable.length, formattedNumber)
//    }
//    return if (justDigits.isEmpty()) {
//      null
//    } else {
//      justDigits.toString()
//    }
//  }

  inner class NumberChangedListener : TextWatcher {
    override fun afterTextChanged(s: Editable) {
//      val number: String = reformatText(s) ?: return
      if (!isUpdating) {
        callbacks.setNationalNumber(s.toString())
      }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    @SuppressLint("SetTextI18n")
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
      if(!isUpdating) {
        if (!s.toString().startsWith("@")) {
          isUpdating = true
          phoneNumberInputLayout.setText("@$s");
          phoneNumberInputLayout.setSelection(s.length + 1)
          isUpdating = false
        }
        val se = phoneNumberInputLayout.selectionEnd
        if(containsUpperCase(s)){
          phoneNumberInputLayout.setText(s.toString().lowercase())
          phoneNumberInputLayout.setSelection(se)
        }
      }
    }

    private fun containsUpperCase(str: CharSequence): Boolean {
      for (element in str) {
        if (element != '@' && Character.isUpperCase(element)) {
            return true
          }
      }
      return false
    }
  }

//  inner class CountryCodeEntryListener : TextWatcher {
//
//    override fun afterTextChanged(s: Editable?) {
//      if (s.isNullOrEmpty()) {
//        return
//      }
//
//      if (s[0] != '+') {
//        s.insert(0, "+")
//      }
//
////      supportedCountryPrefixes.firstOrNull { it.toString() == s.toString() }?.let {
////        setCountryFormatter(it.regionCode)
////        callbacks.setCountry(it.digits)
////        advanceToPhoneNumberInput()
////      }
//    }
//
//    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//  }

  interface Callbacks {
    fun onNumberFocused()
    fun onNumberInputDone(view: View)
    fun setNationalNumber(number: String)
    fun setCountry(countryCode: Int)
  }
}

//data class CountryPrefix(val digits: Int, val regionCode: String) {
//  override fun toString(): String {
//    return "+$digits"
//  }
//}
