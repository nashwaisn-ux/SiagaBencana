<?php

namespace App\Http\Controllers;

use App\Models\Laporan;
use App\Models\Penugasan;
use App\Models\RiwayatStatus;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class LaporanController extends Controller
{
    public function index(Request $request)
    {
        $query = Laporan::with(['kategori', 'pelapor', 'penugasan.relawan']);

        if ($request->has('search') && !empty($request->input('search'))) {
            $search = $request->input('search');
            $query->where(function ($q) use ($search) {
                $q->where('judul', 'like', "%{$search}%")
                  ->orWhere('deskripsi', 'like', "%{$search}%")
                  ->orWhereHas('kategori', function ($kq) use ($search) {
                      $kq->where('nama', 'like', "%{$search}%");
                  });
            });
        }

        if ($request->has('status') && !empty($request->input('status'))) {
            $statuses = array_filter(explode(',', $request->input('status')));
            if (!empty($statuses)) {
                $query->whereIn('status', $statuses);
            }
        }

        if ($request->has('urgensi') && !empty($request->input('urgensi'))) {
            $urgencies = array_filter(explode(',', $request->input('urgensi')));
            if (!empty($urgencies)) {
                $query->whereIn('urgensi', $urgencies);
            }
        }

        if ($request->has('kategori_id') && !empty($request->input('kategori_id'))) {
            $query->where('kategori_id', $request->input('kategori_id'));
        }

        $query->orderBy('created_at', 'desc');

        $perPage = min(max((int) $request->input('per_page', 10), 1), 50);
        $paginator = $query->paginate($perPage);

        return $this->paginatedResponse($paginator, 'OK', function ($l) {
            return $this->formatLaporanList($l);
        });
    }

    public function show(Request $request, $id)
    {
        $laporan = Laporan::with(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user'])->find($id);

        if (!$laporan) {
            return $this->errorResponse('Laporan tidak ditemukan', 404);
        }

        $user = $request->user();
        if ($user->role === 'masyarakat') {
            if ($laporan->user_id !== $user->id) {
                // Masyarakat tetap dapat membaca detail jika diperlukan untuk pantauan
            }
        }

        return $this->successResponse($this->formatLaporanDetail($laporan), 'OK');
    }

    public function store(Request $request)
    {
        $user = $request->user();

        $validated = $request->validate([
            'judul' => 'required|string|max:120',
            'deskripsi' => 'required|string',
            'kategori_id' => 'required|exists:kategori_bencana,id',
            'latitude' => 'required|numeric|between:-90,90',
            'longitude' => 'required|numeric|between:-180,180',
            'jumlah_terdampak' => 'nullable|integer|min:0|max:1000000',
            'foto' => 'nullable|file|image|max:30720',
        ]);

        $fotoPath = null;
        if ($request->hasFile('foto')) {
            $fotoPath = $request->file('foto')->store('laporan', 'public');
        }

        $jumlahTerdampak = $validated['jumlah_terdampak'] ?? 0;
        $isSos = $request->boolean('sos') || $request->input('sos') === 'true' || $request->input('sos') === '1';
        $urgensi = $isSos ? 'kritis' : $this->hitungUrgensi($validated['kategori_id'], $jumlahTerdampak, $validated['deskripsi']);

        $laporan = Laporan::create([
            'user_id' => $user->id,
            'kategori_id' => $validated['kategori_id'],
            'judul' => $validated['judul'],
            'deskripsi' => $validated['deskripsi'],
            'foto' => $fotoPath,
            'latitude' => $validated['latitude'],
            'longitude' => $validated['longitude'],
            'jumlah_terdampak' => $jumlahTerdampak,
            'urgensi' => $urgensi,
            'status' => 'baru',
        ]);

        RiwayatStatus::create([
            'laporan_id' => $laporan->id,
            'user_id' => $user->id,
            'status_lama' => null,
            'status_baru' => 'baru',
            'keterangan' => 'Laporan dibuat',
        ]);

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Laporan berhasil dibuat', 201);
    }

    public function update(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::find($id);

        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role === 'masyarakat') {
            if ($laporan->user_id !== $user->id) {
                return $this->errorResponse('Akses ditolak', 403);
            }
            if ($laporan->status !== 'baru') {
                return $this->errorResponse('Laporan sudah diproses dan tidak dapat diubah', 403);
            }
        } elseif ($user->role === 'relawan') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $validated = $request->validate([
            'judul' => 'sometimes|required|string|max:120',
            'deskripsi' => 'sometimes|required|string',
            'kategori_id' => 'sometimes|required|exists:kategori_bencana,id',
            'latitude' => 'sometimes|required|numeric|between:-90,90',
            'longitude' => 'sometimes|required|numeric|between:-180,180',
            'jumlah_terdampak' => 'sometimes|nullable|integer|min:0|max:1000000',
            'foto' => 'sometimes|nullable|file|image|max:30720',
        ]);

        if ($request->hasFile('foto')) {
            if ($laporan->foto) {
                $decoded = json_decode($laporan->foto, true);
                if (is_array($decoded)) {
                    foreach ($decoded as $f) {
                        Storage::disk('public')->delete($f);
                    }
                } else {
                    Storage::disk('public')->delete($laporan->foto);
                }
            }
            $laporan->foto = $request->file('foto')->store('laporan', 'public');
        }

        $kategoriId = $validated['kategori_id'] ?? $laporan->kategori_id;
        $jumlahTerdampak = isset($validated['jumlah_terdampak']) ? $validated['jumlah_terdampak'] : $laporan->jumlah_terdampak;
        $deskripsi = $validated['deskripsi'] ?? $laporan->deskripsi;

        if ($laporan->urgensi !== 'kritis') {
            $laporan->urgensi = $this->hitungUrgensi($kategoriId, $jumlahTerdampak, $deskripsi);
        }

        $laporan->fill(collect($validated)->except(['foto', 'jumlah_terdampak'])->toArray());
        if (isset($validated['jumlah_terdampak'])) {
            $laporan->jumlah_terdampak = $jumlahTerdampak;
        }
        $laporan->save();

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Laporan berhasil diperbarui');
    }

    public function destroy(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::find($id);

        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role === 'masyarakat') {
            if ($laporan->user_id !== $user->id || $laporan->status !== 'baru') {
                return $this->errorResponse('Akses ditolak', 403);
            }
        } elseif ($user->role === 'relawan') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        if ($laporan->foto) {
            $decoded = json_decode($laporan->foto, true);
            if (is_array($decoded)) {
                foreach ($decoded as $f) {
                    Storage::disk('public')->delete($f);
                }
            } else {
                Storage::disk('public')->delete($laporan->foto);
            }
        }

        RiwayatStatus::create([
            'laporan_id' => $laporan->id,
            'user_id' => $user->id,
            'status_lama' => $laporan->status,
            'status_baru' => 'dihapus',
            'keterangan' => 'Laporan dihapus oleh ' . ($user->nama ?? 'pengguna'),
        ]);

        $laporan->delete();

        return $this->successResponse(null, 'Laporan berhasil dihapus');
    }

    public function updateStatus(Request $request, $id)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Hanya petugas yang dapat mengubah status laporan', 403);
        }

        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Laporan tidak ditemukan', 404);
        }

        $validated = $request->validate([
            'status' => 'required|in:baru,diverifikasi,ditangani,selesai',
            'keterangan' => 'nullable|string|max:255',
        ]);

        $statusLama = $laporan->status;
        $statusBaru = strtolower($validated['status']);

        if ($statusLama === $statusBaru) {
            return $this->successResponse($this->formatLaporanDetail($laporan), 'Status tidak berubah');
        }

        $laporan->status = $statusBaru;
        $laporan->save();

        if ($statusBaru === 'selesai') {
            Penugasan::where('laporan_id', $laporan->id)
                ->whereIn('status_tugas', ['ditugaskan', 'diterima'])
                ->update(['status_tugas' => 'selesai']);
        }

        RiwayatStatus::create([
            'laporan_id' => $laporan->id,
            'user_id' => $user->id,
            'status_lama' => $statusLama,
            'status_baru' => $statusBaru,
            'keterangan' => $validated['keterangan'] ?? ("Status diubah menjadi " . ucfirst($statusBaru)),
        ]);

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Status laporan berhasil diubah');
    }

    public function tugaskanRelawan(Request $request, $id)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Hanya petugas yang dapat menugaskan relawan', 403);
        }

        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Laporan tidak ditemukan', 404);
        }

        $validated = $request->validate([
            'relawan_id' => 'required|exists:users,id',
            'catatan' => 'nullable|string',
        ]);

        $existing = Penugasan::where('laporan_id', $laporan->id)
            ->where('relawan_id', $validated['relawan_id'])
            ->first();

        if ($existing) {
            return $this->errorResponse('Relawan sudah ditugaskan pada laporan ini', 400);
        }

        $penugasan = Penugasan::create([
            'laporan_id' => $laporan->id,
            'petugas_id' => $user->id,
            'relawan_id' => $validated['relawan_id'],
            'catatan' => $validated['catatan'] ?? 'Penugasan lapangan',
            'status_tugas' => 'ditugaskan',
        ]);

        if ($laporan->status === 'baru') {
            $laporan->status = 'diverifikasi';
            $laporan->save();
        }

        RiwayatStatus::create([
            'laporan_id' => $laporan->id,
            'user_id' => $user->id,
            'status_lama' => $laporan->status,
            'status_baru' => 'diverifikasi',
            'keterangan' => 'Relawan ditugaskan untuk penanganan lapangan',
        ]);

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Relawan berhasil ditugaskan');
    }
}
