package com.example.mauxin.bulars.screens

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import com.example.mauxin.bulars.R
import kotlinx.android.synthetic.main.searchable.*

class SearchableActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.searchable)

        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                searchMedicate(query)
            }
        }
    }

    private fun searchMedicate(medicate: String) {
        textView.text = medicate
    }

}