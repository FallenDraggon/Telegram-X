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

import android.widget.Toast;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.component.chat.MessagesManager;
import org.thunderdog.challegram.config.Config;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.InstantViewController;
import org.thunderdog.challegram.ui.MessagesController;

import java.util.ArrayList;
import java.util.List;


/**
 * Rich message (built from page blocks) shown as a text summary.
 * The full version opens in Instant View from the message options.
 */
public class TGMessageRich extends TGMessageText {
  public TGMessageRich (MessagesManager context, TdApi.Message msg, TdApi.MessageRichMessage content) {
    super(context, msg, buildText(content.message));
  }

  @Override
  protected boolean isSupportedMessageContent (TdApi.Message message, TdApi.MessageContent messageContent) {
    // Edited rich messages are rebuilt from scratch, as the photo may appear or disappear
    return false;
  }

  public static void open (MessagesController c, long chatId, long messageId) {
    Tdlib tdlib = c.tdlib();
    tdlib.send(new TdApi.GetFullRichMessage(chatId, messageId), (richMessage, error) -> {
      if (error != null) {
        UI.showError(error);
        return;
      }
      tdlib.send(new TdApi.GetMessageLink(chatId, messageId, 0, 0, "", false, false), (messageLink, linkError) -> {
        String url = messageLink != null ? messageLink.link : "";
        tdlib.ui().post(() -> {
          if (c.isDestroyed()) {
            return;
          }
          TdApi.LinkPreview linkPreview = new TdApi.LinkPreview();
          linkPreview.url = url;
          linkPreview.displayUrl = url;
          linkPreview.siteName = tdlib.chatTitle(chatId);
          TdApi.WebPageInstantView instantView = new TdApi.WebPageInstantView(richMessage.blocks, 0, Config.SUPPORTED_INSTANT_VIEW_VERSION, richMessage.isRtl, true, null);
          InstantViewController controller = new InstantViewController(c.context(), tdlib);
          try {
            InstantViewController.Args args = new InstantViewController.Args(linkPreview, instantView, null);
            args.isRichMessage = true;
            controller.setArguments(args);
            controller.show();
          } catch (Throwable t) {
            Log.e("Unable to open rich message", t);
            UI.showToast(R.string.InstantViewError, Toast.LENGTH_SHORT);
          }
        });
      });
    });
  }

  /**
   * First photo of the message to be shown in the bubble: top-level, or the first item of a collage or slideshow.
   */
  @Nullable
  public static TdApi.PageBlockPhoto findFirstPhoto (TdApi.RichMessage richMessage) {
    for (TdApi.PageBlock block : richMessage.blocks) {
      switch (block.getConstructor()) {
        case TdApi.PageBlockPhoto.CONSTRUCTOR:
          return (TdApi.PageBlockPhoto) block;
        case TdApi.PageBlockCollage.CONSTRUCTOR:
        case TdApi.PageBlockSlideshow.CONSTRUCTOR: {
          TdApi.PageBlock[] items = block.getConstructor() == TdApi.PageBlockCollage.CONSTRUCTOR ?
            ((TdApi.PageBlockCollage) block).blocks :
            ((TdApi.PageBlockSlideshow) block).blocks;
          for (TdApi.PageBlock item : items) {
            if (item.getConstructor() == TdApi.PageBlockPhoto.CONSTRUCTOR) {
              return (TdApi.PageBlockPhoto) item;
            }
          }
          break;
        }
      }
    }
    return null;
  }

  /**
   * Flattens rich message blocks into a plain text summary.
   * Headings and collapsible section titles are bold, contents of closed sections are skipped.
   */
  public static TdApi.FormattedText buildText (TdApi.RichMessage richMessage) {
    return buildText(richMessage, null);
  }

  /**
   * @param shownPhoto photo block already displayed in the bubble, its placeholder line is omitted
   */
  public static TdApi.FormattedText buildText (TdApi.RichMessage richMessage, @Nullable TdApi.PageBlockPhoto shownPhoto) {
    return buildText(richMessage, shownPhoto, false);
  }

  /**
   * @param expandAll include contents of closed collapsible sections
   */
  public static TdApi.FormattedText buildText (TdApi.RichMessage richMessage, @Nullable TdApi.PageBlockPhoto shownPhoto, boolean expandAll) {
    StringBuilder b = new StringBuilder();
    List<TdApi.TextEntity> entities = new ArrayList<>();
    for (TdApi.PageBlock block : richMessage.blocks) {
      appendBlock(block, b, entities, shownPhoto, expandAll);
    }
    return new TdApi.FormattedText(b.toString(), entities.toArray(new TdApi.TextEntity[0]));
  }

  private static void appendLine (StringBuilder b, List<TdApi.TextEntity> entities, String text, boolean bold) {
    if (text == null || text.trim().isEmpty()) {
      return;
    }
    if (b.length() > 0) {
      b.append('\n');
    }
    if (bold) {
      entities.add(new TdApi.TextEntity(b.length(), text.length(), new TdApi.TextEntityTypeBold()));
    }
    b.append(text);
  }

