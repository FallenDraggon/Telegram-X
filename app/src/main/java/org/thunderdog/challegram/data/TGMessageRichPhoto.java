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

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.component.chat.MessagesManager;

/**
 * Rich message that contains a photo: the first photo is shown as media, the rest as a text summary.
 * Tapping the photo opens the full version in Instant View.
 */
public class TGMessageRichPhoto extends TGMessageMedia {
  public TGMessageRichPhoto (MessagesManager context, TdApi.Message msg, TdApi.MessageRichMessage content, TdApi.PageBlockPhoto photo) {
    super(context, msg,
      new TdApi.MessagePhoto(photo.photo, null, null, false, photo.hasSpoiler, false),
      TGMessageRich.buildText(content.message, photo)
    );
    // Media viewer expects photo message content, so open the whole message instead
    getSingularMediaWrapper().setOnClickListener((view, wrapper) -> {
      TGMessageRich.open(messagesController(), msg.chatId, msg.id);
      return true;
    });
  }

  @Override
  protected TdApi.FormattedText getContentCaption (TdApi.Message message) {
    // Called from the super constructor as well, so it must not rely on fields
    if (message.content.getConstructor() == TdApi.MessageRichMessage.CONSTRUCTOR) {
      TdApi.RichMessage richMessage = ((TdApi.MessageRichMessage) message.content).message;
      return TGMessageRich.buildText(richMessage, TGMessageRich.findFirstPhoto(richMessage));
    }
    return super.getContentCaption(message);
  }

  @Override
  protected boolean isSupportedMessageContent (TdApi.Message message, TdApi.MessageContent messageContent) {
    // Edited rich messages are rebuilt from scratch, as the photo may appear or disappear
    return false;
  }
}
