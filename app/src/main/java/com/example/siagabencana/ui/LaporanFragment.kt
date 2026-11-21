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
import com.example.siagabencana.DetailLaporanActivity
import com.example.siagabencana.R
import com.example.siagabencana.databinding.FragmentLaporanBinding
import com.example.siagabencana.databinding.ItemLaporanBinding
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private val laporanList = mutableListOf<Laporan>()
    private lateinit var adapter: LaporanAdapter

    private var currentSearch: String? = null
    private var currentStatus: String? = null
    private var currentUrgensi: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LaporanAdapter(laporanList) { laporan ->
            val intent = Intent(requireContext(), DetailLaporanActivity::class.java).apply {
                putExtra(DetailLaporanActivity.EXTRA_ID, laporan.id)
            }
            startActivity(intent)
        }

        binding.rvLaporan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLaporan.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadLaporan()
        }

        binding.etCari.doAfterTextChanged {
            currentSearch = it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() }
            loadLaporan()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentStatus = null
            currentUrgensi = null
            when (checkedIds.first()) {
                R.id.chipKritis -> currentUrgensi = "kritis"
                R.id.chipDiproses -> currentStatus = "diverifikasi"
                R.id.chipSelesai -> currentStatus = "selesai"
                else -> {
                    // Semua
                }
            }
            loadLaporan()
        }

        loadLaporan()
    }

    override fun onResume() {
        super.onResume()
        loadLaporan()
    }

    private fun loadLaporan() {
        binding.stateLoading.root.isVisible = laporanList.isEmpty()
        binding.stateError.root.isVisible = false
        binding.stateEmpty.root.isVisible = false

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(requireContext()).getLaporan(
                    search = currentSearch,
                    status = currentStatus,
                    urgensi = currentUrgensi
                )
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false

                if (resp.isSuccessful && resp.body()?.success == true) {
                    val paginated = resp.body()?.data
                    val items = paginated?.items.orEmpty()
                    laporanList.clear()
                    laporanList.addAll(items)
                    adapter.notifyDataSetChanged()

                    binding.tvJumlah.text = "${items.size} laporan"
                    binding.stateEmpty.root.isVisible = items.isEmpty()
                } else {
                    binding.stateError.root.isVisible = true
                }
            } catch (_: Exception) {
                binding.stateLoading.root.isVisible = false
                binding.swipeRefresh.isRefreshing = false
                if (laporanList.isEmpty()) {
                    binding.stateError.root.isVisible = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LaporanAdapter(
        private val list: List<Laporan>,
        private val onClick: (Laporan) -> Unit
    ) : RecyclerView.Adapter<LaporanAdapter.VH>() {

        class VH(val b: ItemLaporanBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val l = list[position]
            holder.b.tvJudul.text = l.judul
            val katName = l.kategori?.nama ?: "Bencana"
            holder.b.tvMeta.text = "$katName · ${l.created_at ?: "Baru saja"}"
            val urgStr = (l.urgensi ?: "sedang").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            holder.b.tvBadge.text = urgStr

            holder.b.root.setOnClickListener { onClick(l) }
        }
    }
}
