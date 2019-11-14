package com.lgsdiamond.hbmembers.ui.member

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.kakao.kakaolink.v2.KakaoLinkResponse
import com.kakao.kakaolink.v2.KakaoLinkService
import com.kakao.message.template.LinkObject
import com.kakao.message.template.TextTemplate
import com.kakao.network.ErrorResult
import com.kakao.network.callback.ResponseCallback
import com.kakao.util.KakaoParameterException
import com.kakao.util.helper.log.Logger
import com.lgsdiamond.hbmembers.*
import com.lgsdiamond.hbmembers.LgsUtility.Companion.contentFace
import com.lgsdiamond.hbmembers.LgsUtility.Companion.correctCompanyName
import com.lgsdiamond.hbmembers.LgsUtility.Companion.getSpannedCompany
import com.lgsdiamond.hbmembers.LgsUtility.Companion.titleFace
import com.lgsdiamond.hbmembers.MainActivity.Companion.sInstalled_KAKAOTalk
import kotlinx.android.synthetic.main.custom_memo_dialog.*
import kotlinx.android.synthetic.main.fragment_member.*
import kotlinx.android.synthetic.main.member_pic_dialog.*
import java.util.*

class MemberFragment : Fragment() {
	
	private lateinit var memberViewModel: MemberViewModel
	private var lastFoundName: String = ""
	
