package com.example.siagabencana

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.databinding.ItemChatRelawanBinding

data class PesanRelawan(
    val id: Int = 0,
    val pengirim: String,
    val isi: String,
    val waktu: String,
    val isDariSaya: Boolean
)

class ChatRelawanAdapter(
    private val daftarPesan: MutableList<PesanRelawan>
) : RecyclerView.Adapter<ChatRelawanAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatRelawanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatRelawanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val pesan = daftarPesan[position]
        with(holder.binding) {
            if (pesan.isDariSaya) {
                // Bubble kanan (ujung kanan - pesan keluar / petugas atau pengguna aktif)
                cardBubbleKanan.visibility = View.VISIBLE
                cardBubbleKiri.visibility = View.GONE
                tvPengirimKanan.text = pesan.pengirim
                tvIsiKanan.text = pesan.isi
                tvWaktuKanan.text = pesan.waktu
            } else {
                // Bubble kiri (ujung kiri - relawan / pesan masuk)
                cardBubbleKiri.visibility = View.VISIBLE
                cardBubbleKanan.visibility = View.GONE
                tvPengirimKiri.text = pesan.pengirim
                tvIsiKiri.text = pesan.isi
                tvWaktuKiri.text = pesan.waktu
            }
        }
    }

    override fun getItemCount(): Int = daftarPesan.size

    fun tambahPesan(pesan: PesanRelawan) {
        daftarPesan.add(pesan)
        notifyItemInserted(daftarPesan.size - 1)
    }
}
