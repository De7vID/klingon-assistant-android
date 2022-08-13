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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;
import java.util.Locale;

public class Preferences extends AppCompatPreferenceActivity
    implements OnSharedPreferenceChangeListener {

  // Language preferences.
  private static final String KEY_KLINGON_UI_CHECKBOX_PREFERENCE = "klingon_ui_checkbox_preference";
  private static final String KEY_KLINGON_FONT_LIST_PREFERENCE = "klingon_font_list_preference";
  private static final String KEY_LANGUAGE_DEFAULT_ALREADY_SET = "language_default_already_set";
  public static final String KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE =
      "show_secondary_language_list_preference";

  // Legacy support for German, will eventually be deprecated and replaced by secondary language
  // support.
  public static final String KEY_SHOW_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE =
      "show_german_definitions_checkbox_preference";
  public static final String KEY_SEARCH_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE =
      "search_german_definitions_checkbox_preference";

  // Input preferences.
  public static final String KEY_XIFAN_HOL_CHECKBOX_PREFERENCE = "xifan_hol_checkbox_preference";
  public static final String KEY_SWAP_QS_CHECKBOX_PREFERENCE = "swap_qs_checkbox_preference";

  // Social preferences.
  public static final String KEY_SOCIAL_NETWORK_LIST_PREFERENCE = "social_network_list_preference";

  // Informational preferences.
  public static final String KEY_SHOW_TRANSITIVITY_CHECKBOX_PREFERENCE =
      "show_transitivity_checkbox_preference";
  public static final String KEY_SHOW_ADDITIONAL_INFORMATION_CHECKBOX_PREFERENCE =
      "show_additional_information_checkbox_preference";
  public static final String KEY_KWOTD_CHECKBOX_PREFERENCE = "kwotd_checkbox_preference";
  public static final String KEY_UPDATE_DB_CHECKBOX_PREFERENCE = "update_db_checkbox_preference";

  // Under construction.
  public static final String KEY_SHOW_UNSUPPORTED_FEATURES_CHECKBOX_PREFERENCE =
      "show_unsupported_features_checkbox_preference";

  // Changelogs.
  public static final String KEY_DATA_CHANGELOG_BUTTON_PREFERENCE =
      "data_changelog_button_preference";
  public static final String KEY_CODE_CHANGELOG_BUTTON_PREFERENCE =
      "code_changelog_button_preference";

  // Detect if the system language is a supported language.
  public static String getSystemPreferredLanguage() {
    String language = KlingonAssistant.getSystemLocale().getLanguage();
    if (language == Locale.GERMAN.getLanguage()) {
      return "de";
    } else if (language == new Locale("fa").getLanguage()) {
      return "fa";
    } else if (language == new Locale("ru").getLanguage()) {
      return "ru";
    } else if (language == new Locale("sv").getLanguage()) {
      return "sv";
    } else if (language == Locale.CHINESE.getLanguage()) {
      // TODO: Distinguish different topolects of Chinese. For now, prefer Hong Kong Chinese if the
      // system locale is any topolect of Chinese.
      return "zh-HK";
    } else if (language == new Locale("pt").getLanguage()) {
      // Note: The locale code "pt" is Brazilian Portuguese. (European Portuguese is "pt-PT".)
      return "pt";
    } else if (language == new Locale("fi").getLanguage()) {
      return "fi";
    }
    return "NONE";
  }

  public static void setDefaultSecondaryLanguage(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (!sharedPrefs.getBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, /* default */ false)) {
      SharedPreferences.Editor sharedPrefsEd = sharedPrefs.edit();
      sharedPrefsEd.putString(
          KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, getSystemPreferredLanguage());
      sharedPrefsEd.putBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, true);
      sharedPrefsEd.apply();
    }
  }

  // Whether the UI (menus, hints, etc.) should be displayed in Klingon.
  public static boolean useKlingonUI(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPrefs.getBoolean(
        Preferences.KEY_KLINGON_UI_CHECKBOX_PREFERENCE, /* default */ false);
  }

  // Whether a Klingon font should be used when display Klingon text.
  public static boolean useKlingonFont(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    String value = sharedPrefs.getString(KEY_KLINGON_FONT_LIST_PREFERENCE, /* default */ "LATIN");
    return value.equals("TNG") || value.equals("DSC") || value.equals("CORE");
  }

  // Returns which font should be used for Klingon: returns one of "LATIN", "TNG", "DSC", or "CORE".
  public static String getKlingonFontCode(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPrefs.getString(KEY_KLINGON_FONT_LIST_PREFERENCE, /* default */ "LATIN");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Restore system (non-Klingon) locale.
    restoreLocaleConfiguration();

    // Set up the toolbar for an AppCompatPreferenceActivity.
    setupActionBar();

    // Load the preferences from an XML resource.
    addPreferencesFromResource(R.xml.preferences);

    // Get a reference to the {pIqaD} list preference, and apply the display option to it.
    ListPreference klingonFontListPreference =
        (ListPreference) getPreferenceScreen().findPreference(KEY_KLINGON_FONT_LIST_PREFERENCE);
    String title = klingonFontListPreference.getTitle().toString();
    SpannableString ssb;
    if (!useKlingonFont(getBaseContext())) {
      // Display in bold serif.
      ssb = new SpannableString(title);
      ssb.setSpan(
          new StyleSpan(android.graphics.Typeface.BOLD),
          0,
          ssb.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_INTERMEDIATE);
      ssb.setSpan(new TypefaceSpan("serif"), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      String klingonTitle = KlingonContentProvider.convertStringToKlingonFont(title);
      ssb = new SpannableString(klingonTitle);
      Typeface klingonTypeface = KlingonAssistant.getKlingonFontTypeface(getBaseContext());
      ssb.setSpan(
          new KlingonTypefaceSpan("", klingonTypeface),
          0,
          ssb.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    klingonFontListPreference.setTitle(ssb);

    // TODO: Expand the language list to include incomplete languages if unsupported features is
    // selected. Switch to English if unsupported features has been deselected and an incomplete
    // language has been selected. Enable or disable the search in secondary language checkbox.
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    SharedPreferences.Editor sharedPrefsEd = sharedPrefs.edit();

    // Support the legacy German options.
    final boolean showGerman =
        sharedPrefs.getBoolean(
            KEY_SHOW_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE, /* default */ false);
    if (showGerman) {
      final boolean searchGerman =
          sharedPrefs.getBoolean(
              KEY_SEARCH_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE, /* default */ false);

      // Copy to the new settings.
      sharedPrefsEd.putString(KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, "de");

      // Clear the legacy settings.
      sharedPrefsEd.putBoolean(KEY_SHOW_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE, false);
      sharedPrefsEd.putBoolean(KEY_SEARCH_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE, false);

      sharedPrefsEd.putBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, true);
      sharedPrefsEd.apply();
    }

    // Set the defaults for the other-language options based on the user's language, if it hasn't
    // been already set.
    if (!sharedPrefs.getBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, /* default */ false)) {
      ListPreference mShowOtherLanguageListPreference =
          (ListPreference)
              getPreferenceScreen().findPreference(KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE);
      mShowOtherLanguageListPreference.setValue(getSystemPreferredLanguage());

      sharedPrefsEd.putBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, true);
      sharedPrefsEd.apply();
    }

    Preference dataChangelogButtonPreference =
        getPreferenceScreen().findPreference(KEY_DATA_CHANGELOG_BUTTON_PREFERENCE);
    dataChangelogButtonPreference.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                    getBaseContext());
                String installedVersion =
                    sharedPrefs.getString(
                        KlingonContentDatabase.KEY_INSTALLED_DATABASE_VERSION,
                        /* default */ KlingonContentDatabase.getBundledDatabaseVersion());
                launchExternal("https://github.com/De7vID/klingon-assistant-data/commits/master@{" +
                    installedVersion + "}");
                return true;
            }
        });
    Preference codeChangelogButtonPreference =
        getPreferenceScreen().findPreference(KEY_CODE_CHANGELOG_BUTTON_PREFERENCE);
    codeChangelogButtonPreference.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // The bundled database version is the app's built date.
                launchExternal(
                    "https://github.com/De7vID/klingon-assistant-android/commits/master@{" +
                    KlingonContentDatabase.getBundledDatabaseVersion() + "}");
                return true;
            }
        });
  }

  private void restoreLocaleConfiguration() {
    // Always restore system (non-Klingon) locale here.
    Locale locale = KlingonAssistant.getSystemLocale();
    Configuration configuration = getBaseContext().getResources().getConfiguration();
    configuration.locale = locale;
    getBaseContext()
        .getResources()
        .updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
  }

  private void setupActionBar() {
    // This only works in ICS (API 14) and up.
    ViewGroup root =
        (ViewGroup) findViewById(android.R.id.list).getParent().getParent().getParent();
    Toolbar toolbar =
        (Toolbar) LayoutInflater.from(this).inflate(R.layout.view_toolbar, root, false);
    root.addView(toolbar, 0);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Restore system (non-Klingon) locale.
    restoreLocaleConfiguration();

    // Set up a listener whenever a key changes.
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister the listener whenever a key changes.
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPrefs, final String key) {
    if (key.equals(KEY_KLINGON_FONT_LIST_PREFERENCE)
        || key.equals(KEY_KLINGON_UI_CHECKBOX_PREFERENCE)) {
      // User has changed the Klingon font option or UI language, display a warning.
      new AlertDialog.Builder(this)
          .setIcon(R.drawable.alert_dialog_icon)
          .setTitle(R.string.warning)
          .setMessage(R.string.change_ui_language_warning)
          .setCancelable(false) // Can't be canceled with the BACK key.
          .setPositiveButton(
              android.R.string.yes,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                  // Since the display options have changed, everything needs to be redrawn.
                  recreate();
                }
              })
          .show();
    }
    // TODO: React to unsupported features and secondary language options changes here.
  }

  // Method to launch an external app or web site.
  // See identical method in BaseActivity.
  private void launchExternal(String externalUrl) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    // Set NEW_TASK so the external app or web site is independent.
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse(externalUrl));
    startActivity(intent);
  }
}
