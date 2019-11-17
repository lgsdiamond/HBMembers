package com.lgsdiamond.hbmembers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.View
import android.widget.PopupMenu
import com.lgsdiamond.hbmembers.HbUtility.Companion.isMatchingName
import com.lgsdiamond.hbmembers.MainActivity.Companion.PREF_KEY_CONTACT
import com.lgsdiamond.hbmembers.MainActivity.Companion.PREF_KEY_MEMO
import com.lgsdiamond.lgsutility.LgsUtility.Companion.showSoftKeyboard
import com.lgsdiamond.lgsutility.LgsUtility.Companion.titleFace
import com.lgsdiamond.lgsutility.customFaceMenu
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

/**
 * Created by LgsDiamond on 2018-02-17.
 */

class HbDbAccess
/**
 * Private constructor to avoid object creation from outside classes.
 *
 * @param context
 */
private constructor(context: Context) {
	
	private val openHelper: SQLiteOpenHelper
	private lateinit var mDatabase: SQLiteDatabase
	
	private val dbVersion = 4
	
	init {
		this.openHelper = HbDbHelper(context, FILENAME_DB, dbVersion)
	}
	
	/**
	 * Open the mDatabase connection.
	 */
	fun open() {
		this.mDatabase = openHelper.writableDatabase   // read-only
	}
	
	/**
	 * Close the mDatabase connection.
	 */
	fun close() {
		this.mDatabase.close()
	}
	
	fun getMemberContact(name: String, anchorView: View): String {
		val memberInfo = getMemberInfoByName(name, anchorView)
		return memberInfo?.mContact ?: "?"
	}
	
	fun getMemberInfoByName(name: String, anchorView: View?): MemberInfo? {
		val nameLength = name.length
		
		var cursor: Cursor? = null
		try {
			var memberInfo: MemberInfo? = null
			
			var sqlCmd =
				"SELECT * FROM ${HbDbHelper.dbTableMember} WHERE ${HbDbHelper.dbMemberName} = \'$name\'"
			cursor = mDatabase.rawQuery(sqlCmd, null)
			var count = cursor.count
			
			// try to find a person
			if (count == 0) {
				sqlCmd =
					"SELECT * FROM ${HbDbHelper.dbTableMember} WHERE ${HbDbHelper.dbMemberName} LIKE \'%$name%\'"
				cursor = mDatabase.rawQuery(sqlCmd, null)
				count = cursor.count
				
				// 3글자 이상일 경우 맨 뒤 두글자만 이름으로 간주하여 검색 시도
				if ((count == 0) && (nameLength >= 3)) {
					val array = CharArray(2)
					name.toCharArray(array, 0, nameLength - 2, nameLength)
					sqlCmd =
						"SELECT * FROM ${HbDbHelper.dbTableMember} WHERE ${HbDbHelper.dbMemberName} LIKE " + "\'%" +
								array[0] + array[1] + "%\'"
					cursor = mDatabase.rawQuery(sqlCmd, null)
					count = cursor.count
				}
				
				// 한글 초성 검색" 2개 또는 3개의 초성만 검색
				if ((count == 0) && (nameLength >= 2) && (nameLength <= 4)) {
					val chosungSQL = ChoSearchQuery.makeQuery(HbDbHelper.dbMemberName, name)
					
					sqlCmd = "SELECT * FROM ${HbDbHelper.dbTableMember} WHERE $chosungSQL"
					cursor = mDatabase.rawQuery(sqlCmd, null)
					count = cursor.count
				}
			}
			
			if (count == 1) {    // if single member
				memberInfo = MemberInfo()
				cursor.moveToFirst()
				
				memberInfo.mNumber = getValidString(cursor.getString(MemberDBCol.NUMBER.ordinal))
				memberInfo.mName = getValidString(cursor.getString(MemberDBCol.NAME.ordinal))
				memberInfo.mCompany12 =
					getValidString(cursor.getString(MemberDBCol.COMPANY12.ordinal))
				memberInfo.mCompany34 =
					getValidString(cursor.getString(MemberDBCol.COMPANY34.ordinal))
				memberInfo.mSchool = getValidString(cursor.getString(MemberDBCol.SCHOOL.ordinal))
				memberInfo.mMajor = getValidString(cursor.getString(MemberDBCol.MAJOR.ordinal))
				memberInfo.mContact = getValidString(cursor.getString(MemberDBCol.CONTACT.ordinal))
				memberInfo.mBranch = getValidString(cursor.getString(MemberDBCol.BRANCH.ordinal))
				memberInfo.mReference =
					getValidString(cursor.getString(MemberDBCol.REFERENCE.ordinal))
				
				memberInfo.mCompany12All =
					getValidString(getSingleAll(memberInfo.mCompany12,
						HbDbHelper.dbMemberCompany12))
				memberInfo.mCompany34All =
					getValidString(getSingleAll(memberInfo.mCompany34,
						HbDbHelper.dbMemberCompany34))
				memberInfo.mSchoolAll =
					getValidString(getSingleAll(memberInfo.mSchool, HbDbHelper.dbMemberSchool))
				memberInfo.mMajorAll =
					getValidString(getSingleAll(memberInfo.mMajor, HbDbHelper.dbMemberMajor))
				memberInfo.mBranchAll =
					getValidString(getSingleAll(memberInfo.mBranch, HbDbHelper.dbMemberBranch))
			} else if (count > 1) {     // if multiple members, select one member through popupMenu
				showSoftKeyboard(false)
				
				if (anchorView != null) {
					val popupMenu = PopupMenu(anchorView.context, anchorView)
					val menu = popupMenu.menu
					cursor.moveToFirst()
					var memberName: String
					var index = 0
					while (!cursor.isAfterLast) {
						memberName = cursor.getString(MemberDBCol.NAME.ordinal)
						menu.add(0, index++, 0, memberName)
						cursor.moveToNext()
					}
					
					menu.customFaceMenu(titleFace)
					
					popupMenu.show()
					
					popupMenu.setOnMenuItemClickListener { menuItem ->
						val name = menuItem.title.toString()
						gActivity.memberFragment.updateMemberInfo(null, name)
						true    // consume the click
					}
				}
			}
			
			return memberInfo
		}
		finally {
			cursor?.close()
		}
	}
	
	fun getMemberInfoByContact(contact: String, anchorView: View?): MemberInfo? {
		// contact should be only numbers and at least 4 numbers
		var cursor: Cursor? = null
		try {
			var memberInfo: MemberInfo? = null
			
			var sqlCmd =
				"SELECT * FROM ${HbDbHelper.dbTableMember} WHERE REPLACE(${HbDbHelper.dbMemberContact},'-','') = \'$contact\'"
			
			cursor = mDatabase.rawQuery(sqlCmd, null)
			var count = cursor.count
			
			// try to find a person
			if (count == 0) {
				sqlCmd =
					"SELECT * FROM ${HbDbHelper.dbTableMember} WHERE REPLACE(${HbDbHelper.dbMemberContact},'-','') LIKE \'%$contact%\'"
				
				cursor = mDatabase.rawQuery(sqlCmd, null)
				count = cursor.count
			}
			
			if (count == 1) {    // if single member
				memberInfo = MemberInfo()
				cursor.moveToFirst()
				
				memberInfo.mNumber = getValidString(cursor.getString(MemberDBCol.NUMBER.ordinal))
				memberInfo.mName = getValidString(cursor.getString(MemberDBCol.NAME.ordinal))
				memberInfo.mCompany12 =
					getValidString(cursor.getString(MemberDBCol.COMPANY12.ordinal))
				memberInfo.mCompany34 =
					getValidString(cursor.getString(MemberDBCol.COMPANY34.ordinal))
				memberInfo.mSchool = getValidString(cursor.getString(MemberDBCol.SCHOOL.ordinal))
				memberInfo.mMajor = getValidString(cursor.getString(MemberDBCol.MAJOR.ordinal))
				memberInfo.mContact = getValidString(cursor.getString(MemberDBCol.CONTACT.ordinal))
				memberInfo.mBranch = getValidString(cursor.getString(MemberDBCol.BRANCH.ordinal))
				memberInfo.mReference =
					getValidString(cursor.getString(MemberDBCol.REFERENCE.ordinal))
				
				memberInfo.mCompany12All =
					getValidString(getSingleAll(memberInfo.mCompany12,
						HbDbHelper.dbMemberCompany12))
				memberInfo.mCompany34All =
					getValidString(getSingleAll(memberInfo.mCompany34,
						HbDbHelper.dbMemberCompany34))
				memberInfo.mSchoolAll =
					getValidString(getSingleAll(memberInfo.mSchool, HbDbHelper.dbMemberSchool))
				memberInfo.mMajorAll =
					getValidString(getSingleAll(memberInfo.mMajor, HbDbHelper.dbMemberMajor))
				memberInfo.mBranchAll =
					getValidString(getSingleAll(memberInfo.mBranch, HbDbHelper.dbMemberBranch))
			} else if (count > 1) {     // if multiple members, select one member through popupMenu
				showSoftKeyboard(false)
				
				if (anchorView != null) {
					val popupMenu = PopupMenu(anchorView.context, anchorView)
					val menu = popupMenu.menu
					cursor.moveToFirst()
					var memberName: String
					var index = 0
					while (!cursor.isAfterLast) {
						memberName = cursor.getString(MemberDBCol.NAME.ordinal)
						menu.add(0, index++, 0, memberName)
						cursor.moveToNext()
					}
					
					menu.customFaceMenu(titleFace)
					
					popupMenu.show()
					
					popupMenu.setOnMenuItemClickListener { menuItem ->
						val menuName = menuItem.title.toString()
						gActivity.memberFragment.updateMemberInfo(null, menuName)
						true    // consume the click
					}
				}
			}
			
			return memberInfo
		}
		finally {
			cursor?.close()
		}
	}
	
	private fun getSingleAll(
		singleId: String,
		dbIndexName: String,
		toCountQuestionMark: Boolean = false
	): String? {
		if (singleId.contentEquals("N/A") || (singleId.contentEquals("?") && !toCountQuestionMark))
			return ""
		
		var cursor: Cursor? = null
		try {
			val sqlCmd =
				"SELECT ${HbDbHelper.dbMemberName} FROM ${HbDbHelper.dbTableMember} WHERE $dbIndexName = \'$singleId\' ORDER BY ${HbDbHelper.dbMemberName}"
			cursor = mDatabase.rawQuery(sqlCmd, null)
			
			val count = cursor!!.count
			var normalAll = ""
			var abnormalAll = ""
			
			if (count > 0) {
				cursor.moveToFirst()
				var name: String = cursor.getString(0)
				if (name[0] == '(') {
					abnormalAll += name
				} else {
					normalAll += name
				}
				
				cursor.moveToNext()
				while (!cursor.isAfterLast) {
					name = cursor.getString(0)
					if (name[0] == '(') {
						if (abnormalAll.isEmpty())
							abnormalAll = name
						else
							abnormalAll += ", $name"
					} else {
						if (normalAll.isEmpty())
							normalAll = name
						else
							normalAll += ", $name"
					}
					cursor.moveToNext()
				}
				return if (abnormalAll.isEmpty()) normalAll else {
					if (normalAll.isEmpty()) abnormalAll else "$normalAll, $abnormalAll"
				}
			} else
				return null
		}
		finally {
			cursor?.close()
		}
	}
	
	//===
	fun makeDistinctDataList(
		singles: MutableList<String>, contents: MutableList<String>,
		dbTable: String, distinctCol: String
	) {
		val sqlCmd: String
		
		var cursor: Cursor? = null
		try {
			sqlCmd = "SELECT DISTINCT $distinctCol FROM $dbTable ORDER BY $distinctCol"
			cursor = mDatabase.rawQuery(sqlCmd, null)
			val count = cursor!!.count
			
			var lastName = ""
			var lastContent = ""
			
			var haveLast = false
			if (count > 0) {
				cursor.moveToFirst()
				while (!cursor.isAfterLast) {
					val singleId = cursor.getString(0)
					if ((singleId != null) && singleId.isNotEmpty() && (singleId[0] != '(')
						&& !singleId.contentEquals("N/A")
					) {
						val content = getValidString(getSingleAll(singleId, distinctCol, true))
						if (singleId.contentEquals("?")) {
							haveLast = true
							lastName = singleId
							lastContent = content
						} else {
							singles.add(singleId)
							contents.add(content)
						}
					}
					cursor.moveToNext()
				}
			}
			if (haveLast) {
				singles.add(lastName)
				contents.add(lastContent)
			}
		}
		finally {
			cursor?.close()
		}
	}
	
	fun getNameFromNumber(userNumber: String): String {
		var cursor: Cursor? = null
		try {
			var userName = ""
			val sqlCmd =
				("SELECT ${HbDbHelper.dbMemberName} FROM ${HbDbHelper.dbTableMember} WHERE ${HbDbHelper.dbMemberNumber} = "
						+ "\'" + userNumber + "\'")
			cursor = mDatabase.rawQuery(sqlCmd, null)
			val count = cursor!!.count
			
			if (count > 0) {        // it should be 1 or 0
				cursor.moveToFirst()
				userName = cursor.getString(0)
			}
			return userName
		}
		finally {
			cursor?.close()
		}
	}
	
	fun isValidMember(number: String, name: String): Boolean {
		if (name.length < 2) return false
		
		val nameOut = getNameFromNumber(number)
		
		return isMatchingName(name, nameOut)
	}
	
	fun getInfoByTitle(title: String): MemberInfo? {
		var cursor: Cursor? = null
		try {
			val sqlCmd =
				"SELECT ${HbDbHelper.dbBoardName} FROM ${HbDbHelper.dbTableBoard} WHERE ${HbDbHelper.dbBoardTitle} = \'$title\'"
			cursor = mDatabase.rawQuery(sqlCmd, null)
			val count = cursor.count
			if (count < 1) return null        // no one found
			
			cursor.moveToFirst()
			val name = cursor.getString(0)
			
			return getMemberInfoByName(name, null)
		}
		finally {
			cursor?.close()
		}
	}
	
	enum class MemberDBCol {
		ID, NUMBER, NAME, COMPANY12, COMPANY34, SCHOOL, MAJOR, CONTACT, BRANCH, REFERENCE
	}
	
	inner class MemberInfo {
		var mNumber: String = ""
		var mName: MemberName = ""
		var mCompany12: String = ""
		var mCompany12All: String = ""
		var mCompany34: String = ""
		var mCompany34All: String = ""
		var mSchool: String = ""
		var mSchoolAll: String = ""
		var mMajor: String = ""
		var mMajorAll: String = ""
		var mContact: String = ""
		var mBranch: String = ""
		var mBranchAll: String = ""
		var mReference: String = ""
	}
	
	companion object {
		private var instance: HbDbAccess? = null
		
		/**
		 * Return a singleton instance of HbDbAccess.
		 *
		 * @param context the Context
		 * @return the instance of HbDbAccess
		 */
		fun getInstance(context: Context): HbDbAccess? {
			if (instance == null) {
				instance = HbDbAccess(context)
			}
			return instance
		}
		
		internal fun getValidString(aStr: String?): String {     // "?" is OK
			return if (aStr == null || aStr.contentEquals("N/A"))
				""
			else
				aStr
		}
		
		fun makeMemberMemoKey(name: MemberName): String {
			return "${PREF_KEY_MEMO}_$name"
		}
		
		fun makeMemberContactKey(name: MemberName): String {
			return "${PREF_KEY_CONTACT}_$name"
		}
		
	}
}

