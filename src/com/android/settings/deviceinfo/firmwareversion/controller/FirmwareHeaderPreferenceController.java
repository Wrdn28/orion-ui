/*
 * Copyright (C) 2023 ArrowOS-T
 * Copyright (C) 2024 OrionOS
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

package com.android.settings.deviceinfo.firmwareversion.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.widget.LayoutPreference;

public class FirmwareHeaderPreferenceController extends BasePreferenceController implements View.OnClickListener {

    private static final String TAG = "firmwareDialogCtrl";
    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    private static final String KEY_DEVICE_MAINTAINER_PROP = "ro.orion.maintainer";
    private static final String KEY_DEVICE_MAINTAINER_LINK_PROP = "ro.orion.maintainer_link";

    private static final Uri ROM_URI_DATA = Uri.parse("https://OrionOS-prjkt.vercel.app");
    private static final Uri MT_URI_DATA = Uri.parse(SystemProperties.get(KEY_DEVICE_MAINTAINER_LINK_PROP));

    private final UserManager mUserManager;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;

    private LayoutPreference mPreference;
    private LinearLayout firmwareArea, androidVersionArea, maintainerArea;
    private TextView androidVersionSummary, maintainerSummary;

    public FirmwareHeaderPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        initializeAdminPermissions();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private CharSequence getAndroidVersionSummary() {
        return Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        firmwareArea = mPreference.findViewById(mContext.getResources().getIdentifier("id/firmware_area", null, mContext.getPackageName()));
        androidVersionArea = mPreference.findViewById(mContext.getResources().getIdentifier("id/android_version_area", null, mContext.getPackageName()));
        maintainerArea = mPreference.findViewById(mContext.getResources().getIdentifier("id/maintainer_area", null, mContext.getPackageName()));
        androidVersionSummary = mPreference.findViewById(mContext.getResources().getIdentifier("id/android_version_summary", null, mContext.getPackageName()));
        maintainerSummary = mPreference.findViewById(mContext.getResources().getIdentifier("id/maintainer_summary", null, mContext.getPackageName()));

        androidVersionSummary.setText(getAndroidVersionSummary() + " (U)");
        maintainerSummary.setText(SystemProperties.get(KEY_DEVICE_MAINTAINER_PROP));

        firmwareArea.setOnClickListener(this);
        androidVersionArea.setOnClickListener(this);
        maintainerArea.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == firmwareArea) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(ROM_URI_DATA);
            mContext.startActivity(intent);
        } else if (view == androidVersionArea) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            arrayCopy();
            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
                if (mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                    if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                                mFunDisallowedAdmin);
                    }
                    Log.d(TAG, "Sorry, no fun for you!");
                    return;
                }

                final Intent intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(
                                "android", com.android.internal.app.PlatLogoActivity.class.getName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (view == maintainerArea) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(MT_URI_DATA);
            mContext.startActivity(intent);
        }
    }

    /**
     * Copies the array onto itself to remove the oldest hit.
     */
    @VisibleForTesting
    void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }

    @VisibleForTesting
    void initializeAdminPermissions() {
        mFunDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
    }
}
