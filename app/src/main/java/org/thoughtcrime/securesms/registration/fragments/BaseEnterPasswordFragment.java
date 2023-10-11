package org.thoughtcrime.securesms.registration.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.signal.core.util.concurrent.LifecycleDisposable;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.LoggingFragment;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.registration.VerificationPinKeyboard;
import org.thoughtcrime.securesms.registration.VerifyResponseProcessor;
import org.thoughtcrime.securesms.registration.VerifyResponseWithoutKbs;
import org.thoughtcrime.securesms.registration.viewmodel.BaseRegistrationViewModel;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.concurrent.AssertedSuccessListener;
import org.whispersystems.signalservice.internal.push.LockedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

import static org.thoughtcrime.securesms.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView;

/**
 * Base fragment used by registration and change number flow to input an SMS verification code or request a
 * phone code after requesting SMS.
 *
 * @param <ViewModel> - The concrete view model used by the subclasses, for ease of access in said subclass
 */
public abstract class BaseEnterPasswordFragment<ViewModel extends BaseRegistrationViewModel> extends LoggingFragment {

  private static final String TAG = Log.tag(BaseEnterPasswordFragment.class);

  private ScrollView              scrollView;
  private TextView                subheader;
  private MaterialButton          continueBtn;
  private TextInputEditText       editTextPassword;
  private TextInputEditText       editTextPasswordConfirm;
  private VerificationPinKeyboard keyboard;
  private ViewModel               viewModel;

  protected final LifecycleDisposable disposables = new LifecycleDisposable();

  public BaseEnterPasswordFragment(@LayoutRes int contentLayoutId) {
    super(contentLayoutId);
  }

  @Override
  @CallSuper
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setDebugLogSubmitMultiTapView(view.findViewById(R.id.verify_header));

    scrollView              = view.findViewById(R.id.scroll_view);
    subheader               = view.findViewById(R.id.verification_subheader);
    continueBtn             = view.findViewById(R.id.continue_button);
    editTextPassword        = view.findViewById(R.id.password);
    editTextPasswordConfirm = view.findViewById(R.id.password_confirm);
    keyboard                = view.findViewById(R.id.keyboard);
    ViewUtil.hideKeyboard(requireContext(), view);
    continueBtn.setOnClickListener(v -> onNext());

    disposables.bindTo(getViewLifecycleOwner().getLifecycle());
    viewModel = getViewModel();
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        viewModel.resetSession();
        this.remove();
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
      }
    });
  }

  public void showErrorDialog(Context context, String msg) {
    showErrorDialog(context, msg, null);
  }

  public void showErrorDialog(Context context, String msg, DialogInterface.OnClickListener positiveButtonListener) {
    new MaterialAlertDialogBuilder(context).setMessage(msg).setPositiveButton(R.string.ok, positiveButtonListener).show();
  }

  private void onNext(){
    String pass = Objects.requireNonNull(editTextPassword.getText()).toString();
    String passConfirm = Objects.requireNonNull(editTextPasswordConfirm.getText()).toString();
    if(pass.length() < 9 || pass.length() > 18){
      showErrorDialog(requireContext(), getString(R.string.RegistrationActivity_please_enter_a_valid_password_to_register));
      return;
    }
    if(!pass.equals(passConfirm)){
      showErrorDialog(requireContext(), getString(R.string.RegistrationActivity_please_enter_a_same_password_to_register));
      return;
    }
    keyboard.setVisibility(View.VISIBLE);
    keyboard.displayProgress();
//    return registrationRepository.registerAccount(getRegistrationData(), processor.getResult(), false)
//                                 .map(VerifyResponseWithoutKbs::new);

    Disposable verify = viewModel.registerAccount(pass)
        .flatMap(resp -> Single.just(new VerifyResponseWithoutKbs(resp)))
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribe((VerifyResponseProcessor processor) -> {
                                   if (!processor.hasResult()) {
                                     Log.w(TAG, "post register: ", processor.getError());
                                   }
                                   if (processor.hasResult()) {
                                     handleSuccessfulVerify();
                                   } else if (processor.authorizationFailed()) {
                                     handleIncorrectCodeError();
                                   } else {
                                     Log.w(TAG, "Unable to register", processor.getError());
                                     handleGeneralError();
                                   }
                                 });

    disposables.add(verify);
  }

  protected abstract ViewModel getViewModel();

  protected abstract void handleSuccessfulVerify();

  private void returnToPhoneEntryScreen() {
    viewModel.resetSession();
    Navigation.findNavController(requireView()).navigateUp();
  }

  protected void displaySuccess(@NonNull Runnable runAfterAnimation) {
    keyboard.setVisibility(View.VISIBLE);
    keyboard.displaySuccess().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        runAfterAnimation.run();
        keyboard.setVisibility(View.INVISIBLE);
      }
    });
  }

  protected void handleIncorrectCodeError() {
    Toast.makeText(requireContext(), R.string.RegistrationActivity_incorrect_code, Toast.LENGTH_LONG).show();
    keyboard.setVisibility(View.VISIBLE);
    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        keyboard.setVisibility(View.INVISIBLE);
      }
    });
  }

  protected void handleGeneralError() {
    Toast.makeText(requireContext(), R.string.RegistrationActivity_error_connecting_to_service, Toast.LENGTH_LONG).show();
    keyboard.setVisibility(View.VISIBLE);
    keyboard.displayFailure().addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        keyboard.setVisibility(View.INVISIBLE);
      }
    });
  }
}
