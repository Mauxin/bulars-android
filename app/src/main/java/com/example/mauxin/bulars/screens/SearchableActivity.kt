package com.example.mauxin.bulars.screens

import android.app.Activity
import android.os.Bundle
import com.example.mauxin.bulars.R
import kotlinx.android.synthetic.main.searchable.*

class SearchableActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchable)

        val viewModel = SearchableViewModel(intent.getStringExtra("MEDICATE"))

        viewModel.searchMedicate(intent.getStringExtra("MEDICATE")) {
            resultTextView.text = it
        }
    }

}