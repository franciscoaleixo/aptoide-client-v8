/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.billing.external;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.view.BillingActivity;
import cm.aptoide.pt.billing.view.PaymentThrowableCodeMapper;
import cm.aptoide.pt.billing.view.PurchaseBundleMapper;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.iab.AptoideInAppBillingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rx.Single;

public class ExternalBillingBinder extends AptoideInAppBillingService.Stub {

  public static final int RESULT_OK = 0;
  public static final int RESULT_USER_CANCELLED = 1;
  public static final int RESULT_SERVICE_UNAVAILABLE = 2;
  public static final int RESULT_BILLING_UNAVAILABLE = 3;
  public static final int RESULT_ITEM_UNAVAILABLE = 4;
  public static final int RESULT_DEVELOPER_ERROR = 5;
  public static final int RESULT_ERROR = 6;
  public static final int RESULT_ITEM_ALREADY_OWNED = 7;
  public static final int RESULT_ITEM_NOT_OWNED = 8;

  public static final String RESPONSE_CODE = "RESPONSE_CODE";
  public static final String DETAILS_LIST = "DETAILS_LIST";
  public static final String BUY_INTENT = "BUY_INTENT";

  public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

  public static final String ITEM_ID_LIST = "ITEM_ID_LIST";
  public static final String ITEM_TYPE_LIST = "ITEM_TYPE_LIST";

  public static final String ITEM_TYPE_INAPP = "inapp";
  public static final String ITEM_TYPE_SUBS = "subs";
  public static final String SERVICES_LIST = "SERVICES_LIST";

  private final Context context;
  private final ExternalBillingSerializer serializer;
  private final PaymentThrowableCodeMapper errorCodeFactory;
  private final PurchaseBundleMapper purchaseBundleMapper;
  private final PackageManager packageManager;
  private final CrashReport crashReport;
  private final int supportedApiVersion;
  private final BillingAnalytics analytics;

  private Billing billing;
  private String merchantName;

  public ExternalBillingBinder(Context context, ExternalBillingSerializer serializer,
      PaymentThrowableCodeMapper errorCodeFactory, PurchaseBundleMapper purchaseBundleMapper,
      CrashReport crashReport, int apiVersion, BillingAnalytics analytics,
      PackageManager packageManager) {
    this.context = context;
    this.serializer = serializer;
    this.errorCodeFactory = errorCodeFactory;
    this.purchaseBundleMapper = purchaseBundleMapper;
    this.packageManager = packageManager;
    this.crashReport = crashReport;
    this.supportedApiVersion = apiVersion;
    this.analytics = analytics;
  }

  @Override public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
      throws RemoteException {
    merchantName = packageManager.getPackagesForUid(Binder.getCallingUid())[0];
    billing = ((AptoideApplication) context.getApplicationContext()).getBilling(merchantName);
    billing.setup();
    return super.onTransact(code, data, reply, flags);
  }

  @Override public int isBillingSupported(int apiVersion, String packageName, String type)
      throws RemoteException {
    try {

      if (apiVersion != supportedApiVersion) {
        return RESULT_BILLING_UNAVAILABLE;
      }

      return billing.getMerchant()
          .map(merchant -> RESULT_OK)
          .onErrorResumeNext(throwable -> {
              return Single.just(RESULT_BILLING_UNAVAILABLE);
          })
          .toBlocking()
          .value();
    } catch (Exception exception) {
      crashReport.log(exception);
      return errorCodeFactory.map(exception.getCause());
    }
  }

  @Override
  public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle)
      throws RemoteException {

    final Bundle result = new Bundle();

    if (!skusBundle.containsKey(ITEM_ID_LIST) || apiVersion != supportedApiVersion) {
      result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
      return result;
    }

    final List<String> skus = skusBundle.getStringArrayList(ITEM_ID_LIST);

    if (skus == null || skus.size() <= 0) {
      result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
      return result;
    }

    try {
      final List<String> serializedProducts = billing.getProducts(skus)
          .flatMap(products -> {
            try {
              return Single.just(serializer.serializeProducts(products));
            } catch (IOException e) {
              return Single.error(e);
            }
          })
          .toBlocking()
          .value();

      result.putInt(RESPONSE_CODE, RESULT_OK);
      result.putStringArrayList(DETAILS_LIST, new ArrayList<>(serializedProducts));
      return result;
    } catch (Exception exception) {
      crashReport.log(exception);
      result.putInt(RESPONSE_CODE, errorCodeFactory.map(exception.getCause()));
      return result;
    }
  }

  @Override public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type,
      String developerPayload) throws RemoteException {

    final Bundle result = new Bundle();

    if (apiVersion != supportedApiVersion) {
      result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
      return result;
    }

    try {
      billing.selectProduct(sku, developerPayload);
      result.putInt(RESPONSE_CODE, RESULT_OK);
      result.putParcelable(BUY_INTENT, PendingIntent.getActivity(context, 0,
          BillingActivity.getIntent(context, merchantName),
          PendingIntent.FLAG_UPDATE_CURRENT));
      analytics.sendPaymentViewShowEvent();
    } catch (Exception exception) {
      crashReport.log(exception);
      result.putInt(RESPONSE_CODE, errorCodeFactory.map(exception.getCause()));
    }

    return result;
  }

  @Override public Bundle getPurchases(int apiVersion, String packageName, String type,
      String continuationToken) throws RemoteException {

    if (apiVersion != supportedApiVersion) {
      final Bundle result = new Bundle();
      result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
      return result;
    }

    if (!type.equals(ITEM_TYPE_INAPP)) {
      return purchaseBundleMapper.map(Collections.emptyList());
    }

    try {
      return purchaseBundleMapper.map(billing.getPurchases()
          .toBlocking()
          .value());
    } catch (Exception exception) {
      final Bundle result = new Bundle();
      crashReport.log(exception);
      result.putInt(RESPONSE_CODE, errorCodeFactory.map(exception.getCause()));
      return result;
    }
  }

  @Override public int consumePurchase(int apiVersion, String packageName, String purchaseToken)
      throws RemoteException {

    if (apiVersion != supportedApiVersion) {
      return RESULT_DEVELOPER_ERROR;
    }

    try {
      return billing.consumePurchase(purchaseToken)
          .andThen(Single.just(RESULT_OK))
          .toBlocking()
          .value();
    } catch (Exception exception) {
      crashReport.log(exception);
      return errorCodeFactory.map(exception.getCause());
    }
  }
}
