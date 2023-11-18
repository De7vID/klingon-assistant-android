/*
 * Copyright (C) 2017 De'vID jonpIn (David Yonge-Mallo)
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

package org.tlhInganHol.android.klingonassistant.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;
import org.tlhInganHol.android.klingonassistant.KlingonContentDatabase;

public class UpdateDatabaseService extends JobService {
  private static final String TAG = "UpdateDatabaseService";

  // Save the parameters of the job.
  private JobParameters mParams = null;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "UpdateDatabaseService created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "UpdateDatabaseService destroyed");
  }

  @Override
  public boolean onStartJob(final JobParameters params) {
    mParams = params;

    // Start an async task to fetch the KWOTD.
    new UpdateDatabaseTask().execute();

    Log.d(TAG, "on start job: " + params.getJobId());

    // Return true to hold the wake lock. This is released by the async task.
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    Log.d(TAG, "on stop job: " + params.getJobId());

    // Return false to drop the job.
    return false;
  }

  private class UpdateDatabaseTask extends AsyncTask<Void, Void, Void> {
    // Online database upgrade URL.
    private static final String ONLINE_UPGRADE_PATH = "https://De7vID.github.io/qawHaq/";
    private static final String MANIFEST_JSON_URL = ONLINE_UPGRADE_PATH + "manifest.json";

    // Arbitrary limit on max buffer length to prevent overflows and such.
    private static final int MAX_BUFFER_LENGTH = 1024;

    @Override
    protected Void doInBackground(Void... params) {
      Resources resources = UpdateDatabaseService.this.getResources();
      SharedPreferences sharedPrefs =
          PreferenceManager.getDefaultSharedPreferences(UpdateDatabaseService.this);

      // Set to false if job runs successfully to completion.
      boolean rescheduleJob = true;

      try (BufferedReader bufferedReader =
          new BufferedReader(
              new InputStreamReader(
                  new URL(MANIFEST_JSON_URL).openConnection().getInputStream())); ) {
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = bufferedReader.readLine()) != null && sb.length() < MAX_BUFFER_LENGTH) {
          sb.append(line);
          sb.append('\n');
        }
        String data = sb.toString();
        JSONObject manifestObject = new JSONObject(data);
        JSONObject androidObject = manifestObject.getJSONObject("Android-5");
        String latest = androidObject.getString("latest");
        Log.d(TAG, "Latest database version: " + latest);

        String installedVersion =
            sharedPrefs.getString(
                KlingonContentDatabase.KEY_INSTALLED_DATABASE_VERSION,
                /* default */ KlingonContentDatabase.getBundledDatabaseVersion());
        String updatedVersion =
            sharedPrefs.getString(
                KlingonContentDatabase.KEY_UPDATED_DATABASE_VERSION,
                /* default */ installedVersion);

        // Only download the database if the latest version is lexicographically greater than the
        // installed one and it hasn't already been downloaded.
        if (latest.compareToIgnoreCase(updatedVersion) > 0) {
          JSONObject latestObject = androidObject.getJSONObject(latest);

          // Get the metadata for the latest database for Android.
          String databaseZipUrl = ONLINE_UPGRADE_PATH + latestObject.getString("path");
          int firstExtraEntryId = latestObject.getInt("extra");
          Log.d(TAG, "Database zip URL: " + databaseZipUrl);
          Log.d(TAG, "Id of first extra entry: " + Integer.toString(firstExtraEntryId));
          copyDBFromZipUrl(databaseZipUrl);

          // Save the new version and first extra entry ID.
          SharedPreferences.Editor sharedPrefsEd =
              PreferenceManager.getDefaultSharedPreferences(UpdateDatabaseService.this).edit();
          sharedPrefsEd.putString(KlingonContentDatabase.KEY_UPDATED_DATABASE_VERSION, latest);
          sharedPrefsEd.putInt(
              KlingonContentDatabase.KEY_UPDATED_ID_OF_FIRST_EXTRA_ENTRY, firstExtraEntryId);
          sharedPrefsEd.apply();
        }

        // Success, so no need to reschedule.
        rescheduleJob = false;

      } catch (Exception e) {
        Log.e(TAG, "Failed to update database from server.", e);
      } finally {
        // Release the wakelock, and indicate whether rescheduling the job is needed.
        Log.d(TAG, "jobFinished called with rescheduleJob: " + rescheduleJob);

        UpdateDatabaseService.this.jobFinished(mParams, rescheduleJob);
      }

      return null;
    }

    private void copyDBFromZipUrl(String databaseZipUrl) throws IOException {
      // Read the database from a zip file online.
      URLConnection urlConnection = new URL(databaseZipUrl).openConnection();
      urlConnection.setRequestProperty("Accept-Encoding", "gzip");
      ZipInputStream inStream;
      if ("gzip".equals(urlConnection.getContentEncoding())) {
        inStream = new ZipInputStream(new GZIPInputStream(urlConnection.getInputStream()));
      } else {
        inStream = new ZipInputStream(urlConnection.getInputStream());
      }

      // Write to the replacement database.
      String fullReplacementDBPath =
          UpdateDatabaseService.this
              .getDatabasePath(KlingonContentDatabase.REPLACEMENT_DATABASE_NAME)
              .getAbsolutePath();
      Log.d(TAG, "fullReplacementDBPath: " + fullReplacementDBPath);
      OutputStream outStream = new FileOutputStream(fullReplacementDBPath);

      // Transfer the database from the resources to the system path one block at a time.
      byte[] buffer = new byte[MAX_BUFFER_LENGTH];
      int length;
      int total = 0;
      inStream.getNextEntry();
      while ((length = inStream.read(buffer)) > 0) {
        outStream.write(buffer, 0, length);
        total += length;
      }
      Log.d(TAG, "Copied database from " + databaseZipUrl + ", " + total + " bytes written.");

      // Close the streams.
      outStream.flush();
      outStream.close();
      inStream.closeEntry();
      inStream.close();
    }
  }
}
