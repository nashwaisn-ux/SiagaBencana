package com.example.siagabencana

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app.databinding.FragmentDetailProfilBinding // Sesuaikan dengan nama file XML kamu

class DetailProfilFragment : Fragment() {

    private var _binding: FragmentDetailProfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        tampilkanDataProfil()
    }

    private fun setupListeners() {
        binding.btnKembaliDetailProfil.setOnClickListener {
            // Jika menggunakan Navigation Component:
            findNavController().navigateUp()
            // Atau jika pakai FragmentManager biasa, gunakan:
            // parentFragmentManager.popBackStack()
        }
    }

    private fun tampilkanDataProfil() {
        // Contoh data — ganti dengan data dari ViewModel, SharedPreferences, atau Arguments
        val nama = "Siti Rahma"
        val role = "Masyarakat"
        val email = "siti.rahma@email.com"
        val telepon = "0812-3456-7890"
        val alamat = "Jl. Sudirman No. 123, Pekanbaru"
        val isTerverifikasi = true
        val fotoUrl: String? = null

        binding.tvDetailNama.text = nama
        binding.tvDetailRole.text = role
        binding.tvDetailEmail.text = email
        binding.tvDetailTelepon.text = telepon
        binding.tvDetailAlamat.text = alamat

        // Inisial avatar jika tidak ada foto profil
        binding.tvDetailAvatar.text = buatInisial(nama)

        if (!fotoUrl.isNullOrEmpty()) {
            binding.imgDetailAvatar.visibility = View.VISIBLE
            binding.tvDetailAvatar.visibility = View.GONE
            // Jika pakai Glide / Coil, muat foto ke binding.imgDetailAvatar di sini
            // Glide.with(this).load(fotoUrl).into(binding.imgDetailAvatar)
        } else {
            binding.imgDetailAvatar.visibility = View.GONE
            binding.tvDetailAvatar.visibility = View.VISIBLE
        }

        // Status akun
        if (isTerverifikasi) {
            binding.tvDetailStatusAkun.text = "Terverifikasi"
            binding.tvDetailStatusAkun.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.siaga_green)
            )
        } else {
            binding.tvDetailStatusAkun.text = "Belum Terverifikasi"
            binding.tvDetailStatusAkun.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.siaga_red)
            )
        }
    }

    private fun buatInisial(nama: String): String {
        val bagian = nama.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (bagian.isEmpty()) return "?"
        return if (bagian.size == 1) {
            bagian[0].take(2).uppercase()
        } else {
            "${bagian.first().first()}${bagian.last().first()}".uppercase()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}