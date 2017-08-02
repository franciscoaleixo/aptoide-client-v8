/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 23/11/2016.
 */

package cm.aptoide.pt.database.accessors;

import cm.aptoide.pt.database.realm.PaymentAuthorization;
import java.util.List;
import rx.Observable;
import rx.Scheduler;

public class PaymentAuthorizationAccessor extends SimpleAccessor<PaymentAuthorization> {

  public PaymentAuthorizationAccessor(Database db, Scheduler observingScheduler) {
    super(db, observingScheduler, PaymentAuthorization.class);
  }

  public Observable<List<PaymentAuthorization>> getPaymentAuthorization(String payerId,
      int paymentId) {
    return database.getRealm()
        .map(realm -> realm.where(PaymentAuthorization.class)
            .equalTo(PaymentAuthorization.PAYER_ID, payerId)
            .equalTo(PaymentAuthorization.PAYMENT_ID, paymentId))
        .flatMap(query -> database.findAsList(query));
  }
}