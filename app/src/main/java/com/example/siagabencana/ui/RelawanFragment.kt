package com.example.siagabencana.ui

import android.content.Intent
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
import com.example.siagabencana.RegistrasiRelawanActivity
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.FragmentRelawanBinding
import com.example.siagabencana.databinding.ItemRelawanBinding
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.model.Relawan
import com.example.siagabencana.model.TugaskanRequest
import com.example.siagabencana.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RelawanFragment : Fragment() {

    private var _binding: FragmentRelawanBinding? = null
    private val binding get() = _binding!!
    private val relawanList = mutableListOf<Relawan>()
    private lateinit var adapter: RelawanAdapter
    private val session by lazy { SessionManager(requireContext()) }

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

        val isPetugas = session.role == SessionManager.ROLE_PETUGAS

        adapter = RelawanAdapter(relawanList, isPetugas) { relawan ->
            dialogTugaskanKeLaporan(relawan)
        }
        binding.rvRelawan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRelawan.adapter = adapter

        binding.fabTambahRelawan.isVisible = isPetugas
        binding.fabTambahRelawan.setOnClickListener {
            startActivity(Intent(requireContext(), RegistrasiRelawanActivity::class.java))
        }

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
                    val rawData: List<Relawan> = resp.body()?.data ?: emptyList()
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

    private fun dialogTugaskanKeLaporan(relawan: Relawan) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(requireContext())
                    .getLaporan(status = "baru,diverifikasi,ditangani", perPage = 50)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val laporanList: List<Laporan> = resp.body()?.data?.items ?: emptyList()
                    if (laporanList.isEmpty()) {
                        requireContext().toast("Tidak ada laporan aktif untuk ditugaskan")
                        return@launch
                    }

                    val titles = laporanList.map { "#${it.id} - ${it.judul ?: "Bencana"}" }.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Tugaskan ${relawan.nama} ke laporan:")
                        .setItems(titles) { _, which ->
                            val selectedLaporan = laporanList[which]
                            submitPenugasan(selectedLaporan.id, relawan.id)
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                } else {
                    requireContext().toast("Gagal memuat laporan aktif")
                }
            } catch (_: Exception) {
                requireContext().toast("Gagal terhubung ke server")
            }
        }
    }

    private fun submitPenugasan(laporanId: Int, relawanId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val req = TugaskanRequest(relawan_id = relawanId, catatan = "Penugasan lapangan dari Tab Relawan")
                val resp = RetrofitClient.getApiService(requireContext()).tugaskanRelawan(laporanId, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    requireContext().toast("Relawan berhasil ditugaskan")
                    loadRelawan()
                } else {
                    val msg = resp.body()?.message ?: "Gagal menugaskan relawan"
                    requireContext().toast(msg)
                }
            } catch (_: Exception) {
                requireContext().toast("Gagal terhubung ke server")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class RelawanAdapter(
        private val list: List<Relawan>,
        private val isPetugas: Boolean,
        private val onTugaskan: (Relawan) -> Unit
    ) : RecyclerView.Adapter<RelawanAdapter.VH>() {

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
            } else {
                holder.b.tvStatus.text = "Tersedia"
                holder.b.tvStatus.setBackgroundResource(R.drawable.bg_badge_done)
                holder.b.rowTugas.isVisible = false
            }

            holder.b.tvGanti.isVisible = false
            holder.b.btnTugaskan.isVisible = isPetugas
            holder.b.btnTugaskan.setOnClickListener {
                onTugaskan(r)
            }
        }
    }
}
