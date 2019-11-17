package com.lgsdiamond.hbmembers.ui.single

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.lgsdiamond.hbmembers.HbDbHelper
import com.lgsdiamond.hbmembers.R
import com.lgsdiamond.hbmembers.gActivity
import com.lgsdiamond.lgsutility.LgsUtility.Companion.contentFace
import com.lgsdiamond.lgsutility.LgsUtility.Companion.countCommaItems
import kotlinx.android.synthetic.main.fragment_single_item.view.*
import java.util.*

abstract class SingleFragment : Fragment() {
	
	protected var fragmentID: Int = 0
	
	private lateinit var singleViewModel: SingleViewModel
	private val dbAccess = gActivity.dbAccess
	private val singleContent = SingleContent()
	private var singleAdapter: SingleViewAdapter? = null
	private var sInitiated = false
	private var mColumnCount = 1
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		singleViewModel =
			ViewModelProviders.of(this).get(SingleViewModel::class.java)
		return inflater.inflate(R.layout.fragment_single, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		initFragmentUI(view)
	}
	
	private fun initFragmentUI(view: View) {
		//----------------------------------------------
		if (!gActivity.confirmRegistered()) return
		//----------------------------------------------
		
		initiateListInfo()
		
		// Set the adapter
		if (view is androidx.recyclerview.widget.RecyclerView) {
			val context = view.getContext()
			if (mColumnCount <= 1) {
				view.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
			} else {
				view.layoutManager =
					androidx.recyclerview.widget.GridLayoutManager(context, mColumnCount)
			}
			singleAdapter = SingleViewAdapter(singleContent.singles)
			view.adapter = singleAdapter
		}
		setBackgroundColor(view)
		
	}
	
	private fun initiateListInfo() {
		
		if (sInitiated) return
		
		val itemNames = ArrayList<String>()
		val itemMembers = ArrayList<String>()
		
		val dbIndexName = when (fragmentID) {
			R.id.nav_company_12 -> HbDbHelper.dbMemberCompany12
			R.id.nav_company_34 -> HbDbHelper.dbMemberCompany34
			R.id.nav_school     -> HbDbHelper.dbMemberSchool
			R.id.nav_major      -> HbDbHelper.dbMemberMajor
			R.id.nav_branch     -> HbDbHelper.dbMemberBranch
			else                -> ""
		}
		
		dbAccess.makeDistinctDataList(
			itemNames, itemMembers,
			HbDbHelper.dbTableMember, dbIndexName)
		
		singleContent.makeList(itemNames, itemMembers)
		
		sInitiated = true
	}
	
	class SingleContent {
		class SingleItem(val id: String, val content: String) {
			
			override fun toString(): String {
				return content
			}
		}
		
		val singles: MutableList<SingleItem> = ArrayList()
		private val singleMap: MutableMap<String, SingleItem> = HashMap()
		
		fun makeList(names: List<String>, members: List<String>) {
			val count = names.size
			for (i in 0 until count) {
				val single = SingleItem(names[i], members[i])
				addItem(single)
			}
			
		}
		
		private fun addItem(item: SingleItem) {
			singles.add(item)
			singleMap[item.id] = item
		}
	}
	
	inner class SingleViewAdapter(private val mValues: List<SingleContent.SingleItem>) :
		androidx.recyclerview.widget.RecyclerView.Adapter<SingleViewAdapter.ViewHolder>() {
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.fragment_single_item, parent, false)
			return ViewHolder(view)
		}
		
		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val single = mValues[position]
			
			holder.mView.member_pic.visibility = View.GONE
			when (fragmentID) {
				R.id.nav_company_12, R.id.nav_company_34 -> {
					val company = single.id.trim('0')
					holder.mView.single_id.text = company
					if (fragmentID == R.id.nav_company_34) {
						holder.mView.member_pic.visibility = View.VISIBLE
						holder.mView.member_pic.setOnClickListener {
							gActivity.memberFragment.showCompanyPic(company)
						}
						holder.mView.loSingleHolder.setOnClickListener {
							gActivity.memberFragment.showCompanyPic(company)
						}
					}
				}
				else                                     -> {
					holder.mView.single_id.text = single.id
				}
			}
			
			holder.mView.single_id.text = single.id
			
			holder.mView.single_content.typeface = contentFace
			holder.mView.single_content.text = single.content
			
			val count = countCommaItems(single.content)
			if (count < 1) {
				holder.mView.member_count.visibility = View.GONE
			} else {
				holder.mView.member_count.visibility = View.VISIBLE
				holder.mView.member_count.text = "${count}명"
			}
		}
		
		override fun getItemCount(): Int {
			return mValues.size
		}
		
		inner class ViewHolder(val mView: View) :
			androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
			
			override fun toString(): String {
				return super.toString() + " '" + mView.single_content.text + "'"
			}
		}
	}
	
	private fun setBackgroundColor(view: View) {
		val resID = when (fragmentID) {
			R.id.nav_company_12 -> R.color.back_company12
			R.id.nav_company_34 -> R.color.back_company34
			R.id.nav_school     -> R.color.back_school
			R.id.nav_major      -> R.color.back_major
			R.id.nav_branch     -> R.color.back_branch
			else                -> 0
		}
		if (resID != 0) view.setBackgroundColor(ContextCompat.getColor(gActivity, resID))
	}
}