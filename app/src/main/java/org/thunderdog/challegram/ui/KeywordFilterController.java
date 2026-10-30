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
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.KeywordFilter;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.tool.Intents;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link KeywordFilter} phrases: add, edit, delete, export and import.
 */
public class KeywordFilterController extends RecyclerViewController<Void> implements View.OnClickListener {
  public KeywordFilterController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_keywordFilter;
  }

  @Override
  public CharSequence getName () {
    return Lang.getString(R.string.KeywordFilterPhrases);
  }

  private SettingsAdapter adapter;

  @Override
  protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    adapter = new SettingsAdapter(this);
    buildCells();
    recyclerView.setAdapter(adapter);
  }

  private void buildCells () {
    List<ListItem> items = new ArrayList<>();

    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_keywordFilterAdd, R.drawable.baseline_add_24, R.string.KeywordFilterAddPhrase).setTextColorId(ColorId.textNeutral));
    String[] phrases = KeywordFilter.getPhrases();
    for (String phrase : phrases) {
      items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
      items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_keywordFilterPhrase, 0, phrase, false).setData(phrase));
    }
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, phrases.length == 0 ? R.string.KeywordFilterPhrasesEmpty : R.string.KeywordFilterPhrasesDesc));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.KeywordFilterBackup));
    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_keywordFilterExport, R.drawable.baseline_share_24, R.string.KeywordFilterExport));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR_FULL));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_keywordFilterImport, R.drawable.baseline_content_paste_24, R.string.KeywordFilterImport));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));
    items.add(new ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.KeywordFilterBackupDesc));

    adapter.setItems(items, false);
  }

  @Override
  public void onClick (View v) {
    int id = v.getId();
    if (id == R.id.btn_keywordFilterAdd) {
      openInputAlert(Lang.getString(R.string.KeywordFilterAddPhrase), Lang.getString(R.string.KeywordFilterPhrasesHint), R.string.Add, R.string.Cancel, null, (inputView, result) -> {
        if (result.trim().isEmpty()) {
          return false;
        }
        KeywordFilter.addPhrases(new String[] {result});
        buildCells();
        return true;
      }, true);
    } else if (id == R.id.btn_keywordFilterPhrase) {
      String phrase = (String) ((ListItem) v.getTag()).getData();
      showOptions(phrase, new int[] {R.id.btn_edit, R.id.btn_delete}, new String[] {Lang.getString(R.string.edit), Lang.getString(R.string.Delete)}, (optionItemView, optionId) -> {
        if (optionId == R.id.btn_edit) {
          openInputAlert(Lang.getString(R.string.KeywordFilterEditPhrase), Lang.getString(R.string.KeywordFilterPhrasesHint), R.string.Save, R.string.Cancel, phrase, (inputView, result) -> {
            if (result.trim().isEmpty()) {
              return false;
            }
            KeywordFilter.replacePhrase(phrase, result);
            buildCells();
            return true;
          }, true);
        } else if (optionId == R.id.btn_delete) {
          KeywordFilter.removePhrase(phrase);
          buildCells();
        }
        return true;
      });
    } else if (id == R.id.btn_keywordFilterExport) {
      if (KeywordFilter.getPhrases().length == 0) {
        UI.showToast(R.string.KeywordFilterPhrasesEmpty, Toast.LENGTH_SHORT);
      } else {
        Intents.shareText(KeywordFilter.exportPhrases());
      }
    } else if (id == R.id.btn_keywordFilterImport) {
      CharSequence text = U.getPasteText(context);
      if (text == null || text.toString().trim().isEmpty()) {
        UI.showToast(R.string.KeywordFilterImportEmpty, Toast.LENGTH_SHORT);
        return;
      }
      int added = KeywordFilter.importPhrases(text.toString());
      buildCells();
      UI.showToast(Lang.getString(R.string.KeywordFilterImported, added), Toast.LENGTH_SHORT);
    }
  }
}
