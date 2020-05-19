package com.github.doomsdayrs.apps.shosetsu.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.github.doomsdayrs.apps.shosetsu.common.dto.HResult
import com.github.doomsdayrs.apps.shosetsu.common.dto.loading
import com.github.doomsdayrs.apps.shosetsu.common.ext.launchIO
import com.github.doomsdayrs.apps.shosetsu.common.ext.logID
import com.github.doomsdayrs.apps.shosetsu.domain.usecases.BookMarkNovelIDUseCase
import com.github.doomsdayrs.apps.shosetsu.domain.usecases.GetFormatterNameUseCase
import com.github.doomsdayrs.apps.shosetsu.domain.usecases.GetNovelUIUseCase
import com.github.doomsdayrs.apps.shosetsu.domain.usecases.LoadNovelUseCase
import com.github.doomsdayrs.apps.shosetsu.view.uimodels.NovelUI
import com.github.doomsdayrs.apps.shosetsu.viewmodel.base.INovelInfoViewModel
import kotlinx.coroutines.Dispatchers

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
 * 24 / 04 / 2020
 *
 * @author github.com/doomsdayrs
 */
class NovelInfoViewModel(
		private val getFormatterNameUseCase: GetFormatterNameUseCase,
		private val bookMarkNovelIDUseCase: BookMarkNovelIDUseCase,
		private val loadNovelUIUseCase: GetNovelUIUseCase,
		private val loadNovelUseCase: LoadNovelUseCase

) : INovelInfoViewModel() {
	override val liveData: LiveData<HResult<NovelUI>> by lazy {
		novelID.switchMap {
			liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
				emit(loading())
				emitSource(loadNovelUIUseCase(it))
			}
		}
	}

	private val novelID by lazy { MutableLiveData<Int>() }

	override val formatterName: LiveData<HResult<String>> by lazy {
		novelID.switchMap {
			liveData<HResult<String>>(viewModelScope.coroutineContext + Dispatchers.IO) {
				getFormatterNameUseCase(it)
			}
		}
	}

	override fun setNovelID(novelID: Int) {
		if (liveData.value !is HResult.Success) {
			this.novelID.postValue(novelID)
		}
	}

	override fun toggleBookmark(novelUI: NovelUI) {
		launchIO { bookMarkNovelIDUseCase(novelUI.id) }
	}

	override fun refresh() {
		val v = liveData.value
		if (v is HResult.Success)
			if (!v.data.loaded) {
				Log.d(logID(), "Novel is not loaded, loading")
				launchIO {
					loadNovelUseCase(v.data.id!!, true)
				}
			}
	}
}