package com.pengxh.daily.app.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pengxh.daily.app.R
import com.pengxh.daily.app.bean.DailyTaskBean
import com.pengxh.kt.lite.adapter.NormalRecyclerAdapter.ItemComparator
import com.pengxh.kt.lite.extensions.convertColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyTaskAdapter(
    private val context: Context,
    private val dataBeans: MutableList<DailyTaskBean>
) : RecyclerView.Adapter<DailyTaskAdapter.ItemViewHolder>() {

    private val kTag = "DailyTaskAdapter"
    private var layoutInflater = LayoutInflater.from(context)
    private var mPosition = -1
    private var actualTime = "--:--:--"

    fun updateCurrentTaskState(position: Int) {
        this.mPosition = position
        notifyItemRangeChanged(0, dataBeans.size)
    }

    fun updateCurrentTaskState(position: Int, actualTime: String) {
        this.mPosition = position
        this.actualTime = actualTime
        if (position < 0 || position >= dataBeans.size) {
            return
        }
        notifyItemRangeChanged(0, mPosition + 1)
    }

    override fun getItemCount(): Int = dataBeans.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            layoutInflater.inflate(R.layout.item_daily_task_rv_l, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val taskBean = dataBeans[position]
        holder.taskTimeView.text = taskBean.time
        if (position == mPosition) {
            holder.itemView.isSelected = true
            holder.taskStateView.visibility = View.VISIBLE
            holder.actualTimeCardView.visibility = View.VISIBLE
            holder.actualTimeView.text = actualTime
            holder.taskTimeView.setTextColor("#CCCCCC".toColorInt())
            holder.actualTimeView.setTextColor(R.color.theme_color.convertColor(context))
        } else {
            holder.itemView.isSelected = false
            holder.taskStateView.visibility = View.GONE
            holder.actualTimeCardView.visibility = View.GONE
            holder.actualTimeView.text = "--:--:--"
            holder.taskTimeView.setTextColor(Color.BLACK)
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var actualTimeCardView: LinearLayout = itemView.findViewById(R.id.actualTimeCardView)
        var actualTimeView: TextView = itemView.findViewById(R.id.actualTimeView)
        var taskStateView: CardView = itemView.findViewById(R.id.taskStateView)
        var taskTimeView: TextView = itemView.findViewById(R.id.taskTimeView)
    }

    fun refresh(
        newRows: MutableList<DailyTaskBean>, itemComparator: ItemComparator<DailyTaskBean>? = null
    ) {
        if (newRows.isEmpty()) {
            Log.d(kTag, "refresh: newRows isEmpty")
            return
        }

        val oldSize = dataBeans.size

        if (itemComparator != null) {
            val oldDataSnapshot = ArrayList(dataBeans) // 旧数据副本
            val newDataSnapshot = ArrayList(newRows)  // 新数据副本

            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldDataSnapshot.size
                override fun getNewListSize(): Int = newDataSnapshot.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return itemComparator.areItemsTheSame(
                        oldDataSnapshot[oldItemPosition], newDataSnapshot[newItemPosition]
                    )
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    return itemComparator.areContentsTheSame(
                        oldDataSnapshot[oldItemPosition], newDataSnapshot[newItemPosition]
                    )
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = DiffUtil.calculateDiff(diffCallback)
                    withContext(Dispatchers.Main) {
                        dataBeans.clear()
                        dataBeans.addAll(newDataSnapshot)
                        result.dispatchUpdatesTo(this@DailyTaskAdapter)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            val newSize = newRows.size
            dataBeans.clear()
            dataBeans.addAll(newRows)

            // 新数据比旧数据少，需要通知删除部分 item ，否则会越界
            if (newSize < oldSize) {
                notifyItemRangeRemoved(newSize, oldSize - newSize)
            }
            notifyItemRangeChanged(0, newSize)
        }
    }
}