package com.lgsdiamond.hbmembers.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.lgsdiamond.hbmembers.*
import com.lgsdiamond.hbmembers.HbUtility.Companion.isMatchingName
import com.lgsdiamond.hbmembers.MainActivity.Companion.ADMIN_NAME
import com.lgsdiamond.hbmembers.MainActivity.Companion.ADMIN_NUMBER
import com.lgsdiamond.hbmembers.MainActivity.Companion.ADMIN_REAL_NAME
import com.lgsdiamond.hbmembers.MainActivity.Companion.setRegistered
import com.lgsdiamond.lgsutility.LgsUtility.Companion.showSoftKeyboard
import com.lgsdiamond.lgsutility.LgsUtility.Companion.showToastShort
import com.lgsdiamond.lgsutility.removeWhitespaces
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment() {
	
	private lateinit var loginViewModel: LoginViewModel
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		loginViewModel =
			ViewModelProviders.of(this).get(LoginViewModel::class.java)
		return inflater.inflate(R.layout.fragment_login, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		initFragmentUI()
	}
	
	private fun initFragmentUI() {
		
		gActivity.hideActionButtons()
		
		loginPhoto.scaleType = ImageView.ScaleType.FIT_XY
		
		edtUserNumber.setText(MainActivity.getRegisteredNumber())
		edtUserName.setText(MainActivity.getRegisteredName())
		
		btnRegister.setOnClickListener {
			val number = edtUserNumber.text.toString().removeWhitespaces()
			var nameInput = edtUserName.text.toString().removeWhitespaces()
			
			if (number.contentEquals(ADMIN_NUMBER) && nameInput.contentEquals(ADMIN_NAME)) {
				nameInput = ADMIN_REAL_NAME
			}
			
			if (number.length == 4 && nameInput.length > 1) {     // not too short
				val nameOutput = gActivity.dbAccess.getNameFromNumber(number)
				if (!nameInput.contains("(고)") && isMatchingName(nameInput, nameOutput)) {
					gActivity.setRegisteredUser(number, nameOutput)
					
					edtUserNumber.setText(number)
					edtUserName.setText(nameOutput)
					setRegistered(true)
					
					showToastShort("\"$nameOutput\"(으)로 로그인되었습니다.")
					
					soundOpening.startOnOff()
					MainActivity.notifyPendingName(nameOutput)
					findNavController().navigate(R.id.nav_member)
					
					
				} else {
					gActivity.setRegisteredUser(number, "")
					setRegistered(false)
					showToastShort("유효한 회원 정보가 아닙니다.")
					soundSliding.startOnOff()
				}
			} else {
				showToastShort("입력내용이 적합하지 않습니다.")
				soundSliding.startOnOff()
			}
			showSoftKeyboard(false)
		}
		
		edtUserName.hint = "이름"
	}
}