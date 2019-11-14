package com.lgsdiamond.hbmembers

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.lgsdiamond.hbmembers.LgsUtility.Companion.titleFaceSpan
import java.io.InputStream


//=== Menu & MenuItem ===
private fun MenuItem.applyFontToMenuItem(face: Typeface) {
	val mNewTitle = SpannableString(title)
	mNewTitle.setSpan(
		LgsUtility.CustomTypefaceSpan("", face), 0, mNewTitle.length,
		Spannable.SPAN_INCLUSIVE_INCLUSIVE
	)
	title = mNewTitle
}

fun Menu.customFaceMenu(face: Typeface) {
	for (i in 0 until size()) {
		val menuItem = getItem(i)
		
		val subMenu = menuItem.subMenu
		if ((subMenu != null) && subMenu.size() > 0) {
			subMenu.customFaceMenu(face)
		}
		menuItem.applyFontToMenuItem(face)
	}
}


//=== EditText ===
fun EditText.setReadOnly(toReadOnly: Boolean, inputType: Int = InputType.TYPE_NULL) {
	isFocusable = !toReadOnly
	isFocusableInTouchMode = !toReadOnly
	this.inputType = inputType
}

//=== ImageView ===
fun ImageView.loadDrawableFromAssets(path: String): Drawable? {
	var stream: InputStream? = null
	var drawable: Drawable? = null
	try {
		stream = gActivity.assets.open(path)
		drawable = Drawable.createFromStream(stream, null)
		this.setImageDrawable(drawable)
	} catch (ignored: Exception) {
	}
	finally {
		try {
			stream?.close()
		} catch (ignored: Exception) {
		}
	}
	return drawable
}


//=== CharSequence ===
fun CharSequence.toToast() {
	Toast.makeText(gActivity, this, Toast.LENGTH_SHORT).show()
}


//=== String ===
fun String.toToastTitle() {
	Toast.makeText(gActivity, titleFaceSpan(this), Toast.LENGTH_SHORT).show()
}

fun String.toTitleFace(): CharSequence {
	return titleFaceSpan(this)
}

fun String.toSnackBarOK(view: View) {
	Snackbar.make(view, this, Snackbar.LENGTH_LONG)
		.setAction(R.string.ok_confirm, View.OnClickListener {
		})
		.show()
}

fun String.removeWhitespaces(): String {
	return replace("\\s+".toRegex(), "")
}

fun String.onlyDigits(): String {
	return replace(Regex("""[\D]"""), "")
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