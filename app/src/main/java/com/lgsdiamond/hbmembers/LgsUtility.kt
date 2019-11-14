package com.lgsdiamond.hbmembers

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.telephony.SmsManager
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.View
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.snack_bar_ok.*

/**
 * Created by LgsDiamond on 2018-02-18.
 */
class LgsUtility {
	companion object {
		private lateinit var sActivity: MainActivity
		
		
		lateinit var titleFace: Typeface
		lateinit var contentFace: Typeface
		
		internal fun initUtility(activity: MainActivity) {
			sActivity = activity
			
			titleFace = ResourcesCompat.getFont(sActivity, R.font.title)!!
			contentFace = ResourcesCompat.getFont(sActivity, R.font.content)!!
		}
		
		private var sCompanyName = arrayOf(
			"광개토", "재구", "을지", "진흥", "무열", "유신",
			"관창", "문무", "인헌", "최영", "세종", "율곡", "권율", "충무", "홍의", "충용"
		)
		
		fun showSoftKeyboard(toShow: Boolean) {
			val imm = sActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			var view = sActivity.currentFocus
			if (view == null) {
				view = View(sActivity)
			}
			
			if (toShow) {
				view.isFocusableInTouchMode = true
				view.requestFocus()
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
			} else {
				
				imm.hideSoftInputFromWindow(view.windowToken, 0)
			}
		}
		
		private fun onlyNumericChars(str: String): String {
			return str.replace("[^0-9]".toRegex(), "")
		}
		
		fun getPhoneNumber(number: String): String {
			var str = number
			str = str.trim { it <= ' ' }
			
			val overseaCode = charArrayOf(0.toChar())
			str.toCharArray(overseaCode, 0, 0, 1)
			str = onlyNumericChars(str)
			if (overseaCode[0] == '+') str = "+$str"
			
			return str
		}
		
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
		
		fun installedPackage(packageID: String): Boolean {
			var isExist = false
			
			val pkgMgr = gMainActivity.packageManager
			val mApps: List<ResolveInfo>
			val mainIntent = Intent(Intent.ACTION_MAIN, null)
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
			mApps = pkgMgr.queryIntentActivities(mainIntent, 0)
			
			try {
				for (i in mApps.indices) {
					if (mApps[i].activityInfo.packageName.startsWith(packageID)) {
						isExist = true
						break
					}
				}
			} catch (e: Exception) {
				isExist = false
			}
			
			return isExist
		}
		
		fun showToastShort(message: String) {
			Toast.makeText(sActivity, message, Toast.LENGTH_SHORT).show()
		}
		
		fun showToastLong(message: String) {
			Toast.makeText(sActivity, message, Toast.LENGTH_LONG).show()
		}
		
		
		// call & SMS
		fun sendSMS(number: String, message: String, permit: Boolean) {
			soundMessage.startOnOff()
			
			// number should be already qualified
			val uri = Uri.parse("smsto:$number")
			
			if (permit) {
				val sentIntent = PendingIntent.getBroadcast(
					gMainActivity,
					0, Intent("SMS_SENT_ACTION"), 0
				)
				val deliveredIntent = PendingIntent.getBroadcast(
					gMainActivity,
					0, Intent("SMS_DELIVERED_ACTION"), 0
				)
				
				gMainActivity.registerReceiver(object : BroadcastReceiver() {
					override fun onReceive(context: Context, intent: Intent) {
						when (resultCode) {
							Activity.RESULT_OK                      ->
								"성공: 메세지 전송이 완료되었습니다.".toToastTitle()
							SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
								"실패: SMS 전송가 전송되지 않았습니다.".toToastTitle()
							SmsManager.RESULT_ERROR_NO_SERVICE      ->
								"실패: 서비스 지역이 아닙니다".toToastTitle()
							SmsManager.RESULT_ERROR_RADIO_OFF       ->
								"실패: 무선(Radio)가 꺼져있습니다".toToastTitle()
							SmsManager.RESULT_ERROR_NULL_PDU        ->
								"실패: PDU Null".toToastTitle()
						}
					}
				}, IntentFilter("SMS_SENT_ACTION"))
				
				gMainActivity.registerReceiver(object : BroadcastReceiver() {
					override fun onReceive(context: Context, intent: Intent) {
						when (resultCode) {
							Activity.RESULT_OK       ->
								"SMS 도착 완료".toToastTitle()
							Activity.RESULT_CANCELED ->
								"SMS 도착 실패".toToastTitle()
						}
					}
				}, IntentFilter("SMS_DELIVERED_ACTION"))
				
				val manager = SmsManager.getDefault()
				manager.sendTextMessage(
					uri.toString(), null, message,
					sentIntent, deliveredIntent
				)
			} else {
				val it = Intent(Intent.ACTION_SENDTO, uri)
				it.putExtra("sms_body", message)
				sActivity.startActivity(it)
			}
		}
		
		fun phoneCall(number: String, permit: Boolean) {
			// number should be already qualified
			val uri = Uri.parse("tel:$number")
			val intent = Intent(if (permit) Intent.ACTION_CALL else Intent.ACTION_DIAL)
			intent.data = uri
			try {
				sActivity.startActivity(intent)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		
		fun openAndroidApp(packageID: String, packageName: String) {
			if (installedPackage(packageID)) {
				val intent = sActivity.packageManager.getLaunchIntentForPackage(packageID)
				intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				sActivity.startActivity(intent)
			} else {
				showToastShort("\"" + packageName + "앱이 설치되지 않았습니다.")
			}
		}
		
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
		
		fun countCommaItems(items: String): Int {
			if (items.isEmpty()) return 0
			
			var counter = 0
			for (element in items) {
				if (element == ',') {
					counter++
				}
			}
			return (counter + 1)
		}
		
		//===
		fun getResString(id: Int): String {
			return sActivity.resources.getString(id)
		}
		
		// animations
		fun animateCenterScale(view: View) {
			val set = AnimationSet(true)
			val scale = ScaleAnimation(
				0.8f, 1.0f, 0.8f, 1.0f,
				Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_PARENT, 0.5f
			)
			val alpha = AlphaAnimation(0.0f, 1.0f)
			set.addAnimation(scale)
			set.addAnimation(alpha)
			set.duration = 700L
			set.interpolator = OvershootInterpolator()
			view.startAnimation(set)
		}
		
		fun titleFaceSpan(title: CharSequence): CharSequence {
			val span = SpannableString(title)
			span.setSpan(
				CustomTypefaceSpan("", titleFace),
				0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			return span
		}
		
		fun contentFaceSpan(content: String): CharSequence {
			val span = SpannableString(content)
			span.setSpan(
				CustomTypefaceSpan("", contentFace),
				0, content.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			return span
		}
	}
	
	class CustomTypefaceSpan(family: String, private val newType: Typeface) :
		TypefaceSpan(family) {
		
		override fun updateDrawState(ds: TextPaint) {
			applyCustomTypeFace(ds, newType)
		}
		
		override fun updateMeasureState(paint: TextPaint) {
			applyCustomTypeFace(paint, newType)
		}
		
		private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
			var oldStyle: Int
			val old = paint.typeface
			
			oldStyle = old?.style ?: 0
			
			val fake = oldStyle and tf.style.inv()
			if (fake and Typeface.BOLD != 0) {
				paint.isFakeBoldText = true
			}
			
			if (fake and Typeface.ITALIC != 0) {
				paint.textSkewX = -0.25f
			}
			
			paint.typeface = tf
		}
	}
}