class HbDbHelper(context: Context, fileName: String, dbVersion: Int) :
	SQLiteAssetHelper(context, fileName, null, dbVersion) {
	companion object {
		const val dbTableMember: String = "members"
		const val dbMemberNumber: String = "_number"
		const val dbMemberName: String = "_name"
		const val dbMemberCompany12: String = "_company_12"
		const val dbMemberCompany34: String = "_company_34"
		const val dbMemberSchool: String = "_school"
		const val dbMemberMajor: String = "_major"
		const val dbMemberContact: String = "_contact"
		const val dbMemberBranch: String = "_branch"
		const val dbMemberReference: String = "_reference"
		
		const val dbTableBoard: String = "board"
		const val dbBoardTitle: String = "_title"
		const val dbBoardName: String = "_name"
	}
	
	init {
		setForcedUpgrade()
	}
}

object ChoSearchQuery {
	private const val EVENT_CODE_LENGTH = 6
	
	private const val DIGIT_BEGIN_UNICODE = 0x30 //0
	private const val DIGIT_END_UNICODE = 0x3A //9
	
	private const val QUERY_DELIM = 39//'
	private const val LARGE_ALPHA_BEGIN_UNICODE = 0
	
	private const val HANGUL_BEGIN_UNICODE = 0xAC00 // 가
	private const val HANGUL_END_UNICODE = 0xD7A3 // ?
	private const val HANGUL_CHO_UNIT = 588 //한글 초성글자간 간격
	private const val HANGUL_JUNG_UNIT = 28 //한글 중성글자간 간격
	
