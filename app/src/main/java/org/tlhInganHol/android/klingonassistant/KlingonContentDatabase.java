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

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * Contains logic to return specific entries from the database, and load the database table when it
 * needs to be created.
 */
public class KlingonContentDatabase {
  private static final String TAG = "KlingonContentDatabase";

  // The columns included in the database table.
  public static final String KEY_ID = BaseColumns._ID;
  public static final String KEY_ENTRY_NAME = "entry_name";
  public static final String KEY_PART_OF_SPEECH = "part_of_speech";
  public static final String KEY_DEFINITION = "definition";
  public static final String KEY_SYNONYMS = "synonyms";
  public static final String KEY_ANTONYMS = "antonyms";
  public static final String KEY_SEE_ALSO = "see_also";
  public static final String KEY_NOTES = "notes";
  public static final String KEY_HIDDEN_NOTES = "hidden_notes";
  public static final String KEY_COMPONENTS = "components";
  public static final String KEY_EXAMPLES = "examples";
  public static final String KEY_SEARCH_TAGS = "search_tags";
  public static final String KEY_SOURCE = "source";

  // Languages other than English.
  public static final String KEY_DEFINITION_DE = "definition_de";
  public static final String KEY_NOTES_DE = "notes_de";
  public static final String KEY_EXAMPLES_DE = "examples_de";
  public static final String KEY_SEARCH_TAGS_DE = "search_tags_de";
  public static final String KEY_DEFINITION_FA = "definition_fa";
  public static final String KEY_NOTES_FA = "notes_fa";
  public static final String KEY_EXAMPLES_FA = "examples_fa";
  public static final String KEY_SEARCH_TAGS_FA = "search_tags_fa";
  public static final String KEY_DEFINITION_SV = "definition_sv";
  public static final String KEY_NOTES_SV = "notes_sv";
  public static final String KEY_EXAMPLES_SV = "examples_sv";
  public static final String KEY_SEARCH_TAGS_SV = "search_tags_sv";
  public static final String KEY_DEFINITION_RU = "definition_ru";
  public static final String KEY_NOTES_RU = "notes_ru";
  public static final String KEY_EXAMPLES_RU = "examples_ru";
  public static final String KEY_SEARCH_TAGS_RU = "search_tags_ru";
  public static final String KEY_DEFINITION_ZH_HK = "definition_zh_HK";
  public static final String KEY_NOTES_ZH_HK = "notes_zh_HK";
  public static final String KEY_EXAMPLES_ZH_HK = "examples_zh_HK";
  public static final String KEY_SEARCH_TAGS_ZH_HK = "search_tags_zh_HK";
  public static final String KEY_DEFINITION_PT = "definition_pt";
  public static final String KEY_NOTES_PT = "notes_pt";
  public static final String KEY_EXAMPLES_PT = "examples_pt";
  public static final String KEY_SEARCH_TAGS_PT = "search_tags_pt";
  public static final String KEY_DEFINITION_FI = "definition_fi";
  public static final String KEY_NOTES_FI = "notes_fi";
  public static final String KEY_EXAMPLES_FI = "examples_fi";
  public static final String KEY_SEARCH_TAGS_FI = "search_tags_fi";
  public static final String KEY_DEFINITION_FR = "definition_fr";
  public static final String KEY_NOTES_FR = "notes_fr";
  public static final String KEY_EXAMPLES_FR = "examples_fr";
  public static final String KEY_SEARCH_TAGS_FR = "search_tags_fr";

  // The order of the keys to access the columns.
  public static final int COLUMN_ID = 0;
  public static final int COLUMN_ENTRY_NAME = 1;
  public static final int COLUMN_PART_OF_SPEECH = 2;
  public static final int COLUMN_DEFINITION = 3;
  public static final int COLUMN_SYNONYMS = 4;
  public static final int COLUMN_ANTONYMS = 5;
  public static final int COLUMN_SEE_ALSO = 6;
  public static final int COLUMN_NOTES = 7;
  public static final int COLUMN_HIDDEN_NOTES = 8;
  public static final int COLUMN_COMPONENTS = 9;
  public static final int COLUMN_EXAMPLES = 10;
  public static final int COLUMN_SEARCH_TAGS = 11;
  public static final int COLUMN_SOURCE = 12;

  // Languages other than English.
  public static final int COLUMN_DEFINITION_DE = 13;
  public static final int COLUMN_NOTES_DE = 14;
  public static final int COLUMN_EXAMPLES_DE = 15;
  public static final int COLUMN_SEARCH_TAGS_DE = 16;
  public static final int COLUMN_DEFINITION_FA = 17;
  public static final int COLUMN_NOTES_FA = 18;
  public static final int COLUMN_EXAMPLES_FA = 19;
  public static final int COLUMN_SEARCH_TAGS_FA = 20;
  public static final int COLUMN_DEFINITION_SV = 21;
  public static final int COLUMN_NOTES_SV = 22;
  public static final int COLUMN_EXAMPLES_SV = 23;
  public static final int COLUMN_SEARCH_TAGS_SV = 25;
  public static final int COLUMN_DEFINITION_RU = 25;
  public static final int COLUMN_NOTES_RU = 26;
  public static final int COLUMN_EXAMPLES_RU = 27;
  public static final int COLUMN_SEARCH_TAGS_RU = 28;
  public static final int COLUMN_DEFINITION_ZH_HK = 29;
  public static final int COLUMN_NOTES_ZH_HK = 30;
  public static final int COLUMN_EXAMPLES_ZH_HK = 31;
  public static final int COLUMN_SEARCH_TAGS_ZH_HK = 32;
  public static final int COLUMN_DEFINITION_PT = 33;
  public static final int COLUMN_NOTES_PT = 34;
  public static final int COLUMN_EXAMPLES_PT = 35;
  public static final int COLUMN_SEARCH_TAGS_PT = 36;
  public static final int COLUMN_DEFINITION_FI = 37;
  public static final int COLUMN_NOTES_FI = 38;
  public static final int COLUMN_EXAMPLES_FI = 39;
  public static final int COLUMN_SEARCH_TAGS_FI = 40;
  public static final int COLUMN_DEFINITION_FR = 41;
  public static final int COLUMN_NOTES_FR = 42;
  public static final int COLUMN_EXAMPLES_FR = 43;
  public static final int COLUMN_SEARCH_TAGS_FR = 44;

  // All keys.
  public static final String[] ALL_KEYS = {
    KEY_ID,
    KEY_ENTRY_NAME,
    KEY_PART_OF_SPEECH,
    KEY_DEFINITION,
    KEY_SYNONYMS,
    KEY_ANTONYMS,
    KEY_SEE_ALSO,
    KEY_NOTES,
    KEY_HIDDEN_NOTES,
    KEY_COMPONENTS,
    KEY_EXAMPLES,
    KEY_SEARCH_TAGS,
    KEY_SOURCE,
    KEY_DEFINITION_DE,
    KEY_NOTES_DE,
    KEY_EXAMPLES_DE,
    KEY_SEARCH_TAGS_DE,
    KEY_DEFINITION_FA,
    KEY_NOTES_FA,
    KEY_EXAMPLES_FA,
    KEY_SEARCH_TAGS_FA,
    KEY_DEFINITION_SV,
    KEY_NOTES_SV,
    KEY_EXAMPLES_SV,
    KEY_SEARCH_TAGS_SV,
    KEY_DEFINITION_RU,
    KEY_NOTES_RU,
    KEY_EXAMPLES_RU,
    KEY_SEARCH_TAGS_RU,
    KEY_DEFINITION_ZH_HK,
    KEY_NOTES_ZH_HK,
    KEY_EXAMPLES_ZH_HK,
    KEY_SEARCH_TAGS_ZH_HK,
    KEY_DEFINITION_PT,
    KEY_NOTES_PT,
    KEY_EXAMPLES_PT,
    KEY_SEARCH_TAGS_PT,
    KEY_DEFINITION_FI,
    KEY_NOTES_FI,
    KEY_EXAMPLES_FI,
    KEY_SEARCH_TAGS_FI,
    KEY_DEFINITION_FR,
    KEY_NOTES_FR,
    KEY_EXAMPLES_FR,
    KEY_SEARCH_TAGS_FR,
  };

