package com.github.doomsdayrs.apps.shosetsu.backend.database

import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import app.shosetsu.lib.Novel
import app.shosetsu.lib.Novel.Chapter
import com.github.doomsdayrs.apps.shosetsu.backend.database.Columns.*
import com.github.doomsdayrs.apps.shosetsu.backend.database.Database.DatabaseIdentification.getChapterIDFromChapterURL
import com.github.doomsdayrs.apps.shosetsu.backend.database.Database.DatabaseIdentification.getChapterURLFromChapterID
import com.github.doomsdayrs.apps.shosetsu.backend.database.Database.DatabaseIdentification.getNovelIDFromNovelURL
import com.github.doomsdayrs.apps.shosetsu.backend.database.Tables.*
import com.github.doomsdayrs.apps.shosetsu.variables.enums.ReadingStatus
import com.github.doomsdayrs.apps.shosetsu.variables.ext.*
import com.github.doomsdayrs.apps.shosetsu.variables.recycleObjects.NovelCard
import java.util.*

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
 * ====================================================================
 */
/**
 * Shosetsu
 * 9 / June / 2019
 *
 * @author github.com/doomsdayrs
 */
object Database {

	/**
	 * SQLITEDatabase
	 */
	lateinit var sqLiteDatabase: SQLiteDatabase

	fun isInit(): Boolean {
		return this::sqLiteDatabase.isInitialized
	}

	@Throws(MissingResourceException::class)
	fun getDatabase(): SQLiteDatabase {
		if (!isInit())
			throw MissingResourceException("Missing Database", SQLiteDatabase::javaClass.name, "")
		return sqLiteDatabase
	}

	@Deprecated("ROOM",level = DeprecationLevel.WARNING)
	object DatabaseIdentification {
		/**
		 * Gets rid of novel completely
		 *
		 * @param novelID ID of novel to destroy
		 */
		@Throws(SQLException::class)
		private fun purgeNovel(novelID: Int) {
			getDatabase().delete(
					"$NOVEL_IDENTIFICATION",
					"$ID=?",
					arrayOf("$novelID")
			)
			getDatabase().delete(
					"$NOVELS",
					"$PARENT_ID=?",
					arrayOf("$novelID")
			)
			purgeChaptersOf(novelID)
		}

		/**
		 * Gets rid of chapters of novel. To fix issues
		 *
		 * @param novelID ID of novel
		 */
		@Throws(SQLException::class)
		private fun purgeChaptersOf(novelID: Int) {
			// Deletes chapters FROM identification
			getDatabase().delete(
					"$CHAPTER_IDENTIFICATION",
					"$PARENT_ID=?",
					arrayOf("$novelID")
			)
			// Removes all chapters FROM chapters DB
			getDatabase().delete(
					"$CHAPTERS",
					"$PARENT_ID=?",
					arrayOf("$novelID")
			)

			// Removes chapters FROM updates
			updatesDao.deleteUpdateByNovelID(novelID)
		}

		/**
		 * Finds and deletes all novels that are unbookmarked
		 */
		@Throws(SQLException::class)
		fun purgeUnSavedNovels() {
			val cursor = getDatabase().query(
					NOVELS,
					stringArrayOf(PARENT_ID),
					"${BOOKMARKED}=0",
					null
			)
			while (cursor.moveToNext()) {
				val i = cursor.getInt(PARENT_ID)
				Log.d(logID(), "Removing novel $i")
				purgeNovel(i)
			}
			cursor.close()
		}

		@Throws(MissingResourceException::class)
		fun hasChapter(chapterURL: String): Boolean {
			val cursor = getDatabase().query(
					CHAPTER_IDENTIFICATION,
					arrayOf(),
					"$URL=?",
					arrayOf(chapterURL)
			)
			val count = cursor.count
			cursor.close()
			return count > 0
		}

		@Throws(MissingResourceException::class)
		fun addChapter(novelID: Int, chapterURL: String) {
			val v = ContentValues()
			v.put(PARENT_ID, novelID)
			v.put(URL, chapterURL)
			getDatabase().insert(CHAPTER_IDENTIFICATION, v)
		}