	private val CHO_LIST =
		charArrayOf(
			'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
			'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
		)
	private val CHO_SEARCH_LIST = booleanArrayOf(
		true, false, true, true, false, true, true, true, false, true,
		false, true, true, false, true, true, true, true, true
	)
	
	/**
	 * 문자를 유니코드(10진수)로 변환 후 반환한다.
	 * @param ch 문자
	 * @return 10진수 유니코드
	 */
	private fun convertCharToUnicode(ch: Char): Int {
		return ch.toInt()
	}
	
	/**
	 * 10진수를 16진수 문자열로 변환한다.
	 * @param decimal 10진수 숫자
	 * @return 16진수 문자열
	 */
	private fun toHexString(decimal: Int): String {
		val intDec = java.lang.Long.valueOf(decimal.toLong())
		return java.lang.Long.toHexString(intDec)
	}
	
	/**
	 * 유니코드(16진수)를 문자로 변환 후 반환한다.
	 * @param hexUnicode Unicode Hex String
	 * @return 문자값
	 */
	private fun convertUnicodeToChar(hexUnicode: String): Char {
		return Integer.parseInt(hexUnicode, 16).toChar()
	}
	
	/**
	 * 유니코드(10진수)를 문자로 변환 후 반환한다.
	 * @param unicode
	 * @return 문자값
	 */
	private fun convertUnicodeToChar(unicode: Int): Char {
		return convertUnicodeToChar(toHexString(unicode))
	}
	
