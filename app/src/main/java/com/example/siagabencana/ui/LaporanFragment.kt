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
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private val laporanList = mutableListOf<Laporan>()
    private lateinit var adapter: LaporanAdapter

    private var currentSearch: String? = null
    private var currentStatus: String? = null
    private var currentUrgensi: String? = null

    private var currentPage = 1
    private var lastPage = 1
    private var isLoadingMore = false
    private var searchJob: Job? = null

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

        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvLaporan.layoutManager = layoutManager
        binding.rvLaporan.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            loadLaporan(isRefresh = true)
        }

        binding.etCari.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400) // Debounce 400ms
                currentSearch = text?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() }
                currentPage = 1
                loadLaporan(isRefresh = true)
            }
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentStatus = null
            currentUrgensi = null
            when (checkedIds.first()) {
                R.id.chipKritis -> currentUrgensi = "kritis"
                R.id.chipDiproses -> currentStatus = "diverifikasi,ditangani"
                R.id.chipSelesai -> currentStatus = "selesai"
                else -> {
                    // Semua
                }
            }
            currentPage = 1
            loadLaporan(isRefresh = true)
        }

        binding.rvLaporan.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && !isLoadingMore && currentPage < lastPage) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 3) {
                        currentPage++
                        loadLaporan(isLoadMore = true)
                    }
                }
            }
        })

        loadLaporan(isRefresh = true)
    }

    private fun loadLaporan(isRefresh: Boolean = false, isLoadMore: Boolean = false) {
        if (isLoadMore) {
            isLoadingMore = true
        } else if (isRefresh) {
            binding.stateLoading.root.isVisible = laporanList.isEmpty()
            binding.stateError.root.isVisible = false
            binding.stateEmpty.root.isVisible = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(requireContext()).getLaporan(
                    search = currentSearch,
                    status = currentStatus,
                    urgensi = currentUrgensi,
                    page = currentPage,
                    perPage = 15
                )
            }

            binding.stateLoading.root.isVisible = false
            binding.swipeRefresh.isRefreshing = false
            isLoadingMore = false

            when (res) {
                is ApiResult.Success -> {
                    val paginated = res.data
                    val items = paginated?.items.orEmpty()
                    lastPage = paginated?.meta?.last_page ?: 1
                    val totalCount = paginated?.meta?.total ?: items.size

                    if (isRefresh) {
                        laporanList.clear()
                    }
                    laporanList.addAll(items)
                    adapter.notifyDataSetChanged()

                    binding.tvJumlah.text = "$totalCount laporan"
                    binding.stateEmpty.root.isVisible = laporanList.isEmpty()
                }
                is ApiResult.Failure -> {
                    if (laporanList.isEmpty()) {
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
            holder.b.tvJudul.text = l.judul ?: "Bencana"
            val katName = l.kategori?.nama ?: "Bencana"
            holder.b.tvMeta.text = "$katName · ${formatTanggal(l.created_at)}"
            holder.b.tvBadge.applyBadge(l)
            holder.b.cbPilih.isVisible = false

            holder.b.root.setOnClickListener { onClick(l) }
        }
    }
}
