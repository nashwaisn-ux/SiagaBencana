package com.example.siagabencana.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.R
import com.example.siagabencana.databinding.FragmentRelawanBinding
import com.example.siagabencana.databinding.ItemRelawanBinding
import com.example.siagabencana.model.Relawan
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.launch

class RelawanFragment : Fragment() {

    private var _binding: FragmentRelawanBinding? = null
    private val binding get() = _binding!!
    private val relawanList = mutableListOf<Relawan>()
    private lateinit var adapter: RelawanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelawanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RelawanAdapter(relawanList)
        binding.rvRelawan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRelawan.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadRelawan()
        }

        binding.etCari.doAfterTextChanged {
            loadRelawan(it?.toString())
        }

        loadRelawan()
    }

    private fun loadRelawan(searchQuery: String? = null) {
        binding.stateLoading.root.isVisible = true
        binding.stateError.root.isVisible = false
        binding.stateEmpty.root.isVisible = false

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(requireContext()).getRelawan(search = searchQuery)
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false

                if (resp.isSuccessful && resp.body()?.success == true) {
                    val data = resp.body()?.data.orEmpty()
                    relawanList.clear()
                    relawanList.addAll(data)
                    adapter.notifyDataSetChanged()

                    binding.tvJumlahRelawan.text = "${data.size} terdaftar"
                    binding.stateEmpty.root.isVisible = data.isEmpty()
                } else {
                    binding.stateError.root.isVisible = true
                }
            } catch (_: Exception) {
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false
                binding.stateError.root.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class RelawanAdapter(private val list: List<Relawan>) :
        RecyclerView.Adapter<RelawanAdapter.VH>() {

        class VH(val b: ItemRelawanBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemRelawanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = list[position]
            holder.b.tvNama.text = r.nama
            holder.b.tvInfo.text = "Kontak: ${r.no_hp ?: "Tidak ada"} · Tugas aktif: ${r.tugas_aktif}"

            val inisial = r.nama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.let { parts ->
                when {
                    parts.isEmpty() -> "RL"
                    parts.size == 1 -> parts[0].take(2).uppercase()
                    else -> "${parts.first().first()}${parts.last().first()}".uppercase()
                }
            }
            holder.b.tvInisial.text = inisial

            if (r.tugas_aktif > 0) {
                holder.b.tvStatus.text = "Bertugas (${r.tugas_aktif})"
                holder.b.tvStatus.setBackgroundResource(R.drawable.bg_badge_high)
                holder.b.rowTugas.isVisible = true
                holder.b.tvLaporanTugas.text = "Sedang menangani ${r.tugas_aktif} laporan"
                holder.b.tvGanti.isVisible = false
            } else {
                holder.b.tvStatus.text = "Tersedia"
                holder.b.tvStatus.setBackgroundResource(R.drawable.bg_badge_done)
                holder.b.rowTugas.isVisible = false
            }
            holder.b.btnTugaskan.isVisible = false
        }
    }
}
