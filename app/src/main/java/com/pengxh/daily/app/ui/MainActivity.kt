package com.pengxh.daily.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.pengxh.daily.app.R
import com.pengxh.daily.app.adapter.BaseFragmentAdapter
import com.pengxh.daily.app.databinding.ActivityMainBinding
import com.pengxh.daily.app.extensions.initImmersionBar
import com.pengxh.daily.app.fragment.DailyTaskFragment
import com.pengxh.daily.app.fragment.SettingsFragment
import com.pengxh.daily.app.service.ForegroundRunningService
import com.pengxh.kt.lite.base.KotlinBaseActivity
import com.pengxh.kt.lite.extensions.show
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.pengxh.kt.lite.widget.dialog.AlertMessageDialog

class MainActivity : KotlinBaseActivity<ActivityMainBinding>() {

    private val fragmentPages = ArrayList<Fragment>()
    private var menuItem: MenuItem? = null
    private var clickTime: Long = 0

    init {
        fragmentPages.add(DailyTaskFragment())
        fragmentPages.add(SettingsFragment())
    }

    override fun initViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupTopBarLayout() {
        binding.rootView.initImmersionBar(this, true, R.color.back_ground_color)
    }

    override fun initOnCreate(savedInstanceState: Bundle?) {
        Intent(this, ForegroundRunningService::class.java).apply {
            startService(this)
        }
        val fragmentAdapter = BaseFragmentAdapter(supportFragmentManager, fragmentPages)
        binding.viewPager.adapter = fragmentAdapter
        val isFirst = SaveKeyValues.getValue("isFirst", true) as Boolean
        if (isFirst) {
            AlertMessageDialog.Builder().setContext(this).setTitle("温馨提醒")
                .setMessage("本软件仅供内部使用，严禁商用或者用作其他非法用途")
                .setPositiveButton("知道了").setOnDialogButtonClickListener(object :
                    AlertMessageDialog.OnDialogButtonClickListener {
                    override fun onConfirmClick() {
                        SaveKeyValues.putValue("isFirst", false)
                    }
                }).build().show()
        }
    }

    override fun initEvent() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_daily) {
                binding.viewPager.currentItem = 0
            } else if (item.itemId == R.id.nav_settings) {
                binding.viewPager.currentItem = 1
            }
            false
        }

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (position < 0 || position >= binding.bottomNavigation.menu.size) {
                    return
                }

                // 获取当前选中的菜单项并取消选中
                val currentMenuItem = menuItem ?: binding.bottomNavigation.menu[0]
                currentMenuItem.isChecked = false

                // 更新新的选中菜单项
                menuItem = binding.bottomNavigation.menu[position]
                menuItem?.isChecked = true
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun observeRequestState() {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                return if (System.currentTimeMillis() - clickTime > 2000) {
                    "再按一次退出应用".show(this)
                    clickTime = System.currentTimeMillis()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}