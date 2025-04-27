package com.pengxh.daily.app.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.ScaleAnimation
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.pengxh.daily.app.R
import com.pengxh.daily.app.adapter.BaseFragmentAdapter
import com.pengxh.daily.app.databinding.ActivityMainBinding
import com.pengxh.daily.app.extensions.initImmersionBar
import com.pengxh.daily.app.fragment.DailyTaskFragment
import com.pengxh.daily.app.fragment.SettingsFragment
import com.pengxh.daily.app.service.FloatingWindowService
import com.pengxh.daily.app.service.ForegroundRunningService
import com.pengxh.daily.app.utils.Constant
import com.pengxh.kt.lite.base.KotlinBaseActivity
import com.pengxh.kt.lite.extensions.setScreenBrightness
import com.pengxh.kt.lite.utils.BroadcastManager
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.pengxh.kt.lite.widget.dialog.AlertMessageDialog

class MainActivity : KotlinBaseActivity<ActivityMainBinding>() {

    private val kTag = "MainActivity"
    private val broadcastManager by lazy { BroadcastManager(this) }
    private val keyguardManager by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }
    private val fragmentPages = ArrayList<Fragment>()
    private var menuItem: MenuItem? = null
    private lateinit var insetsController: WindowInsetsControllerCompat

    init {
        fragmentPages.add(DailyTaskFragment())
        fragmentPages.add(SettingsFragment())
    }

    override fun initViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupTopBarLayout() {
        insetsController = WindowCompat.getInsetsController(window, binding.rootView)
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

        val keyguardLock = keyguardManager.newKeyguardLock("KEY_GUARD_TAG")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "${resources.getString(R.string.app_name)}:WAKE_LOCK_TAG"
        )
        broadcastManager.addAction(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (binding.maskView.isVisible) {
                    keyguardLock.disableKeyguard()
                    wakeLock.acquire(1000)
                }
            }
        }, Intent.ACTION_SCREEN_OFF)
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
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (binding.maskView.isVisible) {
                    //恢复状态栏显示
                    insetsController.show(WindowInsetsCompat.Type.statusBars())

                    //隐藏蒙层
                    binding.maskView.visibility = View.GONE
                    val invisibleAction = ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f)
                    invisibleAction.duration = 500
                    binding.maskView.startAnimation(invisibleAction)
                    window.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)

                    //显示任务界面
                    binding.rootView.visibility = View.VISIBLE

                    //恢复悬浮窗显示
                    FloatingWindowService.weakReferenceHandler?.apply {
                        sendEmptyMessage(Constant.SHOW_FLOATING_WINDOW_CODE)
                    }
                } else {
                    //隐藏状态栏显示
                    insetsController.hide(WindowInsetsCompat.Type.statusBars())

                    //显示蒙层
                    binding.maskView.visibility = View.VISIBLE
                    val visibleAction = ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f)
                    visibleAction.duration = 500
                    binding.maskView.startAnimation(visibleAction)
                    window.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF)

                    //隐藏任务界面
                    binding.rootView.visibility = View.GONE

                    //隐藏悬浮窗显示
                    FloatingWindowService.weakReferenceHandler?.apply {
                        sendEmptyMessage(Constant.HIDE_FLOATING_WINDOW_CODE)
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_POWER -> {
                //拦截电源按键
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        if (binding.maskView.isVisible) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            FloatingWindowService.weakReferenceHandler?.apply {
                sendEmptyMessage(Constant.HIDE_FLOATING_WINDOW_CODE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastManager.destroy(Intent.ACTION_SCREEN_OFF)
    }
}