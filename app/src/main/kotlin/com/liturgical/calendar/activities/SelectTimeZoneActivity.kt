package com.liturgical.calendar.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.liturgical.calendar.R
import com.liturgical.calendar.adapters.SelectTimeZoneAdapter
import com.liturgical.calendar.helpers.CURRENT_TIME_ZONE
import com.liturgical.calendar.helpers.TIME_ZONE
import com.liturgical.calendar.helpers.getAllTimeZones
import com.liturgical.calendar.models.MyTimeZone
import com.secure.commons.extensions.hideKeyboard
import com.secure.commons.helpers.NavigationIcon
import kotlinx.android.synthetic.main.activity_select_time_zone.*
import java.util.*

class SelectTimeZoneActivity : SimpleActivity() {
    private var mSearchMenuItem: MenuItem? = null
    private val allTimeZones = getAllTimeZones()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_time_zone)
        setupOptionsMenu()

        SelectTimeZoneAdapter(this, allTimeZones) {
            hideKeyboard()
            val data = Intent()
            data.putExtra(TIME_ZONE, it as MyTimeZone)
            setResult(RESULT_OK, data)
            finish()
        }.apply {
            select_time_zone_list.adapter = this
        }

        val currentTimeZone = intent.getStringExtra(CURRENT_TIME_ZONE) ?: TimeZone.getDefault().id
        val pos = allTimeZones.indexOfFirst { it.zoneName.equals(currentTimeZone, true) }
        if (pos != -1) {
            select_time_zone_list.scrollToPosition(pos)
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(select_time_zone_toolbar, NavigationIcon.Arrow, searchMenuItem = mSearchMenuItem)
    }

    private fun setupOptionsMenu() {
        setupSearch(select_time_zone_toolbar.menu)
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem!!.actionView as SearchView).apply {
            queryHint = getString(R.string.enter_a_country)
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            isIconified = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    searchQueryChanged(newText)
                    return true
                }
            })
        }

        mSearchMenuItem!!.expandActionView()
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                searchQueryChanged("")
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                hideKeyboard()
                finish()
                return true
            }
        })
    }

    private fun searchQueryChanged(text: String) {
        val timeZones = allTimeZones.filter {
            it.zoneName.lowercase(Locale.getDefault()).contains(text.lowercase(Locale.getDefault()))
        }.toMutableList() as ArrayList<MyTimeZone>
        (select_time_zone_list.adapter as? SelectTimeZoneAdapter)?.updateTimeZones(timeZones)
    }
}