		@Throws(MissingResourceException::class)
		fun addNovel(novelURL: String, formatter: Int) {
			val v = ContentValues()
			v.put(URL, novelURL)
			v.put(FORMATTER_ID, formatter)
			getDatabase().insert(NOVEL_IDENTIFICATION, v)
		}

		/**
		 * @param url NovelURL
		 * @return NovelID
		 */
		@Throws(MissingResourceException::class)
		fun getNovelIDFromNovelURL(url: String): Int {
			val cursor = getDatabase().query(
					NOVEL_IDENTIFICATION,
					stringArrayOf(ID),
					"$URL=?",
					arrayOf(url)
			)

			return if (cursor.count <= 0) {
				cursor.close()
				-1
			} else {
				cursor.moveToNext()
				val id = cursor.getInt(ID)
				cursor.close()
				id
			}
		}

		/**
		 * @param url ChapterURL
		 * @return ChapterID
		 */
		@Throws(MissingResourceException::class)
		fun getChapterIDFromChapterURL(url: String): Int {
			val cursor = getDatabase().query(
					CHAPTER_IDENTIFICATION,
					stringArrayOf(ID),
					"$URL=?",
					arrayOf(url)
			)
			return if (cursor.count <= 0) {
				cursor.close()
				0
			} else {
				cursor.moveToNext()
				val id = cursor.getInt(ID)
				cursor.close()
				id
			}
		}

		/**
		 * @param id ChapterID
		 * @return ChapterURL
		 */
		@Throws(MissingResourceException::class)
		fun getChapterURLFromChapterID(id: Int): String {
			val cursor = getDatabase().query(
					CHAPTER_IDENTIFICATION,
					stringArrayOf(URL),
					"$ID=?",
					arrayOf("$id")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				""
			} else {
				cursor.moveToNext()
				val url = cursor.getString(URL)
				cursor.close()
				return url
			}
		}

		/**
		 * @param id ChapterID
		 * @return NovelID
		 */
		@Throws(MissingResourceException::class)
		fun getNovelIDFromChapterID(id: Int): Int {
			val cursor = getDatabase().query(
					CHAPTER_IDENTIFICATION,
					stringArrayOf(PARENT_ID),
					"$ID=?",
					arrayOf("$id")
			)

			return if (cursor.count <= 0) {
				cursor.close()
				0
			} else {
				cursor.moveToNext()
				val parent = cursor.getInt(PARENT_ID)
				cursor.close()
				parent
			}
		}

		/**
		 * @param id Chapter ID
		 * @return Chapter URL
		 */
		@Throws(MissingResourceException::class)
		private fun getNovelURLFromChapterID(id: Int): String? {
			return getNovelURLFromNovelID(getNovelIDFromChapterID(id))
		}

		/**
		 * @param url Chapter url
		 * @return Novel URL
		 */
		@Throws(MissingResourceException::class)
		fun getNovelURLFromChapterURL(url: String): String? {
			return getNovelURLFromChapterID(getChapterIDFromChapterURL(url))
		}

		/**
		 * @param id NovelID
		 * @return NovelURL
		 */
		@Throws(MissingResourceException::class)
		fun getNovelURLFromNovelID(id: Int): String? {
			val cursor = getDatabase().query(
					NOVEL_IDENTIFICATION,
					stringArrayOf(URL),
					"$ID=?",
					arrayOf("$id")
			)
			if (cursor.count <= 0) {
				cursor.close()
			} else {
				cursor.moveToNext()
				val url = cursor.getString(URL)
				cursor.close()
				return url
			}
			return null
		}