  // The name of the database and the database object for accessing it.
  private static final String DATABASE_NAME = "qawHaq.db";
  private static final String FTS_VIRTUAL_TABLE = "mem";

  // The name of the database for updates.
  public static final String REPLACEMENT_DATABASE_NAME = "qawHaq_new.db";

  // This should be kept in sync with the version number in the data/VERSION
  // file used to generate the database which is bundled into the app.
  private static final int BUNDLED_DATABASE_VERSION = 202311201;

  // Metadata about the installed database, and the updated database, if any.
  public static final String KEY_INSTALLED_DATABASE_VERSION = "installed_database_version";
  public static final String KEY_ID_OF_FIRST_EXTRA_ENTRY = "id_of_first_extra_entry";
  public static final String KEY_UPDATED_DATABASE_VERSION = "updated_database_version";
  public static final String KEY_UPDATED_ID_OF_FIRST_EXTRA_ENTRY =
      "updated_id_of_first_extra_entry";

  // Arbitrary limit on max buffer length to prevent overflows and such.
  private static final int MAX_BUFFER_LENGTH = 1024;

  // These are automatically updated by renumber.py in the data directory, and correspond to
  // the IDs of the first entry and one past the ID of the last non-hypothetical,
  // non-extended-canon entry in the database, respectively.
  private static final int ID_OF_FIRST_ENTRY = 10000;
  private static final int ID_OF_FIRST_EXTRA_ENTRY = 15346;

  private final KlingonDatabaseOpenHelper mDatabaseOpenHelper;
  private static final HashMap<String, String> mColumnMap = buildColumnMap();
  private final Context mContext;

  // Keeps track of whether db created/upgraded message has been displayed already.
  private static boolean mNewDatabaseMessageDisplayed = false;

  /**
   * Constructor
   *
   * @param context The Context within which to work, used to create the DB
   */
  public KlingonContentDatabase(Context context) {
    // Create a database helper to access the Klingon Database.
    mDatabaseOpenHelper = new KlingonDatabaseOpenHelper(context);
    mContext = context;

    // Initialise the database, and create it if necessary.
    try {
      // Log.d(TAG, "1. Initialising db.");
      mDatabaseOpenHelper.initDatabase();
    } catch (IOException e) {
      throw new Error("Unable to create database.");
    }

    // Open the database for use.
    try {
      // Log.d(TAG, "2. Opening db.");
      mDatabaseOpenHelper.openDatabase();
    } catch (SQLException e) {
      // Possibly an attempt to write a readonly database.
      // Do nothing.
    }
  }

  /**
   * Builds a map for all columns that may be requested, which will be given to the
   * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include all
   * columns, even if the value is the key. This allows the ContentProvider to request columns w/o
   * the need to know real column names and create the alias itself.
   */
  private static HashMap<String, String> buildColumnMap() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(KEY_ENTRY_NAME, KEY_ENTRY_NAME);
    map.put(KEY_DEFINITION, KEY_DEFINITION);
    map.put(KEY_ID, "rowid AS " + KEY_ID);
    map.put(
        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
        "rowid AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
    map.put(
        SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
        "rowid AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
    return map;
  }

  /**
   * Returns a Cursor positioned at the entry specified by rowId
   *
   * @param rowId id of entry to retrieve
   * @param columns The columns to include, if null then all are included
   * @return Cursor positioned to matching entry, or null if not found.
   */
  public Cursor getEntry(String rowId, String[] columns) {
    // Log.d(TAG, "getEntry called with rowId: " + rowId);

    String selection = "rowid = ?";
    String[] selectionArgs = new String[] {rowId};

    /*
     * This builds a query that looks like: SELECT <columns> FROM <table> WHERE rowid = <rowId>
     */
    return query(selection, selectionArgs, columns);
  }

  /**
   * Convert a string written in "xifan hol" shorthand to {tlhIngan Hol}. This is a mapping which
   * makes it easier to type, since shifting is unnecessary.
   *
   * <p>Make the following replacements: d -> D f -> ng h -> H (see note below) i -> I k -> Q s -> S
   * x -> tlh z -> '
   *
   * <p>When replacing "h" with "H", the following must be preserved: ch -> ch gh -> gh tlh -> tlh
   * ngh -> ngh (n + gh) ngH -> ngH (ng + H)
   *
   * <p>TODO: Consider allowing "invisible h". But this probably makes things too "loose". // c ->
   * ch (but ch -/> chh) // g -> gh (but gh -/> ghh and ng -/> ngh)
   */
  private String expandShorthand(String shorthand) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    if (!sharedPrefs.getBoolean(
        Preferences.KEY_XIFAN_HOL_CHECKBOX_PREFERENCE, /* default */ false)) {
      // The user has disabled the "xifan hol" shorthand, so just do nothing and return.
      return shorthand;
    }
    if (sharedPrefs.getBoolean(Preferences.KEY_SWAP_QS_CHECKBOX_PREFERENCE, /* default */ false)) {
      // Map q to Q and k to q.
      shorthand = shorthand.replaceAll("q", "Q");
      shorthand = shorthand.replaceAll("k", "q");
    }

    // Note: The order of the replacements is important.
    return shorthand
        .replaceAll("ngH", "NGH") // differentiate "ngh" from "ngH"
        .replaceAll("h", "H") // side effects: ch -> cH, gh -> gH (also ngh -> ngH), tlh -> tlH
        .replaceAll("cH", "ch") // restore "ch"
        .replaceAll("gH", "gh") // restore "gh" (also "ngh")
        .replaceAll("tlH", "tlh") // restore "tlh"
        .replaceAll("g", "gX") // g -> gX, side effects: gh -> gXh, ng -> ngX
        .replaceAll("gXh", "gh") // restore "gh"
        .replaceAll("ngX", "ng") // restore "ng"
        .replaceAll("gX", "gh") // g -> gh
        .replaceAll("NGH", "ngH") // restore "ngH"
        .replaceAll("c", "cX") // c -> cX, side effect: ch -> cXh
        .replaceAll("cXh", "ch") // restore "ch"
        .replaceAll("cX", "ch") // c -> ch
        .replaceAll("d", "D") // do unambiguous replacements
        .replaceAll("f", "ng")
        .replaceAll("i", "I")
        .replaceAll("k", "Q") // If the swap Qs preference was selected, this will have no effect.
        .replaceAll("s", "S")
        .replaceAll("z", "'")
        .replaceAll("x", "tlh")
        // At this point, "ngH" is definitely {ng} + {H}, but "ngh" might be either {n} + {gh} or
        // {ng} + {H}. Furthermore, "ng" might be {ng} or {n} + {gh}.
        // These are the possible words with {n} + {gh}: {nenghep}, {QIngheb}, {tlhonghaD}
        // These are the possible words with {ng} + {H}: {chungHa'wI'}, {mangHom}, {qengHoD},
        // {tungHa'}, {vengHom}. Instead of checking both, cheat by hardcoding the possibilities.
        // This means this code has to be updated whenever an entry with {ngH} or {ngh} is
        // added to the database.
        .replaceAll("(chung|mang|qeng|tung|veng)h", "$1H")
        .replaceAll("Hanguq", "Hanghuq")
        .replaceAll("nengep", "nenghep")
        .replaceAll("QIngeb", "QIngheb")
        .replaceAll("tlhongaD", "tlhonghaD");
  }

