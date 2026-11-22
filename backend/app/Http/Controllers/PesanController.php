<?php

namespace App\Http\Controllers;

use App\Models\Laporan;
use App\Models\Penugasan;
use App\Models\Pesan;
use Illuminate\Http\Request;

class PesanController extends Controller
{
    private function boleh($user, $laporan): bool
    {
        if ($user->role === 'petugas') {
            return true;
        }
        if ($user->role === 'relawan') {
            return Penugasan::where('laporan_id', $laporan->id)
                ->where('relawan_id', $user->id)
                ->exists();
        }
        return false;
    }

    private function format($p): array
    {
        return [
            'id' => $p->id,
            'laporan_id' => $p->laporan_id,
            'isi' => $p->isi,
            'pengirim' => $p->pengirim ? [
                'id' => $p->pengirim->id,
                'nama' => $p->pengirim->nama,
                'role' => $p->pengirim->role,
            ] : null,
            'created_at' => $p->created_at?->toIso8601String(),
        ];
    }

    public function index(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Laporan tidak ditemukan', 404);
        }
        if (!$this->boleh($user, $laporan)) {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $query = Pesan::with('pengirim')->where('laporan_id', $laporan->id);
        if ($request->filled('after_id')) {
            $query->where('id', '>', (int) $request->input('after_id'));
        }
        $items = $query->orderBy('id')->limit(200)->get()
            ->map(fn ($p) => $this->format($p))->values();

        return $this->successResponse($items, 'OK');
    }

    public function store(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Laporan tidak ditemukan', 404);
        }
        if (!$this->boleh($user, $laporan)) {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $validated = $request->validate([
            'isi' => 'required|string|max:1000',
        ]);

        $pesan = Pesan::create([
            'laporan_id' => $laporan->id,
            'pengirim_id' => $user->id,
            'isi' => trim($validated['isi']),
        ]);
        $pesan->load('pengirim');

        return $this->successResponse($this->format($pesan), 'Pesan terkirim', 201);
    }
}
