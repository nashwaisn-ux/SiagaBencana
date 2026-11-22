package com.example.siagabencana.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.R
import com.example.siagabencana.model.Laporan

object NotifikasiPopupHelper {
    fun show(
        context: Context,
        laporanList: List<Laporan>,
        onOpenLaporan: (Int) -> Unit,
        onReadStateChanged: (() -> Unit)? = null
    ) {
        val rows = NotifikasiMapper.buildRows(context, laporanList)
        val unreadCount = rows.filterIsInstance<NotifRow.Item>().count { it.unread }

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_notifikasi, null)
        dialog.setContentView(view)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val tvBadge = view.findViewById<TextView>(R.id.tvUnreadBadge)
        val btnTandai = view.findViewById<TextView>(R.id.btnTandaiDibaca)
        val btnTutup = view.findViewById<ImageButton>(R.id.btnTutupNotif)
        val rv = view.findViewById<RecyclerView>(R.id.rvNotifikasi)
        val layoutKosong = view.findViewById<View>(R.id.layoutNotifKosong)

        if (unreadCount > 0) {
            tvBadge.visibility = View.VISIBLE
            tvBadge.text = "$unreadCount baru"
            btnTandai.visibility = View.VISIBLE
        } else {
            tvBadge.visibility = View.GONE
            btnTandai.visibility = View.GONE
        }

        if (rows.isEmpty()) {
            rv.visibility = View.GONE
            layoutKosong.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            layoutKosong.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(context)
            rv.adapter = NotifikasiAdapter(rows) { item ->
                NotifikasiMapper.markRead(context, item.id)
                onReadStateChanged?.invoke()
                dialog.dismiss()
                onOpenLaporan(item.laporanId)
            }
        }

        btnTandai.setOnClickListener {
            NotifikasiMapper.markAllRead(context, rows)
            onReadStateChanged?.invoke()
            dialog.dismiss()
        }

        btnTutup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
