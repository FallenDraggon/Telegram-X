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

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.component.chat.MessagesManager;
import org.thunderdog.challegram.unsorted.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import tgx.td.Td;

/**
 * Collapses incoming messages that contain any of the user-defined phrases.
 * Matching is a case-insensitive substring search, "ё" is treated as "е".
 */
public class KeywordFilter {
  private static final String KEY_ENABLED = "keyword_filter_enabled";
  private static final String KEY_PHRASES = "keyword_filter_phrases";
  private static final String KEY_REVEALED_PREFIX = "keyword_filter_revealed_";

  private static volatile String[] phrases, normalizedPhrases;

  public static boolean isEnabled () {
    return Settings.instance().pmc().getBoolean(KEY_ENABLED, false);
  }

  public static void setEnabled (boolean enabled) {
    Settings.instance().pmc().putBoolean(KEY_ENABLED, enabled);
  }

  public static String[] getPhrases () {
    loadPhrases();
    return phrases;
  }

  public static void setPhrases (String[] newPhrases) {
    List<String> list = new ArrayList<>(newPhrases.length);
    for (String phrase : newPhrases) {
      phrase = phrase.trim();
      if (!phrase.isEmpty() && !list.contains(phrase)) {
        list.add(phrase);
      }
    }
    String[] result = list.toArray(new String[0]);
    Settings.instance().pmc().putStringArray(KEY_PHRASES, result);
    setPhrasesImpl(result);
  }

  /**
   * Appends phrases to the list and turns the filter on.
   */
  public static void addPhrases (String[] newPhrases) {
    String[] current = getPhrases();
    String[] merged = new String[current.length + newPhrases.length];
    System.arraycopy(current, 0, merged, 0, current.length);
    System.arraycopy(newPhrases, 0, merged, current.length, newPhrases.length);
    setPhrases(merged);
    setEnabled(true);
  }

  public static void removePhrase (String phrase) {
    List<String> list = new ArrayList<>(Arrays.asList(getPhrases()));
    list.remove(phrase);
    setPhrases(list.toArray(new String[0]));
  }

  public static void replacePhrase (String oldPhrase, String newPhrase) {
    String[] current = getPhrases().clone();
    for (int i = 0; i < current.length; i++) {
      if (current[i].equals(oldPhrase)) {
        current[i] = newPhrase;
      }
    }
    setPhrases(current);
  }

  /**
   * One phrase per line, as phrases may contain commas.
   */
  public static String exportPhrases () {
    return TextUtils.join("\n", getPhrases());
  }

  /**
   * Adds phrases from {@link #exportPhrases()} format to the existing ones.
   *
   * @return number of new phrases
   */
  public static int importPhrases (String text) {
    int oldCount = getPhrases().length;
    addPhrases(text.split("\n"));
    return getPhrases().length - oldCount;
  }

  private static void loadPhrases () {
    if (phrases == null) {
      // Revealed messages used to be stored permanently, now they are kept only while the chat is open
      Settings.instance().pmc().removeByPrefix(KEY_REVEALED_PREFIX);
      String[] stored = Settings.instance().pmc().getStringArray(KEY_PHRASES);
      setPhrasesImpl(stored != null ? stored : new String[0]);
    }
  }

  private static void setPhrasesImpl (String[] newPhrases) {
    String[] normalized = new String[newPhrases.length];
    for (int i = 0; i < newPhrases.length; i++) {
      normalized[i] = normalize(newPhrases[i]);
    }
    normalizedPhrases = normalized;
    phrases = newPhrases;
  }

  private static String normalize (String text) {
    return text.toLowerCase(Locale.ROOT).replace('ё', 'е');
  }

  /**
   * @return the phrase the message matched, or null if it must be shown as is
   */
  @Nullable
  public static String findMatch (MessagesManager manager, TdApi.Message message) {
    return findMatch(manager, new TdApi.Message[] {message});
  }

  /**
   * Checks the whole album: the caption may belong to any of its messages.
   */
  @Nullable
  public static String findMatch (MessagesManager manager, TdApi.Message[] messages) {
    if (!isEnabled()) {
      return null;
    }
    loadPhrases();
    final String[] phrases = KeywordFilter.phrases, normalizedPhrases = KeywordFilter.normalizedPhrases;
    if (phrases.length == 0) {
      return null;
    }
    for (TdApi.Message message : messages) {
      if (message.isOutgoing || message.chatId == 0 || manager.isKeywordFilterRevealed(message)) {
        return null;
      }
    }
    for (TdApi.Message message : messages) {
      String text = getText(message.content);
      if (text == null || text.isEmpty()) {
        continue;
      }
      text = normalize(text);
      for (int i = 0; i < normalizedPhrases.length; i++) {
        if (text.contains(normalizedPhrases[i])) {
          return phrases[i];
        }
      }
    }
    return null;
  }

  /**
   * Text the filter searches in, e.g. to pick a phrase from it.
   */
  public static String getText (TdApi.Message[] messages) {
    StringBuilder b = new StringBuilder();
    for (TdApi.Message message : messages) {
      String text = getText(message.content);
      if (text != null && !text.trim().isEmpty()) {
        if (b.length() > 0) {
          b.append("\n\n");
        }
        b.append(text);
      }
    }
    return b.toString();
  }

  @Nullable
  private static String getText (TdApi.MessageContent content) {
    if (content == null) {
      return null;
    }
    if (content.getConstructor() == TdApi.MessageRichMessage.CONSTRUCTOR) {
      return TGMessageRich.buildText(((TdApi.MessageRichMessage) content).message, null, true).text;
    }
    TdApi.FormattedText text = Td.textOrCaption(content);
    return text != null ? text.text : null;
  }
}