		/**
		 * Returns Formatter ID via Novel ID
		 *
		 * @param novelID Novel ID
		 * @return Formatter ID
		 */
		@Throws(MissingResourceException::class)
		fun getFormatterIDFromNovelID(novelID: Int): Int {
			val cursor = getDatabase().query(
					NOVEL_IDENTIFICATION,
					stringArrayOf(FORMATTER_ID),
					"$ID=?",
					arrayOf("$novelID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				-1
			} else {
				cursor.moveToNext()
				val id = cursor.getInt(FORMATTER_ID)
				cursor.close()
				return id
			}

		}

		/**
		 * Returns Formatter ID via Novel URL
		 *
		 * @param url Novel URL
		 * @return Formatter ID
		 */
		@Throws(MissingResourceException::class)
		fun getFormatterIDFromNovelURL(url: String): Int {
			val cursor = getDatabase().query(
					NOVEL_IDENTIFICATION,
					stringArrayOf(FORMATTER_ID),
					"$URL=?",
					arrayOf(url)
			)
			return if (cursor.count <= 0) {
				cursor.close()
				-1
			} else {
				cursor.moveToNext()
				val id = cursor.getInt(FORMATTER_ID)
				cursor.close()
				return id
			}

		}

		/**
		 * Returns Formatter ID via ChapterID, this simply compacts a longer line of methods into one.
		 *
		 * @param id Chapter ID
		 * @return Formatter ID
		 */
		@Throws(MissingResourceException::class)
		fun getFormatterIDFromChapterID(id: Int): Int {
			return getFormatterIDFromNovelID(getNovelIDFromChapterID(id))
		}
	}

	@Deprecated("ROOM",level = DeprecationLevel.WARNING)
	object DatabaseChapter {
		/**
		 * @param novelID ID of novel
		 * @return Count of chapters left to read
		 */
		@Throws(MissingResourceException::class)
		fun getCountOfChaptersUnread(novelID: Int): Int {
			val cursor = getDatabase().query(
					CHAPTERS,
					arrayOf(),
					"$PARENT_ID =? AND $READ_CHAPTER !=?",
					arrayOf("$novelID", "${ReadingStatus.READ}")
			)

			val count = cursor.count
			cursor.close()
			return count
		}

		/**
		 * Updates the Y coordinate
		 * Precondition is the chapter is already in the database.
		 *
		 * @param chapterID ID to update
		 * @param y         integer value scroll
		 */
		@Throws(SQLException::class)
		fun updateY(chapterID: Int, y: Int) {
			val v = ContentValues()
			v.put(Y_POSITION, y)
			getDatabase().update(CHAPTERS, v, "$ID=?", arrayOf("$chapterID"))
		}

		/**
		 * Precondition is the chapter is already in the database
		 *
		 * @param chapterID chapterID to the chapter
		 * @return order of chapter
		 */
		@Throws(MissingResourceException::class)
		fun getY(chapterID: Int): Int {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(Y_POSITION),
					"${ID}=?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				0
			} else {
				cursor.moveToNext()
				val y = cursor.getInt(Y_POSITION)
				cursor.close()
				y
			}
		}

		/**
		 * @param chapterID chapter to check
		 * @return returns chapter status
		 */
		@Throws(MissingResourceException::class)
		fun getChapterStatus(chapterID: Int): ReadingStatus {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(READ_CHAPTER),
					"$ID =?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				ReadingStatus.UNREAD
			} else {
				cursor.moveToNext()
				val y = cursor.getInt(READ_CHAPTER)
				cursor.close()
				when (y) {
					0 -> ReadingStatus.UNREAD
					1 -> ReadingStatus.READING
					else -> ReadingStatus.READ
				}
			}
		}


