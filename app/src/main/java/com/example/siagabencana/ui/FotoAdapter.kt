package com.example.siagabencana.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.databinding.ItemFotoBuktiBinding


class FotoAdapter(
    private val fotos: List<Uri>,
    private val onHapus: (Int) -> Unit,
) : RecyclerView.Adapter<FotoAdapter.VH>() {

    class VH(val b: ItemFotoBuktiBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFotoBuktiBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = fotos.size

    override fun onBindViewHolder(h: VH, position: Int) {
        h.b.imgFoto.setImageURI(fotos[position])
        h.b.btnHapusFoto.setOnClickListener {
            val pos = h.adapterPosition
            if (pos != RecyclerView.NO_POSITION) onHapus(pos)
        }
    }
}
