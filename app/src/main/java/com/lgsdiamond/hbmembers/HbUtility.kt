package com.lgsdiamond.hbmembers

import android.media.MediaPlayer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import com.lgsdiamond.lgsutility.removeWhitespaces

/**
 * Created by LgsDiamond on 2018-02-18.
 */
class HbUtility {
	companion object {
		
		fun isMatchingName(nameIn: String, nameOut: String): Boolean {
			var nameOne = nameIn
			var nameTwo = nameOut
			if (nameOne.length < 2 || nameOne.length > 10)
				return false     // too short or too long
			if (nameOne.contentEquals(nameTwo)) return true  // exact match
			
			if (!nameTwo.contains("(") && nameOne.length != nameTwo.length)
			// no "(", should same be length
				return false
			
			val charsIn = CharArray(nameOne.length)
			val charsOut = CharArray(nameTwo.length)
			for (i in nameOne.indices) charsIn[i] = ' '
			for (i in nameTwo.indices) charsOut[i] = ' '
			
			nameOne.toCharArray(charsIn, 0, 0, 1) // first char
			nameTwo.toCharArray(charsOut, 0, 0, 1)
			
			if (charsIn[0] != charsOut[0]) return false    // first char mismatch
			
			// special cases
			nameTwo = nameTwo.replace("(교수)", "")
			nameTwo = nameTwo.replace("(제로)", "")
			
			nameOne.toCharArray(charsIn, 0, 1, nameOne.length) // except
			// first char
			nameTwo.toCharArray(charsOut, 0, 1, nameTwo.length)
			
			nameOne = String(charsIn).removeWhitespaces()
			nameTwo = String(charsOut).removeWhitespaces()
			
			return nameTwo.contains(nameOne)
		}
		
		private var sCompanyName = arrayOf(
			"광개토", "재구", "을지", "진흥", "무열", "유신",
			"관창", "문무", "인헌", "최영", "세종", "율곡", "권율", "충무", "홍의", "충용"
		)
		
		fun correctCompanyName(company: String, toAddName: Boolean): String {
			var companyName = company
			
			if (companyName.isEmpty() || companyName.contentEquals("?")) return companyName
			
			if (companyName.substring(0, 1).contentEquals("0"))
				companyName = companyName.substring(1, companyName.length)
			
			if (toAddName) {
				val digits = when {
					(companyName.length == 3) -> companyName.substring(0, 1)
					(companyName.length == 4) -> companyName.substring(0, 2)
					else                      -> ""
				}
				
				val index = try {
					Integer.parseInt(digits) - 1
				} catch (e: NumberFormatException) {
					0
				}
				
				companyName = sCompanyName[index] + " " + companyName
			}
			return companyName
		}
		
		fun getSpannedCompany(company: String): Spannable {
			val spanText = SpannableString(company)
			val pos = company.indexOf(" ")
			if (pos < 0) return spanText
			
			spanText.setSpan(ForegroundColorSpan(-0xff8000), 0, pos, 0)
			spanText.setSpan(RelativeSizeSpan(0.85f), 0, pos, 0)
			
			return spanText
		}
	}
}

//=== MediaPlayer =============================
fun MediaPlayer.startOnOff() = if (gIsSoundOn) this.start() else Unit

val soundTick: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.tick)
}

val soundClick: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.click)
}

val soundList: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.list)
}

val soundMessage: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.message)
}

val soundOpening: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.openning)
}

val soundSliding: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.sliding)
}

val soundBook: MediaPlayer by lazy {
	MediaPlayer.create(gActivity, R.raw.book)
}