  public static String sanitizeInput(String s) {
    // Sanitize for SQL. Assume double-quote is a typo for single-quote. Convert {pIqaD} to Latin.
    // Also trim.
    return s.replaceAll("\"", "'")
        .replaceAll("", "gh")
        .replaceAll("", "ng")
        .replaceAll("", "tlh")
        .replaceAll("", "a")
        .replaceAll("", "b")
        .replaceAll("", "ch")
        .replaceAll("", "D")
        .replaceAll("", "e")
        .replaceAll("", "H")
        .replaceAll("", "I")
        .replaceAll("", "j")
        .replaceAll("", "l")
        .replaceAll("", "m")
        .replaceAll("", "n")
        .replaceAll("", "o")
        .replaceAll("", "p")
        .replaceAll("", "q")
        .replaceAll("", "Q")
        .replaceAll("", "r")
        .replaceAll("", "S")
        .replaceAll("", "t")
        .replaceAll("", "u")
        .replaceAll("", "v")
        .replaceAll("", "w")
        .replaceAll("", "y")
        .replaceAll("", "'")
        .replaceAll("", "0")
        .replaceAll("", "1")
        .replaceAll("", "2")
        .replaceAll("", "3")
        .replaceAll("", "4")
        .replaceAll("", "5")
        .replaceAll("", "6")
        .replaceAll("", "7")
        .replaceAll("", "8")
        .replaceAll("", "9")
        .replaceAll("’", "'") // "smart" quote
        .replaceAll("‘", "'") // "smart" left quote
        .replaceAll("\u2011", "-") // non-breaking hyphen
        .trim();
  }

