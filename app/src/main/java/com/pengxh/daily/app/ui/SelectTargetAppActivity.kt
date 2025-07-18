package com.pengxh.daily.app.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import com.pengxh.daily.app.R
import com.pengxh.daily.app.databinding.ActivitySelectTargetAppBinding
import com.pengxh.daily.app.extensions.initImmersionBar
import com.pengxh.daily.app.model.AppInfoModel
import com.pengxh.daily.app.utils.Constant
import com.pengxh.kt.lite.adapter.SingleChoiceAdapter
import com.pengxh.kt.lite.adapter.ViewHolder
import com.pengxh.kt.lite.base.KotlinBaseActivity
import com.pengxh.kt.lite.divider.RecyclerViewItemDivider
import com.pengxh.kt.lite.extensions.dp2px
import com.pengxh.kt.lite.utils.LiteKitConstant
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.pengxh.kt.lite.widget.TitleBarView

class SelectTargetAppActivity : KotlinBaseActivity<ActivitySelectTargetAppBinding>() {

    private var selectedItem: AppInfoModel? = null

    override fun initViewBinding(): ActivitySelectTargetAppBinding {
        return ActivitySelectTargetAppBinding.inflate(layoutInflater)
    }

    override fun setupTopBarLayout() {
        binding.rootView.initImmersionBar(this, true, R.color.white)
        binding.titleView.setOnClickListener(object : TitleBarView.OnClickListener {
            override fun onLeftClick() {
                finish()
            }

            override fun onRightClick() {
                selectedItem?.let {
                    val intent = Intent()
                    intent.putExtra(LiteKitConstant.INTENT_PARAM_KEY, it.appName)
                    setResult(RESULT_OK, intent)
                    //保存名称和包名
                    SaveKeyValues.putValue(Constant.TARGET_APP_NAME_KEY, it.appName)
                    SaveKeyValues.putValue(Constant.TARGET_APP_PACKAGE_KEY, it.packageName)
                    finish()
                }
            }
        })
    }

    override fun initOnCreate(savedInstanceState: Bundle?) {
        val apps = ArrayList<AppInfoModel>()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            // 跳过系统应用
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue
            }

            val appName = app.loadLabel(packageManager).toString()
            val packageName = app.packageName
            // 获取版本名
            var versionName = "未知"
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                versionName = packageInfo.versionName ?: "未知"
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            val icon = packageManager.getApplicationIcon(app)

            val info = AppInfoModel().apply {
                this.appName = appName
                this.packageName = packageName
                this.versionName = versionName
                this.appIcon = icon
            }
            apps.add(info)
        }

        val applicationAdapter = object : SingleChoiceAdapter<AppInfoModel>(
            R.layout.item_app_rv_l, apps
        ) {
            override fun convertView(viewHolder: ViewHolder, position: Int, item: AppInfoModel) {
                viewHolder.setImageResource(R.id.iconView, item.appIcon)
                    .setText(R.id.appNameView, item.appName)
                    .setText(R.id.versionNameView, item.versionName)
                    .setText(R.id.packageNameView, item.packageName)
            }
        }
        binding.recyclerView.adapter = applicationAdapter
        binding.recyclerView.addItemDecoration(
            RecyclerViewItemDivider(80f.dp2px(this), 0f, Color.LTGRAY)
        )
        applicationAdapter.setOnItemCheckedListener(object :
            SingleChoiceAdapter.OnItemCheckedListener<AppInfoModel> {
            override fun onItemChecked(position: Int, item: AppInfoModel) {
                selectedItem = item
            }
        })
    }

    override fun observeRequestState() {

    }

    override fun initEvent() {

    }
}