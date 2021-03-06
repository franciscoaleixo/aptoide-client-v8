package cm.aptoide.pt.social.view.viewholder;

import android.view.View;
import android.widget.Button;
import cm.aptoide.pt.R;
import cm.aptoide.pt.social.data.CardTouchEvent;
import cm.aptoide.pt.social.view.TimelineUser;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 05/07/2017.
 */

public class TimelineLoginPostViewHolder extends PostViewHolder<TimelineUser> {
  private final PublishSubject<CardTouchEvent> cardTouchEventPublishSubject;
  private Button button;

  public TimelineLoginPostViewHolder(View view,
      PublishSubject<CardTouchEvent> cardTouchEventPublishSubject) {
    super(view, cardTouchEventPublishSubject);
    this.cardTouchEventPublishSubject = cardTouchEventPublishSubject;
    this.button = (Button) itemView.findViewById(R.id.login_button);
  }

  @Override public void setPost(TimelineUser card, int position) {
    this.button.setOnClickListener(click -> cardTouchEventPublishSubject.onNext(
        new CardTouchEvent(card, position, CardTouchEvent.Type.LOGIN)));
  }
}
