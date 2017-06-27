package cm.aptoide.pt.spotandshareapp.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import cm.aptoide.pt.spotandshareapp.AppModel;
import cm.aptoide.pt.spotandshareapp.Header;
import cm.aptoide.pt.spotandshareapp.InstalledRepositoryDummy;
import cm.aptoide.pt.spotandshareapp.R;
import cm.aptoide.pt.spotandshareapp.presenter.SpotAndShareAppSelectionPresenter;
import java.util.List;

/**
 * Created by filipe on 12-06-2017.
 */

public class SpotAndShareAppSelectionFragment extends FragmentView
    implements SpotAndShareAppSelectionView {

  private RecyclerView recyclerView;
  private SpotAndShareAppSelectionAdapter adapter;

  public static Fragment newInstance() {
    Fragment fragment = new SpotAndShareAppSelectionFragment();
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override public void finish() {
    getActivity().finish();
  }

  @Override public void setupRecyclerView(List<AppModel> installedApps) {
    adapter = new SpotAndShareAppSelectionAdapter(
        new Header(getResources().getString(R.string.spotandshare_title_pick_apps_to_send)),
        installedApps);
    recyclerView.setAdapter(adapter);
    setupLayoutManager();
    recyclerView.setHasFixedSize(true);
  }

  private void setupLayoutManager() {
    GridLayoutManager gridLayoutManager = new GridLayoutManager(this.getContext(), 3);
    gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override public int getSpanSize(int position) {
        if (adapter.isPositionHeader(position)) {
          return gridLayoutManager.getSpanCount();
        }
        return 1;
      }
    });
    recyclerView.setLayoutManager(gridLayoutManager);
  }

  @Override public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    recyclerView = (RecyclerView) view.findViewById(R.id.app_selection_recycler_view);
    attachPresenter(new SpotAndShareAppSelectionPresenter(this,
        new InstalledRepositoryDummy(getContext().getPackageManager())), savedInstanceState);
  }

  @Override public void onDestroyView() {
    adapter = null;
    recyclerView = null;
    super.onDestroyView();
  }

  @Nullable @Override
  public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_spotandshare_app_selection, container, false);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    finish();
    return super.onOptionsItemSelected(item);
  }
}