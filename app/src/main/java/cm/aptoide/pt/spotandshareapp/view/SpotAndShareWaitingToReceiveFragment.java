package cm.aptoide.pt.spotandshareapp.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.spotandshareapp.JoinGroupView;
import cm.aptoide.pt.spotandshareapp.presenter.SpotAndShareWaitingToReceivePresenter;
import cm.aptoide.pt.view.BackButtonFragment;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import rx.Observable;

/**
 * Created by filipe on 12-06-2017.
 */

public class SpotAndShareWaitingToReceiveFragment extends BackButtonFragment
    implements SpotAndShareWaitingToReceiveView {

  private ImageView refreshButton;
  private Toolbar toolbar;
  private PublishRelay<Void> backRelay;
  private ClickHandler clickHandler;
  private RxAlertDialog backDialog;
  private JoinGroupView joinGroupView;

  public static Fragment newInstance() {
    Fragment fragment = new SpotAndShareWaitingToReceiveFragment();
    return fragment;
  }

  @Override public void onAttach(Activity activity) {
    if (activity instanceof JoinGroupView) {
      joinGroupView = (JoinGroupView) activity;
    } else {
      throw new IllegalArgumentException(
          "In order to use this framgent, you need to implement JoinGroupView");
    }
    super.onAttach(activity);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    backRelay = PublishRelay.create();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    refreshButton = (ImageView) view.findViewById(R.id.sync_image);
    toolbar = (Toolbar) view.findViewById(R.id.spotandshare_toolbar);
    setupToolbar();
    clickHandler = () -> {
      backRelay.call(null);
      return true;
    };
    registerClickHandler(clickHandler);
    backDialog = new RxAlertDialog.Builder(getContext()).setMessage(
        R.string.spotandshare_message_leave_group_warning)
        .setPositiveButton(R.string.spotandshare_button_leave_group)
        .setNegativeButton(R.string.spotandshare_button_cancel_leave_group)
        .build();

    joinGroupView.registerJoinGroupSuccessCallback(
        onSuccess -> openSpotandShareTransferRecordFragment());

    attachPresenter(new SpotAndShareWaitingToReceivePresenter(this,
            ((AptoideApplication) getActivity().getApplicationContext()).getSpotAndShare()),
        savedInstanceState);
  }

  @Override public void onDestroyView() {
    refreshButton = null;
    toolbar = null;
    unregisterClickHandler(clickHandler);
    clickHandler = null;
    backDialog = null;
    joinGroupView.unregisterJoinGroupSuccessCallback();
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    backRelay = null;
    super.onDestroy();
  }

  private void setupToolbar() {
    setHasOptionsMenu(true);
    toolbar.setTitle(R.string.spotandshare_title_toolbar);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_spotandshare_waiting_to_receive, container, false);
  }

  @Override public void finish() {
    getActivity().finish();
  }

  @Override public Observable<Void> startSearch() {
    return RxView.clicks(refreshButton);
  }

  @Override public void openSpotandShareTransferRecordFragment() {
    getFragmentNavigator().cleanBackStack();
    getFragmentNavigator().navigateTo(SpotAndShareTransferRecordFragment.newInstance());
  }

  @Override public Observable<Void> backButtonEvent() {
    return backRelay;
  }

  @Override public void showExitWarning() {
    backDialog.show();
  }

  @Override public Observable<Void> exitEvent() {
    return backDialog.positiveClicks()
        .map(dialogInterface -> null);
  }

  @Override public void navigateBack() {
    getFragmentNavigator().cleanBackStack();
    getFragmentNavigator().navigateToWithoutBackSave(SpotAndShareMainFragment.newInstance());
  }

  @Override public void onLeaveGroupError() {
    showLeaveGroupErrorMessage();
  }

  @Override public void joinGroup() {
    joinGroupView.joinGroup();
  }

  private void showLeaveGroupErrorMessage() {
    Toast.makeText(getContext(), "There was an error while trying to leave the group",
        Toast.LENGTH_SHORT)
        .show();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      backRelay.call(null);
    }
    return false;
  }
}