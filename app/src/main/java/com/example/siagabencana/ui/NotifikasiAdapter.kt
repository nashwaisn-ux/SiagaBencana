package com.example.siagabencana.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.R

class NotifikasiAdapter(
    private val rows: List<NotifRow>,
    private val onClick: (NotifRow.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is NotifRow.Header -> TYPE_HEADER
            is NotifRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_notifikasi_header, parent, false))
        } else {
            ItemVH(inflater.inflate(R.layout.item_notifikasi, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is NotifRow.Header -> (holder as HeaderVH).bind(row)
            is NotifRow.Item -> (holder as ItemVH).bind(row, onClick)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeaderTanggal)
        fun bind(row: NotifRow.Header) {
            tvHeader.text = row.label
        }
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val boxIcon: FrameLayout = view.findViewById(R.id.boxKategoriIcon)
        private val ivIcon: ImageView = view.findViewById(R.id.ivKategoriNotif)
        private val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        private val tvMeta: TextView = view.findViewById(R.id.tvNotifMeta)
        private val tvSummary: TextView = view.findViewById(R.id.tvNotifSummary)
        private val dotUnread: View = view.findViewById(R.id.viewUnreadDot)

        fun bind(item: NotifRow.Item, onClick: (NotifRow.Item) -> Unit) {
            tvTitle.text = item.title
            tvMeta.text = item.meta
            tvSummary.text = item.summary
            dotUnread.visibility = if (item.unread) View.VISIBLE else View.GONE

            val lower = item.kategoriNama.lowercase()
            when {
                lower.contains("banjir") -> {
                    boxIcon.setBackgroundResource(R.drawable.bg_notif_icon_blue)
                    ivIcon.setImageResource(R.drawable.ic_water)
                    ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(Color.parseColor("#1D4ED8")))
                }
                lower.contains("kebakaran") -> {
                    boxIcon.setBackgroundResource(R.drawable.bg_notif_icon_red)
                    ivIcon.setImageResource(R.drawable.ic_fire)
                    ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(Color.parseColor("#C62D1B")))
                }
                lower.contains("longsor") -> {
                    boxIcon.setBackgroundResource(R.drawable.bg_notif_icon_orange)
                    ivIcon.setImageResource(R.drawable.ic_landslide)
                    ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(Color.parseColor("#8A4A06")))
                }
                else -> {
                    boxIcon.setBackgroundResource(R.drawable.bg_notif_icon_gray)
                    ivIcon.setImageResource(R.drawable.ic_warning)
                    ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(Color.parseColor("#5B6778")))
                }
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
