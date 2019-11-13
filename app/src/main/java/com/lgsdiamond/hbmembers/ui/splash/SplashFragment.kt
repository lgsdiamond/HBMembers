package com.lgsdiamond.hbmembers.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.lgsdiamond.hbmembers.R

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_splash, container, false)

        findNavController()
            .navigate(
                R.id.action_splashFragment_to_homeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(
                        R.id.splashFragment,
                        true
                    ).build()
            )

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFragmentUI(view)
    }

    private fun initFragmentUI(view: View) {

    }

}