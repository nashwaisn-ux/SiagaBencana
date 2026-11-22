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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RelawanFragment : Fragment() {

    private var _binding: FragmentRelawanBinding? = null
    private val binding get() = _binding!!
    private val relawanList = mutableListOf<Relawan>()
    private lateinit var adapter: RelawanAdapter

    private var currentSearch: String? = null
    private var currentStatusFilter: Int = 0 // 0: Semua, 1: Tersedia, 2: Bertugas
    private var searchJob: Job? = null

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

        binding.etCari.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400)
                currentSearch = text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                loadRelawan()
            }
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentStatusFilter = when (checkedIds.first()) {
                R.id.chipTersedia -> 1
                R.id.chipBertugas -> 2
                else -> 0
            }
            loadRelawan()
        }

        loadRelawan()
    }

    override fun onResume() {
        super.onResume()
        loadRelawan()
    }

    private fun loadRelawan() {
        binding.stateLoading.root.isVisible = relawanList.isEmpty()
        binding.stateError.root.isVisible = false
        binding.stateEmpty.root.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(requireContext()).getRelawan(search = currentSearch)
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false

                if (resp.isSuccessful && resp.body()?.success == true) {
                    val rawData = resp.body()?.data.orEmpty()
                    val filteredData = when (currentStatusFilter) {
                        1 -> rawData.filter { it.tugas_aktif == 0 }
                        2 -> rawData.filter { it.tugas_aktif > 0 }
                        else -> rawData
                    }

                    relawanList.clear()
                    relawanList.addAll(filteredData)
                    adapter.notifyDataSetChanged()

                    binding.tvJumlahRelawan.text = "${filteredData.size} terdaftar"
                    binding.stateEmpty.root.isVisible = filteredData.isEmpty()
                } else {
                    if (relawanList.isEmpty()) {
                        binding.stateError.root.isVisible = true
                    }
                }
            } catch (_: Exception) {
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false
                if (relawanList.isEmpty()) {
                    binding.stateError.root.isVisible = true
                }
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
