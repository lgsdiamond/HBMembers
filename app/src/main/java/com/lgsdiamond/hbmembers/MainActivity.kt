package com.lgsdiamond.hbmembers

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.lgsdiamond.hbmembers.LgsUtility.Companion.getResString
import com.lgsdiamond.hbmembers.ui.member.MemberFragment
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
	val defPreferences: SharedPreferences by lazy { getSharedPreferences(PREF_NAME, 0) }
	
	lateinit var memberDBAccess: DatabaseAccess
	lateinit var memberFragment: MemberFragment
	
	companion object {
		const val TAG = "HBMembers"
		private const val APP_STORE_ADDRESS =
			"https://play.google.com/store/apps/details?id=com.lgsdiamond.hbmembers"
		
		private const val REQUEST_CALL = 0       // should 0, because 1st element in array
		private const val REQUEST_SMS = 1        // should 1, because 2nd element in array
		private const val REQUEST_INITIAL = 2
		private val REQUIRED_PERMISSIONS =
			arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS)
		
		internal const val NAVERBAND_PACKAGE_ID = "com.nhn.android.band"
		internal const val NAVERBAND_PACKAGE_NAME = "네이버밴드(Band)"
		internal const val KAKAOTALK_PACKAGE_ID = "com.kakao.talk"
		internal const val KAKAOTALK_PACKAGE_NAME = "카카오톡(KakaoTalk)"
		internal const val PREF_NAME = "HBSetting"
		internal const val PREF_KEY_NUMBER = "pref_key_number"
		internal const val PREF_KEY_MEMO = "pref_key_memo"
		internal const val PREF_KEY_CONTACT = "pref_key_contact"
		internal const val PREF_KEY_SOUND = "pref_key_sound"
		internal const val PREF_KEY_NAME = "pref_key_name"
		internal const val PREF_HELP_COUNT = "pref_help_count"
		
		internal const val ADMIN_NUMBER = "8147"
		internal const val ADMIN_NAME = "admin"
		internal const val ADMIN_REAL_NAME = "이광석"
		
		var sInstalled_NAVERBand = false
		var sInstalled_KAKAOTalk = false
		
		private var registeredNumber: String = ""
		private var registeredName: String = ""
		private var isRegistered = false
		
		fun getRegisteredNumber() = registeredNumber
		fun getRegisteredName() = registeredName
		fun setRegistered(isOK: Boolean) {
			isRegistered = isOK
		}
		
		private var sPendingName: String = ""
		
		// pending member for memberFragment update
		fun notifyPendingName(name: String) {
			sPendingName = name
		}
		
		fun emptyPendingName() {
			sPendingName = ""
		}
		
		fun popupPendingName(): String = sPendingName
	}
	
	init {
		gMainActivity = this@MainActivity           // Global property
	}
	
	private lateinit var appBarConfiguration: AppBarConfiguration
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		setContentView(R.layout.activity_main)
		
		val toolbar: Toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		
		val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
		val navView: NavigationView = findViewById(R.id.nav_view)
		val navController = findNavController(R.id.nav_host_fragment)
		
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		appBarConfiguration = AppBarConfiguration(
			setOf(
				R.id.nav_member,
				R.id.nav_company_12,
				R.id.nav_company_34,
				R.id.nav_school,
				R.id.nav_major,
				R.id.nav_branch,
				R.id.nav_board,
				R.id.nav_report,
				R.id.nav_message
			), drawerLayout
		)
		setupActionBarWithNavController(navController, appBarConfiguration)
		navView.setupWithNavController(navController)
		
		// Utility initialization
		LgsUtility.initUtility(this)
		
		// Activate DB access
		activateDB()
		
		// initialize UI
		initMainUI()
		
		// initiate activity
		initiateActivity()
	}
	
	private fun initiateActivity() {
		readRegisteredUserName()
		
		if (memberDBAccess.isValidMember(registeredNumber, registeredName)) {
			setRegistered(true)
			LgsUtility.showToastShort("사용자는 \"$registeredName\"입니다.")
			notifyPendingName(registeredName)
			
			soundOpening.startOnOff()
		} else {
			setRegistered(false)
			LgsUtility.showToastShort("로그인되지 않았습니다.")
			
			soundSliding.startOnOff()
			findNavController(R.id.nav_host_fragment).navigate(R.id.action_login)
		}
		setRegisteredUser(registeredNumber, registeredName)
	}
	
	private fun initMainUI() {
		
		gIsSoundOn = defPreferences.getBoolean(PREF_KEY_SOUND, true)
		
		// checking package installed or not
		sInstalled_NAVERBand = LgsUtility.installedPackage(NAVERBAND_PACKAGE_ID)
		sInstalled_KAKAOTalk = LgsUtility.installedPackage(KAKAOTALK_PACKAGE_ID)
		
		// bind global action buttons
		with(findNavController(R.id.nav_host_fragment)) {
			btnCompany_12.setOnClickListener { navigate(R.id.nav_company_12) }
			btnCompany_34.setOnClickListener { navigate(R.id.nav_company_34) }
			btnSchool.setOnClickListener { navigate(R.id.nav_school) }
			btnMajor.setOnClickListener { navigate(R.id.nav_major) }
			btnBranch.setOnClickListener { navigate(R.id.nav_branch) }
			btnBoard.setOnClickListener { navigate(R.id.nav_board) }
			btnReport.setOnClickListener { navigate(R.id.nav_report) }
			btnMessage.setOnClickListener { navigate(R.id.nav_message) }
		}
		
		initialRequestPermissions()
	}
	
	@SuppressLint("RestrictedApi")
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.main, menu)
		
		if (menu is MenuBuilder) {
			menu.setOptionalIconsVisible(true)
		}
		menu.findItem(R.id.action_sound)
			.setIcon(if (gIsSoundOn) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
		
		return true
	}
	
	override fun onSupportNavigateUp(): Boolean {
		val navController = findNavController(R.id.nav_host_fragment)
		return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		when (item.itemId) {
			R.id.action_login     -> return item.onNavDestinationSelected(findNavController(
				R.id.nav_host_fragment))
			R.id.action_sound     -> {
				gIsSoundOn = !gIsSoundOn
				val pref = defPreferences
				pref.edit()
					.putBoolean(PREF_KEY_SOUND, gIsSoundOn)
					.apply()
				
				item.setIcon(if (gIsSoundOn) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
			}
			R.id.action_update    -> updateApp()
			R.id.action_naverband -> LgsUtility.openAndroidApp(NAVERBAND_PACKAGE_ID,
				NAVERBAND_PACKAGE_NAME)
			R.id.action_kakaotalk -> LgsUtility.openAndroidApp(KAKAOTALK_PACKAGE_ID,
				KAKAOTALK_PACKAGE_NAME)
			
			R.id.action_finish    -> finishApp(false)
		}
		return super.onOptionsItemSelected(item)
	}
	
	private fun finishApp(toAsk: Boolean) {
		if (toAsk) {
			val builder = AlertDialog.Builder(this)
				.setTitle(LgsUtility.titleFaceSpan("종료"))
				.setMessage(LgsUtility.titleFaceSpan("\"한백인\"앱을 종료할까요?"))
				.setPositiveButton(LgsUtility.titleFaceSpan("예")) { _, _ ->
					moveTaskToBack(true)
					finish()
					android.os.Process.killProcess(android.os.Process.myPid())
				}
				.setNegativeButton(LgsUtility.titleFaceSpan("아니요"), null)
			
			val dialog = builder.create()
			dialog.setIcon(R.drawable.ic_finish)
			dialog.show()
		} else {
			moveTaskToBack(true)
			finish()
			android.os.Process.killProcess(android.os.Process.myPid())
		}
	}
	
	
	// preferences
	fun readHelpCount(): Int {
		val count = defPreferences.getInt(PREF_HELP_COUNT, 0)
		if (count > 80) {
			defPreferences.edit().putInt(PREF_HELP_COUNT, 0)
				.apply()
		}
		return defPreferences.getInt(PREF_HELP_COUNT, 0)
	}
	
	fun writeHelpCount(count: Int) {
		defPreferences.edit()
			.putInt(PREF_HELP_COUNT, count)
			.apply()
	}
	
	fun makeMemberMemoKey(name: MemberName): String {
		return "${PREF_KEY_MEMO}_$name"
	}
	
	fun makeMemberContactKey(name: MemberName): String {
		return "${PREF_KEY_CONTACT}_$name"
	}
	
	// registered user
	private fun readRegisteredUserName() {
		registeredNumber = defPreferences.getString(PREF_KEY_NUMBER, "0000")!!
		registeredName = defPreferences.getString(PREF_KEY_NAME, "")!!
	}
	
	fun setRegisteredUser(number: String, name: String) {
		registeredNumber = number
		registeredName = name
		
		writeRegisteredUser(number, name)
		
		isRegistered = name.isNotEmpty()
	}
	
	private fun writeRegisteredUser(number: String, name: String) {
		defPreferences.edit()
			.putString(PREF_KEY_NUMBER, number)
			.putString(PREF_KEY_NAME, name)
			.apply()
	}
	
	
	// db access
	private fun activateDB() {
		val dbAccess = DatabaseAccess.getInstance(this)
		
		if (dbAccess == null) {
			"DB 파일이 없습니다".toToastTitle()
			gMainActivity.finishApp(false)
		} else {
			memberDBAccess = dbAccess
			memberDBAccess.open()
		}
	}
	
	
	// backPressed
	override fun onBackPressed() {
		val fm = supportFragmentManager
		val count = fm.backStackEntryCount
		if (count > 0)
			super.onBackPressed() else finishApp(true)
	}
	
	// navigation boxes
	fun showNavActionButtons() {
		loActionButtons?.visibility = View.VISIBLE
	}
	
	fun hideActionButtons() {
		loActionButtons?.visibility = View.GONE
	}
	
	
	// navigation
	fun confirmRegistered(): Boolean {
		if (isRegistered) return true
		
		findNavController(R.id.nav_host_fragment).navigate(R.id.action_login)
		return false
	}
	
	// update
	private fun updateApp() {
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data =
			Uri.parse(APP_STORE_ADDRESS)
		startActivity(intent)
	}
	
	
	// permission
	private fun initialRequestPermissions() {
		ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_INITIAL)
	}
	
	private fun isPermissionGranted(permit: String) =
		(ContextCompat.checkSelfPermission(this, permit) == PackageManager.PERMISSION_GRANTED)
	
	fun confirmPermission(permit: String): Boolean {
		if (isPermissionGranted(permit)) return true       // already granted
		
		val requestCode: Int = when (permit) {
			Manifest.permission.CALL_PHONE -> REQUEST_CALL
			Manifest.permission.SEND_SMS   -> REQUEST_SMS
			else                           -> 99
		}
		
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, permit)) {
			val rationaleResId: Int = when (permit) {
				Manifest.permission.CALL_PHONE -> R.string.permission_call_rationale
				Manifest.permission.SEND_SMS   -> R.string.permission_sms_rationale
				else                           -> 99
			}
			
			if (rationaleResId != 99) {
				val rationale = getResString(rationaleResId)
				Snackbar.make(loActionButtons, rationale, Snackbar.LENGTH_INDEFINITE)
					.setAction(R.string.ok_confirm, View.OnClickListener {
						ActivityCompat.requestPermissions(this, arrayOf(permit), requestCode)
					})
					.show()
			}
		} else {
			ActivityCompat.requestPermissions(this, arrayOf(permit), requestCode)
		}
		return false
	}
	
	private fun notifyPermissionResult(grantResult: Int, grantedResID: Int, deniedResID: Int) {
		Snackbar.make(loActionButtons,
			(if (grantResult == PackageManager.PERMISSION_GRANTED) grantedResID else deniedResID),
			Snackbar.LENGTH_SHORT).show()
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
											grantResults: IntArray) {
		when (requestCode) {
			REQUEST_CALL    -> notifyPermissionResult(grantResults[0],  // 0
				R.string.permission_available_call, R.string.permission_denied_call)
			REQUEST_SMS     -> notifyPermissionResult(grantResults[0],  // 1
				R.string.permission_available_sms, R.string.permission_denied_sms)
			REQUEST_INITIAL -> {                                        // 2
				notifyPermissionResult(grantResults[REQUEST_CALL],      // 0
					R.string.permission_available_call, R.string.permission_denied_call)
				notifyPermissionResult(grantResults[REQUEST_SMS],       // 1
					R.string.permission_available_sms, R.string.permission_denied_sms)
			}
			else            -> super.onRequestPermissionsResult(requestCode,
				permissions, grantResults)
		}
	}
}
