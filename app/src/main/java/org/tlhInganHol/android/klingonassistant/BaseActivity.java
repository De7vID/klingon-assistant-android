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

import android.Manifest;
import android.app.SearchManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.tlhInganHol.android.klingonassistant.service.KwotdService;
import org.tlhInganHol.android.klingonassistant.service.UpdateDatabaseService;

public class BaseActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {
  private static final String TAG = "BaseActivity";

  // This must uniquely identify the {boQwI'} entry.
  protected static final String QUERY_FOR_ABOUT = "boQwI':n";

  // Help pages.
  private static final String QUERY_FOR_PRONUNCIATION = "QIch wab Ho'DoS:n";
  private static final String QUERY_FOR_PREFIXES = "moHaq:n";
  private static final String QUERY_FOR_NOUN_SUFFIXES = "DIp:n";
  private static final String QUERY_FOR_VERB_SUFFIXES = "wot:n";

  // Classes of phrases.
  private static final String QUERY_FOR_EMPIRE_UNION_DAY = "*:sen:eu";
  // private static final String QUERY_FOR_IDIOMS                 = "*:sen:idiom";
  private static final String QUERY_FOR_CURSE_WARFARE = "*:sen:mv";
  private static final String QUERY_FOR_NENTAY = "*:sen:nt";
  // private static final String QUERY_FOR_PROVERBS               = "*:sen:prov";
  private static final String QUERY_FOR_QI_LOP = "*:sen:Ql";
  private static final String QUERY_FOR_REJECTION = "*:sen:rej";
  private static final String QUERY_FOR_REPLACEMENT_PROVERBS = "*:sen:rp";
  private static final String QUERY_FOR_SECRECY_PROVERBS = "*:sen:sp";
  private static final String QUERY_FOR_TOASTS = "*:sen:toast";
  private static final String QUERY_FOR_LYRICS = "*:sen:lyr";
  private static final String QUERY_FOR_BEGINNERS_CONVERSATION = "*:sen:bc";
  private static final String QUERY_FOR_JOKES = "*:sen:joke";

  // For beta languages, query for autotranslated definitions.
  private static final String QUERY_FOR_AUTOTRANSLATED_DEFINITIONS = "[AUTOTRANSLATED]";

  // Job ID for the KwotdService jobs. Just has to be unique.
  private static final int KWOTD_SERVICE_PERSISTED_JOB_ID = 0;
  private static final int KWOTD_SERVICE_ONE_OFF_JOB_ID = 1;

  // Job ID for the UpdateDatabaseService jobs. Just has to be unique.
  private static final int UPDATE_DB_SERVICE_PERSISTED_JOB_ID = 10;
  private static final int UPDATE_DB_SERVICE_ONE_OFF_JOB_ID = 11;

  // References to UI components.
  private DrawerLayout mDrawer = null;

