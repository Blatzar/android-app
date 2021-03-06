package app.shosetsu.android.ui.updates

/*
 * This file is part of Shosetsu.
 *
 * Shosetsu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shosetsu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shosetsu.  If not, see <https://www.gnu.org/licenses/>.
 */

import app.shosetsu.android.common.ext.*
import app.shosetsu.android.view.controller.FastAdapterRefreshableRecyclerController
import app.shosetsu.android.view.controller.base.CollapsedToolBarController
import app.shosetsu.android.view.decoration.StickyHeaderDecor
import app.shosetsu.android.view.uimodels.model.UpdateUI
import app.shosetsu.android.viewmodel.abstracted.IUpdatesViewModel
import app.shosetsu.common.dto.HResult
import com.github.doomsdayrs.apps.shosetsu.R
import com.mikepenz.fastadapter.FastAdapter
import org.joda.time.DateTime

/**
 * shosetsu
 * 15 / 07 / 2019
 *
 * @author github.com/doomsdayrs
 */
class UpdatesController : FastAdapterRefreshableRecyclerController<UpdateUI>(),
	CollapsedToolBarController {

	val viewModel: IUpdatesViewModel by viewModel()

	override val viewTitleRes: Int = R.string.updates

	override fun setupRecyclerView() {
		super.setupRecyclerView()
		recyclerView.apply {
			setPadding(0, 0, 0, 8)
			addItemDecoration(StickyHeaderDecor(recyclerView.context, UpdateCallback()))
		}
	}

	override fun FastAdapter<UpdateUI>.setupFastAdapter() {
		setOnClickListener { _, _, (chapterID, novelID), _ ->
			activity?.openChapter(chapterID, novelID)
			true
		}
		startObservation()
	}

	private fun startObservation() = viewModel.liveData.observeRecyclerUpdates()

	override fun handleErrorResult(e: HResult.Error) {
		super.handleErrorResult(e)
		viewModel.reportError(e)
	}

	private inner class UpdateCallback : StickyHeaderDecor.SectionCallback {
		override fun isSection(pos: Int): Boolean {
			if (pos == 0) return true
			fastAdapter.getItem(pos)?.let { currentItem ->
				fastAdapter.getItem(pos - 1)?.let { previousItem ->
					val currentDate = DateTime(currentItem.time).trimDate()
					val previousDate = DateTime(previousItem.time).trimDate()
					return currentDate.plusDays(1) == previousDate
				}
			}
			return false
		}

		override fun getSectionHeaderName(pos: Int): String {
			fastAdapter.getItem(pos)?.let {
				return when (val dateTime = DateTime(it.time).trimDate()) {
					DateTime(System.currentTimeMillis()).trimDate() ->
						context!!.getString(R.string.today)
					DateTime(System.currentTimeMillis()).trimDate().minusDays(1) ->
						context!!.getString(R.string.yesterday)
					else -> "${dateTime.dayOfMonth}/${dateTime.monthOfYear}/${dateTime.year}"
				}
			}
			return "No Bogga"
		}
	}

	override fun onRefresh() {
		if (viewModel.isOnline())
			viewModel.startUpdateManager()
		else toast(R.string.you_not_online)
	}
}