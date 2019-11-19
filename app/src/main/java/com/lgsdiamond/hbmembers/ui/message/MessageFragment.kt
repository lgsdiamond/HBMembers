package com.lgsdiamond.hbmembers.ui.message

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.lgsdiamond.hbmembers.HbDbAccess
import com.lgsdiamond.hbmembers.R
import com.lgsdiamond.hbmembers.gActivity
import com.lgsdiamond.lgslibrary.LgsUtility.Companion.sendSMS
import com.lgsdiamond.lgslibrary.LgsUtility.Companion.showSoftKeyboard
import com.lgsdiamond.lgslibrary.LgsUtility.Companion.titleFace
import com.lgsdiamond.lgslibrary.toToastTitle
import kotlinx.android.synthetic.main.fragment_message.*

class MessageFragment : Fragment() {
	
	private lateinit var messageViewModel: MessageViewModel
	private lateinit var dbAccess: HbDbAccess
	private lateinit var mMemberToWhom: HbDbAccess.MemberInfo
	
	private lateinit var mInfoSecretary: HbDbAccess.MemberInfo
	private lateinit var mInfoContact: HbDbAccess.MemberInfo
	private lateinit var mInfoEditor: HbDbAccess.MemberInfo
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		messageViewModel =
			ViewModelProviders.of(this).get(MessageViewModel::class.java)
		return inflater.inflate(R.layout.fragment_message, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		initFragmentUI(view)
	}
	
	private fun initFragmentUI(view: View) {
		
		dbAccess = gActivity.dbAccess
		
		rgToWhom.setOnCheckedChangeListener { group, checkedId ->
			when (checkedId) {
				R.id.rbSecretary -> mMemberToWhom = mInfoSecretary
				R.id.rbContact   -> mMemberToWhom = mInfoContact
				R.id.rbEditor    -> mMemberToWhom = mInfoEditor
			}
		}
		
		mInfoSecretary = dbAccess.getInfoByTitle(rbSecretary.text.toString())!!
		rbSecretary.text = "${rbSecretary.text}(${mInfoSecretary.mName})"
		
		mInfoContact = dbAccess.getInfoByTitle(rbContact.text.toString())!!
		rbContact.text = "${rbContact.text}(${mInfoContact.mName})"
		
		mInfoEditor = dbAccess.getInfoByTitle(rbEditor.text.toString())!!
		rbEditor.text = "${rbEditor.text}(${mInfoEditor.mName})"
		
		btnSendMessage.typeface = titleFace
		btnSendMessage.setOnClickListener { _ ->
			val permitted = gActivity.confirmPermission(Manifest.permission.SEND_SMS)
			showSoftKeyboard(false)
			var message = edtMessage.text.toString()
			message = message.trim { it <= ' ' }
			
			if (message.isEmpty()) {
				"보낼 메시지 내용이 없습니다.".toToastTitle()
			} else {
				message = "[한백]\n\"$message"
				sendSMS(mMemberToWhom.mContact, message, permitted)
			}
		}
		
		rbSecretary.isChecked = true
		edtMessage.hint = "메시지를 여기에 입력하세요."
	}
}