	/**
	 * 검색 문자열을 파싱해서 SQL Query 조건 문자열을 만든다.
	 * @param searchWhat 검색 문자열
	 * @return SQL Query 조건 문자열
	 */
	fun makeQuery(field: String, searchWhat: String): String {
		var strSearch = searchWhat
		
		strSearch = strSearch.trim { it <= ' ' }
		
		val retQuery = StringBuilder()
		
		var nChoPosition: Int
		var nNextChoPosition: Int
		var startUnicode: Int
		var sndUnicode: Int
		
		var nQueryIndex = 0
		//            boolean bChosung = false;
		val query = StringBuilder()
		for (nIndex in strSearch.indices) {
			nChoPosition = -1
			nNextChoPosition = -1
			startUnicode = -1
			sndUnicode = -1
			
			if (strSearch[nIndex].toInt() == QUERY_DELIM)
				continue
			
			if (nQueryIndex != 0) {
				query.append(" AND ")
			}
			
			for (nChoIndex in CHO_LIST.indices) {
				if (strSearch[nIndex] == CHO_LIST[nChoIndex]) {
					nChoPosition = nChoIndex
					nNextChoPosition = nChoPosition + 1
					while (nNextChoPosition < CHO_SEARCH_LIST.size) {
						if (CHO_SEARCH_LIST[nNextChoPosition])
							break
						nNextChoPosition++
					}
					break
				}
			}
			
			if (nChoPosition >= 0) { //초성이 있을 경우
				startUnicode = HANGUL_BEGIN_UNICODE + nChoPosition * HANGUL_CHO_UNIT
				sndUnicode = HANGUL_BEGIN_UNICODE + nNextChoPosition * HANGUL_CHO_UNIT
			} else {
				val unicode = convertCharToUnicode(strSearch[nIndex])
				if ((unicode >= HANGUL_BEGIN_UNICODE) && (unicode <= HANGUL_END_UNICODE)) {
					val jong = (unicode - HANGUL_BEGIN_UNICODE) % HANGUL_CHO_UNIT % HANGUL_JUNG_UNIT
					
					if (jong == 0) {// 초성+중성으로 되어 있는 경우
						startUnicode = unicode
						sndUnicode = unicode + HANGUL_JUNG_UNIT
					} else {
						startUnicode = unicode
						sndUnicode = unicode
					}
				}
			}
			
			//Log.d("SearchQuery","query "+strSearch.codePointAt(nIndex));
			if (startUnicode > 0 && sndUnicode > 0) {
				if (startUnicode == sndUnicode)
					query.append("substr($field," + (nIndex + 1) + ",1)='" + strSearch[nIndex] + "'")
				else
					query.append(
						"(substr($field," + (nIndex + 1) + ",1)>='" + convertUnicodeToChar(
							startUnicode
						)
								+ "' AND substr($field," + (nIndex + 1) + ",1)<'" + convertUnicodeToChar(
							sndUnicode
						) + "')"
					)
			} else {
				if (Character.isLowerCase(strSearch[nIndex])) { //영문 소문자
					query.append(
						"(substr($field," + (nIndex + 1) + ",1)='" + strSearch[nIndex] + "'"
								+ " OR substr($field," + (nIndex + 1) + ",1)='" + Character.toUpperCase(
							strSearch[nIndex]
						) + "')"
					)
				} else if (Character.isUpperCase(strSearch[nIndex])) { //영문 대문자
					query.append(
						"(substr($field," + (nIndex + 1) + ",1)='" + strSearch[nIndex] + "'"
								+ " OR substr($field," + (nIndex + 1) + ",1)='" + Character.toLowerCase(
							strSearch[nIndex]
						) + "')"
					)
				} else
				//기타 문자
					query.append("substr($field," + (nIndex + 1) + ",1)='" + strSearch[nIndex] + "'")
			}
			
			nQueryIndex++
		}
		
		if (query.isNotEmpty() && strSearch.trim { it <= ' ' }.isNotEmpty()) {
			retQuery.append("($query)")
			
			if (strSearch.indexOf(" ") != -1) {
				// 공백 구분 단어에 대해 단어 모두 포함 검색
				val tokens =
					strSearch.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				retQuery.append(" OR (")
				var i = 0
				val iSize = tokens.size
				while (i < iSize) {
//                    val token = tokens[i]
					if (i != 0) {
						retQuery.append(" AND ")
					}
//                    retQuery.append("$field like '%$token%'")
					i++
				}
				retQuery.append(")")
			} else {
				// LIKE 검색 추가
//                retQuery.append(" OR $field like '%$strSearch%'")
			}
		} else {
			retQuery.append(query.toString())
		}
		
		return retQuery.toString()
	}
}