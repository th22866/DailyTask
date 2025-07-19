package com.pengxh.daily.app.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.pengxh.daily.app.DailyTaskApplication
import com.pengxh.daily.app.R
import com.pengxh.daily.app.adapter.DailyTaskAdapter
import com.pengxh.daily.app.bean.DailyTaskBean
import com.pengxh.daily.app.databinding.FragmentDailyTaskBinding
import com.pengxh.daily.app.extensions.backToMainActivity
import com.pengxh.daily.app.extensions.diffCurrent
import com.pengxh.daily.app.extensions.formatTime
import com.pengxh.daily.app.extensions.getTaskIndex
import com.pengxh.daily.app.extensions.sendEmail
import com.pengxh.daily.app.extensions.showTimePicker
import com.pengxh.daily.app.service.CountDownTimerService
import com.pengxh.daily.app.service.FloatingWindowService
import com.pengxh.daily.app.utils.Constant
import com.pengxh.daily.app.utils.OnTimeSelectedCallback
import com.pengxh.daily.app.utils.TimeKit
import com.pengxh.kt.lite.adapter.NormalRecyclerAdapter
import com.pengxh.kt.lite.base.KotlinBaseFragment
import com.pengxh.kt.lite.divider.RecyclerViewItemOffsets
import com.pengxh.kt.lite.extensions.convertColor
import com.pengxh.kt.lite.extensions.dp2px
import com.pengxh.kt.lite.extensions.show
import com.pengxh.kt.lite.utils.SaveKeyValues
import com.pengxh.kt.lite.utils.WeakReferenceHandler
import com.pengxh.kt.lite.widget.dialog.AlertControlDialog
import com.pengxh.kt.lite.widget.dialog.AlertInputDialog
import com.pengxh.kt.lite.widget.dialog.AlertMessageDialog
import com.pengxh.kt.lite.widget.dialog.BottomActionSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger


class DailyTaskFragment : KotlinBaseFragment<FragmentDailyTaskBinding>(), Handler.Callback {

    private val kTag = "DailyTaskFragment"
    private val dailyTaskDao by lazy { DailyTaskApplication.get().dataBase.dailyTaskDao() }
    private val marginOffset by lazy { 10.dp2px(requireContext()) }
    private val gson by lazy { Gson() }
    private val weakReferenceHandler = WeakReferenceHandler(this)
    private val repeatTaskHandler = Handler(Looper.getMainLooper())
    private val dailyTaskHandler = Handler(Looper.getMainLooper())
    private lateinit var dailyTaskAdapter: DailyTaskAdapter
    private var taskBeans: MutableList<DailyTaskBean> = ArrayList()
    private var diffSeconds = AtomicInteger(0)
    private var isTaskStarted = false
    private var timeoutTimer: CountDownTimer? = null
    private var countDownTimerService: CountDownTimerService? = null
    private var isRefresh = false

    override fun setupTopBarLayout() {

    }

    override fun observeRequestState() {

    }

