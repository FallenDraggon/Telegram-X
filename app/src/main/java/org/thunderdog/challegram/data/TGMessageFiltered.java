/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.thunderdog.challegram.data;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.component.chat.MessageView;
import org.thunderdog.challegram.component.chat.MessagesManager;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.tool.Drawables;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.PorterDuffPaint;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.ui.MessagesController;
import org.thunderdog.challegram.util.text.Text;

/**
 * Placeholder for a message (or a whole album) collapsed by {@link KeywordFilter}.
 * Tapping it shows the original message until the chat is closed.
 */
public class TGMessageFiltered extends TGMessage {
  private final String phrase;

  public TGMessageFiltered (MessagesManager context, TdApi.Message msg, String phrase) {
    super(context, msg);
    this.phrase = phrase;
  }

  private String trimmedTitle, trimmedSubtitle;
  private boolean needFakeTitle;
  private float titleWidth, subtitleWidth;

  @Override
  protected void buildContent (int maxWidth) {
    String title = Lang.getString(R.string.KeywordFilterHidden);
    String subtitle = Lang.getString(R.string.KeywordFilterHiddenHint, phrase);
    maxWidth -= Screen.dp(32f);
    this.needFakeTitle = Text.needFakeBold(title);
    this.trimmedTitle = TextUtils.ellipsize(title, Paints.getMediumTextPaint(15f, needFakeTitle), maxWidth, TextUtils.TruncateAt.END).toString();
    this.trimmedSubtitle = TextUtils.ellipsize(subtitle, Paints.getSubtitlePaint(), maxWidth, TextUtils.TruncateAt.END).toString();
    this.titleWidth = U.measureText(trimmedTitle, Paints.getMediumTextPaint(15f, needFakeTitle));
    this.subtitleWidth = U.measureText(trimmedSubtitle, Paints.getSubtitlePaint());
  }

  @Override
  protected int getBubbleContentPadding () {
    return xBubblePadding + xBubblePaddingSmall;
  }

  @Override
  protected void drawContent (MessageView view, Canvas c, int startX, int startY, int maxWidth) {
    Drawable icon = view.getSparseDrawable(R.drawable.baseline_eye_off_24, ColorId.NONE);
    int colorId = isOutgoingBubble() ? ColorId.bubbleOut_file : ColorId.file;
    Drawables.draw(c, icon, startX, startY + getContentHeight() / 2f - icon.getMinimumHeight() / 2f, PorterDuffPaint.get(colorId));
    startX += Screen.dp(32f);
    if (useBubbles()) {
      startY -= Screen.dp(4f);
    }
    c.drawText(trimmedTitle, startX, startY + Screen.dp(21f), Paints.getMediumTextPaint(15f, getTextColor(), needFakeTitle));
    c.drawText(trimmedSubtitle, startX, startY + Screen.dp(41f), Paints.getRegularTextPaint(13f, getDecentColor()));
  }

  @Override
  public boolean onMessageClick (MessageView v, MessagesController c) {
    manager().setKeywordFilterRevealed(getAllMessages());
    manager().rebuildMessage(this);
    return true;
  }

  @Override
  protected boolean isSupportedMessageContent (TdApi.Message message, TdApi.MessageContent messageContent) {
    // Edited messages are checked against the filter again
    return false;
  }

  @Override
  protected int getContentWidth () {
    return (int) Math.max(titleWidth, subtitleWidth) + Screen.dp(32f);
  }

  @Override
  protected int getContentHeight () {
    return Screen.dp(46f);
  }
}
