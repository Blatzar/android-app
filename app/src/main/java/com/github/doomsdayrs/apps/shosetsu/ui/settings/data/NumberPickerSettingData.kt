package com.github.doomsdayrs.apps.shosetsu.ui.settings.data

import android.view.View
import android.widget.NumberPicker
import com.github.doomsdayrs.apps.shosetsu.ui.settings.data.base.SettingsItemData
import com.github.doomsdayrs.apps.shosetsu.ui.settings.viewHolder.SettingsItem

/*
 * This file is part of shosetsu.
 *
 * shosetsu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shosetsu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shosetsu.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * shosetsu
 * 25 / 06 / 2020
 */
class NumberPickerSettingData(id: Int) : SettingsItemData(id) {
	var lowerBound = 0
	var upperBound = 0
	var numberPickerValue: Int = 0
	var numberPickerOnValueChangedListener: (
			picker: NumberPicker?,
			oldVal: Int,
			newVal: Int
	) -> Unit = { _, _, _ -> }

	override fun setupView(settingsItem: SettingsItem) {
		with(settingsItem) {
			numberPicker.visibility = View.VISIBLE
			numberPicker.minValue = lowerBound
			numberPicker.maxValue = upperBound
			numberPicker.value = numberPickerValue
			numberPicker.setOnValueChangedListener(numberPickerOnValueChangedListener)
		}
	}
}