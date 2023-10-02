/*
 * Copyright (C) 2014 De'vID jonpIn (David Yonge-Mallo)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tlhInganHol.android.klingonassistant;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.Locale;

/**
 * The main activity for the dictionary. Displays search results triggered by the search dialog and
 * handles actions from search suggestions.
 */
public class KlingonAssistant extends BaseActivity {
  private static final String TAG = "KlingonAssistant";

  // Preference key for whether to show help.
  public static final String KEY_SHOW_HELP = "show_help";

  // These holds the {pIqaD} typefaces.
  private static Typeface mTNGKlingonFontTypeface = null;
  private static Typeface mDSCKlingonFontTypeface = null;
  private static Typeface mCoreKlingonFontTypeface = null;

  // The two main views in app's main screen.
  private TextView mTextView;
  private ListView mListView;

  // The query to pre-populate when the user presses the "Search" button.
  private String mPrepopulatedQuery = null;

  // private int mTutorialCounter;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setDrawerContentView(R.layout.main);

    mTextView = (TextView) findViewById(R.id.text);
    mListView = (ListView) findViewById(R.id.list);

    handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // Because this activity has set launchMode="singleTop", the system calls this method
    // to deliver the intent if this activity is currently the foreground activity when
    // invoked again (when the user executes a search from this activity, we don't create
    // a new instance of this activity, so the system delivers the search intent here)
    super.onNewIntent(intent);
    handleIntent(intent);
  }

  // Helper method to determine if a shared text came from Twitter, and if so, strip it of
  // everything but the actual tweet.
  private String stripTweet(String text) {
    // Log.d(TAG, "Tweet text = " + text);
    if (text.indexOf("https://twitter.com/download") == -1) {
      // All shared tweets contain the Twitter download link, regardless of the UI language.
      // So if this isn't found, then it's not a tweet.
      return text;
    }
    // If it's a tweet, the second line is the actual content.
    String[] textParts = text.split("\n");
    if (textParts.length >= 2) {
      return textParts[1];
    }
    return text;
  }

  private void handleIntent(Intent intent) {
    // Log.d(TAG, "Intent: " + intent);
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      // handles a click on a search suggestion; launches activity to show entry
      String entryId = intent.getDataString();
      // Log.d(TAG, "entryId = " + entryId);
      launchEntry(entryId);

    } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      // handles a search query
      String mQuery = intent.getStringExtra(SearchManager.QUERY);
      Log.d(TAG, "ACTION_SEARCH: " + mQuery);
      showResults(mQuery);

    } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
      // handles another plain text shared from another app
      if ("text/plain".equals(intent.getType())) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
          /* if (BuildConfig.DEBUG) {
            Log.d(TAG, "Incoming text:\n" + sharedText);
          } */
          // Sanitise incoming text. Also cap at 140 chars, for reasons of speed and because that's
          // the limit used by Twitter.
          sharedText = stripTweet(sharedText);
          sharedText = sharedText.replaceAll("[:\\*<>\\n]", " ").trim().replaceAll("\\s+", " ");
          if (sharedText.length() > 140) {
            sharedText = sharedText.substring(0, 140);
          }
          /* if (BuildConfig.DEBUG) {
            Log.d(TAG, "Shared text:\n" + sharedText);
          } */
          // Override (disable) "xifan hol" mode for this search, since it doesn't really make sense
          // here.
          showResults("+" + sharedText);
        }
      }

    } else {
      // Show just the help screen.
      displayHelp(QUERY_FOR_ABOUT);
    }
  }

  public static Typeface getKlingonFontTypeface(Context context) {
    String klingonFontCode = Preferences.getKlingonFontCode(context);
    if (klingonFontCode.equals("CORE")) {
      if (mCoreKlingonFontTypeface == null) {
        mCoreKlingonFontTypeface =
            Typeface.createFromAsset(context.getAssets(), "fonts/qolqoS-pIqaD.ttf");
      }
      return mCoreKlingonFontTypeface;
    } else if (klingonFontCode.equals("DSC")) {
      if (mDSCKlingonFontTypeface == null) {
        mDSCKlingonFontTypeface =
            Typeface.createFromAsset(context.getAssets(), "fonts/DSC-pIqaD.ttf");
      }
      return mDSCKlingonFontTypeface;
    } else {
      // Return TNG-style as the default as that's how we want to display the app name
      // when Latin is chosen.
      if (mTNGKlingonFontTypeface == null) {
        mTNGKlingonFontTypeface =
            Typeface.createFromAsset(context.getAssets(), "fonts/TNG-pIqaD.ttf");
      }
      return mTNGKlingonFontTypeface;
    }
  }

  @TargetApi(Build.VERSION_CODES.N)
  public static Locale getSystemLocale() {
    Locale locale;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      locale = Resources.getSystem().getConfiguration().getLocales().get(0);
    } else {
      locale = Resources.getSystem().getConfiguration().locale;
    }
    return locale;
  }

  // Launch an entry activity with the entry's info.
  private void launchEntry(String entryId) {
    if (entryId == null) {
      return;
    }

    Intent entryIntent = new Intent(this, EntryActivity.class);

    // Form the URI for the entry.
    Uri uri = Uri.parse(KlingonContentProvider.CONTENT_URI + "/get_entry_by_id/" + entryId);
    entryIntent.setData(uri);
    startActivity(entryIntent);
  }

  class EntryAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

    private final Cursor mCursor;
    private final LayoutInflater mInflater;

    public EntryAdapter(Cursor cursor) {
      mCursor = cursor;
      mInflater =
          (LayoutInflater) KlingonAssistant.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
      return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
      return position;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TwoLineListItem view =
          (convertView != null) ? (TwoLineListItem) convertView : createView(parent);
      mCursor.moveToPosition(position);
      bindView(view, mCursor);
      return view;
    }

    private TwoLineListItem createView(ViewGroup parent) {
      TwoLineListItem item =
          (TwoLineListItem) mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);

      // Set single line to true if you want shorter definitions.
      item.getText2().setSingleLine(false);
      item.getText2().setEllipsize(TextUtils.TruncateAt.END);

      return item;
    }

    private void bindView(TwoLineListItem view, Cursor cursor) {
      KlingonContentProvider.Entry entry =
          new KlingonContentProvider.Entry(cursor, getBaseContext());

      // Note that we override the typeface and text size here, instead of in
      // the xml, because putting it there would also change the appearance of
      // the Preferences page. We fully indent suffixes, but only half-indent verbs.
      String indent1 =
          entry.isIndented() ? (entry.isVerb() ? "&nbsp;&nbsp;" : "&nbsp;&nbsp;&nbsp;&nbsp;") : "";
      String indent2 =
          entry.isIndented()
              ? (entry.isVerb()
                  ? "&nbsp;&nbsp;&nbsp;&nbsp;"
                  : "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
              : "";

      SharedPreferences sharedPrefs =
          PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      if (Preferences.useKlingonFont(getBaseContext())) {
        // Preference is set to display this in {pIqaD}!
        view.getText1()
            .setText(
                new SpannableStringBuilder(Html.fromHtml(indent1))
                    .append(entry.getFormattedEntryNameInKlingonFont()));
      } else {
        // Use serif for the entry, so capital-I and lowercase-l are distinguishable.
        view.getText1().setTypeface(Typeface.SERIF);
        view.getText1()
            .setText(Html.fromHtml(indent1 + entry.getFormattedEntryName(/* isHtml */ true)));
      }
      view.getText1().setTextSize(22);

      // TODO: Colour attached affixes differently from verb.
      view.getText1().setTextColor(entry.getTextColor());

      // Use sans serif for the definition.
      view.getText2().setTypeface(Typeface.SANS_SERIF);
      view.getText2()
          .setText(Html.fromHtml(indent2 + entry.getFormattedDefinition(/* isHtml */ true)));
      view.getText2().setTextSize(14);
      view.getText2().setTextColor(0xFFC0C0C0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (getCount() == 1) {
        // Launch entry the regular way, as there's only one result.
        mCursor.moveToPosition(position);
        launchEntry(mCursor.getString(KlingonContentDatabase.COLUMN_ID));
      } else {
        // There's a list of results, so launch a list of entries. Instead of passing in
        // one ID, we pass in a comma-separated list. We also append the position of the
        // selected entry to the end.
        StringBuilder entryList = new StringBuilder();
        for (int i = 0; i < getCount(); i++) {
          mCursor.moveToPosition(i);
          entryList.append(mCursor.getString(KlingonContentDatabase.COLUMN_ID));
          entryList.append(",");
        }
        entryList.append(position);
        mCursor.moveToPosition(position);
        launchEntry(entryList.toString());
      }
    }
  }

  /**
   * Searches the dictionary and displays results for the given query. The query may be prepended
   * with a plus to disable "xifan hol" mode.
   *
   * @param query The search query
   */
  private void showResults(String query) {

    // Note: managedQuery is deprecated since API 11.
    Cursor cursor =
        managedQuery(
            Uri.parse(KlingonContentProvider.CONTENT_URI + "/lookup"),
            null /* all columns */,
            null,
            new String[] {query},
            null);

    // A query may be preceded by a plus to override (disable) "xifan hol" mode. This is used
    // for internal searches. After it is passed to managedQuery (above), it can be removed.
    boolean overrideXifanHol = false;
    if (!query.isEmpty() && query.charAt(0) == '+') {
      overrideXifanHol = true;
      query = query.substring(1);
    }

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    KlingonContentProvider.Entry queryEntry =
        new KlingonContentProvider.Entry(query, getBaseContext());
    boolean qWillBeRemapped =
        queryEntry.getEntryName().indexOf('q') != -1
            && sharedPrefs.getBoolean(
                Preferences.KEY_XIFAN_HOL_CHECKBOX_PREFERENCE, /* default */ false)
            && sharedPrefs.getBoolean(
                Preferences.KEY_SWAP_QS_CHECKBOX_PREFERENCE, /* default */ false);
    String entryNameWithPoS =
        queryEntry.getEntryName() + queryEntry.getBracketedPartOfSpeech(/* isHtml */ true);
    if (!overrideXifanHol && qWillBeRemapped) {
      // Alert the user to the visual inconsistency of the query containing "q" but the results
      // containing {Q} instead, as some users forget that they have this option activated in their
      // settings. (Note that "k" would be mapped to {q} in that case, and "Q" would still be mapped
      // to {Q}. It's only "q" which isn't obvious.)
      entryNameWithPoS += " [q=Q]";
    }

    if (cursor == null || cursor.getCount() == 0) {
      // There are no results.
      mTextView.setText(
          Html.fromHtml(getString(R.string.no_results, new Object[] {entryNameWithPoS})));
      // The user probably made a typo, so allow them to edit the query.
      mPrepopulatedQuery = queryEntry.getEntryName();

    } else {
      // Display the number of results.
      int count = cursor.getCount();
      String countString;
      if (queryEntry.getEntryName().equals("*")) {
        // Searching for a class of phrases.
        countString = queryEntry.getSentenceType();
        if (countString.equals("")) {
          // The sentence type was indeterminate.
          // This only ever happens if the user enters "*:sen" as a search string.
          count = 0;
          countString = "Sentences:";
        } else {
          // Display, e.g., "Lyrics:".
          countString += ":";
        }
      } else {
        countString =
            getResources()
                .getQuantityString(
                    R.plurals.search_results, count, new Object[] {count, entryNameWithPoS});
        // Allow the user to edit the query by pressing the search button.
        mPrepopulatedQuery = queryEntry.getEntryName();
        // If "xifan hol" mode was overridden (disabled) to get this set of search results, but it
        // is currently enabled by the user with q mapped to Q, then we ensure that if the user
        // edits the search query, that it performs a search with "xifan hol" overridden again.
        if (overrideXifanHol && qWillBeRemapped) {
          mPrepopulatedQuery = "+" + mPrepopulatedQuery;
        }
      }
      mTextView.setText(Html.fromHtml(countString));

      // TODO: Allow TTS to speak queryEntry.getEntryName().

      // Create a cursor adapter for the entries and apply them to the ListView.
      EntryAdapter entryAdapter = new EntryAdapter(cursor);
      mListView.setAdapter(entryAdapter);
      mListView.setOnItemClickListener(entryAdapter);

      // Launch the entry automatically.
      // TODO: See if list view above can be skipped entirely.
      if (count == 1) {
        launchEntry(cursor.getString(KlingonContentDatabase.COLUMN_ID));
      }
    }
  }

  @Override
  public boolean onSearchRequested() {
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    if (searchManager != null) {
      searchManager.startSearch(
          mPrepopulatedQuery, true, new ComponentName(this, KlingonAssistant.class), null, false);
      return true;
    }
    return false;
  }
}
