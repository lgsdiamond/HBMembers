package com.lgsdiamond.hbmembers.ui.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.lgsdiamond.hbmembers.*
import kotlinx.android.synthetic.main.fragment_pdf.*

abstract class PdfFragment : Fragment() {
	
	protected var fragmentID: Int = 0
	private lateinit var pdfViewModel: PdfViewModel
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		pdfViewModel =
			ViewModelProviders.of(this).get(PdfViewModel::class.java)
		return inflater.inflate(R.layout.fragment_pdf, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		initFragmentUI(view)
	}
	
	private fun initFragmentUI(view: View) {
		//----------------------------------------------
		if (!gMainActivity.confirmRegistered()) return
		//----------------------------------------------
		
		val pdfName = when (fragmentID) {
			R.id.nav_board  -> FILENAME_BOARD
			R.id.nav_report -> FILENAME_REPORT
			else            -> ""
		}
		
		pdfHolder.fromAsset(pdfName)
			.swipeHorizontal(true)
			.pageFitPolicy(FitPolicy.BOTH)
			.load()
		
		LgsUtility.animateCenterScale(view)
	}
}