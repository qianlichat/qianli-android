package org.thoughtcrime.securesms.registration.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import org.signal.core.util.concurrent.SimpleTask;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.registration.viewmodel.RegistrationViewModel;
import org.thoughtcrime.securesms.util.FeatureFlags;
import org.thoughtcrime.securesms.util.navigation.SafeNavigation;

import java.io.IOException;

public final class PasswordCreationFragment extends BaseEnterPasswordFragment<RegistrationViewModel> {

  RegistrationViewModel registrationViewModel;

  private static final String TAG = Log.tag(PasswordCreationFragment.class);


  public PasswordCreationFragment() {
    super(R.layout.fragment_registration_enter_password);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (isAccountExists()) {
      verifyHeader.setText(R.string.RegistrationActivity_verification_login_account);
      subheader.setText(R.string.RegistrationActivity_enter_the_password_login);
      editTextPasswordConfirm.setVisibility(View.GONE);
      passInputLayout.setHint(R.string.RegistrationActivity_enter_password);
    }
  }

  @Override protected boolean isAccountExists() {
    return getViewModel().isAccountsExists();
  }

  @Override
  protected @NonNull RegistrationViewModel getViewModel() {
    if(registrationViewModel == null) {
      registrationViewModel = new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
    }
    return registrationViewModel;
  }

  @Override
  protected void handleSuccessfulVerify() {
    displaySuccess(() -> SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), PasswordCreationFragmentDirections.actionSuccessfulRegistration()));
  }
}
