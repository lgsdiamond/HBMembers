package com.lgsdiamond.hbmembers.ui.single

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SingleViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is single Fragment"
    }
    val text: LiveData<String> = _text
}