  private static void appendLine (StringBuilder b, List<TdApi.TextEntity> entities, TdApi.RichText text, boolean bold) {
    appendLine(b, entities, TD.getText(text), bold);
  }

  private static void appendCaption (StringBuilder b, List<TdApi.TextEntity> entities, TdApi.PageBlockCaption caption) {
    if (caption != null) {
      appendLine(b, entities, caption.text, false);
    }
  }

  private static void appendBlock (TdApi.PageBlock block, StringBuilder b, List<TdApi.TextEntity> entities, @Nullable TdApi.PageBlockPhoto shownPhoto, boolean expandAll) {
    switch (block.getConstructor()) {
      case TdApi.PageBlockTitle.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockTitle) block).title, true);
        break;
      case TdApi.PageBlockSubtitle.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockSubtitle) block).subtitle, true);
        break;
      case TdApi.PageBlockHeader.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockHeader) block).header, true);
        break;
      case TdApi.PageBlockSubheader.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockSubheader) block).subheader, true);
        break;
      case TdApi.PageBlockSectionHeading.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockSectionHeading) block).text, true);
        break;
      case TdApi.PageBlockKicker.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockKicker) block).kicker, false);
        break;
      case TdApi.PageBlockParagraph.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockParagraph) block).text, false);
        break;
      case TdApi.PageBlockPreformatted.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockPreformatted) block).text, false);
        break;
      case TdApi.PageBlockFooter.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockFooter) block).footer, false);
        break;
      case TdApi.PageBlockThinking.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockThinking) block).text, false);
        break;
      case TdApi.PageBlockMathematicalExpression.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockMathematicalExpression) block).expression, false);
        break;
      case TdApi.PageBlockPullQuote.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockPullQuote) block).text, false);
        break;
      case TdApi.PageBlockExpandableBlockQuote.CONSTRUCTOR:
        appendLine(b, entities, ((TdApi.PageBlockExpandableBlockQuote) block).text, false);
        break;
      case TdApi.PageBlockBlockQuote.CONSTRUCTOR:
        for (TdApi.PageBlock child : ((TdApi.PageBlockBlockQuote) block).blocks) {
          appendBlock(child, b, entities, shownPhoto, expandAll);
        }
        break;
      case TdApi.PageBlockList.CONSTRUCTOR:
        for (TdApi.PageBlockListItem item : ((TdApi.PageBlockList) block).items) {
          int start = b.length();
          for (TdApi.PageBlock child : item.blocks) {
            appendBlock(child, b, entities, shownPhoto, expandAll);
          }
          if (b.length() > start) {
            int offset = start > 0 ? start + 1 : start;
            String label = (item.label.isEmpty() ? "•" : item.label) + " ";
            b.insert(offset, label);
            for (TdApi.TextEntity entity : entities) {
              if (entity.offset >= offset) {
                entity.offset += label.length();
              }
            }
          }
        }
        break;
      case TdApi.PageBlockDetails.CONSTRUCTOR: {
        TdApi.PageBlockDetails details = (TdApi.PageBlockDetails) block;
        String header = TD.getText(details.header);
        appendLine(b, entities, (details.isOpen ? "▾ " : "▸ ") + header, true);
        if (details.isOpen || expandAll) {
          for (TdApi.PageBlock child : details.blocks) {
            appendBlock(child, b, entities, shownPhoto, expandAll);
          }
        }
        break;
      }
      case TdApi.PageBlockPhoto.CONSTRUCTOR:
        if (block != shownPhoto) {
          appendLine(b, entities, "🖼 " + Lang.getString(R.string.ChatContentPhoto), false);
        }
        appendCaption(b, entities, ((TdApi.PageBlockPhoto) block).caption);
        break;
      case TdApi.PageBlockVideo.CONSTRUCTOR:
        appendLine(b, entities, "🎬 " + Lang.getString(R.string.ChatContentVideo), false);
        appendCaption(b, entities, ((TdApi.PageBlockVideo) block).caption);
        break;
      case TdApi.PageBlockCollage.CONSTRUCTOR:
        for (TdApi.PageBlock child : ((TdApi.PageBlockCollage) block).blocks) {
          appendBlock(child, b, entities, shownPhoto, expandAll);
        }
        appendCaption(b, entities, ((TdApi.PageBlockCollage) block).caption);
        break;
      case TdApi.PageBlockSlideshow.CONSTRUCTOR:
        for (TdApi.PageBlock child : ((TdApi.PageBlockSlideshow) block).blocks) {
          appendBlock(child, b, entities, shownPhoto, expandAll);
        }
        appendCaption(b, entities, ((TdApi.PageBlockSlideshow) block).caption);
        break;
      default:
        // Other blocks are visible in the full view only
        break;
    }
  }
}