  // Helper method to determine whether the device is (likely) a tablet in horizontal orientation.
  public boolean isHorizontalTablet() {
    // Configuration config = getResources().getConfiguration();
    // if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
    //     && (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
    //         >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
    //   return true;
    // }
    // return false;
    return getResources().getBoolean(R.bool.drawer_layout_locked);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Change locale to Klingon if Klingon UI option is set.
    updateLocaleConfiguration();

    // Set default secondary language if it isn't already set.
    Preferences.setDefaultSecondaryLanguage(getBaseContext());

    setContentView(R.layout.activity_base);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (isHorizontalTablet()) {
      // Remove "back" caret on a tablet in horizontal orientation.
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    } else {
      // Show the "back" caret.
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // getSupportActionBar().setIcon(R.drawable.ic_ka);

    // Display the title in Klingon font.
    SpannableString klingonAppName =
        new SpannableString(
            KlingonContentProvider.convertStringToKlingonFont(
                getBaseContext().getResources().getString(R.string.app_name)));
    Typeface klingonTypeface = KlingonAssistant.getKlingonFontTypeface(getBaseContext());
    klingonAppName.setSpan(
        new KlingonTypefaceSpan("", klingonTypeface),
        0,
        klingonAppName.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    getSupportActionBar().setTitle(klingonAppName);
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (mDrawer != null) {
      ActionBarDrawerToggle toggle =
          new ActionBarDrawerToggle(
              this,
              mDrawer,
              toolbar,
              R.string.navigation_drawer_open,
              R.string.navigation_drawer_close);
      mDrawer.setDrawerListener(toggle);
      toggle.syncState();
    }

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
    Menu navMenu = navigationView.getMenu();
    for (int i = 0; i < navMenu.size(); i++) {
      MenuItem menuItem = navMenu.getItem(i);
      SubMenu subMenu = menuItem.getSubMenu();
      if (subMenu != null && subMenu.size() > 0) {
        for (int j = 0; j < subMenu.size(); j++) {
          MenuItem subMenuItem = subMenu.getItem(j);
          applyTypefaceToMenuItem(subMenuItem, /* enlarge */ false);
        }
      }
      applyTypefaceToMenuItem(menuItem, /* enlarge */ true);
    }

    View headerView = navigationView.getHeaderView(0);
    TextView appNameView = (TextView) headerView.findViewById(R.id.app_name_view);
    TextView versionsView = (TextView) headerView.findViewById(R.id.versions_view);
    appNameView.setText(klingonAppName);

    // We use the version of the built-in database as the app's version, since they're in sync.
    String bundledVersion = KlingonContentDatabase.getBundledDatabaseVersion();
    String installedVersion =
        sharedPrefs.getString(
            KlingonContentDatabase.KEY_INSTALLED_DATABASE_VERSION, /* default */ bundledVersion);
    if (bundledVersion.compareToIgnoreCase(installedVersion) >= 0) {
      versionsView.setText(
          String.format(
              getBaseContext().getResources().getString(R.string.app_version), bundledVersion));
    } else {
      versionsView.setText(
          String.format(
              getBaseContext().getResources().getString(R.string.app_and_db_versions),
              bundledVersion,
              installedVersion));
    }

    // If the device is in landscape orientation and the screen size is large (or bigger), then
    // lock the navigation drawer in open mode.
    // make the slide-out menu static. Otherwise, hide it by default.
    // // MenuDrawer.Type drawerType = MenuDrawer.Type.BEHIND;
    // if (isHorizontalTablet()) {
    //   // drawerType = MenuDrawer.Type.STATIC;
    //   drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    // }

    // Schedule the KWOTD service if it hasn't already been started.
    if (sharedPrefs.getBoolean(Preferences.KEY_KWOTD_CHECKBOX_PREFERENCE, /* default */ true)) {
      requestPermissionForKwotdServiceJob(/* isOneOffJob */ false);
    }

    // Schedule the update database service if it hasn't already been started.
    if (sharedPrefs.getBoolean(Preferences.KEY_UPDATE_DB_CHECKBOX_PREFERENCE, /* default */ true)) {
      runUpdateDatabaseServiceJob(/* isOneOffJob */ false);
    }

    // Activate type-to-search for local search. Typing will automatically
    // start a search of the database.
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Change locale to Klingon if Klingon UI option is set.
    updateLocaleConfiguration();

    // Schedule the KWOTD service if it hasn't already been started. It's necessary to do this here
    // because the setting might have changed in Preferences. Note that we don't request permission
    // here because the preference should already be set by the call in onCreate().
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    if (sharedPrefs.getBoolean(Preferences.KEY_KWOTD_CHECKBOX_PREFERENCE, /* default */ true)) {
      runKwotdServiceJob(/* isOneOffJob */ false);
    } else {
      // If the preference is unchecked, cancel the persisted job.
      JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
      scheduler.cancel(KWOTD_SERVICE_PERSISTED_JOB_ID);
    }

    // Schedule the update database service if it hasn't already been started. It's necessary to do
    // this here because the setting might have changed in Preferences.
    if (sharedPrefs.getBoolean(Preferences.KEY_UPDATE_DB_CHECKBOX_PREFERENCE, /* default */ true)) {
      runUpdateDatabaseServiceJob(/* isOneOffJob */ false);
    } else {
      // If the preference is unchecked, cancel the persisted job.
      JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
      scheduler.cancel(UPDATE_DB_SERVICE_PERSISTED_JOB_ID);
    }

    // Put a notification dot in the hamburger menu if a database update is available.
    if (sharedPrefs.getBoolean(
        Preferences.KEY_SHOW_UNSUPPORTED_FEATURES_CHECKBOX_PREFERENCE, /* default */ false)) {
      String installedVersion =
          sharedPrefs.getString(
              KlingonContentDatabase.KEY_INSTALLED_DATABASE_VERSION,
              /* default */ KlingonContentDatabase.getBundledDatabaseVersion());
      String updatedVersion =
          sharedPrefs.getString(
              KlingonContentDatabase.KEY_UPDATED_DATABASE_VERSION, /* default */ installedVersion);
      if (updatedVersion.compareToIgnoreCase(installedVersion) > 0) {
        TextView hamburgerDot = (TextView) findViewById(R.id.hamburger_dot);
        hamburgerDot.setVisibility(View.VISIBLE);

        // TODO: Write this as a separate message instead of overwriting the app version.
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView versionsView = (TextView) headerView.findViewById(R.id.versions_view);
        versionsView.setText(
            String.format(
                getBaseContext().getResources().getString(R.string.database_upgrade_available),
                updatedVersion));
      }
    }
  }

  private void updateLocaleConfiguration() {
    // Override for Klingon language.
    Locale locale;
    if (Preferences.useKlingonUI(getBaseContext())) {
      locale = new Locale("tlh", "CAN");
    } else {
      locale = KlingonAssistant.getSystemLocale();
    }
    Configuration configuration = getBaseContext().getResources().getConfiguration();
    configuration.locale = locale;
    getBaseContext()
        .getResources()
        .updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
  }

  private void applyTypefaceToMenuItem(MenuItem menuItem, boolean enlarge) {
    boolean useKlingonUI = Preferences.useKlingonUI(getBaseContext());
    boolean useKlingonFont = Preferences.useKlingonFont(getBaseContext());
    Typeface klingonTypeface = KlingonAssistant.getKlingonFontTypeface(getBaseContext());
    String title = menuItem.getTitle().toString();
    SpannableString spannableTitle;
    if (useKlingonUI && useKlingonFont) {
      // The UI is displayed in Klingon, in a Klingon font.
      String klingonTitle = KlingonContentProvider.convertStringToKlingonFont(title);
      if (menuItem.getItemId() == R.id.about) {
        // This replacement doesn't get made in convertStringToKlingonFont
        // because it has nothing to do with the usual Klingon sentences which
        // need to be displayed and is really only used for the "about" menu
        // item.
        klingonTitle = klingonTitle.replaceAll("-", "▶");
      }
      spannableTitle = new SpannableString(klingonTitle);
      spannableTitle.setSpan(
          new KlingonTypefaceSpan("", klingonTypeface),
          0,
          spannableTitle.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      spannableTitle = new SpannableString(title);
      if (useKlingonUI) {
        // The UI is in Klingon (but in Latin script), use a serif typeface.
        spannableTitle.setSpan(
            new TypefaceSpan("serif"),
            0,
            spannableTitle.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      if (menuItem.getItemId() == R.id.about) {
        // For the "{boQwI'} - Help" menu item, display the app name in bold serif.
        String appName = getBaseContext().getResources().getString(R.string.app_name);
        int loc = spannableTitle.toString().indexOf(appName);
        if (loc != -1) {
          spannableTitle.setSpan(
              new StyleSpan(android.graphics.Typeface.BOLD),
              loc,
              loc + appName.length(),
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_INTERMEDIATE);
          spannableTitle.setSpan(
              new TypefaceSpan("serif"),
              loc,
              loc + appName.length(),
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
      }
    }
    if (enlarge) {
      spannableTitle.setSpan(
          new RelativeSizeSpan(1.2f),
          0,
          spannableTitle.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      int accent = getResources().getColor(R.color.colorAccent);
      spannableTitle.setSpan(
          new ForegroundColorSpan(accent),
          0,
          spannableTitle.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    menuItem.setTitle(spannableTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);
    for (int i = 0; i < menu.size(); i++) {
      MenuItem menuItem = menu.getItem(i);
      applyTypefaceToMenuItem(menuItem, /* enlarge */ false);
    }

    // Show normally-hidden menu items if "unsupported features" option is selected.
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    if (sharedPrefs.getBoolean(
        Preferences.KEY_SHOW_UNSUPPORTED_FEATURES_CHECKBOX_PREFERENCE, /* default */ false)) {

      MenuItem kwotdButton = menu.findItem(R.id.action_kwotd);
      kwotdButton.setVisible(true);

      MenuItem updateDatabaseButton = menu.findItem(R.id.action_update_db);
      updateDatabaseButton.setVisible(true);

      final String editLang =
          sharedPrefs.getString(
              Preferences.KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, /* default */
              Preferences.getSystemPreferredLanguage());
      // Display menu item to list autotranslated definitions. (Do this even
      // for German, Portuguese, and Finnish, as a database update might mean
      // there are new autotranslations.)
      if (!editLang.equals("NONE")) {
        MenuItem autotranslateButton = menu.findItem(R.id.action_autotranslate);
        autotranslateButton.setVisible(true);
      }
    }

    return true;
  }

  // Set the content view for the menu drawer.
  protected void setDrawerContentView(int layoutResId) {
    ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.drawer_content);
    constraintLayout.removeAllViews();
    LayoutInflater.from(BaseActivity.this).inflate(layoutResId, constraintLayout, true);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.

    int itemId = item.getItemId();
    if (itemId == R.id.pronunciation) { // Show "Pronunciation" screen.
      displayHelp(QUERY_FOR_PRONUNCIATION);
    } else if (itemId == R.id.prefixes) { // Show "Prefixes" screen.
      displayHelp(QUERY_FOR_PREFIXES);
    } else if (itemId == R.id.prefix_chart) { // Show "Prefix chart" screen.
      displayPrefixChart();
    } else if (itemId == R.id.noun_suffixes) { // Show "Noun Suffixes" screen.
      displayHelp(QUERY_FOR_NOUN_SUFFIXES);
    } else if (itemId == R.id.verb_suffixes) { // Show "Verb Suffixes" screen.
      displayHelp(QUERY_FOR_VERB_SUFFIXES);
    } else if (itemId == R.id.sources) { // Show "Sources" screen.
      displaySources();

      // Handle media.
    } else if (itemId == R.id.media_1) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_1_list_id));
    } else if (itemId == R.id.media_2) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_2_list_id));
    } else if (itemId == R.id.media_3) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_3_list_id));
    } else if (itemId == R.id.media_4) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_4_list_id));
    } else if (itemId == R.id.media_5) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_5_list_id));
    } else if (itemId == R.id.media_6) {
      launchYouTubePlaylist(getBaseContext().getResources().getString(R.string.media_6_list_id));

      // Handle KLI activities here.
    } else if (itemId == R.id.kli_lessons) {
      launchExternal("http://www.kli.org/learn-klingon-online/");
    } else if (itemId == R.id.kli_questions) {
      launchExternal("http://www.kli.org/questions/categories/");

      // This is disabled because the KLI channel requires an invite.
      /*
      case R.id.kli_discord:
        launchExternal("https://discordapp.com/channels/235416538927202304/");
        break;
        */

      // Handle social networks.
      /*
      case R.id.gplus:
        // Launch Google+ Klingon speakers community.
        launchExternal("https://plus.google.com/communities/108380135139365833546");
        break;

      case R.id.facebook:
        // Launch Facebook "Learn Klingon" group.
        launchFacebook("LearnKlingon");
        break;

      case R.id.kag:
        // Launch KAG Communications.
        launchExternal("http://www.kag.org/groups/hol-ampas/forum/");
        break;

      case R.id.kidc:
        // Launch KIDC's Klingon Imperial Forums.
        launchExternal("http://www.klingon.org/smboard/index.php?board=6.0");
        break;
      */

      // Handle classes of phrases.
    } else if (itemId == R.id.empire_union_day) {
      displaySearchResults(QUERY_FOR_EMPIRE_UNION_DAY);
      /*
       * case R.id.idioms: displaySearchResults(QUERY_FOR_IDIOMS); return true;
       */
    } else if (itemId == R.id.curse_warfare) {
      displaySearchResults(QUERY_FOR_CURSE_WARFARE);
    } else if (itemId == R.id.nentay) {
      displaySearchResults(QUERY_FOR_NENTAY);
      /*
       * case R.id.proverbs: displaySearchResults(QUERY_FOR_PROVERBS); return true;
       */
    } else if (itemId == R.id.military_celebration) {
      displaySearchResults(QUERY_FOR_QI_LOP);
    } else if (itemId == R.id.rejection) {
      displaySearchResults(QUERY_FOR_REJECTION);
    } else if (itemId == R.id.replacement_proverbs) {
      displaySearchResults(QUERY_FOR_REPLACEMENT_PROVERBS);
    } else if (itemId == R.id.secrecy_proverbs) {
      displaySearchResults(QUERY_FOR_SECRECY_PROVERBS);
    } else if (itemId == R.id.toasts) {
      displaySearchResults(QUERY_FOR_TOASTS);
    } else if (itemId == R.id.lyrics) {
      displaySearchResults(QUERY_FOR_LYRICS);
    } else if (itemId == R.id.beginners_conversation) {
      displaySearchResults(QUERY_FOR_BEGINNERS_CONVERSATION);
    } else if (itemId == R.id.jokes) {
      displaySearchResults(QUERY_FOR_JOKES);

      // Lists.
      // TODO: Handle lists here.
    }

    if (mDrawer != null) {
      mDrawer.closeDrawer(GravityCompat.START);
    }
    return true;
  }

  // Private method to launch a YouTube playlist.
  private void launchYouTubePlaylist(String listId) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    // Set CLEAR_TOP so that hitting the "back" key comes back here.
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.setData(Uri.parse("http://www.youtube.com/playlist?list=" + listId));
    startActivity(intent);
  }

  // Method to launch an external app or web site.
  // TODO: Refactor the identical code in Preferences.
  protected void launchExternal(String externalUrl) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    // Set NEW_TASK so the external app or web site is independent.
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse(externalUrl));
    startActivity(intent);
  }

  // Private method to launch a Facebook group.
  private void launchFacebook(String groupId) {
    Intent intent;
    try {
      // adb shell
      // am start -a android.intent.action.VIEW -d fb://group/LearnKlingon
      getBaseContext().getPackageManager().getPackageInfo("com.facebook.katana", 0);
      intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://group/" + groupId));
    } catch (Exception e) {
      intent =
          new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/" + groupId));
    }
    // Set CLEAR_TOP so that hitting the "back" key comes back here.
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
  }

  // Protected method to display the "help" entries.
  protected void displayHelp(String helpQuery) {
    // Note: managedQuery is deprecated since API 11.
    Cursor cursor =
        managedQuery(
            Uri.parse(KlingonContentProvider.CONTENT_URI + "/lookup"),
            null /* all columns */,
            null,
            new String[] {helpQuery},
            null);
    // Assume cursor.getCount() == 1.
    Uri uri =
        Uri.parse(
            KlingonContentProvider.CONTENT_URI
                + "/get_entry_by_id/"
                + cursor.getString(KlingonContentDatabase.COLUMN_ID));

    Intent entryIntent = new Intent(this, EntryActivity.class);

    // Form the URI for the entry.
    entryIntent.setData(uri);

    startActivity(entryIntent);
  }

  // Protected method to display the prefix chart.
  protected void displayPrefixChart() {
    Intent prefixChartIntent = new Intent(this, PrefixChartActivity.class);
    startActivity(prefixChartIntent);
  }

  // Protected method to display the sources page.
  protected void displaySources() {
    Intent sourcesIntent = new Intent(this, SourcesActivity.class);
    startActivity(sourcesIntent);
  }

  // Protected method to display search results.
  protected void displaySearchResults(String helpQuery) {
    Intent intent = new Intent(this, KlingonAssistant.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setAction(Intent.ACTION_SEARCH);
    intent.putExtra(SearchManager.QUERY, helpQuery);

    startActivity(intent);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // noinspection SimplifiableIfStatement
    int itemId = item.getItemId();
    if (itemId == R.id.action_search) {
      onSearchRequested();
      return true;
    } else if (itemId == android.R.id.home) { // TODO: Toggle menu.
      // mDrawer.toggleMenu();
      /*
      case R.id.social_network:
        SharedPreferences sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (sharedPrefs
            .getString(Preferences.KEY_SOCIAL_NETWORK_LIST_PREFERENCE, "gplus")
            .equals("gplus")) {
          // Launch Google+ Klingon speakers community.
          launchExternal("https://plus.google.com/communities/108380135139365833546");
        } else {
          // Launch Facebook "Learn Klingon" group.
          launchFacebook("LearnKlingon");
        }
        break;
        */
    } else if (itemId == R.id.action_kwotd) {
      requestPermissionForKwotdServiceJob(/* isOneOffJob */ true);
      return true;
    } else if (itemId == R.id.action_update_db) {
      runUpdateDatabaseServiceJob(/* isOneOffJob */ true);
      return true;
    } else if (itemId == R.id.action_autotranslate) {
      displaySearchResults(QUERY_FOR_AUTOTRANSLATED_DEFINITIONS);
      return true;
    } else if (itemId == R.id.about) { // Show "About" screen.
      displayHelp(QUERY_FOR_ABOUT);
      return true;
    } else if (itemId == R.id.preferences) { // Show "Preferences" screen.
      startActivity(new Intent(this, Preferences.class));
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  // Register the permissions callbacks, which handles the user's response to the systems
  // permissions dialog.
  // TODO: Combine the one-off job and scheduled job callbacks somehow.
  private ActivityResultLauncher<String> oneOffJobPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          isGranted -> {
            if (isGranted) {
              runKwotdServiceJob(/* isOneOffJob */ true);
            } else {
              // The user requested KWOTD but also refused notifications permission. Explain to
              // the user why this permission is needed.
              Toast.makeText(
                      this,
                      getResources().getString(R.string.kwotd_requires_notifications_permission),
                      Toast.LENGTH_LONG)
                  .show();
            }
          });
  private ActivityResultLauncher<String> scheduledJobPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          isGranted -> {
            if (isGranted) {
              runKwotdServiceJob(/* isOneOffJob */ false);
            } else {
              // The user has denied notifications permission, so turn off KWOTD if it is on, and
              // inform the user.
              SharedPreferences sharedPrefs =
                  PreferenceManager.getDefaultSharedPreferences(getBaseContext());
              if (sharedPrefs.getBoolean(
                  Preferences.KEY_KWOTD_CHECKBOX_PREFERENCE, /* default */ true)) {
                SharedPreferences.Editor sharedPrefsEd =
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                sharedPrefsEd.putBoolean(Preferences.KEY_KWOTD_CHECKBOX_PREFERENCE, false);
                sharedPrefsEd.apply();

                Toast.makeText(
                        this,
                        getResources().getString(R.string.kwotd_requires_notifications_permission),
                        Toast.LENGTH_LONG)
                    .show();
              }
            }
          });

  protected void requestPermissionForKwotdServiceJob(boolean isOneOffJob) {
    // Starting in API 33, it is necessary to request the POST_NOTIFICATIONS permission to display
    // the KWOTD notification.

    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED) {
      runKwotdServiceJob(isOneOffJob);
    } else if (isOneOffJob) {
      oneOffJobPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    } else {
      scheduledJobPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
  }

  // Helper method to run the KWOTD service job. If isOneOffJob is set to true,
  // this will trigger a job immediately which runs only once. Otherwise, this
  // will schedule a job to run once every 24 hours, if one hasn't already been
  // scheduled.
  protected void runKwotdServiceJob(boolean isOneOffJob) {
    boolean jobAlreadyExists = false;
    JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (!isOneOffJob) {
      // Check if persisted job is already running.
      for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
        if (jobInfo.getId() == KWOTD_SERVICE_PERSISTED_JOB_ID) {
          // Log.d(TAG, "KWOTD job already exists.");
          jobAlreadyExists = true;
          break;
        }
      }
    }

    // Start job.
    if (!jobAlreadyExists) {
      JobInfo.Builder builder;

      if (isOneOffJob) {
        // A one-off request to the KWOTD server needs Internet access.
        ConnectivityManager cm =
            (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
          // Inform the user the fetch will happen when there is an Internet connection.
          Toast.makeText(
                  this,
                  getResources().getString(R.string.kwotd_requires_internet),
                  Toast.LENGTH_LONG)
              .show();
        } else {
          // Inform the user operation is under way.
          Toast.makeText(
                  this, getResources().getString(R.string.kwotd_fetching), Toast.LENGTH_SHORT)
              .show();
        }

        // Either way, schedule the job for when Internet access is available.
        builder =
            new JobInfo.Builder(
                KWOTD_SERVICE_ONE_OFF_JOB_ID, new ComponentName(this, KwotdService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

      } else {
        // Set the job to run every 24 hours, during a window with network connectivity, and
        // exponentially back off if it fails with a delay of 1 hour. (Note that Android caps the
        // backoff at 5 hours, so this will retry at 1 hour, 2 hours, and 4 hours, before it
        // gives up.)
        builder =
            new JobInfo.Builder(
                KWOTD_SERVICE_PERSISTED_JOB_ID, new ComponentName(this, KwotdService.class));
        builder.setPeriodic(TimeUnit.HOURS.toMillis(24));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setBackoffCriteria(TimeUnit.HOURS.toMillis(1), JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiresCharging(false);
        builder.setPersisted(true);
      }

      // Pass custom params to job.
      PersistableBundle extras = new PersistableBundle();
      extras.putBoolean(KwotdService.KEY_IS_ONE_OFF_JOB, isOneOffJob);
      builder.setExtras(extras);

      Log.d(TAG, "Scheduling KwotdService job, isOneOffJob: " + isOneOffJob);
      scheduler.schedule(builder.build());
    }
  }

  // Helper method to run the update database service job. If isOneOffJob is set to true,
  // this will trigger a job immediately which runs only once. Otherwise, this
  // will schedule a job to run once every 30 days, if one hasn't already been
  // scheduled.
  // TODO: Refactor and combine with runKwotdServiceJob.
  protected void runUpdateDatabaseServiceJob(boolean isOneOffJob) {
    boolean jobAlreadyExists = false;
    JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (!isOneOffJob) {
      // Check if persisted job is already running.
      for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
        if (jobInfo.getId() == UPDATE_DB_SERVICE_PERSISTED_JOB_ID) {
          // Log.d(TAG, "Update database job already exists.");
          jobAlreadyExists = true;
          break;
        }
      }
    }

    // Start job.
    if (!jobAlreadyExists) {
      JobInfo.Builder builder;

      if (isOneOffJob) {
        // A one-off request to the update database server needs Internet access.
        ConnectivityManager cm =
            (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
          // Inform the user the fetch will happen when there is an Internet connection.
          Toast.makeText(
                  this,
                  getResources().getString(R.string.update_db_requires_internet),
                  Toast.LENGTH_LONG)
              .show();
        } else {
          // Inform the user operation is under way.
          Toast.makeText(
                  this, getResources().getString(R.string.update_db_fetching), Toast.LENGTH_SHORT)
              .show();
        }

        // Either way, schedule the job for when Internet access is available. For a one-off job,
        // we don't care if Internet access is metered, since the job was triggered by the user.
        builder =
            new JobInfo.Builder(
                UPDATE_DB_SERVICE_ONE_OFF_JOB_ID,
                new ComponentName(this, UpdateDatabaseService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

      } else {
        // Set the job to run every 30 days, during a window with unmetered network connectivity.
        builder =
            new JobInfo.Builder(
                UPDATE_DB_SERVICE_PERSISTED_JOB_ID,
                new ComponentName(this, UpdateDatabaseService.class));
        builder.setPeriodic(TimeUnit.DAYS.toMillis(30));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        builder.setBackoffCriteria(TimeUnit.HOURS.toMillis(1), JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiresCharging(false);
        builder.setPersisted(true);
      }

      Log.d(TAG, "Scheduling UpdateDatabaseService job, isOneOffJob: " + isOneOffJob);
      scheduler.schedule(builder.build());
    }
  }

  // Collapse slide-out menu if "Back" key is pressed and it's open.
  @Override
  public void onBackPressed() {
    if (mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.START)) {
      mDrawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }
}
