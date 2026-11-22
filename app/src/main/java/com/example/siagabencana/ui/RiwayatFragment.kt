package com.example.siagabencana.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.siagabencana.R
import com.example.siagabencana.databinding.FragmentRiwayatBinding
import com.example.siagabencana.databinding.ItemRiwayatBinding
import com.example.siagabencana.model.RiwayatStatus
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.launch

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!
    private val riwayatList = mutableListOf<RiwayatStatus>()
    private lateinit var adapter: RiwayatAdapter

    private var currentFilterMode: Int = 0 // 0: Semua, 1: Dibuat, 2: Diubah

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chipDihapus.isVisible = false

        adapter = RiwayatAdapter(riwayatList)
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadRiwayat()
        }

        binding.chipGroupAksi.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilterMode = when (checkedIds.first()) {
                R.id.chipDibuat -> 1
                R.id.chipDiubah -> 2
                else -> 0
            }
            loadRiwayat()
        }

        loadRiwayat()
    }

    override fun onResume() {
        super.onResume()
        loadRiwayat()
    }

    private fun loadRiwayat() {
        binding.stateLoading.root.isVisible = riwayatList.isEmpty()
        binding.stateError.root.isVisible = false
        binding.stateEmpty.root.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(requireContext()).getRiwayat()
            }

            binding.stateLoading.root.isVisible = false
            binding.swipeRefresh.isRefreshing = false

            when (res) {
                is ApiResult.Success -> {
                    val items = res.data?.items.orEmpty()
                    val filtered = when (currentFilterMode) {
                        1 -> items.filter { it.status_lama == null }
                        2 -> items.filter { it.status_lama != null }
                        else -> items
                    }

                    riwayatList.clear()
                    riwayatList.addAll(filtered)
                    adapter.notifyDataSetChanged()

                    binding.stateEmpty.root.isVisible = filtered.isEmpty()
                }
                is ApiResult.Failure -> {
                    if (riwayatList.isEmpty()) {
                        binding.stateError.root.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class RiwayatAdapter(private val list: List<RiwayatStatus>) :
        RecyclerView.Adapter<RiwayatAdapter.VH>() {

        class VH(val b: ItemRiwayatBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = list[position]
            val isCreated = r.status_lama == null

            if (isCreated) {
                holder.b.tvAksi.text = "Laporan dibuat"
                holder.b.boxIkon.setBackgroundResource(R.drawable.bg_icon_box_green)
                holder.b.imgIkon.setImageResource(R.drawable.ic_add)
            } else {
                holder.b.tvAksi.text = "Status: ${r.status_lama ?: "baru"} ➔ ${r.status_baru ?: "diverifikasi"}"
                holder.b.boxIkon.setBackgroundResource(R.drawable.bg_icon_box_orange)
                holder.b.imgIkon.setImageResource(R.drawable.ic_edit)
            }

            val laporanJudul = r.laporan?.judul ?: "Laporan"
            val userNama = r.user?.nama ?: "Sistem"
            val ket = r.keterangan ?: ""
            holder.b.tvKeterangan.text = "$laporanJudul · $ket (oleh $userNama)"
            holder.b.tvJam.text = formatTanggal(r.created_at)
        }
    }
}
