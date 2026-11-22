<?php

namespace App\Http\Controllers;

use App\Models\Penugasan;
use App\Models\RiwayatStatus;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class PenugasanController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        $query = Penugasan::with(['relawan', 'petugas', 'laporan']);

        if ($user->role === 'relawan') {
            $query->where('relawan_id', $user->id);
        } elseif ($user->role === 'masyarakat') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        if ($request->has('status_tugas')) {
            $query->where('status_tugas', $request->input('status_tugas'));
        }

        $query->orderBy('created_at', 'desc');
        $perPage = min(max((int) $request->input('per_page', 10), 1), 50);
        $paginator = $query->paginate($perPage);

        $transform = function ($p) {
            return [
                'id' => $p->id,
                'status_tugas' => $p->status_tugas,
                'catatan' => $p->catatan,
                'foto_bukti_url' => $p->foto_bukti ? url('storage/' . $p->foto_bukti) : null,
                'created_at' => $p->created_at?->toIso8601String(),
                'relawan' => $this->formatUserPelapor($p->relawan),
                'laporan' => $p->laporan ? [
                    'id' => $p->laporan->id,
                    'judul' => $p->laporan->judul,
                    'urgensi' => $p->laporan->urgensi,
                    'status' => $p->laporan->status,
                ] : null,
            ];
        };

        return $this->paginatedResponse($paginator, 'OK', $transform);
    }

    public function updateStatus(Request $request, $id)
    {
        $user = $request->user();
        $penugasan = Penugasan::with(['laporan', 'relawan', 'petugas'])->find($id);

        if (!$penugasan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role !== 'relawan' || $penugasan->relawan_id !== $user->id) {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $validated = $request->validate([
            'status_tugas' => 'required|in:diterima,selesai',
        ]);

        if ($penugasan->status_tugas === 'selesai' && $validated['status_tugas'] !== 'selesai') {
            return $this->errorResponse('Penugasan yang sudah selesai tidak dapat diubah kembali.', 422);
        }

        if ($penugasan->status_tugas !== $validated['status_tugas']) {
            $oldStatus = $penugasan->status_tugas;
            $newStatus = $validated['status_tugas'];

            $penugasan->update([
                'status_tugas' => $newStatus,
            ]);

            if ($penugasan->laporan_id) {
                RiwayatStatus::create([
                    'laporan_id' => $penugasan->laporan_id,
                    'user_id' => $user->id,
                    'status_lama' => $penugasan->laporan->status ?? null,
                    'status_baru' => $penugasan->laporan->status ?? null,
                    'keterangan' => "Status penugasan relawan (" . ($penugasan->relawan->nama ?? 'Relawan') . ") diubah dari {$oldStatus} menjadi {$newStatus}",
                ]);
            }
        }

        return $this->successResponse([
            'id' => $penugasan->id,
            'status_tugas' => $penugasan->status_tugas,
            'catatan' => $penugasan->catatan,
            'foto_bukti_url' => $penugasan->foto_bukti ? url('storage/' . $penugasan->foto_bukti) : null,
            'created_at' => $penugasan->created_at?->toIso8601String(),
            'relawan' => $this->formatUserPelapor($penugasan->relawan),
            'laporan' => $penugasan->laporan ? [
                'id' => $penugasan->laporan->id,
                'judul' => $penugasan->laporan->judul,
                'urgensi' => $penugasan->laporan->urgensi,
                'status' => $penugasan->laporan->status,
            ] : null,
        ], 'Status tugas berhasil diperbarui');
    }

    public function uploadBukti(Request $request, $id)
    {
        $user = $request->user();
        $penugasan = Penugasan::with(['laporan', 'relawan', 'petugas'])->find($id);

        if (!$penugasan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role !== 'relawan' || $penugasan->relawan_id !== $user->id) {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $validated = $request->validate([
            'foto_bukti' => 'required|image|max:4096',
            'catatan' => 'nullable|string',
        ]);

        if ($request->hasFile('foto_bukti')) {
            if ($penugasan->foto_bukti) {
                Storage::disk('public')->delete($penugasan->foto_bukti);
            }
            $path = $request->file('foto_bukti')->store('bukti', 'public');
            $penugasan->foto_bukti = $path;
        }

        if (isset($validated['catatan'])) {
            $penugasan->catatan = $validated['catatan'];
        }

        $penugasan->save();

        return $this->successResponse([
            'id' => $penugasan->id,
            'status_tugas' => $penugasan->status_tugas,
            'catatan' => $penugasan->catatan,
            'foto_bukti_url' => $penugasan->foto_bukti ? url('storage/' . $penugasan->foto_bukti) : null,
            'created_at' => $penugasan->created_at?->toIso8601String(),
            'relawan' => $this->formatUserPelapor($penugasan->relawan),
            'laporan' => $penugasan->laporan ? [
                'id' => $penugasan->laporan->id,
                'judul' => $penugasan->laporan->judul,
                'urgensi' => $penugasan->laporan->urgensi,
                'status' => $penugasan->laporan->status,
            ] : null,
        ], 'Bukti penugasan berhasil diunggah');
    }
}
