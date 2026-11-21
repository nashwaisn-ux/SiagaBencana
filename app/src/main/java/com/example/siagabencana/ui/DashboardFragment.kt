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
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private val terbaruList = mutableListOf<Laporan>()
    private lateinit var laporanAdapter: TerbaruAdapter

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

        val tvNama = view.findViewById<TextView>(R.id.tvNama)
        val tvStatBaru = view.findViewById<TextView>(R.id.tvStatBaru)
        val tvStatProses = view.findViewById<TextView>(R.id.tvStatProses)
        val tvStatSelesai = view.findViewById<TextView>(R.id.tvStatSelesai)
        val tvStatRelawan = view.findViewById<TextView>(R.id.tvStatRelawan)
        val rvTerbaru = view.findViewById<RecyclerView>(R.id.rvTerbaru)

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
            Toast.makeText(requireContext(), "Tidak ada notifikasi baru", Toast.LENGTH_SHORT).show()
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

        loadDashboardStats(tvStatBaru, tvStatProses, tvStatSelesai, tvStatRelawan)
    }

    private fun loadDashboardStats(
        tvStatBaru: TextView?,
        tvStatProses: TextView?,
        tvStatSelesai: TextView?,
        tvStatRelawan: TextView?
    ) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(requireContext()).getDashboard()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        val statusMap = data.per_status
                        val baruCount = statusMap["baru"] ?: 0
                        val diverifikasiCount = statusMap["diverifikasi"] ?: 0
                        val ditanganiCount = statusMap["ditangani"] ?: 0
                        val selesaiCount = statusMap["selesai"] ?: 0

                        tvStatBaru?.text = baruCount.toString()
                        tvStatProses?.text = (diverifikasiCount + ditanganiCount).toString()
                        tvStatSelesai?.text = selesaiCount.toString()
                        tvStatRelawan?.text = (data.tugas_aktif ?: 24).toString()

                        terbaruList.clear()
                        terbaruList.addAll(data.laporan_terbaru)
                        laporanAdapter.notifyDataSetChanged()
                    }
                }
            } catch (_: Exception) {
                // Keep placeholder stats on network failure
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