  private boolean IsPotentialComplexWordOrSentence(
      KlingonContentProvider.Entry queryEntry, String query) {
    // If the POS is unknown and the query is greater than 4 characters, try to parse it
    // as a complex word or sentence. Most queries of 4 characters or fewer are not complex,
    // so for efficiency reasons we don't try to parse them, but there are a few exceptional
    // verbs which are two letters long, which need to be handled as a special case.
    if (queryEntry.basePartOfSpeechIsUnknown()) {
      if (query.length() > 4) {
        return true;
      }

      // A shortlist of two-letter verbs. These plus a prefix or suffix might make a 4-character
      // complex word. This check needs to be updated whenever a 2-letter verb is added to the
      // database.
      if (query.length() == 4) {
        switch (query.substring(2, 4)) {
          case "Da":
          case "lu":
          case "Qa":
          case "Qu":
          case "Sa":
          case "tu":
          case "yo":
            return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a Cursor over all entries that match the given query.
   *
   * @param query The query, including entry name and metadata, to search for.
   * @return Cursor over all entries that match, or null if none found.
   */
  public Cursor getEntryMatches(String query) {
    // A query may be preceded by a plus to override (disable) "xifan hol" mode. This is used
    // for internal searches.
    boolean overrideXifanHol = false;
    if (!query.isEmpty() && query.charAt(0) == '+') {
      overrideXifanHol = true;
      query = query.substring(1);
    }

    // Sanitize input.
    query = sanitizeInput(query);

    // Log.d(TAG, "getEntryMatches called with query: \"" + query + "\"");
    MatrixCursor resultsCursor = new MatrixCursor(ALL_KEYS);
    HashSet<Integer> resultsSet = new HashSet<Integer>();

    // Parse the query's metadata, and get the base query.
    KlingonContentProvider.Entry queryEntry = new KlingonContentProvider.Entry(query, mContext);
    String queryBase = queryEntry.getEntryName();

    // If the query has components specified, then we're in analysis mode, and the solution is
    // already given to us.
    ArrayList<KlingonContentProvider.Entry> analysisComponents =
        queryEntry.getComponentsAsEntries();
    if (!analysisComponents.isEmpty()) {
      // Add the given list of components to the results.
      addGivenComponentsToResults(analysisComponents, resultsCursor, resultsSet);

      // Finally, add the complete query entry itself.
      addExactMatch(queryBase, queryEntry, resultsCursor, /* indent */ false);

      // Since the components are in the db, do no further analysis.
      return resultsCursor;
    }

    String looseQuery;
    if (query.indexOf(':') != -1) {
      // If this is a system query, don't use "xifan hol" loosening.
      looseQuery = queryBase;
      if (queryBase.equals("*") && queryEntry.isSentence()) {
        // Specifically, if this is a query for a sentence class, search exactly for the matching
        // sentences.
        // We know the query begins with "*:" so strip that to get the sentence class.
        return getMatchingSentences(query.substring(2));
      }
    } else if (overrideXifanHol) {
      looseQuery = queryBase;
    } else {
      // Assume the user is searching for an "exact" Klingon word or phrase, subject to
      // "xifan hol" loosening (if enabled).
      looseQuery = expandShorthand(queryBase);
    }

    // TODO: Add option to search English and other-language fields first, followed by Klingon.
    // (Many users are searching for a Klingon word using a non-Klingon search query, rather
    // than the other way around.)
    if (IsPotentialComplexWordOrSentence(queryEntry, looseQuery)) {
      // If the query matches some heuristics, try to parse it as a complex word or sentence.
      parseQueryAsComplexWordOrSentence(looseQuery, resultsCursor, resultsSet);
    } else {
      // Otherwise, assume the base query is a prefix of the desired result.
      Cursor resultsWithGivenPrefixCursor =
          getEntriesContainingQuery(looseQuery, /* isPrefix */ true);
      copyCursorEntries(
          resultsCursor, resultsSet, resultsWithGivenPrefixCursor, /* filter */ true, queryEntry);
      if (resultsWithGivenPrefixCursor != null) {
        resultsWithGivenPrefixCursor.close();
      }
    }

    // If the query was made without a base part of speech, expand the
    // search to include entries not beginning with the query, and also
    // search on the (English) definition and search tags.
    if (queryEntry.basePartOfSpeechIsUnknown()) {
      // Try the entries, but not from the beginning. Limit to at
      // least 2 characters as anything less than that isn't meaningful in
      // Klingon, but 2 characters allow searching from the end for
      // "rhyming" purposes.
      int klingonNonPrefixMinLength = 2;
      if (queryEntry.getEntryName().length() >= klingonNonPrefixMinLength) {
        Cursor resultsWithGivenQueryCursor =
            getEntriesContainingQuery(looseQuery, /* isPrefix */ false);
        copyCursorEntries(
            resultsCursor, resultsSet, resultsWithGivenQueryCursor, /* filter */ false, null);
        if (resultsWithGivenQueryCursor != null) {
          resultsWithGivenQueryCursor.close();
        }
      }

      // Match definitions, from beginning. Since the definition is (almost
      // always) canonical, always search in English. Additionally search in
      // other-language if that option is set.
      matchDefinitionsOrSearchTags(
          queryBase,
          true, /* isPrefix */
          false, /* useSearchTags */
          false, /* searchOtherLanguageDefinitions */
          resultsCursor,
          resultsSet);
      matchDefinitionsOrSearchTags(
          queryBase,
          true, /* isPrefix */
          false, /* useSearchTags */
          true, /* searchOtherLanguageDefinitions */
          resultsCursor,
          resultsSet);

      // Match definitions, anywhere else. Again, always search in English, and
      // additionally search in other-language if that option is set. Limit to 3
      // characters as there would be too many coincidental hits otherwise, except
      // if other-language is Chinese.
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      final String otherLang =
          sharedPrefs.getString(
              Preferences.KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, /* default */
              Preferences.getSystemPreferredLanguage());
      int englishNonPrefixMinLength = 3;
      int otherLanguageNonPrefixMinLength = otherLang.equals("zh-HK") ? 1 : 3;

      if (queryEntry.getEntryName().length() >= englishNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            false, /* isPrefix */
            false, /* useSearchTags */
            false, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }
      if (queryEntry.getEntryName().length() >= otherLanguageNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            false, /* isPrefix */
            false, /* useSearchTags */
            true, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }

      // Match search tags, from beginning, then anywhere else.
      if (queryEntry.getEntryName().length() >= englishNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            true, /* isPrefix */
            true, /* useSearchTags */
            false, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }
      if (queryEntry.getEntryName().length() >= otherLanguageNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            true, /* isPrefix */
            true, /* useSearchTags */
            true, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }
      if (queryEntry.getEntryName().length() >= englishNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            false, /* isPrefix */
            true, /* useSearchTags */
            false, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }
      if (queryEntry.getEntryName().length() >= otherLanguageNonPrefixMinLength) {
        matchDefinitionsOrSearchTags(
            queryBase,
            false, /* isPrefix */
            true, /* useSearchTags */
            true, /* searchOtherLanguageDefinitions */
            resultsCursor,
            resultsSet);
      }
    }

    return resultsCursor;
  }

  // Helper method to add a list of components to the list of search results.
  private void addGivenComponentsToResults(
      ArrayList<KlingonContentProvider.Entry> analysisComponents,
      MatrixCursor resultsCursor,
      HashSet<Integer> resultsSet) {
    // Create a list of complex words.
    ArrayList<KlingonContentProvider.ComplexWord> complexWordsList =
        new ArrayList<KlingonContentProvider.ComplexWord>();

    // Keep track of current state. The verb suffix level is required for analysing rovers.
    KlingonContentProvider.ComplexWord currentComplexWord = null;
    KlingonContentProvider.Entry currentPrefixEntry = null;
    int verbSuffixLevel = 0;
    for (KlingonContentProvider.Entry componentEntry : analysisComponents) {
      String componentEntryName = componentEntry.getEntryName();
      boolean isNoun = componentEntry.isNoun();
      boolean isVerb = componentEntry.isVerb();
      boolean isPrefix = componentEntry.isPrefix();
      boolean isSuffix = componentEntry.isSuffix();

      if (!isSuffix && (!isVerb || currentPrefixEntry == null)) {
        // A new word is about to begin, so flush a complex word if there is one.
        if (currentComplexWord != null) {
          // We set a strict match because this is information given explicitly in the db.
          addComplexWordToResults(
              currentComplexWord, resultsCursor, resultsSet, /* isLenient */ false);
          currentComplexWord = null;
        }
      }

      if (!isNoun && !isVerb && !isPrefix && !isSuffix) {
        // Add this word directly.
        addExactMatch(componentEntryName, componentEntry, resultsCursor, /* indent */ false);
        continue;
      }

      // At this point, we know this is either a suffix, or a prefix, verb, or noun which begins a
      // new word.
      if (isSuffix && (currentComplexWord != null)) {
        // A suffix, attach to the current word.
        // Note that isNoun here indicates whether the suffix is a noun suffix, not
        // whether the stem is a noun or verb. This is important since noun suffixes
        // can be attached to nouns formed from verbs using {-wI'} or {-ghach}.
        verbSuffixLevel =
            currentComplexWord.attachSuffix(componentEntryName, isNoun, verbSuffixLevel);
      } else if (isPrefix) {
        // A prefix, save to attach to the next verb.
        currentPrefixEntry = componentEntry;
      } else if (isNoun || isVerb) {
        // Create a new complex word, so reset suffix level.
        // Note that this can be a noun, a verb, or an unattached suffix (like in the entry {...-Daq
        // qaDor.}.
        currentComplexWord = new KlingonContentProvider.ComplexWord(componentEntryName, isNoun);
        currentComplexWord.setHomophoneNumber(componentEntry.getHomophoneNumber());
        verbSuffixLevel = 0;
        if (isVerb && currentPrefixEntry != null) {
          currentComplexWord.attachPrefix(currentPrefixEntry.getEntryName());
          currentPrefixEntry = null;
        }
      }
    }
    if (currentComplexWord != null) {
      // Flush any outstanding word.
      addComplexWordToResults(currentComplexWord, resultsCursor, resultsSet, /* isLenient */ false);
    }
  }

  // Helper method to copy entries from one cursor to another.
  // If filter is true, queryEntry must be provided.
  private void copyCursorEntries(
      MatrixCursor destCursor,
      HashSet<Integer> destSet,
      Cursor srcCursor,
      boolean filter,
      KlingonContentProvider.Entry queryEntry) {
    if (srcCursor != null && srcCursor.getCount() != 0) {
      srcCursor.moveToFirst();
      do {
        KlingonContentProvider.Entry resultEntry =
            new KlingonContentProvider.Entry(srcCursor, mContext);

        // Filter by the query if requested to do so. If filter is
        // true, the entry will be added only if it is a match that
        // satisfies certain requirements.
        if (!filter || queryEntry.isSatisfiedBy(resultEntry)) {
          // Prevent duplicates.
          Object[] entryObject = convertEntryToCursorRow(resultEntry, /* indent */ false);
          Integer intId = Integer.valueOf(resultEntry.getId());
          if (!destSet.contains(intId)) {
            destSet.add(intId);
            destCursor.addRow(entryObject);
          }
        }
      } while (srcCursor.moveToNext());
    }

    // Modify cursor to be like query() below.
    destCursor.moveToFirst();
  }

  // Helper method to search for entries whose prefixes match the query.
  private Cursor getEntriesContainingQuery(String queryBase, boolean isPrefix) {
    // Note: it is important to use the double quote character for quotes
    // because the single quote character is a letter in (transliterated)
    // Klingon. Also, force LIKE to be case-sensitive to distinguish
    // {q} and {Q}.
    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
    db.rawQuery("PRAGMA case_sensitive_like = ON", null);
    // If the query must be a prefix of the entry name, do not precede with wildcard.
    String precedingWildcard = isPrefix ? "" : "%";
    Cursor cursor = null;
    try {
      cursor =
          db.query(
              true,
              FTS_VIRTUAL_TABLE,
              ALL_KEYS,
              KlingonContentDatabase.KEY_ENTRY_NAME
                  + " LIKE \""
                  + precedingWildcard
                  + queryBase.trim()
                  + "%\"",
              null,
              null,
              null,
              null,
              null);
    } catch (SQLiteException e) {
      // Do nothing.
    }
    return cursor;
  }

  // Helper method to search for an exact match.
  private Cursor getExactMatches(String entryName) {
    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
    db.rawQuery("PRAGMA case_sensitive_like = ON", null);
    Cursor cursor = null;
    try {
      cursor =
          db.query(
              true,
              FTS_VIRTUAL_TABLE,
              ALL_KEYS,
              KlingonContentDatabase.KEY_ENTRY_NAME + " LIKE \"" + entryName.trim() + "\"",
              null,
              null,
              null,
              null,
              null);
    } catch (SQLiteException e) {
      // Do nothing.
    }
    return cursor;
  }

  // Helper method to search for a sentence class.
  private Cursor getMatchingSentences(String sentenceClass) {
    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
    db.rawQuery("PRAGMA case_sensitive_like = ON", null);
    Cursor cursor = null;
    try {
      cursor =
          db.query(
              true,
              FTS_VIRTUAL_TABLE,
              ALL_KEYS,
              KlingonContentDatabase.KEY_PART_OF_SPEECH + " LIKE \"" + sentenceClass + "\"",
              null,
              null,
              null,
              null,
              null);
    } catch (SQLiteException e) {
      // Do nothing.
    }
    return cursor;
  }

  // Helper method to search for entries whose definitions or search tags match the query.
  // Note that matches are case-insensitive.
  private Cursor getEntriesMatchingDefinition(
      String piece,
      boolean isPrefix,
      boolean useSearchTags,
      boolean searchOtherLanguageDefinitions) {

    // The search key is either the definition or the search tags.
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    final String otherLang =
        sharedPrefs.getString(
            Preferences.KEY_SHOW_SECONDARY_LANGUAGE_LIST_PREFERENCE, /* default */
            Preferences.getSystemPreferredLanguage());
    String key =
        useSearchTags
            ? KlingonContentDatabase.KEY_SEARCH_TAGS
            : KlingonContentDatabase.KEY_DEFINITION;
    if (searchOtherLanguageDefinitions) {
      switch (otherLang) {
        case "de":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_DE
                  : KlingonContentDatabase.KEY_DEFINITION_DE;
          break;

        case "fa":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_FA
                  : KlingonContentDatabase.KEY_DEFINITION_FA;
          break;

        case "ru":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_RU
                  : KlingonContentDatabase.KEY_DEFINITION_RU;
          break;

        case "sv":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_SV
                  : KlingonContentDatabase.KEY_DEFINITION_SV;
          break;

        case "zh-HK":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_ZH_HK
                  : KlingonContentDatabase.KEY_DEFINITION_ZH_HK;
          break;

        case "pt":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_PT
                  : KlingonContentDatabase.KEY_DEFINITION_PT;
          break;

        case "fi":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_FI
                  : KlingonContentDatabase.KEY_DEFINITION_FI;
          break;
        case "fr":
          key =
              useSearchTags
                  ? KlingonContentDatabase.KEY_SEARCH_TAGS_FR
                  : KlingonContentDatabase.KEY_DEFINITION_FR;
          break;
      }
    }

    // If searching for a prefix (here, this means not a verb prefix, but
    // a query which is a prefix of the definition), nothing can precede
    // the query; otherwise, it must be preceded by a space (it begins a word),
    // except in Chinese.
    String nonPrefixPrecedingWildCard = otherLang.equals("zh-HK") ? "%" : "% ";
    String precedingWildcard = isPrefix ? "" : nonPrefixPrecedingWildCard;

    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
    db.rawQuery("PRAGMA case_sensitive_like = OFF", null);

    Cursor cursor = null;
    try {
      cursor =
          db.query(
              true,
              FTS_VIRTUAL_TABLE,
              ALL_KEYS,
              key + " LIKE \"" + precedingWildcard + piece.trim() + "%\"",
              null,
              null,
              null,
              null,
              null);
    } catch (SQLiteException e) {
      // Do nothing.
    }
    return cursor;
  }

  // Helper method to make it easier to search either definitions or search tags, in either English
  // or other-language.
  private void matchDefinitionsOrSearchTags(
      String piece,
      boolean isPrefix,
      boolean useSearchTags,
      boolean searchOtherLanguageDefinitions,
      MatrixCursor resultsCursor,
      HashSet<Integer> resultsSet) {
    Cursor matchingResults =
        getEntriesMatchingDefinition(
            piece, isPrefix, useSearchTags, searchOtherLanguageDefinitions);
    copyCursorEntries(resultsCursor, resultsSet, matchingResults, /* filter */ false, null);
    if (matchingResults != null) {
      matchingResults.close();
    }
  }

  // Helper method to add one exact match to the results cursor.
  private void addExactMatch(
      String query,
      KlingonContentProvider.Entry filterEntry,
      MatrixCursor resultsCursor,
      boolean indent) {
    Cursor exactMatchesCursor = getExactMatches(query);
    // There must be a match.
    if (exactMatchesCursor == null || exactMatchesCursor.getCount() == 0) {
      Log.e(TAG, "Exact match error on query: " + query);
      return;
    }
    // Log.d(TAG, "Exact matches found: " + exactMatchesCursor.getCount());
    exactMatchesCursor.moveToFirst();
    do {
      KlingonContentProvider.Entry resultEntry =
          new KlingonContentProvider.Entry(exactMatchesCursor, mContext);
      if (filterEntry.isSatisfiedBy(resultEntry)) {
        Object[] exactMatchObject = convertEntryToCursorRow(resultEntry, indent);
        /*
         * if (BuildConfig.DEBUG) { Log.d(TAG, "addExactMatch: " + resultEntry.getEntryName()); }
         */
        resultsCursor.addRow(exactMatchObject);
        // Log.d(TAG, "added exact match to results: " + query);
        // Only add each one once.
        break;
      }
    } while (exactMatchesCursor.moveToNext());
    exactMatchesCursor.close();
  }

  // Helper method to parse a complex word or a sentence.
  private void parseQueryAsComplexWordOrSentence(
      String query, MatrixCursor resultsCursor, HashSet<Integer> resultsSet) {
    // This set stores the complex words.
    ArrayList<KlingonContentProvider.ComplexWord> complexWordsList =
        new ArrayList<KlingonContentProvider.ComplexWord>();

    // Split the query into sentences.
    String[] sentences = query.split(";,\\.?!");
    for (String sentence : sentences) {
      // Remove all non-valid characters and split the sentence into words (separated by spaces).
      String[] words = sentence.replaceAll("[^A-Za-z' ]", "").split("\\s+");
      for (int i = 0; i < words.length; i++) {
        String word = words[i];

        // Try to parse n-tuples of words as complex nouns.
        // Do this from longest to shortest, since we want longest matches first.
        // TODO: Refactor for space and time efficiency.
        for (int j = words.length; j > i; j--) {
          String compoundNoun = words[i];
          for (int k = i + 1; k < j; k++) {
            compoundNoun += " " + words[k];
          }
          // Log.d(TAG, "parseQueryAsComplexWordOrSentence: compoundNoun = " + compoundNoun);
          KlingonContentProvider.parseComplexWord(
              compoundNoun, /* isNounCandidate */ true, complexWordsList);
        }

        // Next, try to parse this as a verb.
        // Log.d(TAG, "parseQueryAsComplexWordOrSentence: verb = " + word);
        KlingonContentProvider.parseComplexWord(
            word, /* isNounCandidate */ false, complexWordsList);
      }
    }
    for (KlingonContentProvider.ComplexWord complexWord : complexWordsList) {
      // Be a little lenient and also match non-nouns and non-verbs.
      addComplexWordToResults(complexWord, resultsCursor, resultsSet, /* isLenient */ true);
    }
  }

  private void addComplexWordToResults(
      KlingonContentProvider.ComplexWord complexWord,
      MatrixCursor resultsCursor,
      HashSet<Integer> resultsSet,
      boolean isLenient) {
    // The isLenient flag is for determining whether we are doing a real analysis (set to true), or
    // whether the correct analysis has already been supplied in the components (set to false). When
    // set to true, a bare word will match any part of speech (not just noun or verb). But for this
    // reason, duplicates are removed (since there may be many of them). However, when set to false,
    // duplicates will be kept (since the given correct analysis contains them).
    KlingonContentProvider.Entry filterEntry =
        new KlingonContentProvider.Entry(complexWord.filter(isLenient), mContext);
    Cursor exactMatchesCursor = getExactMatches(complexWord.stem());

    boolean stemAdded = false;
    if (exactMatchesCursor != null && exactMatchesCursor.getCount() != 0) {
      Log.d(TAG, "found stem = " + complexWord.stem());
      String prefix = complexWord.getVerbPrefix();

      // Add all exact matches for stem.
      exactMatchesCursor.moveToFirst();
      boolean prefixAdded = false;
      do {
        KlingonContentProvider.Entry resultEntry =
            new KlingonContentProvider.Entry(exactMatchesCursor, mContext);
        // An archaic or hypothetical word or phrase, even if it's an exact match, will never be
        // part of a complex word. However, allow slang, regional, and extended canon. Also,
        // verbs are satisfied by pronouns, but we exclude a pronoun if there is a prefix.
        if (filterEntry.isSatisfiedBy(resultEntry)
            && !resultEntry.isArchaic()
            && !resultEntry.isHypothetical()
            && !(resultEntry.isPronoun() && !prefix.equals(""))) {
          Log.d(
              TAG,
              "adding: " + resultEntry.getEntryName() + " (" + resultEntry.getPartOfSpeech() + ")");

          // If this is a bare word, prevent duplicates.
          Integer intId = Integer.valueOf(resultEntry.getId());
          if (!complexWord.isBareWord() || !resultsSet.contains(intId) || !isLenient) {
            // Add the verb prefix if one exists, before the verb stem itself.
            if (!prefix.equals("") && !prefixAdded) {
              Log.d(TAG, "verb prefix = " + prefix);
              KlingonContentProvider.Entry prefixFilterEntry =
                  new KlingonContentProvider.Entry(prefix + ":v:pref", mContext);
              addExactMatch(prefix, prefixFilterEntry, resultsCursor, /* indent */ false);
              prefixAdded = true;
            }
            Object[] exactMatchObject = complexWordCursorRow(resultEntry, complexWord, prefixAdded);

            if (BuildConfig.DEBUG) {
              Log.d(TAG, "addComplexWordToResults: " + resultEntry.getEntryName());
            }
            resultsCursor.addRow(exactMatchObject);
            stemAdded = true;
            if (complexWord.isBareWord()) {
              resultsSet.add(intId);
            }
          }
        }
      } while (exactMatchesCursor.moveToNext());
      exactMatchesCursor.close();
    }

    // Whether or not there was an exact match, if the complex word is a number, add its components.
    if (complexWord.isNumberLike()) {
      String numberRoot = complexWord.getNumberRoot();
      String numberRootAnnotation = complexWord.getNumberRootAnnotation();
      String numberModifier = complexWord.getNumberModifier();
      String numberSuffix = complexWord.getNumberSuffix();

      // First, add the root as a word. (The annotation is already included.)
      if (!numberRoot.equals("") && (!stemAdded || !numberRoot.equals(complexWord.stem()))) {
        filterEntry =
            new KlingonContentProvider.Entry(numberRoot + ":" + numberRootAnnotation, mContext);
        if (BuildConfig.DEBUG) {
          Log.d(TAG, "numberRoot: " + numberRoot);
        }
        addExactMatch(numberRoot, filterEntry, resultsCursor, /* indent */ false);
        stemAdded = true;
      }

      // Next, add the modifier as a word.
      if (!numberModifier.equals("")) {
        filterEntry = new KlingonContentProvider.Entry(numberModifier + ":n:num", mContext);
        addExactMatch(numberModifier, filterEntry, resultsCursor, /* indent */ true);
      }

      // Finally, add the number suffix.
      if (!numberSuffix.equals("")) {
        numberSuffix = "-" + numberSuffix;
        filterEntry = new KlingonContentProvider.Entry(numberSuffix + ":n:num,suff", mContext);
        addExactMatch(numberSuffix, filterEntry, resultsCursor, /* indent */ true);
      }
    }

    // Now add all suffixes, but only if one of the corresponding stems was a legitimate entry.
    if (stemAdded) {
      // Add verb suffixes. Verb suffixes must go before noun suffixes since two of them
      // can turn a verb into a noun.
      // For purposes of analysis, pronouns are also verbs, but they cannot have prefixes.
      String[] verbSuffixes = complexWord.getVerbSuffixes();
      for (int j = 0; j < verbSuffixes.length; j++) {
        // Check verb suffix of the current type.
        if (!verbSuffixes[j].equals("")) {
          Log.d(TAG, "verb suffix = " + verbSuffixes[j]);
          filterEntry = new KlingonContentProvider.Entry(verbSuffixes[j] + ":v:suff", mContext);
          addExactMatch(verbSuffixes[j], filterEntry, resultsCursor, /* indent */ true);
        }

        // Check for the true rovers.
        String[] rovers = complexWord.getRovers(j);
        for (String rover : rovers) {
          Log.d(TAG, "rover = " + rover);
          filterEntry = new KlingonContentProvider.Entry(rover + ":v:suff", mContext);
          addExactMatch(rover, filterEntry, resultsCursor, /* indent */ true);
        }
      }

      // Add noun suffixes.
      String[] nounSuffixes = complexWord.getNounSuffixes();
      for (int j = 0; j < nounSuffixes.length; j++) {
        if (!nounSuffixes[j].equals("")) {
          Log.d(TAG, "noun suffix = " + nounSuffixes[j]);
          filterEntry = new KlingonContentProvider.Entry(nounSuffixes[j] + ":n:suff", mContext);
          addExactMatch(nounSuffixes[j], filterEntry, resultsCursor, /* indent */ true);
        }
      }
    }
  }

  private Object[] complexWordCursorRow(
      KlingonContentProvider.Entry entry,
      KlingonContentProvider.ComplexWord complexWord,
      boolean indent) {
    // TODO: Add warnings for mismatched affixes here.
    return new Object[] {
      entry.getId(),
      complexWord.getVerbPrefixString() + entry.getEntryName() + complexWord.getSuffixesString(),
      // This works only because all verbs are tagged with transitivity information, so we know the
      // POS looks like "v:t" which we turn into "v:t,indent".
      entry.getPartOfSpeech() + (indent ? ",indent" : ""),
      entry.getDefinition(),
      entry.getSynonyms(),
      entry.getAntonyms(),
      entry.getSeeAlso(),
      entry.getNotes(),
      entry.getHiddenNotes(),
      entry.getComponents(),
      entry.getExamples(),
      entry.getSearchTags(),
      entry.getSource(),
      entry.getDefinition_DE(),
      entry.getNotes_DE(),
      entry.getExamples_DE(),
      entry.getSearchTags_DE(),
      entry.getDefinition_FA(),
      entry.getNotes_FA(),
      entry.getExamples_FA(),
      entry.getSearchTags_FA(),
      entry.getDefinition_SV(),
      entry.getNotes_SV(),
      entry.getExamples_SV(),
      entry.getSearchTags_SV(),
      entry.getDefinition_RU(),
      entry.getNotes_RU(),
      entry.getExamples_RU(),
      entry.getSearchTags_RU(),
      entry.getDefinition_ZH_HK(),
      entry.getNotes_ZH_HK(),
      entry.getExamples_ZH_HK(),
      entry.getSearchTags_ZH_HK(),
      entry.getDefinition_PT(),
      entry.getNotes_PT(),
      entry.getExamples_PT(),
      entry.getSearchTags_PT(),
      entry.getDefinition_FI(),
      entry.getNotes_FI(),
      entry.getExamples_FI(),
      entry.getSearchTags_FI(),
      entry.getDefinition_FR(),
      entry.getNotes_FR(),
      entry.getExamples_FR(),
      entry.getSearchTags_FR(),
    };
  }

  private Object[] convertEntryToCursorRow(KlingonContentProvider.Entry entry, boolean indent) {
    return new Object[] {
      entry.getId(),
      entry.getEntryName(),
      entry.getPartOfSpeech() + (indent ? ",indent" : ""),
      entry.getDefinition(),
      entry.getSynonyms(),
      entry.getAntonyms(),
      entry.getSeeAlso(),
      entry.getNotes(),
      entry.getHiddenNotes(),
      entry.getComponents(),
      entry.getExamples(),
      entry.getSearchTags(),
      entry.getSource(),
      entry.getDefinition_DE(),
      entry.getNotes_DE(),
      entry.getExamples_DE(),
      entry.getSearchTags_DE(),
      entry.getDefinition_FA(),
      entry.getNotes_FA(),
      entry.getExamples_FA(),
      entry.getSearchTags_FA(),
      entry.getDefinition_SV(),
      entry.getNotes_SV(),
      entry.getExamples_SV(),
      entry.getSearchTags_SV(),
      entry.getDefinition_RU(),
      entry.getNotes_RU(),
      entry.getExamples_RU(),
      entry.getSearchTags_RU(),
      entry.getDefinition_ZH_HK(),
      entry.getNotes_ZH_HK(),
      entry.getExamples_ZH_HK(),
      entry.getSearchTags_ZH_HK(),
      entry.getDefinition_PT(),
      entry.getNotes_PT(),
      entry.getExamples_PT(),
      entry.getSearchTags_PT(),
      entry.getDefinition_FI(),
      entry.getNotes_FI(),
      entry.getExamples_FI(),
      entry.getSearchTags_FI(),
      entry.getDefinition_FR(),
      entry.getNotes_FR(),
      entry.getExamples_FR(),
      entry.getSearchTags_FR(),
    };
  }

  /**
   * Returns a cursor for one entry given its _id.
   *
   * @param entryId The ID of the entry to search for
   * @param columns The columns to include, if null then all are included
   * @return Cursor over all entries that match, or null if none found.
   */
  public Cursor getEntryById(String entryId, String[] columns) {
    // Log.d(TAG, "getEntryById called with entryid: " + entryId);
    Cursor cursor =
        mDatabaseOpenHelper
            .getReadableDatabase()
            .query(
                true,
                FTS_VIRTUAL_TABLE,
                columns,
                KlingonContentDatabase.KEY_ID + "=" + entryId + "",
                null,
                null,
                null,
                null,
                null);
    if (cursor != null) {
      cursor.moveToFirst();
    }
    // Log.d(TAG, "cursor.getCount() = " + cursor.getCount());
    return cursor;
  }

  /** Returns a cursor containing a random entry. */
  public Cursor getRandomEntry(String[] columns) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    int onePastLastId =
        sharedPrefs.getInt(KEY_ID_OF_FIRST_EXTRA_ENTRY, /* default */ ID_OF_FIRST_EXTRA_ENTRY);
    int randomId = new Random().nextInt(onePastLastId - ID_OF_FIRST_ENTRY) + ID_OF_FIRST_ENTRY;
    return getEntryById(Integer.toString(randomId), ALL_KEYS);
  }

  /**
   * Performs a database query.
   *
   * @param selection The selection clause
   * @param selectionArgs Selection arguments for "?" components in the selection
   * @param columns The columns to return
   * @return A Cursor over all rows matching the query
   */
  private Cursor query(String selection, String[] selectionArgs, String[] columns) {
    /*
     * The SQLiteBuilder provides a map for all possible columns requested to actual columns in the
     * database, creating a simple column alias mechanism by which the ContentProvider does not need
     * to know the real column names
     */
    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
    builder.setTables(FTS_VIRTUAL_TABLE);
    builder.setProjectionMap(mColumnMap);

    // DEBUG
    // Log.d(TAG, "query - columns: " + Arrays.toString(columns));
    // Log.d(TAG, "query - selection: " + selection);
    // Log.d(TAG, "query - selectionArgs: " + Arrays.toString(selectionArgs));

    Cursor cursor =
        builder.query(
            mDatabaseOpenHelper.getReadableDatabase(),
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null);

    // DEBUG
    // Log.d(TAG, "query - cursor: " + cursor.toString());

    if (cursor == null) {
      return null;
    } else if (!cursor.moveToFirst()) {

      cursor.close();
      return null;
    }
    return cursor;
  }

  public static String getBundledDatabaseVersion() {
    return dottedVersion(BUNDLED_DATABASE_VERSION);
  }

  private static String dottedVersion(int version) {
    String s = Integer.toString(version);
    return s.substring(0, 4)
        + "."
        + s.substring(4, 6)
        + "."
        + s.substring(6, 8)
        + Character.toString((char) (s.charAt(8) - '0' + 'a'));
  }

  /** This class helps create, open, and upgrade the Klingon database. */
  private static class KlingonDatabaseOpenHelper extends SQLiteOpenHelper {

    // For storing the context the helper was called with for use.
    private final Context mHelperContext;

    // The Klingon database.
    private SQLiteDatabase mDatabase;

    /**
     * Constructor Takes and keeps a reference of the passed context in order to access the
     * application assets and resources.
     *
     * @param context
     */
    KlingonDatabaseOpenHelper(Context context) {
      super(context, DATABASE_NAME, null, BUNDLED_DATABASE_VERSION);
      mHelperContext = context;
    }

    // The system path of the Klingon database.
    private String getDatabasePath(String name) {
      return mHelperContext.getDatabasePath(name).getAbsolutePath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      // This method is called when the database is created for the first
      // time. It would normally create the database using an SQL
      // command, then load the content. We do nothing here, and leave
      // the work of copying the pre-made database to the constructor of
      // the KlingonContentDatabase class.
      // Log.d(TAG, "onCreate called.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int existingBundledVersion, int newBundledVersion) {
      if (newBundledVersion <= existingBundledVersion) {
        // Bundled version hasn't changed, do nothing.
        return;
      }

      // Note that if the previous version of the app was bundled with database version A, and an
      // updated database version B has been downloaded (but not installed), and this is the first
      // run of a new version bundled with database version C , the installedVersion should
      // default to A (not C).
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mHelperContext);
      String installedVersion =
          sharedPrefs.getString(
              KEY_INSTALLED_DATABASE_VERSION, /* default */ dottedVersion(existingBundledVersion));
      String updatedVersion =
          sharedPrefs.getString(KEY_UPDATED_DATABASE_VERSION, /* default */ installedVersion);
      if (updatedVersion.compareToIgnoreCase(dottedVersion(newBundledVersion)) >= 0) {
        // Either a new database is already installed, or is about to be, so do nothing.
        return;
      }

      // The database needs to be updated from the bundled database, so clear any existing
      // databases.
      mHelperContext.deleteDatabase(DATABASE_NAME);
      mHelperContext.deleteDatabase(REPLACEMENT_DATABASE_NAME);

      // Reset to bundled database version.
      SharedPreferences.Editor sharedPrefsEd =
          PreferenceManager.getDefaultSharedPreferences(mHelperContext).edit();
      sharedPrefsEd.remove(KEY_INSTALLED_DATABASE_VERSION);
      sharedPrefsEd.remove(KEY_ID_OF_FIRST_EXTRA_ENTRY);
      sharedPrefsEd.remove(KEY_UPDATED_DATABASE_VERSION);
      sharedPrefsEd.remove(KEY_UPDATED_ID_OF_FIRST_EXTRA_ENTRY);
      sharedPrefsEd.apply();

      Toast.makeText(
              mHelperContext,
              String.format(
                  mHelperContext.getResources().getString(R.string.database_upgraded),
                  installedVersion,
                  dottedVersion(newBundledVersion)),
              Toast.LENGTH_LONG)
          .show();
      mNewDatabaseMessageDisplayed = true;

      // Show help after database upgrade.
      setShowHelpFlag();
    }

    private void setShowHelpFlag() {
      // Set the flag to show the help screen (but not necessarily the tutorial).
      SharedPreferences.Editor sharedPrefsEd =
          PreferenceManager.getDefaultSharedPreferences(mHelperContext).edit();
      sharedPrefsEd.putBoolean(KlingonAssistant.KEY_SHOW_HELP, true);
      sharedPrefsEd.apply();
      // Log.d(TAG, "Flag set to show help.");
    }

    /**
     * Initialises the database by creating an empty database and writing to it from application
     * resource.
     */
    public void initDatabase() throws IOException {
      // TODO: Besides checking whether it exists, also check if its data needs to be updated.
      // This may not be necessary due to onUpgrade(...) above.
      if (checkDBExists(DATABASE_NAME)) {
        // Log.d(TAG, "Database exists.");
        // Get a writeable database so that onUpgrade will be called on it if the version number has
        // increased. This will delete the existing datbase.
        try {
          // Log.d(TAG, "Getting writable database.");
          SQLiteDatabase writeDB = this.getWritableDatabase();
          writeDB.close();
        } catch (SQLiteDiskIOException e) {
          // TODO: Log error or do something here and below.
          // Log.e(TAG, "SQLiteDiskIOException on getWritableDatabase().");
        } catch (SQLiteException e) {
          // Possibly unable to get provider because no transaction is active.
          // Do nothing.
        }
      }

      // Update the database if that's available.
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mHelperContext);
      String installedVersion =
          sharedPrefs.getString(
              KEY_INSTALLED_DATABASE_VERSION, /* default */ getBundledDatabaseVersion());
      String updatedVersion =
          sharedPrefs.getString(KEY_UPDATED_DATABASE_VERSION, /* default */ installedVersion);
      if (updatedVersion.compareToIgnoreCase(installedVersion) > 0
          && checkDBExists(REPLACEMENT_DATABASE_NAME)) {
        copyDBFromReplacement();

        int firstExtraEntryId =
            sharedPrefs.getInt(
                KEY_UPDATED_ID_OF_FIRST_EXTRA_ENTRY, /* default */ ID_OF_FIRST_EXTRA_ENTRY);
        SharedPreferences.Editor sharedPrefsEd =
            PreferenceManager.getDefaultSharedPreferences(mHelperContext).edit();
        sharedPrefsEd.putString(KEY_INSTALLED_DATABASE_VERSION, updatedVersion);
        sharedPrefsEd.putInt(KEY_ID_OF_FIRST_EXTRA_ENTRY, firstExtraEntryId);
        sharedPrefsEd.remove(KEY_UPDATED_DATABASE_VERSION);
        sharedPrefsEd.remove(KEY_UPDATED_ID_OF_FIRST_EXTRA_ENTRY);
        sharedPrefsEd.apply();

        Toast.makeText(
                mHelperContext,
                String.format(
                    mHelperContext.getResources().getString(R.string.database_upgraded),
                    installedVersion,
                    updatedVersion),
                Toast.LENGTH_LONG)
            .show();
        mNewDatabaseMessageDisplayed = true;

        // Show help after database upgrade.
        setShowHelpFlag();
      }

      // Create the database from included bundle if it doesn't exist.
      if (!checkDBExists(DATABASE_NAME)) {
        // This will create the empty database if it doesn't already exist.
        // Log.d(TAG, "Getting readable database.");
        SQLiteDatabase readDB = this.getReadableDatabase();
        readDB.close();

        // Try to create the database from the bundled database file.
        try {
          // Log.d(TAG, "Copying database from resources.");
          copyDBFromResources();
        } catch (IOException e) {
          throw new Error("Error copying database from resources.");
        }

        // Inform the user the database has been created.
        if (!mNewDatabaseMessageDisplayed) {
          Toast.makeText(
                  mHelperContext,
                  String.format(
                      mHelperContext.getResources().getString(R.string.database_created),
                      getBundledDatabaseVersion()),
                  Toast.LENGTH_LONG)
              .show();
          mNewDatabaseMessageDisplayed = true;
        }

        // Show help after database creation.
        setShowHelpFlag();
      }
    }

    /**
     * Check if the database already exists so that it isn't copied every time the activity is
     * started.
     *
     * @return true if the database exists, false otherwise
     */
    private boolean checkDBExists(String databaseName) {
      // The commented way below is the proper way of checking for the
      // existence of the database. However, we do it this way to
      // prevent the "sqlite3_open_v2 open failed" error.
      File dbFile = new File(getDatabasePath(databaseName));
      return dbFile.exists();

      // TODO: Investigate the below. It may be the reason why there
      // are problems on some devices.
      /*
       * SQLiteDatabase checkDB = null; try { String fullDBPath = getDatabasePath(DATABASE_NAME);
       * checkDB = SQLiteDatabase.openDatabase(fullDBPath, null, SQLiteDatabase.OPEN_READONLY);
       *
       * } catch(SQLiteCantOpenDatabaseException e) { // The database doesn't exist yet. It's fine
       * to do nothing // here, we just want to return false at the end. // Log.d(TAG,
       * "SQLiteCantOpenDatabaseException thrown: " + e);
       *
       * } catch(SQLiteDatabaseLockedException e) { // The database is locked. Also return false. //
       * Log.d(TAG, "SQLiteDatabaseLockedException thrown: " + e); }
       *
       * if( checkDB != null ) { checkDB.close(); }
       *
       * // Log.d(TAG, "checkDB == null: " + (checkDB == null)); return ( checkDB != null );
       */
    }

    /**
     * Copies the database from the application resources' assets folder to the newly created
     * database in the system folder.
     */
    private void copyDBFromResources() throws IOException {

      // Open the file in the assets folder as an input stream.
      InputStream inStream = mHelperContext.getAssets().open(DATABASE_NAME);

      // Path to the newly created empty database.
      String fullDBPath = getDatabasePath(DATABASE_NAME);

      // Open the empty database as the output stream.
      OutputStream outStream = new FileOutputStream(fullDBPath);

      // Transfer the database from the resources to the system path one block at a time.
      byte[] buffer = new byte[MAX_BUFFER_LENGTH];
      int length;
      while ((length = inStream.read(buffer)) > 0) {
        outStream.write(buffer, 0, length);
      }

      // Close the streams.
      outStream.flush();
      outStream.close();
      inStream.close();

      // Log.d(TAG, "Database copy successful.");
    }

    /** Copies the database from the replacement (update) database. */
    private void copyDBFromReplacement() throws IOException {
      String fullReplacementDBPath = getDatabasePath(REPLACEMENT_DATABASE_NAME);
      String fullDBPath = getDatabasePath(DATABASE_NAME);

      InputStream inStream = new FileInputStream(fullReplacementDBPath);
      OutputStream outStream = new FileOutputStream(fullDBPath);

      // Transfer the database from the resources to the system path one block at a time.
      byte[] buffer = new byte[MAX_BUFFER_LENGTH];
      int length;
      int total = 0;
      while ((length = inStream.read(buffer)) > 0) {
        outStream.write(buffer, 0, length);
        total += length;
      }
      Log.d(TAG, "Copied database from replacement, " + total + " bytes copied.");

      // Close the streams.
      outStream.flush();
      outStream.close();
      inStream.close();

      // Delete the replacement database.
      mHelperContext.deleteDatabase(REPLACEMENT_DATABASE_NAME);
    }

    /** Opens the database. */
    public void openDatabase() throws SQLException {
      String fullDBPath = getDatabasePath(DATABASE_NAME);
      // Log.d(TAG, "openDatabase() called on path " + fullDBPath + ".");
      mDatabase = SQLiteDatabase.openDatabase(fullDBPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    /** Closes the database. */
    @Override
    public synchronized void close() {
      // Log.d(TAG, "Closing database.");
      if (mDatabase != null) {
        mDatabase.close();
      }
      super.close();
    }
  } // KlingonDatabaseOpenHelper
} // KlingonContentDatabase
