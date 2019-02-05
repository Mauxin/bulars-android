package com.example.mauxin.bulars.components

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsEvents {

    companion object {
        private var analytics: FirebaseAnalytics? = null

        fun clickEvent(context: Context, content: String) {

            analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, content)
            analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }

        fun searchingEvent(context: Context, term: String, type: String) {

            analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, term)
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
            analytics?.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }
}