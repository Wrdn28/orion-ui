/*
 * Copyright (C) 2024 the OrionOS Android Project
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

package com.android.settings.preferences.ui

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.widget.LayoutPreference

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class orionInfoPreferenceController(context: Context) : AbstractPreferenceController(context) {

    private val defaultFallback = mContext.getString(R.string.device_info_default)

    private fun getPropertyOrDefault(propName: String): String {
        return SystemProperties.get(propName, defaultFallback)
    }

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun getOrionBuildVersion(): String {
        return getPropertyOrDefault(ORION_BUILD_VERSION)
    }

    private fun getOrionChipset(): String {
        return getPropertyOrDefault(ORION_CHIPSET)
    }

    private fun getOrionBattery(): String {
        return getPropertyOrDefault(ORION_BATTERY)
    }
    
    private fun getOrionResolution(): String {
        return getPropertyOrDefault(ORION_DISPLAY)
    }

    private fun getOrionSecurity(): String {
        return getPropertyOrDefault(ORION_SECURITY)
    }

    private fun getOrionVersion(): String {
        return SystemProperties.get(ORION_VERSION)
    }

    private fun getOrionReleaseType(): String {
        val releaseType = getPropertyOrDefault(ORION_RELEASETYPE)
        return releaseType.substring(0, 1).uppercase() +
               releaseType.substring(1).lowercase()
    }

    private fun getOrionBuildStatus(releaseType: String): String {
        return mContext.getString(if (releaseType == "official") R.string.build_official_title else R.string.build_unofficial_title)
    }

    private fun getOrionMaintainer(releaseType: String): String {
        val orionMaintainer = getPropertyOrDefault(ORION_MAINTAINER)
        if (orionMaintainer.equals("Unknown", ignoreCase = true)) {
            return mContext.getString(R.string.unknown_maintainer)
        }
        return mContext.getString(R.string.maintainer_summary, orionMaintainer)
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val releaseType = getPropertyOrDefault(ORION_RELEASETYPE).lowercase()
        val orionMaintainer = getOrionMaintainer(releaseType)
        val isOfficial = releaseType == "official"
        
        val hwInfoPreference = screen.findPreference<LayoutPreference>(KEY_HW_INFO)!!
        val swInfoPreference = screen.findPreference<LayoutPreference>(KEY_DEVICE_INFO)!!
        val statusPreference = screen.findPreference<Preference>(KEY_BUILD_STATUS)!!
        val deviceText = swInfoPreference.findViewById<TextView>(R.id.device_name_model)
       // val editBtn = swInfoPreference.findViewById<TextView>(R.id.edit_device_name_model)*/

        statusPreference.setTitle(getOrionBuildStatus(releaseType))
        statusPreference.setSummary(orionMaintainer)
        statusPreference.setIcon(if (isOfficial) R.drawable.verified else R.drawable.unverified)

        val settingsDeviceName = Settings.Global.getString(
            mContext.contentResolver,
            Settings.Global.DEVICE_NAME
        )

        if (!settingsDeviceName.isNullOrBlank()) {
            deviceText.text = settingsDeviceName
        }

        //editBtn.setOnClickListener {
        //    showEditDialog(deviceText)
        //}

        hwInfoPreference.apply {
            findViewById<TextView>(R.id.device_chipset).text = getOrionChipset()
            findViewById<TextView>(R.id.device_storage).text = DeviceInfoUtil.getTotalRam() + " | " + DeviceInfoUtil.getStorageTotal(mContext)
            findViewById<TextView>(R.id.device_battery_capacity).text = getOrionBattery()
            findViewById<TextView>(R.id.device_resolution).text =  getOrionResolution()
        }
    }

    private fun showEditDialog(tv: TextView) {
        val layoutInflater = LayoutInflater.from(mContext)
        val promptView = layoutInflater.inflate(R.layout.edit_text_dialog, null)
        val editText = promptView.findViewById<EditText>(R.id.editText)
        val currentDeviceName = Settings.Global.getString(
            mContext.contentResolver,
            Settings.Global.DEVICE_NAME
        )
        editText!!.hint = currentDeviceName
        AlertDialog.Builder(mContext)
            .setTitle(R.string.edit_device_name_title)
            .setView(promptView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val deviceName = editText.text.toString().trim()
                Settings.Global.putString(
                    mContext.contentResolver,
                    Settings.Global.DEVICE_NAME,
                    deviceName
                )
                tv.text = deviceName
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String {
        return KEY_DEVICE_INFO
    }

    companion object {
        private const val KEY_HW_INFO = "my_device_hw_header"
        private const val KEY_DEVICE_INFO = "my_device_info_header"
        private const val KEY_BUILD_STATUS = "rom_build_status"

        private const val ORION_BUILD_TYPE = "ro.orion.build.variant"
        private const val ORION_VERSION = "ro.orion.modversion"
        private const val ORION_RELEASETYPE = "ro.orion.release.type"
        private const val ORION_MAINTAINER = "ro.orion.maintainer"
        private const val ORION_BUILD_VERSION = "ro.orion.version"
        private const val ORION_CHIPSET = "ro.orion.chipset"
        private const val ORION_DISPLAY = "ro.orion.display_resolution"
        private const val ORION_BATTERY = "ro.orion.battery"
        private const val ORION_SECURITY = "ro.build.version.security_patch"
    }
}
