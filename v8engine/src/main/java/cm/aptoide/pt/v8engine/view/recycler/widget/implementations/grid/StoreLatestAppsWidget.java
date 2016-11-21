package cm.aptoide.pt.v8engine.view.recycler.widget.implementations.grid;

import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.BuildConfig;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.interfaces.FragmentShower;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.StoreLatestAppsDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import com.jakewharton.rxbinding.view.RxView;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by marcelobenites on 6/21/16.
 */
public class StoreLatestAppsWidget extends Widget<StoreLatestAppsDisplayable> {

  private final LayoutInflater inflater;
  private TextView title;
  private TextView subtitle;
  private LinearLayout appsContaner;
  private ImageView image;
  private View store;
  private StoreLatestAppsDisplayable displayable;
  private Map<View, Long> apps;
  private Map<Long, String> appsPackages;
  private CompositeSubscription subscriptions;
  private CardView cardView;

  public StoreLatestAppsWidget(View itemView) {
    super(itemView);
    inflater = LayoutInflater.from(itemView.getContext());
    apps = new HashMap<>();
    appsPackages = new HashMap<>();
  }

  @Override protected void assignViews(View itemView) {
    store = itemView.findViewById(R.id.displayable_social_timeline_store_latest_apps_header);
    title = (TextView) itemView.findViewById(
        R.id.displayable_social_timeline_store_latest_apps_card_title);
    image = (ImageView) itemView.findViewById(
        R.id.displayable_social_timeline_store_latest_apps_card_image);
    subtitle = (TextView) itemView.findViewById(
        R.id.displayable_social_timeline_store_latest_apps_card_subtitle);
    appsContaner = (LinearLayout) itemView.findViewById(
        R.id.displayable_social_timeline_store_latest_apps_container);
    cardView =
        (CardView) itemView.findViewById(R.id.displayable_social_timeline_store_latest_apps_card);
  }

  @Override public void bindView(StoreLatestAppsDisplayable displayable) {
    this.displayable = displayable;
    title.setText(displayable.getStoreName());
    subtitle.setText(displayable.getTimeSinceLastUpdate(getContext()));
    setCardviewMargin(displayable);
    ImageLoader.loadWithShadowCircleTransform(displayable.getAvatarUrl(), image);

    appsContaner.removeAllViews();
    apps.clear();
    View latestAppView;
    ImageView latestAppIcon;
    for (StoreLatestAppsDisplayable.LatestApp latestApp : displayable.getLatestApps()) {
      latestAppView = inflater.inflate(R.layout.social_timeline_latest_app, appsContaner, false);
      latestAppIcon = (ImageView) latestAppView.findViewById(R.id.social_timeline_latest_app);
      ImageLoader.load(latestApp.getIconUrl(), latestAppIcon);
      appsContaner.addView(latestAppView);
      apps.put(latestAppView, latestApp.getAppId());
      appsPackages.put(latestApp.getAppId(), latestApp.getPackageName());
    }
  }

  //// TODO: 31/08/16 refactor this out of here
  private void knockWithSixpackCredentials(String url) {
    if (url == null) {
      return;
    }

    String credential = Credentials.basic(BuildConfig.SIXPACK_USER, BuildConfig.SIXPACK_PASSWORD);

    OkHttpClient client = new OkHttpClient();

    Request click = new Request.Builder().url(url).addHeader("authorization", credential).build();

    client.newCall(click).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {
        Logger.d(this.getClass().getSimpleName(), "sixpack request fail " + call.toString());
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        Logger.d(this.getClass().getSimpleName(), "knock success");
        response.body().close();
      }
    });
  }

  private void setCardviewMargin(StoreLatestAppsDisplayable displayable) {
    CardView.LayoutParams layoutParams =
        new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT,
            CardView.LayoutParams.WRAP_CONTENT);
    layoutParams.setMargins(displayable.getMarginWidth(getContext(),
        getContext().getResources().getConfiguration().orientation), 0,
        displayable.getMarginWidth(getContext(),
            getContext().getResources().getConfiguration().orientation), 30);
    cardView.setLayoutParams(layoutParams);
  }

  @Override public void onViewAttached() {
    if (subscriptions == null) {
      subscriptions = new CompositeSubscription();

      for (View app : apps.keySet()) {
        subscriptions.add(RxView.clicks(app).subscribe(click -> {
          knockWithSixpackCredentials(displayable.getAbUrl());
          Analytics.AppsTimeline.clickOnCard("Latest Apps", appsPackages.get(apps.get(app)),
              Analytics.AppsTimeline.BLANK, displayable.getStoreName(),
              Analytics.AppsTimeline.OPEN_APP_VIEW);
          ((FragmentShower) getContext()).pushFragmentV4(
              V8Engine.getFragmentProvider().newAppViewFragment(apps.get(app)));
        }));
      }

      subscriptions.add(RxView.clicks(store).subscribe(click -> {
        knockWithSixpackCredentials(displayable.getAbUrl());
        Analytics.AppsTimeline.clickOnCard("Latest Apps", Analytics.AppsTimeline.BLANK,
            Analytics.AppsTimeline.BLANK, displayable.getStoreName(),
            Analytics.AppsTimeline.OPEN_STORE);
        ((FragmentShower) getContext()).pushFragmentV4(
            V8Engine.getFragmentProvider().newStoreFragment(displayable.getStoreName()));
      }));
    }
  }

  @Override public void onViewDetached() {
    if (subscriptions != null) {
      subscriptions.unsubscribe();
      subscriptions = null;
    }
  }
}
