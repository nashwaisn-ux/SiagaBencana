package com.example.siagabencana.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

/** Menampilkan satu layout apa adanya (untuk layar yang logikanya dikerjakan anggota lain). */
class StaticFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(requireArguments().getInt(ARG_LAYOUT), container, false)

    companion object {
        private const val ARG_LAYOUT = "layout"
        fun of(layoutRes: Int) = StaticFragment().apply { arguments = bundleOf(ARG_LAYOUT to layoutRes) }
    }
}
