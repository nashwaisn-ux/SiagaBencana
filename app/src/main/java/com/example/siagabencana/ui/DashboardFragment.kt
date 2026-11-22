package com.example.siagabencana.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.siagabencana.BuatLaporanActivity
import com.example.siagabencana.DetailLaporanActivity
import com.example.siagabencana.MainActivity
import com.example.siagabencana.PetaSiagaActivity
import com.example.siagabencana.R
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ItemLaporanBinding
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class DashboardFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private val terbaruList = mutableListOf<Laporan>()
    private lateinit var laporanAdapter: TerbaruAdapter

    private var tvSapaan: TextView? = null
    private var tvNama: TextView? = null
    private var tvStatBaru: TextView? = null
    private var tvStatProses: TextView? = null
    private var tvStatSelesai: TextView? = null
    private var tvStatRelawan: TextView? = null
    private var tvBannerJudul: TextView? = null
    private var tvBannerDesc: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sessionManager = SessionManager(requireContext())
        val role = sessionManager.getUserRole()

        val layoutRes = when (role) {
            SessionManager.ROLE_PETUGAS -> R.layout.fragment_dashboard_petugas
            SessionManager.ROLE_RELAWAN -> R.layout.fragment_dashboard_relawan
            else -> R.layout.fragment_dashboard_masyarakat
        }
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSapaan = view.findViewById(R.id.tvSapaan)
        tvNama = view.findViewById(R.id.tvNama)
        tvStatBaru = view.findViewById(R.id.tvStatBaru)
        tvStatProses = view.findViewById(R.id.tvStatProses)
        tvStatSelesai = view.findViewById(R.id.tvStatSelesai)
        tvStatRelawan = view.findViewById(R.id.tvStatRelawan)
        tvBannerJudul = view.findViewById(R.id.tvBannerJudul)
        tvBannerDesc = view.findViewById(R.id.tvBannerDesc)

        val rvTerbaru = view.findViewById<RecyclerView>(R.id.rvTerbaru)

        tvSapaan?.text = getSapaanIndonesia()
        tvNama?.text = sessionManager.getUserName().ifEmpty { "Pengguna" }

        laporanAdapter = TerbaruAdapter(terbaruList) { laporan ->
            val intent = Intent(requireContext(), DetailLaporanActivity::class.java).apply {
                putExtra(DetailLaporanActivity.EXTRA_ID, laporan.id)
            }
            startActivity(intent)
        }

        rvTerbaru?.layoutManager = LinearLayoutManager(requireContext())
        rvTerbaru?.adapter = laporanAdapter

        view.findViewById<ImageButton>(R.id.btnNotifikasi)?.setOnClickListener {
            NotifikasiPopupHelper.show(
                context = requireContext(),
                laporanList = terbaruList,
                onOpenLaporan = { laporanId ->
                    val intent = Intent(requireContext(), DetailLaporanActivity::class.java).apply {
                        putExtra(DetailLaporanActivity.EXTRA_ID, laporanId)
                    }
                    startActivity(intent)
                }
            )
        }

        view.findViewById<LinearLayout>(R.id.aksiLapor)?.setOnClickListener {
            startActivity(Intent(requireContext(), BuatLaporanActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.aksiPeta)?.setOnClickListener {
            startActivity(Intent(requireContext(), PetaSiagaActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.aksiRelawan)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_relawan)
        }

        view.findViewById<LinearLayout>(R.id.aksiTugas)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_laporan)
        }

        view.findViewById<LinearLayout>(R.id.aksiRiwayat)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_riwayat)
        }

        view.findViewById<LinearLayout>(R.id.bannerSiaga)?.setOnClickListener {
            startActivity(Intent(requireContext(), PetaSiagaActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tvLihatSemua)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_laporan)
        }

        loadDashboardStats()
    }

    override fun onResume() {
        super.onResume()
        tvSapaan?.text = getSapaanIndonesia()
        tvNama?.text = sessionManager.getUserName().ifEmpty { "Pengguna" }
        loadDashboardStats()
    }

    private fun getSapaanIndonesia(): String {
        val hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..10 -> "Selamat pagi,"
            in 11..14 -> "Selamat siang,"
            in 15..18 -> "Selamat sore,"
            else -> "Selamat malam,"
        }
    }

    private fun loadDashboardStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(requireContext()).getDashboard()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        val role = sessionManager.getUserRole()
                        val statusMap = data.per_status
                        val baruCount = statusMap["baru"] ?: 0
                        val diverifikasiCount = statusMap["diverifikasi"] ?: 0
                        val ditanganiCount = statusMap["ditangani"] ?: 0
                        val selesaiCount = statusMap["selesai"] ?: 0

                        if (role == SessionManager.ROLE_RELAWAN) {
                            tvStatBaru?.text = (baruCount + diverifikasiCount).toString()
                            tvStatProses?.text = ditanganiCount.toString()
                            tvStatSelesai?.text = selesaiCount.toString()
                            tvStatRelawan?.text = (data.tugas_aktif ?: 0).toString()
                        } else {
                            tvStatBaru?.text = baruCount.toString()
                            tvStatProses?.text = (diverifikasiCount + ditanganiCount).toString()
                            tvStatSelesai?.text = selesaiCount.toString()
                            tvStatRelawan?.text = (data.relawan_total ?: 0).toString()
                        }

                        val kritisCount = data.per_urgensi["kritis"] ?: 0
                        if (kritisCount > 0) {
                            tvBannerJudul?.text = "Siaga Bencana Kritis"
                            tvBannerDesc?.text = "$kritisCount laporan kritis memerlukan penanganan segera. Buka peta siaga."
                        } else {
                            tvBannerJudul?.text = "Kondisi Wilayah Aman"
                            tvBannerDesc?.text = "Tidak ada laporan berstatus kritis saat ini."
                        }

                        terbaruList.clear()
                        terbaruList.addAll(data.laporan_terbaru)
                        laporanAdapter.notifyDataSetChanged()
                    }
                }
            } catch (_: Exception) {
                // Keep UI values intact on network failure
            }
        }
    }

    private class TerbaruAdapter(
        private val list: List<Laporan>,
        private val onClick: (Laporan) -> Unit
    ) : RecyclerView.Adapter<TerbaruAdapter.VH>() {

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

            if (!l.foto_url.isNullOrBlank()) {
                Glide.with(holder.b.root.context)
                    .load(l.foto_url)
                    .placeholder(R.drawable.bg_thumb)
                    .into(holder.b.imgThumb)
            } else {
                holder.b.imgThumb.setImageResource(R.drawable.bg_thumb)
            }

            holder.b.root.setOnClickListener { onClick(l) }
        }
    }
}
