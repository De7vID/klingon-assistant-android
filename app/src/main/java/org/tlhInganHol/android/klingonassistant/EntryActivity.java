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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Displays an entry and its definition. */
public class EntryActivity extends BaseActivity
    // TTS:
    implements TextToSpeech.OnInitListener {

  private static final String TAG = "EntryActivity";

  // The currently-displayed entry (which can change due to page selection).
  private KlingonContentProvider.Entry mEntry = null;

  // The parent query that this entry is a part of.
  // private String mParentQuery = null;

  // The intent holding the data to be shared, and the associated UI.
  private Intent mShareEntryIntent = null;
  MenuItem mShareButton = null;

  // Intents for the bottom navigation buttons.
  // Note that the renumber.py script ensures that the IDs of adjacent entries
  // are consecutive across the entire database.
  private Intent mPreviousEntryIntent = null;
  private Intent mNextEntryIntent = null;
  private static final int MAX_ENTRY_ID_DIFF = 5;

  // TTS:
  /** The {@link TextToSpeech} used for speaking. */
  private TextToSpeech mTts = null;

  private MenuItem mSpeakButton;
  private boolean ttsInitialized = false;

  // Handle swipe. The pager widget handles animation and allows swiping
  // horizontally. The pager adapter provides the pages to the pager widget.
  private ViewPager mPager;
  private PagerAdapter mPagerAdapter;
  private int mEntryIndex = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // TTS:
    // Log.d(TAG, "Initialising TTS");
    clearTTS();
    mTts = initTTS();

    setDrawerContentView(R.layout.entry_swipe);

    Uri inputUri = getIntent().getData();
    // Log.d(TAG, "EntryActivity - inputUri: " + inputUri.toString());
    // TODO: Disable the "About" menu item if this is the "About" entry.
    // mParentQuery = getIntent().getStringExtra(SearchManager.QUERY);

    // Determine whether we're launching a single entry, or a list of entries.
    // If it's a single entry, the URI will end in "get_entry_by_id/" followed
    // by the one ID. In the case of a list, the URI will end in "get_entry_by_id/"
    // follwoed by a list of comma-separated IDs, with one additional item at the
    // end for the position of the current entry. For a random entry, the URI will
    // end in "get_random_entry", with no ID at all.
    String[] ids = inputUri.getLastPathSegment().split(",");
    Uri queryUri = null;
    List<String> entryIdsList = new ArrayList<String>(Arrays.asList(ids));
    if (entryIdsList.size() == 1) {
      // There is only one entry to display. Either its ID was explicitly
      // given, or we want a random entry.
      queryUri = inputUri;
      mEntryIndex = 0;
    } else {
      // Parse the comma-separated list, the last entry of which is the
      // position index. We nees to construct the queryUri based on the
      // intended current entry.
      mEntryIndex = Integer.parseInt(entryIdsList.get(ids.length - 1));
      entryIdsList.remove(ids.length - 1);
      queryUri =
          Uri.parse(
              KlingonContentProvider.CONTENT_URI
                  + "/get_entry_by_id/"
                  + entryIdsList.get(mEntryIndex));
    }

    // Retrieve the entry's data.
    // Note: managedQuery is deprecated since API 11.
    Cursor cursor = managedQuery(queryUri, KlingonContentDatabase.ALL_KEYS, null, null, null);
    final KlingonContentProvider.Entry entry =
        new KlingonContentProvider.Entry(cursor, getBaseContext());
    int entryId = entry.getId();

    // Update the entry, which is used for TTS output. This is also updated in onPageSelected.
    mEntry = entry;

    if (entryIdsList.size() == 1 && entryIdsList.get(0).equals("get_random_entry")) {
      // For a random entry, replace "get_random_entry" with the ID of randomly
      // chosen entry.
      entryIdsList.clear();
      entryIdsList.add(Integer.toString(entryId));
    }

    // Set the share intent. This is also done in onPageSelected.
    setShareEntryIntent(entry);

    // Update the bottom navigation buttons. This is also done in onPageSelected.
    updateBottomNavigationButtons(entryId);

    // Update the edit button. This is also done in onPageSelected.
    updateEditButton();

    // Instantiate a ViewPager and a PagerAdapter.
    mPager = (ViewPager) findViewById(R.id.entry_pager);
    mPagerAdapter = new SwipeAdapter(getSupportFragmentManager(), entryIdsList);
    mPager.setAdapter(mPagerAdapter);
    mPager.setCurrentItem(mEntryIndex, /* smoothScroll */ false);
    mPager.setOnPageChangeListener(new SwipePageChangeListener(entryIdsList));

    // Don't display the tab dots if there's only one entry, or if there are 25
    // or more (at which point the dots become not that useful). Note that the
    // entry with the most components at the moment ({cheqotlhchugh...}) has
    // 22 components. The Beginner's Conversation category has over 30 entries,
    // but being able to quickly go between them isn't that useful.
    if (entryIdsList.size() > 1 && entryIdsList.size() < 25) {
      TabLayout tabLayout = (TabLayout) findViewById(R.id.entry_tab_dots);
      tabLayout.setupWithViewPager(mPager, true);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // TTS:
    // This is needed in onResume because we send the user to the Google Play Store to
    // install the TTS engine if it isn't already installed, so the status of the TTS
    // engine may change when this app resumes.
    // Log.d(TAG, "Initialising TTS");
    clearTTS();
    mTts = initTTS();
  }

  private TextToSpeech initTTS() {
    // TTS:
    // Initialize text-to-speech. This is an asynchronous operation.
    // The OnInitListener (second argument) is called after initialization completes.
    return new TextToSpeech(
        this,
        this, // TextToSpeech.OnInitListener
        "org.tlhInganHol.android.klingonttsengine"); // Requires API 14.
  }

  private void clearTTS() {
    if (mTts != null) {
      mTts.stop();
      mTts.shutdown();
    }
  }

  @Override
  protected void onDestroy() {
    // TTS:
    // Don't forget to shutdown!
    // Log.d(TAG, "Shutting down TTS");
    clearTTS();
    super.onDestroy();
  }

  /*
   * TODO: Override onSave/RestoreInstanceState, onPause/Resume/Stop, to re-create links.
   *
   * public onSaveInstanceState() { // Save the text and views here. super.onSaveInstanceState(); }
   * public onRestoreInstanceState() { // Restore the text and views here.
   * super.onRestoreInstanceState(); }
   */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    mShareButton = menu.findItem(R.id.action_share);

    // This is also updated in onPageSelected.
    if (mShareEntryIntent != null) {
      // Enable "Share" button.
      mShareButton.setVisible(true);
    }

    // TTS:
    // The button is disabled in the layout. It should only be enabled in EntryActivity.
    mSpeakButton = menu.findItem(R.id.action_speak);
    // if (ttsInitialized) {
    //   // Log.d(TAG, "enabling TTS button in onCreateOptionsMenu");
    mSpeakButton.setVisible(true);
    // }

    return true;
  }

  // Set the share intent for this entry.
  private void setShareEntryIntent(KlingonContentProvider.Entry entry) {
    if (entry.isAlternativeSpelling()) {
      // Disable sharing alternative spelling entries.
      mShareEntryIntent = null;
      return;
    }

    Resources resources = getResources();
    mShareEntryIntent = new Intent(Intent.ACTION_SEND);
    mShareEntryIntent.putExtra(Intent.EXTRA_TITLE, resources.getString(R.string.share_popup_title));
    mShareEntryIntent.setType("text/plain");
    String subject = "{" + entry.getFormattedEntryName(/* isHtml */ false) + "}";
    mShareEntryIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    String snippet = subject + "\n" + entry.getFormattedDefinition(/* isHtml */ false);
    mShareEntryIntent.putExtra(
        Intent.EXTRA_TEXT, snippet + "\n\n" + resources.getString(R.string.shared_from));
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_speak) { // TTS:
      if (!ttsInitialized) {
        // The TTS engine is not installed (or disabled). Send user to Google Play Store or other
        // market.
        try {
          launchExternal("market://details?id=org.tlhInganHol.android.klingonttsengine");
        } catch (ActivityNotFoundException e) {
          // Fall back to browser.
          launchExternal(
              "https://play.google.com/store/apps/details?id=org.tlhInganHol.android.klingonttsengine");
        }
      } else if (mEntry != null) {
        // The TTS engine is working, and there's something to say, say it.
        // Log.d(TAG, "Speaking");
        // Toast.makeText(getBaseContext(), mEntry.getEntryName(), Toast.LENGTH_LONG).show();
        mTts.speak(mEntry.getEntryName(), TextToSpeech.QUEUE_FLUSH, null);
      }
      return true;
    } else if (itemId == R.id.action_share) { // Share using the Android Sharesheet.
      Intent shareIntent =
          Intent.createChooser(
              mShareEntryIntent, getResources().getString(R.string.share_popup_title));
      startActivity(shareIntent);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  // TTS:
  // Implements TextToSpeech.OnInitListener.
  @Override
  public void onInit(int status) {
    // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
    if (status == TextToSpeech.SUCCESS) {
      // Set preferred language to Canadian Klingon.
      // Note that a language may not be available, and the result will indicate this.
      int result = mTts.setLanguage(new Locale("tlh", "", ""));
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Lanuage data is missing or the language is not supported.
        Log.e(TAG, "Language is not available.");
      } else {
        // Check the documentation for other possible result codes.
        // For example, the language may be available for the locale,
        // but not for the specified country and variant.

        // The TTS engine has been successfully initialized.
        ttsInitialized = true;
        // if (mSpeakButton != null) {
        //   // Log.d(TAG, "enabling TTS button in onInit");
        //   mSpeakButton.setVisible(true);
        // }
      }
    } else {
      // Initialization failed.
      Log.e(TAG, "Could not initialize TextToSpeech.");
    }
  }

  // Swipe
  private class SwipeAdapter extends FragmentStatePagerAdapter {
    private List<EntryFragment> entryFragments = null;

    public SwipeAdapter(FragmentManager fm, List<String> entryIdsList) {
      super(fm);

      // Set up all of the entry fragments.
      entryFragments = new ArrayList<EntryFragment>();
      for (int i = 0; i < entryIdsList.size(); i++) {
        Uri uri =
            Uri.parse(
                KlingonContentProvider.CONTENT_URI + "/get_entry_by_id/" + entryIdsList.get(i));
        entryFragments.add(EntryFragment.newInstance(uri));
      }
    }

    @Override
    public Fragment getItem(int position) {
      return entryFragments.get(position);
    }

    @Override
    public int getCount() {
      return entryFragments.size();
    }
  }

  private class SwipePageChangeListener implements ViewPager.OnPageChangeListener {
    List<String> mEntryIdsList = null;

    public SwipePageChangeListener(List<String> entryIdsList) {
      mEntryIdsList = entryIdsList;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
      Uri uri =
          Uri.parse(
              KlingonContentProvider.CONTENT_URI
                  + "/get_entry_by_id/"
                  + mEntryIdsList.get(position));

      // Note: managedQuery is deprecated since API 11.
      Cursor cursor = managedQuery(uri, KlingonContentDatabase.ALL_KEYS, null, null, null);
      final KlingonContentProvider.Entry entry =
          new KlingonContentProvider.Entry(cursor, getBaseContext());
      int entryId = entry.getId();

      // Update the entry (used for TTS output). This is also set in onCreate.
      mEntry = entry;

      // Update share menu and set the visibility of the share button. The intent is also set in
      // onCreate, while the visibility is also set in onCreateOptionsMenu.
      setShareEntryIntent(entry);
      if (mShareEntryIntent != null) {
        // Enable "Share" button. Note that mShareButton can be null if the device has been rotated.
        if (mShareButton != null) {
          mShareButton.setVisible(true);
        }
      } else {
        // Disable "Share" button.
        if (mShareButton != null) {
          mShareButton.setVisible(false);
        }
      }

      // Update the bottom navigation buttons. This is also done in onCreate.
      updateBottomNavigationButtons(entryId);

      // Update the edit button. This is also done in onCreate.
      updateEditButton();
    }

    @Override
    public void onPageScrollStateChanged(int state) {}
  }

  private void updateBottomNavigationButtons(int entryId) {
    BottomNavigationView bottomNavView =
        (BottomNavigationView) findViewById(R.id.bottom_navigation);
    Menu bottomNavMenu = bottomNavView.getMenu();

    // Check for a previous entry.
    mPreviousEntryIntent = null;
    for (int i = 1; i <= MAX_ENTRY_ID_DIFF; i++) {
      Intent entryIntent = getEntryByIdIntent(entryId - i);
      if (entryIntent != null) {
        mPreviousEntryIntent = entryIntent;
        break;
      }
    }

    // Update the state of the "Previous" button.
    MenuItem previousButton = (MenuItem) bottomNavMenu.findItem(R.id.action_previous);
    if (mPreviousEntryIntent == null) {
      previousButton.setEnabled(false);
      bottomNavView.findViewById(R.id.action_previous).setVisibility(View.INVISIBLE);
    } else {
      previousButton.setEnabled(true);
      bottomNavView.findViewById(R.id.action_previous).setVisibility(View.VISIBLE);
    }

    // Check for a next entry.
    mNextEntryIntent = null;
    for (int i = 1; i <= MAX_ENTRY_ID_DIFF; i++) {
      Intent entryIntent = getEntryByIdIntent(entryId + i);
      if (entryIntent != null) {
        mNextEntryIntent = entryIntent;
        break;
      }
    }

    // Update the state of the "Next" button.
    MenuItem nextButton = (MenuItem) bottomNavMenu.findItem(R.id.action_next);
    if (mNextEntryIntent == null) {
      nextButton.setEnabled(false);
      bottomNavView.findViewById(R.id.action_next).setVisibility(View.INVISIBLE);
    } else {
      nextButton.setEnabled(true);
      bottomNavView.findViewById(R.id.action_next).setVisibility(View.VISIBLE);
    }

    bottomNavView.setOnNavigationItemSelectedListener(
        new BottomNavigationView.OnNavigationItemSelectedListener() {
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_previous) {
              goToPreviousEntry();
            } else if (itemId == R.id.action_random) {
              goToRandomEntry();
            } else if (itemId == R.id.action_next) {
              goToNextEntry();
            }
            return false;
          }
        });
  }

  private void updateEditButton() {
    // Enable FAB if conditions are met:
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    final String editLang =
        sharedPrefs.getString(
            Preferences.KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, /* default */
            Preferences.getSystemPreferredLanguage());
    final boolean showUnsupportedFeatures =
        sharedPrefs.getBoolean(
            Preferences.KEY_SHOW_UNSUPPORTED_FEATURES_CHECKBOX_PREFERENCE, /* default */ false);
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    // Show the "edit" button for secondary languages other than German, Portuguese, and Finnish
    // (as these are complete except for the addition of new words), but also show for these
    // languages if unsupported features are enabled.
    if (mEntry != null
        && !editLang.equals("NONE")
        && (showUnsupportedFeatures
            || (!editLang.equals("de") && !editLang.equals("pt") && !editLang.equals("fi")))) {
      fab.setVisibility(View.VISIBLE);
      fab.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              String definitionTranslation = null;
              switch (editLang) {
                case "de":
                  definitionTranslation = mEntry.getDefinition_DE();
                  break;
                case "fa":
                  definitionTranslation = mEntry.getDefinition_FA();
                  break;
                case "ru":
                  definitionTranslation = mEntry.getDefinition_RU();
                  break;
                case "sv":
                  definitionTranslation = mEntry.getDefinition_SV();
                  break;
                case "zh-HK":
                  definitionTranslation = mEntry.getDefinition_ZH_HK();
                  break;
                case "pt":
                  definitionTranslation = mEntry.getDefinition_PT();
                  break;
                case "fi":
                  definitionTranslation = mEntry.getDefinition_FI();
                  break;
                case "fr":
                  definitionTranslation = mEntry.getDefinition_FR();
                  break;
              }
              // Open a form with fields filled in.
              SubmitCorrectionTask submitCorrectionTask = new SubmitCorrectionTask();
              submitCorrectionTask.execute(
                  mEntry.getEntryName(),
                  mEntry.getPartOfSpeech(),
                  mEntry.getDefinition(),
                  editLang,
                  definitionTranslation.replace(" [AUTOTRANSLATED]", ""));
            }
          });
    } else {
      fab.setVisibility(View.INVISIBLE);
    }
  }

  private Intent getEntryByIdIntent(int entryId) {
    Cursor cursor;
    cursor =
        managedQuery(
            Uri.parse(KlingonContentProvider.CONTENT_URI + "/get_entry_by_id/" + entryId),
            null /* all columns */,
            null,
            null,
            null);
    if (cursor.getCount() == 1) {
      Uri uri =
          Uri.parse(
              KlingonContentProvider.CONTENT_URI
                  + "/get_entry_by_id/"
                  + cursor.getString(KlingonContentDatabase.COLUMN_ID));

      Intent entryIntent = new Intent(this, EntryActivity.class);

      // Form the URI for the entry.
      entryIntent.setAction(Intent.ACTION_VIEW);
      entryIntent.setData(uri);

      return entryIntent;
    }
    return null;
  }

  private void goToPreviousEntry() {
    if (mPreviousEntryIntent != null) {
      startActivity(mPreviousEntryIntent);
    }
  }

  private void goToRandomEntry() {
    Uri uri = Uri.parse(KlingonContentProvider.CONTENT_URI + "/get_random_entry");
    Intent randomEntryIntent = new Intent(this, EntryActivity.class);
    randomEntryIntent.setAction(Intent.ACTION_VIEW);
    randomEntryIntent.setData(uri);
    startActivity(randomEntryIntent);
  }

  private void goToNextEntry() {
    if (mNextEntryIntent != null) {
      startActivity(mNextEntryIntent);
    }
  }

  // Generate a Google Forms form for submitting corrections to non-English definitions.
  private class SubmitCorrectionTask extends AsyncTask<String, Void, Boolean> {
    private static final String CORRECTION_FORM_URL =
        "https://docs.google.com/forms/d/e/1FAIpQLSdubRpIpbPFHAclzNx3jrOT85nQLGYCgWPOjIHxPocrecZUzw/viewform";
    private static final String CORRECTION_ENTRY_NAME_KEY = "entry.1852970057";
    private static final String CORRECTION_PART_OF_SPEECH_KEY = "entry.1015346696";
    private static final String CORRECTION_DEFINITION_KEY = "entry.166391661";
    private static final String CORRECTION_LANGUAGE_KEY = "entry.2030201514";
    private static final String CORRECTION_DEFINITION_TRANSLATION_KEY = "entry.1343345";

    @Override
    protected Boolean doInBackground(String... correction) {
      Boolean result = true;
      String entry_name = correction[0];
      String part_of_speech = correction[1];
      String definition = correction[2];
      String language = correction[3];
      String definition_translation = correction[4];
      String params = "";
      try {
        params =
            CORRECTION_ENTRY_NAME_KEY
                + "="
                + URLEncoder.encode(entry_name, "UTF-8")
                + "&"
                + CORRECTION_PART_OF_SPEECH_KEY
                + "="
                + URLEncoder.encode(part_of_speech, "UTF-8")
                + "&"
                + CORRECTION_DEFINITION_KEY
                + "="
                + URLEncoder.encode(definition, "UTF-8")
                + "&"
                + CORRECTION_LANGUAGE_KEY
                + "="
                + URLEncoder.encode(language, "UTF-8")
                + "&"
                + CORRECTION_DEFINITION_TRANSLATION_KEY
                + "="
                + URLEncoder.encode(definition_translation, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        Log.e(TAG, "Failed to encode params.");
        return false;
      }
      launchExternal(CORRECTION_FORM_URL + "?" + params);
      return true;
    }
  }
}