    /**
     * 服务绑定
     * */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CountDownTimerService.LocaleBinder
            countDownTimerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    override fun initViewBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentDailyTaskBinding {
        return FragmentDailyTaskBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DailyTaskApplication.get().eventViewModel.eventFlow.collect { code ->
                    when (code) {
                        Constant.START_COUNT_DOWN_TIMER_CODE -> {
                            Log.d(kTag, "开始超时倒计时")
                            val time = SaveKeyValues.getValue(
                                Constant.STAY_DD_TIMEOUT_KEY, Constant.DEFAULT_OVER_TIME
                            ) as String
                            //去掉时间的s
                            val timeValue = time.dropLast(1).toInt()
                            timeoutTimer = object : CountDownTimer(timeValue * 1000L, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val tick = millisUntilFinished / 1000
                                    FloatingWindowService.weakReferenceHandler?.apply {
                                        val message = obtainMessage()
                                        message.what = Constant.TICK_TIME_CODE
                                        message.obj = tick
                                        sendMessage(message)
                                    }
                                }

                                override fun onFinish() {
                                    //如果倒计时结束，那么表明没有收到打卡成功的通知
                                    requireContext().backToMainActivity()
                                    "未监听到打卡通知，即将发送异常日志邮件，请注意查收".show(
                                        requireContext()
                                    )
                                    "".sendEmail(requireContext(), null, false)
                                }
                            }
                            timeoutTimer?.start()
                        }

                        Constant.CANCEL_COUNT_DOWN_TIMER_CODE -> {
                            timeoutTimer?.cancel()
                            timeoutTimer = null
                            Log.d(kTag, "取消超时定时器，执行下一个任务")
                            weakReferenceHandler.sendEmptyMessage(Constant.EXECUTE_NEXT_TASK_CODE)
                        }

                        Constant.START_DAILY_TASK_CODE -> startExecuteTask(true)

                        Constant.STOP_DAILY_TASK_CODE -> stopExecuteTask(true)
                    }
                }
            }
        }
    }

    override fun initOnCreate(savedInstanceState: Bundle?) {
        taskBeans = dailyTaskDao.loadAll()

        updateEmptyViewVisibility()

        dailyTaskAdapter = DailyTaskAdapter(requireContext(), taskBeans)
        binding.recyclerView.adapter = dailyTaskAdapter
        binding.recyclerView.addItemDecoration(
            RecyclerViewItemOffsets(
                marginOffset, marginOffset shr 1, marginOffset, marginOffset shr 1
            )
        )

        Intent(requireContext(), CountDownTimerService::class.java).apply {
            requireContext().bindService(this, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateEmptyViewVisibility() {
        binding.emptyView.visibility = if (taskBeans.isEmpty()) View.VISIBLE else View.GONE
    }

    private val itemComparator = object : NormalRecyclerAdapter.ItemComparator<DailyTaskBean> {
        override fun areItemsTheSame(oldItem: DailyTaskBean, newItem: DailyTaskBean): Boolean {
            return oldItem.id == newItem.id && oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItem: DailyTaskBean, newItem: DailyTaskBean): Boolean {
            return oldItem.time == newItem.time
        }
    }

    override fun initEvent() {
        binding.executeTaskButton.setOnClickListener {
            if (!isTaskStarted) {
                startExecuteTask(false)
            } else {
                stopExecuteTask(false)
            }
        }

        binding.refreshView.setOnRefreshListener {
            isRefresh = true
            lifecycleScope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) {
                    dailyTaskDao.loadAll()
                }
                delay(500)
                binding.refreshView.finishRefresh()
                isRefresh = false
                dailyTaskAdapter.refresh(result, itemComparator)
            }
        }

        binding.refreshView.setEnableLoadMore(false)

        dailyTaskAdapter.setOnItemClickListener(object :
            DailyTaskAdapter.OnItemClickListener<DailyTaskBean> {
            override fun onItemClick(item: DailyTaskBean, position: Int) {
                if (isTaskStarted) {
                    "任务进行中，无法修改，请先取消当前任务".show(requireContext())
                    return
                }
                AlertControlDialog.Builder().setContext(requireContext()).setTitle("修改打卡任务")
                    .setMessage("是否需要调整打卡时间？").setNegativeButton("取消")
                    .setPositiveButton("确定").setOnDialogButtonClickListener(object :
                        AlertControlDialog.OnDialogButtonClickListener {
                        override fun onConfirmClick() {
                            requireActivity().showTimePicker(item, object : OnTimeSelectedCallback {
                                override fun onTimePicked(time: String) {
                                    item.time = time
                                    dailyTaskDao.update(item)
                                    taskBeans.sortBy { x -> x.time }
                                    dailyTaskAdapter.notifyItemRangeChanged(0, taskBeans.size)
                                }
                            })
                        }

                        override fun onCancelClick() {

                        }
                    }).build().show()
            }

            override fun onItemLongClick(item: DailyTaskBean, position: Int) {
                if (isTaskStarted) {
                    "任务进行中，无法删除，请先取消当前任务".show(requireContext())
                    return
                }
                AlertControlDialog.Builder().setContext(requireContext()).setTitle("删除提示")
                    .setMessage("确定要删除这个任务吗").setNegativeButton("取消")
                    .setPositiveButton("确定").setOnDialogButtonClickListener(object :
                        AlertControlDialog.OnDialogButtonClickListener {
                        override fun onConfirmClick() {
                            dailyTaskDao.delete(item)
                            taskBeans.removeAt(position)
                            dailyTaskAdapter.notifyItemRemoved(position)
                            dailyTaskAdapter.notifyItemRangeChanged(
                                position, taskBeans.size - position
                            )
                            updateEmptyViewVisibility()
                        }

                        override fun onCancelClick() {

                        }
                    }).build().show()
            }
        })

        binding.addTimerButton.setOnClickListener {
            if (isTaskStarted) {
                "任务进行中，无法添加，请先取消当前任务".show(requireContext())
                return@setOnClickListener
            }

            if (taskBeans.isNotEmpty()) {
                addTask()
            } else {
                BottomActionSheet.Builder()
                    .setContext(requireContext())
                    .setActionItemTitle(arrayListOf("添加任务", "导入任务"))
                    .setItemTextColor(R.color.theme_color.convertColor(requireContext()))
                    .setOnActionSheetListener(object : BottomActionSheet.OnActionSheetListener {
                        override fun onActionItemClick(position: Int) {
                            when (position) {
                                0 -> addTask()
                                1 -> importTask()
                            }
                        }
                    }).build().show()
            }
        }
    }

    private fun startExecuteTask(isRemote: Boolean) {
        if (dailyTaskDao.loadAll().isEmpty()) {
            "循环任务启动失败，请先添加任务时间点".sendEmail(
                requireContext(), "启动循环任务通知", false
            )
            return
        }
        diffSeconds.set(TimeKit.getResetTaskSeconds())
        repeatTaskHandler.post(repeatTaskRunnable)
        Log.d(kTag, "startExecuteTask: 开启周期任务Runnable")
        executeDailyTask()
        isTaskStarted = true
        binding.executeTaskButton.setImageResource(R.mipmap.ic_stop)
        if (isRemote) {
            "循环任务启动成功，请注意下次打卡时间".sendEmail(
                requireContext(), "启动循环任务通知", false
            )
        }
    }

    private fun stopExecuteTask(isRemote: Boolean) {
        repeatTaskHandler.removeCallbacks(repeatTaskRunnable)
        Log.d(kTag, "stopExecuteTask: 取消周期任务Runnable")
        countDownTimerService?.cancelCountDown()
        isTaskStarted = false
        binding.repeatTimeView.text = "--秒后刷新每日任务"
        binding.executeTaskButton.setImageResource(R.mipmap.ic_start)
        binding.tipsView.text = ""
        dailyTaskAdapter.updateCurrentTaskState(-1)
        if (isRemote) {
            "循环任务停止成功，请及时打开下次任务".sendEmail(
                requireContext(), "暂停循环任务通知", false
            )
        }
    }

    private fun addTask() {
        requireActivity().showTimePicker(object : OnTimeSelectedCallback {
            override fun onTimePicked(time: String) {
                if (dailyTaskDao.queryTaskByTime(time) > 0) {
                    "任务时间点已存在".show(requireContext())
                    return
                }

                val bean = DailyTaskBean()
                bean.time = time
                dailyTaskDao.insert(bean)
                taskBeans.add(bean)
                taskBeans.sortBy { x -> x.time }
                dailyTaskAdapter.notifyItemRangeChanged(0, taskBeans.size)
                binding.emptyView.visibility = View.GONE
            }
        })
    }

    private fun importTask() {
        AlertInputDialog.Builder()
            .setContext(requireContext())
            .setTitle("导入任务")
            .setHintMessage("请将导出的任务粘贴到这里")
            .setNegativeButton("取消")
            .setPositiveButton("确定")
            .setOnDialogButtonClickListener(object :
                AlertInputDialog.OnDialogButtonClickListener {
                override fun onConfirmClick(value: String) {
                    val type = object : TypeToken<List<DailyTaskBean>>() {}.type
                    try {
                        val tasks = gson.fromJson<List<DailyTaskBean>>(value, type)
                        tasks.forEach {
                            dailyTaskDao.insert(it)
                            taskBeans.add(it)
                        }
                        taskBeans.sortBy { x -> x.time }
                        dailyTaskAdapter.notifyItemRangeChanged(0, taskBeans.size)
                        binding.emptyView.visibility = View.GONE
                        "任务导入成功".show(requireContext())
                    } catch (e: JsonSyntaxException) {
                        e.printStackTrace()
                        AlertMessageDialog.Builder().setContext(requireContext())
                            .setTitle("温馨提醒")
                            .setMessage("导入失败，请确认导入的是正确的任务数据")
                            .setPositiveButton("好的").setOnDialogButtonClickListener(object :
                                AlertMessageDialog.OnDialogButtonClickListener {
                                override fun onConfirmClick() {

                                }
                            }).build().show()
                    }
                }

                override fun onCancelClick() {}
            }).build().show()
    }

    /**
     * 循环任务Runnable
     * */
    private val repeatTaskRunnable = object : Runnable {
        override fun run() {
            val currentDiffSeconds = diffSeconds.decrementAndGet()
            if (currentDiffSeconds > 0) {
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.repeatTimeView.text = "${currentDiffSeconds.formatTime()}后刷新每日任务"
                }
                repeatTaskHandler.postDelayed(this, 1000)
            } else {
                // 刷新任务，并重启repeatTaskRunnable
                diffSeconds.set(TimeKit.getResetTaskSeconds())
                // 确保移除旧的回调
                repeatTaskHandler.removeCallbacks(this)
                repeatTaskHandler.post(this)
                Log.d(kTag, "run: 零点，刷新任务，并重新执行repeatTaskRunnable")
                executeDailyTask()
            }
        }
    }

    private fun executeDailyTask() {
        Log.d(kTag, "executeDailyTask: 执行周期任务")
        dailyTaskHandler.post(dailyTaskRunnable)
    }

    /**
     * 当日串行任务Runnable
     * */
    private val dailyTaskRunnable = Runnable {
        val taskIndex = taskBeans.getTaskIndex()
        Log.d(kTag, "任务index是: $taskIndex")
        if (taskIndex == -1) {
            weakReferenceHandler.sendEmptyMessage(Constant.COMPLETED_ALL_TASK_CODE)
        } else {
            val message = weakReferenceHandler.obtainMessage()
            message.what = Constant.START_TASK_CODE
            message.obj = taskIndex
            weakReferenceHandler.sendMessage(message)
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            Constant.START_TASK_CODE -> {
                val index = msg.obj as Int
                val task = taskBeans[index]
                binding.tipsView.text = "即将执行第 ${index + 1} 个任务"
                binding.tipsView.setTextColor(R.color.theme_color.convertColor(requireContext()))

                val pair = task.diffCurrent()
                dailyTaskAdapter.updateCurrentTaskState(index, pair.first)
                val diff = pair.second
                Log.d(kTag, "任务时间差是: $diff 秒")
                "即将执行第 ${index + 1} 个任务，任务时间点是: ${task.time}".sendEmail(
                    requireContext(), "任务执行通知", false
                )
                countDownTimerService?.startCountDown(index + 1, diff)
            }

            Constant.EXECUTE_NEXT_TASK_CODE -> {
                dailyTaskHandler.post(dailyTaskRunnable)
            }

            Constant.COMPLETED_ALL_TASK_CODE -> {
                binding.tipsView.text = "当天所有任务已执行完毕"
                binding.tipsView.setTextColor(R.color.ios_green.convertColor(requireContext()))
                dailyTaskAdapter.updateCurrentTaskState(-1)
                dailyTaskHandler.removeCallbacks(dailyTaskRunnable)
                countDownTimerService?.updateDailyTaskState()
            }
        }
        return true
    }
}