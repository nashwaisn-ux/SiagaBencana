<?php

namespace App\Http\Controllers;

abstract class Controller
{
    protected function successResponse($data = null, $message = 'OK', $code = 200)
    {
        return response()->json([
            'success' => true,
            'message' => $message,
            'data' => $data,
        ], $code);
    }

    protected function paginatedResponse($paginator, $message = 'OK', $transformCallback = null)
    {
        $items = $paginator->items();
        if ($transformCallback && is_callable($transformCallback)) {
            $items = array_map($transformCallback, $items);
        }

        return response()->json([
            'success' => true,
            'message' => $message,
            'data' => [
                'items' => array_values($items),
                'meta' => [
                    'current_page' => $paginator->currentPage(),
                    'last_page' => $paginator->lastPage(),
                    'per_page' => $paginator->perPage(),
                    'total' => $paginator->total(),
                ],
            ],
        ]);
    }

    protected function errorResponse($message, $code = 400, $errors = null)
    {
        $response = [
            'success' => false,
            'message' => $message,
        ];
        if (!is_null($errors)) {
            $response['errors'] = $errors;
        }
        return response()->json($response, $code);
    }

    protected function formatUser($user)
    {
        if (!$user) return null;
        $fotoUrl = null;
        if (!empty($user->foto_url)) {
            $fotoUrl = str_starts_with($user->foto_url, 'http') ? $user->foto_url : url('storage/' . $user->foto_url);
        }
        return [
            'id' => $user->id,
            'nama' => $user->nama,
            'email' => $user->email ?? null,
            'role' => $user->role ?? null,
            'no_hp' => $user->no_hp ?? null,
            'alamat' => $user->alamat ?? null,
            'foto_url' => $fotoUrl,
        ];
    }

    protected function formatKategori($kategori)
    {
        if (!$kategori) return null;
        return [
            'id' => $kategori->id,
            'nama' => $kategori->nama,
        ];
    }

    protected function formatLaporanList($laporan)
    {
        return [
            'id' => $laporan->id,
            'judul' => $laporan->judul,
            'deskripsi' => $laporan->deskripsi,
            'foto_url' => $laporan->foto ? url('storage/' . $laporan->foto) : null,
            'latitude' => (float) $laporan->latitude,
            'longitude' => (float) $laporan->longitude,
            'jumlah_terdampak' => (int) $laporan->jumlah_terdampak,
            'urgensi' => $laporan->urgensi,
            'status' => $laporan->status,
            'kategori' => $this->formatKategori($laporan->kategori),
            'pelapor' => $this->formatUserPelapor($laporan->pelapor),
            'created_at' => $laporan->created_at?->toIso8601String(),
            'updated_at' => $laporan->updated_at?->toIso8601String(),
        ];
    }

    protected function formatUserPelapor($user)
    {
        if (!$user) return null;
        return [
            'id' => $user->id,
            'nama' => $user->nama,
            'no_hp' => $user->no_hp ?? null,
        ];
    }

    protected function formatLaporanDetail($laporan)
    {
        $data = $this->formatLaporanList($laporan);

        $data['penugasan'] = $laporan->penugasan->map(function ($p) {
            return [
                'id' => $p->id,
                'relawan' => $this->formatUserPelapor($p->relawan),
                'petugas' => $this->formatUserPelapor($p->petugas),
                'catatan' => $p->catatan,
                'status_tugas' => $p->status_tugas,
                'foto_bukti_url' => $p->foto_bukti ? url('storage/' . $p->foto_bukti) : null,
                'created_at' => $p->created_at?->toIso8601String(),
            ];
        })->values();

        $data['riwayat'] = $laporan->riwayat->map(function ($r) {
            return [
                'id' => $r->id,
                'status_lama' => $r->status_lama,
                'status_baru' => $r->status_baru,
                'keterangan' => $r->keterangan,
                'user' => $this->formatUserPelapor($r->user),
                'created_at' => $r->created_at?->toIso8601String(),
            ];
        })->values();

        return $data;
    }

    protected function hitungUrgensi($kategoriId, $jumlahTerdampak, $deskripsi)
    {
        $kategori = \App\Models\KategoriBencana::find($kategoriId);
        $bobot = $kategori ? $kategori->bobot_urgensi : 1;

        $skorTerdampak = 0;
        if ($jumlahTerdampak >= 50) {
            $skorTerdampak = 3;
        } elseif ($jumlahTerdampak >= 20) {
            $skorTerdampak = 2;
        } elseif ($jumlahTerdampak >= 5) {
            $skorTerdampak = 1;
        }

        $skorKata = 0;
        $keywords = ['korban', 'terjebak', 'meluas', 'evakuasi', 'meninggal'];
        $descLower = strtolower($deskripsi);
        foreach ($keywords as $kw) {
            if (str_contains($descLower, $kw)) {
                $skorKata = 2;
                break;
            }
        }

        $totalSkor = $bobot + $skorTerdampak + $skorKata;

        if ($totalSkor >= 7) return 'kritis';
        if ($totalSkor >= 5) return 'tinggi';
        if ($totalSkor >= 3) return 'sedang';
        return 'rendah';
    }
}
