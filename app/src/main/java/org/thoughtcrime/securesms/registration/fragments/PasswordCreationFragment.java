package org.thoughtcrime.securesms.registration.fragments;

import androidx.annotation.NonNull;
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

  private static final String TAG = Log.tag(PasswordCreationFragment.class);

  public PasswordCreationFragment() {
    super(R.layout.fragment_registration_enter_password);
  }

  @Override
  protected @NonNull RegistrationViewModel getViewModel() {
    return new ViewModelProvider(requireActivity()).get(RegistrationViewModel.class);
  }

  @Override
  protected void handleSuccessfulVerify() {
    SimpleTask.run(() -> {
      long startTime = System.currentTimeMillis();
      try {
        FeatureFlags.refreshSync();
        Log.i(TAG, "Took " + (System.currentTimeMillis() - startTime) + " ms to get feature flags.");
      } catch (IOException e) {
        Log.w(TAG, "Failed to refresh flags after " + (System.currentTimeMillis() - startTime) + " ms.", e);
      }
      return null;
    }, none -> displaySuccess(() -> SafeNavigation.safeNavigate(Navigation.findNavController(requireView()), PasswordCreationFragmentDirections.actionSuccessfulRegistration())));
  }
}
