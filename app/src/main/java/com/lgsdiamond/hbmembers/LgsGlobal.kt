package com.lgsdiamond.hbmembers

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import java.util.*


//=== global property ===

lateinit var gMainActivity: MainActivity
val gRandom = Random()
var gIsSoundOn = true

typealias MemberName = String

fun MemberName.isAbsent() = startsWith("(고)")

const val FILENAME_DB: String = "hb_members.db"
const val FILENAME_BOARD: String = "hb_board.pdf"
const val FILENAME_REPORT: String = "hb_report.pdf"


//=== global class ===

class ClickableNameTextView : AppCompatTextView {
	
	var baseName: String? = null
	
	// constructor
	constructor(context: Context) : super(context)
	
	constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
	
	override fun setText(text: CharSequence, type: BufferType) {
		val allText: String = text.toString()
		val span = SpannableString(allText)
		
		var fromIndex = 0
		var toIndex: Int
		while (allText.indexOf(", ", fromIndex) >= 0) {
			toIndex = allText.indexOf(", ", fromIndex)
			makeNameSpan(span, allText, fromIndex, toIndex)
			fromIndex = toIndex + 2
		}
		
		toIndex = text.length
		if (toIndex > fromIndex) {
			makeNameSpan(span, allText, fromIndex, toIndex)
		}
		
		fromIndex = 0
		while (allText.indexOf("(고)", fromIndex) >= 0) {
			fromIndex = allText.indexOf("(고)", fromIndex)
			toIndex = fromIndex + 3
			span.setSpan(
				RelativeSizeSpan(0.8f),
				fromIndex,
				toIndex,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			span.setSpan(
				ForegroundColorSpan(Color.BLACK),
				fromIndex,
				toIndex,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			fromIndex = toIndex
		}
		
		highlightColor = ContextCompat.getColor(gMainActivity, R.color.search_highlight)
		movementMethod = LinkMovementMethod.getInstance()
		
		super.setText(span, BufferType.SPANNABLE)
	}
	
	private fun makeNameSpan(span: SpannableString, allText: String, fromIndex: Int, toIndex: Int) {
		val clickSpan = ClickableNameSpan(allText.substring(fromIndex, toIndex))
		span.setSpan(clickSpan, fromIndex, toIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		val currentName = allText.substring(fromIndex, toIndex)
		if ((baseName != null) && (baseName == currentName)) {
			span.setSpan(ForegroundColorSpan(Color.rgb(0, 0, 160)),
				fromIndex, toIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			span.setSpan(UnderlineSpan(), fromIndex, toIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
	}
	
	// sub class
	private inner class ClickableNameSpan(var mName: String) : ClickableSpan() {
		
		override fun onClick(view: View) {
			soundClick.startOnOff()
			MainActivity.notifyPendingName(mName)
			findNavController().navigate(R.id.nav_member)
		}
		
		override fun updateDrawState(ds: TextPaint) {
			ds.color = Color.BLACK
			ds.isUnderlineText = false
		}
	}
}