		@Throws(MissingResourceException::class)
		fun getTitle(chapterID: Int): String {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(TITLE),
					"$ID =?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				"UNKNOWN"
			} else {
				cursor.moveToNext()
				val y = cursor.getString(TITLE)
				cursor.close()
				y.checkStringDeserialize()
			}
		}

		/**
		 * Sets chapter status
		 *
		 * @param chapterID chapter to be set
		 * @param readingStatus    status to be set
		 */
		@Throws(SQLException::class, MissingResourceException::class)
		fun setChapterStatus(chapterID: Int, readingStatus: ReadingStatus) {
			val v = ContentValues()
			v.put(READ_CHAPTER, readingStatus.toString())
			getDatabase().update(CHAPTERS, v, "$ID=?", arrayOf("$chapterID"))

			if (readingStatus === ReadingStatus.READ) updateY(chapterID, 0)
		}

		/**
		 * Sets bookmark true or false (1 for true, 0 is false)
		 *
		 * @param chapterID chapterID
		 * @param b         1 is true, 0 is false
		 */
		@Throws(MissingResourceException::class)
		fun setBookMark(chapterID: Int, b: Int) {
			val v = ContentValues()
			v.put(BOOKMARKED, b)
			getDatabase().update(CHAPTERS, v, "$ID=?", arrayOf("$chapterID"))
		}

		/**
		 * is this chapter bookmarked?
		 *
		 * @param chapterID id of chapter
		 * @return if bookmarked?
		 */
		@Throws(MissingResourceException::class)
		fun isBookMarked(chapterID: Int): Boolean {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(BOOKMARKED),
					"$ID=?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				false
			} else {
				cursor.moveToNext()
				val y = cursor.getInt(BOOKMARKED)
				cursor.close()
				y == 1
			}
		}

		/**
		 * Removes save path FROM chapter
		 *
		 * @param chapterID chapter to remove save path of
		 */
		@Throws(SQLException::class)
		fun removePath(chapterID: Int) {
			val v = ContentValues()
			v.put(SAVE_PATH, "NULL")
			v.put(IS_SAVED, 0)
			getDatabase().update(CHAPTERS, v, "$ID=?", arrayOf("$chapterID"))
		}

		/**
		 * Adds save path
		 *
		 * @param chapterID   chapter to update
		 * @param chapterPath save path to set
		 */
		@Throws(SQLException::class)
		private fun addSavedPath(chapterID: Int, chapterPath: String) {
			val v = ContentValues()
			v.put(SAVE_PATH, chapterPath)
			v.put(IS_SAVED, 1)
			getDatabase().update(CHAPTERS, v, "$ID=?", arrayOf("$chapterID"))
		}

		@Throws(SQLException::class)
		fun addSavedPath(chapterURL: String, chapterPath: String) =
				addSavedPath(getChapterIDFromChapterURL(chapterURL), chapterPath)

		/**
		 * Is the chapter saved
		 *
		 * @param chapterID novelURL of the chapter
		 * @return true if saved, false otherwise
		 */
		@Throws(MissingResourceException::class)
		fun isSaved(chapterID: Int): Boolean {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(IS_SAVED),
					"$ID=?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				false
			} else {
				cursor.moveToNext()
				val y = cursor.getInt(IS_SAVED)
				cursor.close()
				y == 1
			}
		}

		@Throws(MissingResourceException::class)
		fun getSavedNovelPath(chapterID: Int): String {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(SAVE_PATH),
					"$ID=?",
					arrayOf("$chapterID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				""
			} else {
				cursor.moveToNext()
				val savedData = cursor.getString(SAVE_PATH)
				cursor.close()
				savedData
			}
		}

		/**
		 * If the chapter URL is present or not
		 *
		 * @param chapterURL chapter url
		 * @return if present
		 */
		@Throws(MissingResourceException::class)
		fun isNotInChapters(chapterURL: String): Boolean {
			val cursor = getDatabase().query(
					CHAPTERS,
					arrayOf(),
					"$ID=?",
					arrayOf("${getChapterIDFromChapterURL(chapterURL)}")
			)
			val a = cursor.count
			cursor.close()
			return a <= 0
		}

		@Throws(MissingResourceException::class)
		fun updateChapter(novelChapter: Chapter) {
			val v = ContentValues()
			v.put(TITLE, novelChapter.title.checkStringSerialize())
			v.put(RELEASE_DATE, novelChapter.release.checkStringSerialize())
			v.put(ORDER, novelChapter.order)

			getDatabase().update(
					CHAPTERS,
					v,
					"$ID=?",
					arrayOf("${getChapterIDFromChapterURL(novelChapter.link)}")
			)
		}

		/**
		 * Adds chapter to database
		 *
		 * @param novelID      ID of novel
		 * @param novelChapter chapterURL
		 * @return chapterID
		 */
		@Throws(MissingResourceException::class)
		fun addToChapters(novelID: Int, novelChapter: Chapter): Int {
			if (!DatabaseIdentification.hasChapter(novelChapter.link))
				DatabaseIdentification.addChapter(novelID, novelChapter.link)

			val v = ContentValues()
			val chapterID = getChapterIDFromChapterURL(novelChapter.link)
			v.put(ID, chapterID)
			v.put(PARENT_ID, novelID)
			v.put(TITLE, novelChapter.title.checkStringSerialize())
			v.put(RELEASE_DATE, novelChapter.release.checkStringSerialize())
			v.put(ORDER, novelChapter.order)
			v.put(Y_POSITION, 0)
			v.put(READ_CHAPTER, 0)
			v.put(BOOKMARKED, 0)
			v.put(IS_SAVED, 0)
			getDatabase().insert(CHAPTERS, v)
			return chapterID
		}

		/**
		 * Gets chapters of a novel
		 *
		 * @param novelID ID to retrieve from
		 * @return List of chapters saved of novel
		 */
		@Throws(MissingResourceException::class)
		fun getChapters(novelID: Int): List<Chapter> {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(ID, TITLE, RELEASE_DATE, ORDER),
					"${PARENT_ID}=?",
					arrayOf("$novelID"),
					orderBy = "$ORDER ASC"
			)

			return if (cursor.count <= 0) {
				cursor.close()
				ArrayList()
			} else {
				val novelChapters = ArrayList<Chapter>()
				while (cursor.moveToNext()) {
					val novelChapter = Chapter(
							cursor.getString(RELEASE_DATE).checkStringDeserialize(),
							cursor.getString(TITLE).checkStringDeserialize(),
							getChapterURLFromChapterID(cursor.getInt(ID)),
							cursor.getDouble(ORDER))
					novelChapters.add(novelChapter)
				}
				cursor.close()
				novelChapters
			}
		}

		/**
		 * Gets chapters of a novel
		 *
		 * @param novelID ID to retrieve from
		 * @return List of chapters saved of novel (ID only)
		 */
		@Throws(MissingResourceException::class)
		fun getChaptersOnlyIDs(novelID: Int): List<Int> {
			val cursor = getDatabase().query(
					CHAPTERS,
					stringArrayOf(ID, ORDER),
					"$PARENT_ID=?",
					arrayOf("$novelID"),
					orderBy = "$ORDER DESC"
			)
			return if (cursor.count <= 0) {
				cursor.close()
				ArrayList()
			} else {
				val integers = ArrayList<Int>()
				while (cursor.moveToNext()) integers.add(cursor.getInt(ID))
				cursor.close()
				integers
			}
		}

		/**
		 * Gets a chapter by it's URL
		 *
		 * @param chapterID id of chapter
		 * @return NovelChapter of said chapter
		 */
		@Throws(MissingResourceException::class)
		fun getChapter(chapterID: Int): Chapter? {
			val cursor = getDatabase().query(CHAPTERS, stringArrayOf(
					TITLE,
					ID,
					RELEASE_DATE,
					ORDER
			), "$ID=?", arrayOf("$chapterID"))
			return if (cursor.count <= 0) {
				cursor.close()
				null
			} else {
				cursor.moveToNext()
				val novelChapter = Chapter()
				novelChapter.title = cursor.getString(TITLE).checkStringDeserialize()
				novelChapter.link = getChapterURLFromChapterID(cursor.getInt(ID))
				novelChapter.release = cursor.getString(RELEASE_DATE).checkStringDeserialize()
				novelChapter.order = cursor.getDouble(ORDER)
				novelChapter
			}
		}

	}

	@Deprecated("ROOM",level = DeprecationLevel.WARNING)
	object DatabaseNovels {
		/**
		 * Bookmarks the novel
		 *
		 * @param novelID novelID of the novel
		 */
		@Throws(SQLException::class)
		fun bookmarkNovel(novelID: Int) {
			getDatabase().execSQL("update $NOVELS set $BOOKMARKED=1 WHERE $PARENT_ID=$novelID")
		}

		/**
		 * UnBookmarks the novel
		 *
		 * @param novelID id
		 */
		@Throws(SQLException::class)

		fun unBookmarkNovel(novelID: Int) {
			getDatabase().execSQL("update $NOVELS set $BOOKMARKED=0 WHERE $PARENT_ID=$novelID")
		}

		@Throws(MissingResourceException::class)
		fun isNovelBookmarked(novelID: Int): Boolean {
			val cursor = getDatabase()
					.rawQuery(
							"SELECT $BOOKMARKED FROM $NOVELS WHERE $PARENT_ID=$novelID",
							null
					)
			if (cursor.count <= 0) {
				cursor.close()
				return false
			}
			cursor.moveToNext()
			println(Arrays.toString(cursor.columnNames))
			val a = cursor.getInt(BOOKMARKED)
			cursor.close()
			return a > 0
		}

		@Throws(SQLException::class)

		fun setReaderType(novelID: Int, reader: Int) {
			getDatabase()
					.execSQL("update $NOVELS set $READER_TYPE=$reader WHERE $PARENT_ID=$novelID")
		}

		/**
		 * Gets reader type for novel
		 *
		 * @param novelID novelID
		 * @return -2 is no such novel, -1 is default, 0 is the same as -1, and 1+ is a specific reading type
		 */
		@Throws(MissingResourceException::class)
		fun getReaderType(novelID: Int): Int {
			val cursor = getDatabase()
					.rawQuery(
							"SELECT $READER_TYPE FROM $NOVELS WHERE $PARENT_ID=$novelID",
							null
					)
			if (cursor.count <= 0) {
				cursor.close()
				return -2
			}
			cursor.moveToNext()
			println(Arrays.toString(cursor.columnNames))
			val a = cursor.getInt(READER_TYPE)
			cursor.close()
			return a
		}

		@Throws(MissingResourceException::class)
		fun addNovelToDatabase(
				formatter: Int,
				novelPage: Novel.Info,
				novelURL: String,
				readingStatus: Int,
				novelID: Int = getNovelIDFromNovelURL(novelURL)
		) {
			DatabaseIdentification.addNovel(novelURL, formatter)
			val imageURL = novelPage.imageURL

			val v = ContentValues()
			v.put(PARENT_ID, novelID)
			v.put(BOOKMARKED, 0)
			v.put(READING_STATUS, readingStatus)
			v.put(READER_TYPE, -1)
			v.put(TITLE, novelPage.title.checkStringSerialize())
			v.put(IMAGE_URL, imageURL)
			v.put(DESCRIPTION, novelPage.description.checkStringSerialize())
			v.put(GENRES, novelPage.genres.convertArrayToString().checkStringSerialize())
			v.put(AUTHORS, novelPage.authors.convertArrayToString().checkStringSerialize())
			v.put(STATUS, novelPage.status.title)
			v.put(TAGS, novelPage.tags.convertArrayToString().checkStringSerialize())
			v.put(ARTISTS, novelPage.artists.convertArrayToString().checkStringSerialize())
			v.put(LANGUAGE, novelPage.language.checkStringSerialize())
			getDatabase().insert(NOVELS, v)
		}

		// --Commented out by Inspection START (12/22/19 11:09 AM):
		//        /**
		//         * @param novelURL url of novel to remove
		//         * @return if successful
		//         */
		//        public static boolean removeFromLibrary(@NotNull String novelURL) {
		//            boolean a = getDatabase()..delete(Tables.NOVELS.toString(), Columns.PARENT_ID + "=" + getNovelIDFromNovelURL(novelURL), null) > 0;
		//            boolean b = getDatabase()..delete(Tables.NOVEL_IDENTIFICATION.toString(), Columns.ID + "=" + getNovelIDFromNovelURL(novelURL), null) > 0;
		//            return a && b;
		//        }
		// --Commented out by Inspection STOP (12/22/19 11:09 AM)

		/**
		 * Is a novel in the library or not
		 *
		 * @param novelID Novel novelID
		 * @return yes or no
		 */
		@Throws(MissingResourceException::class)
		fun isNotInNovels(novelID: Int): Boolean {
			val cursor = getDatabase().query(
					NOVEL_IDENTIFICATION,
					arrayOf(),
					"$ID=?",
					arrayOf("$novelID")
			)
			val i = cursor.count
			cursor.close()
			return i <= 0
		}

		@Throws(MissingResourceException::class)
		fun isNotInNovels(novelURL: String): Boolean {
			return -1 == getNovelIDFromNovelURL(novelURL)
		}

		// --Commented out by Inspection START (12/22/19 11:09 AM):
		//        /**
		//         * Get's the entire library to be listed
		//         *
		//         * @return the library
		//         */
		//        @NonNull
		//        public static ArrayList<NovelCard> getLibrary() {
		//            Log.d("DL", "Getting");
		//            Cursor cursor = getDatabase()..query(Tables.NOVELS.toString(),
		//                    new String[]{Columns.PARENT_ID.toString(), Columns.TITLE.toString(), Columns.IMAGE_URL.toString()},
		//                    Columns.BOOKMARKED + "=1", null, null, null, null);
		//
		//            ArrayList<NovelCard> novelCards = new ArrayList<>();
		//            if (cursor.getCount() <= 0) {
		//                cursor.close();
		//                return new ArrayList<>();
		//            } else {
		//                while (cursor.moveToNext()) {
		//                    try {
		//                        int parent = cursor.getInt(Columns.PARENT_ID);
		//                        novelCards.add(new NovelCard(
		//                                checkStringDeserialize(cursor.getString(Columns.TITLE)),
		//                                parent, DatabaseIdentification.getNovelURLfromNovelID(parent),
		//                                cursor.getString(Columns.IMAGE_URL),
		//                                DatabaseIdentification.getFormatterIDFromNovelID(parent)
		//                        ));
		//                    } catch (Exception e) {
		//                        e.printStackTrace();
		//                    }
		//                }
		//                cursor.close();
		//                return novelCards;
		//            }
		//        }
		// --Commented out by Inspection STOP (12/22/19 11:09 AM)
		val intLibrary: ArrayList<Int>
			@Throws(MissingResourceException::class)
			get() {
				Log.d(logID(), "Getting")
				val cursor = getDatabase()
						.query(NOVELS, stringArrayOf(PARENT_ID), "$BOOKMARKED=1", orderBy = "$TITLE ASC")
				val novelCards = ArrayList<Int>()
				return if (cursor.count <= 0) {
					cursor.close()
					ArrayList()
				} else {
					while (cursor.moveToNext())
						novelCards.add(cursor.getInt(PARENT_ID))
					cursor.close()
					novelCards
				}
			}

		@Throws(MissingResourceException::class)
		fun getNovel(novelID: Int): NovelCard {
			Log.d(logID(), "Getting")
			val cursor = getDatabase()
					.query(NOVELS, stringArrayOf(PARENT_ID, TITLE, IMAGE_URL),
							"$BOOKMARKED=1 AND $PARENT_ID=$novelID")
			if (cursor.count <= 0) {
				cursor.close()
			} else {
				cursor.moveToNext()
				val novelCard = NovelCard(
						cursor.getString(TITLE).checkStringDeserialize(),
						novelID, DatabaseIdentification.getNovelURLFromNovelID(novelID)
						?: "",
						cursor.getString(IMAGE_URL),
						DatabaseIdentification.getFormatterIDFromNovelID(novelID)
				)
				cursor.close()
				return novelCard
			}
			return NovelCard("", -2, "", "", -1)
		}

		@Throws(MissingResourceException::class)
		fun getNovelTitle(novelID: Int): String {
			Log.d(logID(), "Getting")
			val cursor = getDatabase().query(NOVELS, stringArrayOf(TITLE),
					"$BOOKMARKED=1 and $PARENT_ID=$novelID")
			return if (cursor.count <= 0) {
				cursor.close()
				"unknown"
			} else {
				cursor.moveToNext()
				val title = cursor.getString(TITLE).checkStringDeserialize()
				cursor.close()
				title
			}
		}

		/**
		 * Gets saved novelPage
		 *
		 * @param novelID novel to retrieve
		 * @return Saved novelPage
		 */
		@Throws(MissingResourceException::class)
		fun getNovelPage(novelID: Int): Novel.Info {
			val cursor = getDatabase().query(
					NOVELS,
					stringArrayOf(
							TITLE,
							IMAGE_URL,
							DESCRIPTION,
							GENRES,
							AUTHORS,
							STATUS,
							TAGS,
							ARTISTS,
							LANGUAGE,
							MAX_CHAPTER_PAGE
					),
					"${PARENT_ID}=?",
					arrayOf("$novelID")
			)
			if (cursor.count <= 0) {
				cursor.close()
				return Novel.Info()
			} else {
				cursor.moveToNext()
				val novelPage = Novel.Info()
				novelPage.title = cursor.getString(TITLE).checkStringDeserialize()
				novelPage.imageURL = cursor.getString(IMAGE_URL)
				novelPage.description = cursor.getString(DESCRIPTION).checkStringDeserialize()
				novelPage.genres = cursor.getString(GENRES).checkStringDeserialize().convertStringToArray()
				novelPage.authors = cursor.getString(AUTHORS).checkStringDeserialize().convertStringToArray()
				novelPage.status = cursor.getString(STATUS).convertStringToStati()
				novelPage.tags = cursor.getString(TAGS).checkStringDeserialize().convertStringToArray()
				novelPage.artists = cursor.getString(ARTISTS).checkStringDeserialize().convertStringToArray()
				novelPage.language = cursor.getString(LANGUAGE).checkStringDeserialize()
				cursor.close()
				return novelPage
			}
		}

		// --Commented out by Inspection START (12/22/19 11:09 AM):
		//        public static void setStatus(int novelID, @NotNull Status status) {
		//            getDatabase()..execSQL("update " + Tables.NOVELS + " set " + Columns.READING_STATUS + "=" + status + " WHERE " + Columns.PARENT_ID + "=" + novelID);
		//        }
		// --Commented out by Inspection STOP (12/22/19 11:09 AM)

		@Throws(MissingResourceException::class)
		fun getNovelStatus(novelID: Int): ReadingStatus {
			val cursor = getDatabase().query(
					NOVELS,
					stringArrayOf(READING_STATUS),
					"${PARENT_ID}=?",
					arrayOf("$novelID")
			)
			return if (cursor.count <= 0) {
				cursor.close()
				ReadingStatus.UNREAD
			} else {
				cursor.moveToNext()
				val y = cursor.getInt(READING_STATUS)
				cursor.close()
				when (y) {
					0 -> ReadingStatus.UNREAD
					1 -> ReadingStatus.READING
					2 -> ReadingStatus.READ
					3 -> ReadingStatus.ONHOLD
					else -> ReadingStatus.DROPPED
				}
			}
		}

		@Throws(SQLException::class)
		fun updateNovel(novelURL: String, novelPage: Novel.Info) {
			val imageURL = novelPage.imageURL
			val v = ContentValues()
			v.put(TITLE, novelPage.title.checkStringSerialize())
			v.put(IMAGE_URL, imageURL)
			v.put(DESCRIPTION, novelPage.description.checkStringSerialize())
			v.put(GENRES, novelPage.genres.convertArrayToString().checkStringSerialize())
			v.put(AUTHORS, novelPage.authors.convertArrayToString().checkStringSerialize())
			v.put(STATUS, novelPage.status.title)
			v.put(TAGS, novelPage.tags.convertArrayToString().checkStringSerialize())
			v.put(ARTISTS, novelPage.artists.convertArrayToString().checkStringSerialize())
			v.put(LANGUAGE, novelPage.language.checkStringSerialize())
			getDatabase().update(
					NOVELS,
					v,
					"${PARENT_ID}=?",
					arrayOf("${getNovelIDFromNovelURL(novelURL)}")
			)
		}

		@Throws(SQLException::class)
		fun migrateNovel(
				oldID: Int,
				newURL: String,
				formatterID: Int,
				newNovel: Novel.Info,
				status: Int
		) {
			unBookmarkNovel(oldID)
			if (isNotInNovels(newURL)) addNovelToDatabase(formatterID, newNovel, newURL, status)
			bookmarkNovel(getNovelIDFromNovelURL(newURL))
		}
	}
}