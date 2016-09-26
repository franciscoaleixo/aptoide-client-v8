/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v3;

import cm.aptoide.pt.model.v3.InAppBillingAvailableResponse;
import cm.aptoide.pt.networkclient.util.HashMapNotNull;
import rx.Observable;

/**
 * Created by marcelobenites on 8/11/16.
 */
public class InAppBillingAvailableRequest extends V3<InAppBillingAvailableResponse> {

  private HashMapNotNull<String, String> args;

  public InAppBillingAvailableRequest(String baseHost, HashMapNotNull<String, String> args) {
    super(baseHost);
    this.args = args;
  }

  public static InAppBillingAvailableRequest of(int apiVersion, String packageName, String type) {
    final HashMapNotNull<String, String> args = new HashMapNotNull<>();
    args.put("mode", "json");
    args.put("apiversion", String.valueOf(apiVersion));
    args.put("reqtype", "iabavailable");
    args.put("package", packageName);
    args.put("purchasetype", type);
    return new InAppBillingAvailableRequest(BASE_HOST, args);
  }

  @Override
  protected Observable<InAppBillingAvailableResponse> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    return interfaces.getInAppBillingAvailable(args);
  }
}
