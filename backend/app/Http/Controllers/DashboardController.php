<?php

namespace App\Http\Controllers;

use App\Models\Laporan;
use App\Models\KategoriBencana;
use Illuminate\Http\Request;

class DashboardController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        $baseQuery = Laporan::query();

        if ($user->role === 'masyarakat') {
            $baseQuery->where('user_id', $user->id);
        } elseif ($user->role === 'relawan') {
            $baseQuery->whereHas('penugasan', function ($q) use ($user) {
                $q->where('relawan_id', $user->id);
            });
        }

        $totalLaporan = (clone $baseQuery)->count();

        // Per status
        $statuses = ['baru', 'diverifikasi', 'ditangani', 'selesai'];
        $perStatus = [];
        foreach ($statuses as $st) {
            $perStatus[$st] = (clone $baseQuery)->where('status', $st)->count();
        }

        // Per urgensi
        $urgencies = ['rendah', 'sedang', 'tinggi', 'kritis'];
        $perUrgensi = [];
        foreach ($urgencies as $urg) {
            $perUrgensi[$urg] = (clone $baseQuery)->where('urgensi', $urg)->count();
        }

        // Per kategori
        $kategoris = KategoriBencana::all();
        $perKategori = [];
        foreach ($kategoris as $kat) {
            $count = (clone $baseQuery)->where('kategori_id', $kat->id)->count();
            $perKategori[] = [
                'kategori' => $kat->nama,
                'total' => $count,
            ];
        }

        // Laporan terbaru (max 5)
        $laporanTerbaru = (clone $baseQuery)->with(['kategori', 'pelapor'])
            ->orderBy('created_at', 'desc')
            ->limit(5)
            ->get()
            ->map(function ($l) {
                return $this->formatLaporanList($l);
            });

        // Tugas aktif (hanya relawan)
        $tugasAktif = null;
        if ($user->role === 'relawan') {
            $tugasAktif = \App\Models\Penugasan::where('relawan_id', $user->id)
                ->whereIn('status_tugas', ['ditugaskan', 'diterima'])
                ->count();
        }

        return $this->successResponse([
            'total_laporan' => $totalLaporan,
            'per_status' => $perStatus,
            'per_urgensi' => $perUrgensi,
            'per_kategori' => $perKategori,
            'laporan_terbaru' => $laporanTerbaru,
            'tugas_aktif' => $tugasAktif,
        ], 'OK');
    }
}