	init {
		gActivity.memberFragment = this
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		memberViewModel =
			ViewModelProviders.of(this).get(MemberViewModel::class.java)
		return inflater.inflate(R.layout.fragment_member, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		val helpCount = gActivity.readHelpCount()
		if (helpCount < 20) {
			when (gRandom.nextInt(4)) {
				0    -> LgsUtility.showToastShort("회원 이름을 누르면 해당 회원의 정보를 볼 수 있습니다.")
				1    -> LgsUtility.showToastShort("아래 버튼을 클릭하여 다양한 동기회 정보를 볼 수 있습니다.")
				2    -> LgsUtility.showToastShort("사진 버튼을 클릭하여 회원의 사진을 볼 수 있습니다.")
				else -> LgsUtility.showToastShort("연락처와 메모를 수정하여 저장할 수 있습니다.")
			}
			gActivity.writeHelpCount(helpCount + 1)
		}
		
		initFragmentUI(view)
	}
	
	
	// ui
	private fun initFragmentUI(view: View) {
		
		//----------------------------------------------
		if (!gActivity.confirmRegistered()) return
		//----------------------------------------------
		
		gActivity.showNavActionButtons()
		
		loMemberMain.setOnClickListener { LgsUtility.showSoftKeyboard(false) }
		
		txtCompany_34_pic.setOnClickListener { showCompanyPic() }
		txtCompany_34.setOnClickListener { showCompanyPic() }
		labCompany_34.setOnClickListener { showCompanyPic() }
		
		svMemberInfo.setOnClickListener { showMemberPic() }
		loMemberInfo.setOnClickListener { showMemberPic() }
		
		btnMemberPic.setOnClickListener {
			soundTick.startOnOff()
			showMemberPic()
		}
		
		txtMemberMemo.setOnClickListener { changeMemberMemo() }
		
		btnContact.setOnClickListener {
			soundTick.startOnOff()
			changeContactNumber()
		}
		
		txtMemberMemo.typeface = titleFace
		btnMemberMemo.setOnClickListener {
			soundTick.startOnOff()
			changeMemberMemo()
		}
		
		btnSearch.typeface = titleFace
		btnSearch.setOnClickListener { v ->
			soundTick.startOnOff()
			
			LgsUtility.showSoftKeyboard(false)
			val strName = edtFindName.text.toString().removeWhitespaces()
			var strContact = edtContact.text.toString().removeWhitespaces()
			
			strContact = strContact.onlyDigits()
			
			if (strName.isEmpty() && (strContact.length < 4)) {
				"검색할 전화번호는 최소 4자리 이상이어야 합니다.".toSnackBarOK(edtContact)
			} else {
				if (strContact.length < 4) strContact = ""        // not eligible
				val info = updateMemberInfo(v, strName, strContact)
				lastFoundName = info?.mName ?: ""
				if (info == null) {
					"검색 조건에 맞는 회원이 없습니다.".toSnackBarOK(edtContact)
				}
			}
		}
		
		btnCall.setOnClickListener {
			val permitted = gActivity.confirmPermission(Manifest.permission.CALL_PHONE)
			val number = LgsUtility.getPhoneNumber(edtContact.text.toString())
			if (number.contains("+")) {
				val builder = AlertDialog.Builder(activity)
					.setTitle("국제전화 확인".toTitleFace())
					.setMessage("$number 은(는) 국제전화입니다. 전화할까요?".toTitleFace())
					.setPositiveButton("예".toTitleFace()) { _, _ ->
						LgsUtility.phoneCall(edtContact.text.toString(), permitted)
					}
					.setNegativeButton("아니요".toTitleFace(), null)
				
				val dialog = builder.create()
				dialog.setIcon(R.drawable.ic_phone_call)
				dialog.show()
			} else {
				LgsUtility.phoneCall(edtContact.text.toString(), permitted)
			}
		}
		
		btnSMS.setOnClickListener {
			val permitted = gActivity.confirmPermission(Manifest.permission.SEND_SMS)
			val number = LgsUtility.getPhoneNumber(edtContact.text.toString())
			
			val name = edtFindName.text.toString()
			val edtMessage =
				EditText(ContextThemeWrapper(activity, R.style.dialog_input_box), null, 0)
			edtMessage.hint = "보낼 문자 메시지를 작성하세요"
			
			var title = "문자 메시지 전송"
			if (number.contains("+")) title = "$title(국제전화)"
			
			val builder = AlertDialog.Builder(activity)
				.setTitle(title.toTitleFace())
				.setView(edtMessage)
				.setMessage("$name 회원에게 전송할 메시지를 입력하세요.".toTitleFace())
				.setPositiveButton("보내기".toTitleFace()) { _, _ ->
					var msg = edtMessage.text.toString().trim { it <= ' ' }
					if (msg.isEmpty()) {
						"보낼 메시지 내용이 없습니다.".toToastTitle()
					} else {
						msg = "[한백]\n\"$msg"
						LgsUtility.sendSMS(number, msg, permitted)
					}
				}
				.setNegativeButton("취소".toTitleFace(), null)
			
			val dialog = builder.create()
			dialog.setIcon(R.drawable.ic_send_sms)
			dialog.show()
		}
		
		btnKakaoLink.setOnClickListener {
			try {
				sendKakaoMessage()
			} catch (e: KakaoParameterException) {
				e.printStackTrace()
				LgsUtility.openAndroidApp(
					MainActivity.KAKAOTALK_PACKAGE_ID,
					MainActivity.KAKAOTALK_PACKAGE_NAME
				)
			}
		}
		
		if (!sInstalled_KAKAOTalk) {
			btnKakaoLink.visibility = View.INVISIBLE
		}
		
		edtFindName.setOnFocusChangeListener { _, hasFocus ->
			if (hasFocus) {
				LgsUtility.showSoftKeyboard(true)
			} else {
				LgsUtility.showSoftKeyboard(false)
			}
		}
		
		// search button on keyboard
		edtFindName.setOnEditorActionListener { _, id, _ ->
			if (id == EditorInfo.IME_ACTION_SEARCH) {
				btnSearch.performClick()
				true
			} else
				false
		}
		
		edtFindName.typeface = contentFace
		txtCompany_12_all.typeface = contentFace
		txtCompany_34_all.typeface = contentFace
		txtSchool_all.typeface = contentFace
		txtMajor_all.typeface = contentFace
		txtBranch_all.typeface = contentFace
		
		edtContact.typeface = titleFace
		txtReference.typeface = titleFace
		
		// search button on keyboard
		edtContact.setOnEditorActionListener { _, id, _ ->
			if (id == EditorInfo.IME_ACTION_SEARCH) {
				btnSearch.performClick()
				true
			} else
				false
		}
		
		edtFindName.hint = "회원 이름"
		
		populateMemberInfo(null)
		
		edtFindName.clearFocus()
		
		// Pending task
		doPendingTask()
	}
	
	
	// member handling
	private fun searchMemberByName(name: String) {
		edtFindName.setText(name)
		btnSearch.performClick()
	}
	
	private fun doPendingTask() {
		
		val name = MainActivity.popupPendingName()
		
		if (name.isNotEmpty()) {
			searchMemberByName(name)
			MainActivity.emptyPendingName()
		}
	}
	
	// show pic
	private fun showCompanyPic() {
		val company = txtCompany_34.text.toString().trim()
		val listString = company.split(" ")
		showCompanyPic(listString[1])
	}
	
	fun showCompanyPic(company: String) {
		soundTick.startOnOff()
		
		val innerView = layoutInflater.inflate(R.layout.member_pic_dialog, null)
		val dialog = Dialog(gActivity)
		
		dialog.setContentView(innerView)
		dialog.setTitle("<$company> 사진".toTitleFace())
		dialog.setCancelable(true)
		dialog.btnCloseMemberPhoto.text = "닫기: <$company>회원 사진".toTitleFace()
		dialog.btnCloseMemberPhoto.setOnClickListener {
			soundClick.startOnOff()
			dialog.hide()
		}
		
		val params = dialog.window?.attributes
		params!!.width = ViewGroup.LayoutParams.MATCH_PARENT
		params.height = ViewGroup.LayoutParams.MATCH_PARENT
		dialog.window?.attributes = params
		
		val path = "pic_company/$company.jpg"
		val drawable = dialog.pvMemberPhoto.loadDrawableFromAssets(path)
		if (drawable == null) {
			"<$company> 사진을 찾을 수 없습니다".toToastTitle()
		} else {
			dialog.show()
		}
	}
	
	private fun showMemberPic() {
		soundTick.startOnOff()
		
		val name = edtFindName.text.toString().trim()
		val innerView = layoutInflater.inflate(R.layout.member_pic_dialog, null)
		val dialog = Dialog(gActivity)
		
		dialog.setContentView(innerView)
		dialog.setTitle("<$name>회원 사진".toTitleFace())
		dialog.setCancelable(true)
		dialog.btnCloseMemberPhoto.text = "닫기: <$name>회원 사진".toTitleFace()
		dialog.btnCloseMemberPhoto.setOnClickListener {
			soundClick.startOnOff()
			dialog.hide()
		}
		
		val params = dialog.window?.attributes!!
		params.width = ViewGroup.LayoutParams.MATCH_PARENT
		params.height = ViewGroup.LayoutParams.MATCH_PARENT
		dialog.window?.attributes = params
		
		dialog.loMemberPic.setBackgroundColor(if (name.isAbsent()) Color.LTGRAY else Color.WHITE)
		
		val path = "pic_member/$name.jpg"
		val drawable = dialog.pvMemberPhoto.loadDrawableFromAssets(path)
		if (drawable == null) {
			"<$name> 사진을 찾을 수 없습니다".toToastTitle()
		} else {
			dialog.show()
		}
	}
	
	// change ui value
	private fun createChangeDialog(
		title: String, labOld: String, oldContent: String,
		labNew: String, hintNew: String,
		btnSaveTitle: String
	): Dialog {
		val innerView = layoutInflater.inflate(R.layout.custom_memo_dialog, null)
		val dialog = Dialog(gActivity)
		dialog.setContentView(innerView)
		dialog.setTitle(title.toTitleFace())
		dialog.setCancelable(true)
		
		dialog.labOldContent.typeface = titleFace
		dialog.labNewContent.typeface = titleFace
		dialog.btnSaveNewContent.typeface = titleFace
		dialog.btnCancelContent.typeface = titleFace
		
		dialog.txtOldContent.typeface = contentFace
		dialog.edtNewContent.typeface = contentFace
		
		dialog.txtOldContent.text = oldContent
		if (oldContent.isEmpty()) {
			dialog.labOldContent.visibility = View.GONE
			dialog.txtOldContent.visibility = View.GONE
		} else {
			dialog.labOldContent.visibility = View.VISIBLE
			dialog.txtOldContent.visibility = View.VISIBLE
		}
		
		dialog.labNewContent.text = labNew
		dialog.edtNewContent.setText(oldContent)
		dialog.edtNewContent.setSelection(oldContent.length)
		dialog.btnSaveNewContent.text = btnSaveTitle
		
		val params = dialog.window?.attributes
		params!!.width = ViewGroup.LayoutParams.MATCH_PARENT
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT
		dialog.window?.attributes = params
		
		return dialog
	}
	
	private fun changeMemberMemo() {
		val name = edtFindName.text.toString().trim()
		val oldMemo = txtMemberMemo.text.toString()
		
		val dialog = createChangeDialog(
			"<$name>회원에 대한 메모", "현재 메모", oldMemo,
			"새로운 메모", "새로운 메모를 입력하세요.", "메모 저장"
		)
		dialog.show()
		
		dialog.btnSaveNewContent.setOnClickListener {
			val newMemo = dialog.edtNewContent.text.toString().trim()
			if (newMemo != oldMemo) {
				val pref = gActivity.defPreferences
				if (newMemo.isEmpty()) {
					labMemberMemo.visibility = View.GONE
					txtMemberMemo.visibility = View.GONE
					pref.edit().remove(gActivity.makeMemberMemoKey(name)).apply()
				} else {
					labMemberMemo.visibility = View.VISIBLE
					txtMemberMemo.visibility = View.VISIBLE
					txtMemberMemo.invalidate()
					pref.edit()
						.putString(gActivity.makeMemberMemoKey(name), newMemo)
						.apply()
				}
				txtMemberMemo.text = newMemo
			}
			dialog.hide()
		}
		
		dialog.btnCancelContent.setOnClickListener { dialog.hide() }
	}
	
	private fun changeContactNumber() {
		val name = edtFindName.text.toString().trim()
		val oldContact = edtContact.text.toString().trim()
		
		val dialog = createChangeDialog(
			"<$name>회원의 연락처 변경", "현재 연락처", oldContact,
			"새로운 연락처", "새로운 연락처를 입력하세요.", "연락처 저장"
		)
		dialog.edtNewContent.hint = "연락처 입력(빈칸일 경우 DB 값으로 초기화합니다)"
		dialog.show()
		
		dialog.btnSaveNewContent.setOnClickListener { v ->
			val dbContact = gActivity.dbAccess.getMemberContact(name, v)
			val newContact = dialog.edtNewContent.text.toString().trim()
			val pref = gActivity.defPreferences
			if (newContact.isEmpty() || (newContact == dbContact)) {      // means use db contact
				edtContact.setText(dbContact)
				edtContact.setTextColor(Color.BLUE)
				pref.edit().remove(gActivity.makeMemberContactKey(name)).apply()
			} else if (newContact != oldContact) {
				edtContact.setTextColor(Color.BLACK)
				edtContact.setText(newContact)
				pref.edit()
					.putString(gActivity.makeMemberContactKey(name), newContact)
					.apply()
			}
			edtContact.invalidate()
			dialog.hide()
		}
		dialog.btnCancelContent.setOnClickListener {
			dialog.hide()
		}
	}
	
	// populate data
	private fun populateMemberInfo(newInfo: HbDbAccess.MemberInfo?) {
		edtFindName.clearFocus()
		
		if (newInfo == null) {
			
			// hide member info
			svMemberInfo.visibility = View.GONE
			
			// enable to search by name or contact
			edtFindName.setText("")
			edtContact.setText("")
			btnContact.visibility = View.VISIBLE
			btnSearch.visibility = View.VISIBLE
			
			// contact editable
			edtContact.setReadOnly(false, InputType.TYPE_CLASS_PHONE)
			
			// hide other buttons
			btnCall.visibility = View.INVISIBLE
			btnSMS.visibility = View.INVISIBLE
			btnKakaoLink.visibility = View.INVISIBLE
			btnMemberMemo.visibility = View.INVISIBLE
			btnMemberPic.visibility = View.INVISIBLE
		} else {
			// show member info
			svMemberInfo.visibility = View.VISIBLE
			
			// show buttons
			btnCall.visibility = View.VISIBLE
			btnSMS.visibility = View.VISIBLE
			btnKakaoLink.visibility = View.VISIBLE
			btnMemberMemo.visibility = View.VISIBLE
			btnMemberPic.visibility = View.VISIBLE
			
			btnContact.visibility = View.VISIBLE
			edtContact.visibility = View.VISIBLE
			
			// contact readOnly
			edtContact.setReadOnly(true)
			
			val pref = gActivity.defPreferences
			
			var contact = pref.getString(gActivity.makeMemberContactKey(newInfo.mName), "")
			if (contact!!.isNotEmpty() && (contact == newInfo.mContact)) {
				contact = ""
				pref.edit()
					.putString(gActivity.makeMemberContactKey(newInfo.mName), "")
					.apply()
			}
			
			if (contact.isEmpty()) {
				pref.edit().remove(gActivity.makeMemberContactKey(newInfo.mName)).apply()
				edtContact.setText(newInfo.mContact)
				edtContact.setTextColor(Color.BLUE)
			} else {
				edtContact.setText(contact)
				edtContact.setTextColor(Color.BLACK)
			}
			
			contact = edtContact.text.toString()
			
			if (contact.isEmpty()) {
				btnContact.visibility = View.GONE
				edtContact.visibility = View.GONE
			} else {
				btnContact.visibility = View.VISIBLE
				edtContact.visibility = View.VISIBLE
			}
			
			if (contact.isEmpty() || contact.contains("?")) {
				btnCall.visibility = View.INVISIBLE
				btnSMS.visibility = View.INVISIBLE
				btnKakaoLink.visibility = View.INVISIBLE
			} else {
				btnCall.visibility = View.VISIBLE
				btnSMS.visibility = View.VISIBLE
				if (sInstalled_KAKAOTalk) {
					btnKakaoLink.visibility = View.VISIBLE
				}
			}
			
			edtFindName.setText(newInfo.mName)
			edtFindName.setSelection(newInfo.mName.length)
			
			labCompany_12.visibility = View.VISIBLE
			txtCompany_12.visibility = View.VISIBLE
			txtCompany_12.text = getSpannedCompany(correctCompanyName(newInfo.mCompany12, true))
			
			if (newInfo.mCompany12All.isEmpty()) {
				txtCompany_12_all.visibility = View.GONE
			} else {
				txtCompany_12_all.visibility = View.VISIBLE
				txtCompany_12_all.baseName = newInfo.mName
				txtCompany_12_all.setText(newInfo.mCompany12All, TextView.BufferType.SPANNABLE)
			}
			
			if (newInfo.mCompany34.isEmpty()) {
				labCompany_34.visibility = View.GONE
				txtCompany_34.visibility = View.GONE
				txtCompany_34_pic.visibility = View.GONE
			} else {
				labCompany_34.visibility = View.VISIBLE
				txtCompany_34.visibility = View.VISIBLE
				txtCompany_34.text = getSpannedCompany(correctCompanyName(newInfo.mCompany34, true))
				txtCompany_34_pic.visibility = View.VISIBLE
			}
			
			if (newInfo.mCompany34All.isEmpty()) {
				txtCompany_34_all.visibility = View.GONE
			} else {
				txtCompany_34_all.visibility = View.VISIBLE
				txtCompany_34_all.baseName = newInfo.mName
				txtCompany_34_all.text = newInfo.mCompany34All
			}
			
			if (newInfo.mSchool.isEmpty()) {
				labSchool.visibility = View.GONE
				txtSchool.visibility = View.GONE
			} else {
				labSchool.visibility = View.VISIBLE
				txtSchool.visibility = View.VISIBLE
				txtSchool.text = newInfo.mSchool
			}
			
			if (newInfo.mSchoolAll.isEmpty()) {
				txtSchool_all.visibility = View.GONE
			} else {
				txtSchool_all.visibility = View.VISIBLE
				txtSchool_all.baseName = newInfo.mName
				txtSchool_all.text = newInfo.mSchoolAll
			}
			
			if (newInfo.mMajor.isEmpty()) {
				labMajor.visibility = View.GONE
				txtMajor.visibility = View.GONE
			} else {
				labMajor.visibility = View.VISIBLE
				txtMajor.visibility = View.VISIBLE
				txtMajor.text = newInfo.mMajor
			}
			
			if (newInfo.mMajorAll.isEmpty()) {
				txtMajor_all.visibility = View.GONE
			} else {
				txtMajor_all.visibility = View.VISIBLE
				txtMajor_all.baseName = newInfo.mName
				txtMajor_all.text = newInfo.mMajorAll
			}
			
			if (newInfo.mBranch.isEmpty()) {
				labBranch.visibility = View.GONE
				txtBranch.visibility = View.GONE
			} else {
				labBranch.visibility = View.VISIBLE
				txtBranch.visibility = View.VISIBLE
				txtBranch.text = newInfo.mBranch
			}
			
			if (newInfo.mBranchAll.isEmpty()) {
				txtBranch_all.visibility = View.GONE
			} else {
				txtBranch_all.visibility = View.VISIBLE
				txtBranch_all.baseName = newInfo.mName
				txtBranch_all.text = newInfo.mBranchAll
			}
			
			if (newInfo.mReference.isEmpty()) {
				labReference.visibility = View.GONE
				txtReference.visibility = View.GONE
			} else {
				labReference.visibility = View.VISIBLE
				txtReference.visibility = View.VISIBLE
				txtReference.text = newInfo.mReference
			}
			
			val memo = pref.getString(gActivity.makeMemberMemoKey(newInfo.mName), "")
			txtMemberMemo.typeface = titleFace
			if (memo!!.isEmpty()) {
				labMemberMemo.visibility = View.GONE
				txtMemberMemo.visibility = View.GONE
			} else {
				labMemberMemo.visibility = View.VISIBLE
				txtMemberMemo.text = memo
				txtMemberMemo.visibility = View.VISIBLE
			}
		}
		
		LgsUtility.animateCenterScale(loMemberMain)
	}
	
	fun updateMemberInfo(v: View?,
						 name: String,
						 contact: String = ""): HbDbAccess.MemberInfo? {
		
		var member: HbDbAccess.MemberInfo? = null
		if (contact.isNotEmpty())
			member = gActivity.dbAccess.getMemberInfoByContact(contact, v)
		if ((member == null) && name.isNotEmpty())
			member = gActivity.dbAccess.getMemberInfoByName(name, v)
		
		populateMemberInfo(member)
		return member
	}
	
	// kakao
	private fun sendKakaoMessage() {
		try {
			val edtMessage =
				EditText(ContextThemeWrapper(activity, R.style.dialog_input_box), null, 0)
			edtMessage.hint = "공유할 메시지"
			val builder = AlertDialog.Builder(activity)
				.setTitle("카카오톡에서 메시지 공유".toTitleFace())
				.setView(edtMessage)
				.setMessage("공유할 메시지를 입력하세요.(공유할 대상은 카카오톡에서 정합니다.)".toTitleFace())
				.setPositiveButton("공유하기".toTitleFace()) { _, _ ->
					val msg = edtMessage.text.toString().trim { it <= ' ' }
					if (msg.isEmpty()) {
						LgsUtility.showToastShort("공유할 메시지 내용이 없습니다.")
					} else {
						try {
							val params: TextTemplate? = TextTemplate.newBuilder(
								msg,
								LinkObject.newBuilder().setWebUrl("https://developers.kakao.com").setMobileWebUrl(
									"https://developers.kakao.com"
								).build()
							)
								.setButtonTitle("한백인 앱으로 돌아가기").build()
							KakaoLinkService.getInstance().sendDefault(gActivity,
								params, serverCallbackArgs,
								object : ResponseCallback<KakaoLinkResponse?>() {
									override fun onFailure(errorResult: ErrorResult) {
										Logger.e(errorResult.toString())
									}
									
									override fun onSuccess(result: KakaoLinkResponse?) {
									}
								})
						} catch (e: Exception) {
						}
						finally {
						}
					}
				}
				.setNegativeButton("취소".toTitleFace(), null)
			
			val dialog = builder.create()
			dialog.setIcon(R.drawable.ic_kakao_link)
			dialog.show()
		} catch (e: KakaoParameterException) {
			e.printStackTrace()
			LgsUtility.openAndroidApp(
				MainActivity.KAKAOTALK_PACKAGE_ID,
				MainActivity.KAKAOTALK_PACKAGE_NAME
			)
		}
	}
	
	private val serverCallbackArgs: Map<String, String> = getServerCallbackArgs()
	private fun getServerCallbackArgs(): Map<String, String> {
		val callbackParameters: Map<String, String> = HashMap()
		//		callbackParameters.put("user_id", "1234")
		//		callbackParameters.put("title", "프로방스 자동차 여행 !@#$%")
		return callbackParameters
	}
}