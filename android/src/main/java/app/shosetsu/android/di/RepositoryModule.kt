package app.shosetsu.android.di

import app.shosetsu.android.domain.repository.impl.AppUpdatesRepository
import app.shosetsu.android.domain.repository.impl.ExtensionLibrariesRepository
import app.shosetsu.common.domain.repositories.base.*
import app.shosetsu.common.domain.repositories.impl.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

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
 * 25 / 04 / 2020
 *
 * @author github.com/doomsdayrs
 */

val repositoryModule: Kodein.Module = Kodein.Module("repository_module") {
	bind<IChaptersRepository>() with singleton {
		ChaptersRepository(instance(), instance(), instance(), instance(), instance())
	}

	bind<IDownloadsRepository>() with singleton { DownloadsRepository(instance()) }

	bind<IExtensionsRepository>() with singleton {
		ExtensionsRepository(instance(), instance(), instance(), instance(), instance())
	}

	bind<IExtensionLibrariesRepository>() with singleton {
		ExtensionLibrariesRepository(instance(), instance(), instance(), instance())
	}

	bind<IExtensionRepoRepository>() with singleton { ExtRepoRepository(instance(), instance()) }

	bind<INovelsRepository>() with singleton {
		NovelsRepository(
			instance(),
			instance(),
			instance()
		)
	}

	bind<IUpdatesRepository>() with singleton { UpdatesRepository(instance()) }

	bind<IAppUpdatesRepository>() with singleton { AppUpdatesRepository(instance(), instance()) }

	bind<ISettingsRepository>() with singleton { SettingsRepository(instance()) }

	bind<IBackupRepository>() with singleton { BackupRepository(instance()) }

	bind<INovelSettingsRepository>() with singleton { TempNovelSettingsRepository() }
}