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
        $user = $request->user();
        $query = Laporan::with(['kategori', 'pelapor']);

        if ($user->role === 'masyarakat') {
            $query->where('user_id', $user->id);
        } elseif ($user->role === 'relawan') {
            $query->whereHas('penugasan', function ($q) use ($user) {
                $q->where('relawan_id', $user->id);
            });
        }

        if ($request->has('search')) {
            $search = $request->input('search');
            $query->where(function ($q) use ($search) {
                $q->where('judul', 'like', "%{$search}%")
                  ->orWhere('deskripsi', 'like', "%{$search}%");
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

        if ($request->has('kategori_id')) {
            $query->where('kategori_id', $request->input('kategori_id'));
        }

        $query->orderBy('created_at', 'desc');
        $perPage = min(max((int) $request->input('per_page', 10), 1), 50);
        $paginator = $query->paginate($perPage);

        $transform = function ($l) {
            return $this->formatLaporanList($l);
        };

        return $this->paginatedResponse($paginator, 'OK', $transform);
    }

    public function show(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::with(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user'])->find($id);

        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role === 'masyarakat' && $laporan->user_id !== $user->id) {
            return $this->errorResponse('Akses ditolak', 403);
        }

        if ($user->role === 'relawan') {
            $isAssigned = $laporan->penugasan()->where('relawan_id', $user->id)->exists();
            if (!$isAssigned) {
                return $this->errorResponse('Akses ditolak', 403);
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
            'jumlah_terdampak' => 'nullable|integer|min:0',
            'foto' => 'nullable|image|max:30720',
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
            'jumlah_terdampak' => 'sometimes|nullable|integer|min:0',
            'foto' => 'sometimes|nullable|image|max:30720',
        ]);

        if ($request->hasFile('foto')) {
            if ($laporan->foto) {
                Storage::disk('public')->delete($laporan->foto);
            }
            $laporan->foto = $request->file('foto')->store('laporan', 'public');
        }

        $kategoriId = $validated['kategori_id'] ?? $laporan->kategori_id;
        $jumlahTerdampak = isset($validated['jumlah_terdampak']) ? $validated['jumlah_terdampak'] : $laporan->jumlah_terdampak;
        $deskripsi = $validated['deskripsi'] ?? $laporan->deskripsi;

        $laporan->urgensi = $this->hitungUrgensi($kategoriId, $jumlahTerdampak, $deskripsi);

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
            Storage::disk('public')->delete($laporan->foto);
        }

        $laporan->delete();

        return $this->successResponse(null, 'Laporan dihapus');
    }

    public function bulkDelete(Request $request)
    {
        $user = $request->user();
        $validated = $request->validate([
            'ids' => 'required|array|min:1|max:50',
            'ids.*' => 'required|integer',
        ]);

        $ids = $validated['ids'];
        $deletedCount = 0;
        $skipped = [];

        foreach ($ids as $id) {
            $laporan = Laporan::find($id);
            if (!$laporan) {
                $skipped[] = $id;
                continue;
            }

            if ($user->role === 'masyarakat' && ($laporan->user_id !== $user->id || $laporan->status !== 'baru')) {
                $skipped[] = $id;
                continue;
            }

            if ($user->role === 'relawan') {
                $skipped[] = $id;
                continue;
            }

            if ($laporan->foto) {
                Storage::disk('public')->delete($laporan->foto);
            }
            $laporan->delete();
            $deletedCount++;
        }

        return $this->successResponse([
            'deleted' => $deletedCount,
            'skipped' => $skipped,
        ], "{$deletedCount} laporan dihapus");
    }

    public function tugaskan(Request $request, $id)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($laporan->status === 'selesai') {
            return $this->errorResponse('Laporan sudah selesai dan tidak dapat ditugaskan relawan baru.', 422);
        }

        $validated = $request->validate([
            'relawan_id' => [
                'required',
                'integer',
                function ($attribute, $value, $fail) {
                    $relawan = \App\Models\User::where('id', $value)->where('role', 'relawan')->first();
                    if (!$relawan) {
                        $fail('relawan_id bukan user ber-role relawan.');
                    }
                },
            ],
            'catatan' => 'nullable|string',
        ]);

        $alreadyAssigned = Penugasan::where('laporan_id', $laporan->id)
            ->where('relawan_id', $validated['relawan_id'])
            ->whereIn('status_tugas', ['ditugaskan', 'diterima'])
            ->exists();

        if ($alreadyAssigned) {
            return $this->errorResponse('Relawan sudah ditugaskan pada laporan ini.', 422);
        }

        $penugasan = Penugasan::create([
            'laporan_id' => $laporan->id,
            'relawan_id' => $validated['relawan_id'],
            'petugas_id' => $user->id,
            'catatan' => $validated['catatan'] ?? null,
            'status_tugas' => 'ditugaskan',
        ]);

        if ($laporan->status === 'baru') {
            $statusLama = $laporan->status;
            $laporan->status = 'diverifikasi';
            $laporan->save();

            RiwayatStatus::create([
                'laporan_id' => $laporan->id,
                'user_id' => $user->id,
                'status_lama' => $statusLama,
                'status_baru' => 'diverifikasi',
                'keterangan' => 'Laporan diverifikasi otomatis saat ditugaskan',
            ]);
        }

        $penugasan->load(['relawan', 'petugas']);

        return $this->successResponse([
            'id' => $penugasan->id,
            'laporan_id' => $penugasan->laporan_id,
            'relawan' => $this->formatUserPelapor($penugasan->relawan),
            'petugas' => $this->formatUserPelapor($penugasan->petugas),
            'catatan' => $penugasan->catatan,
            'status_tugas' => $penugasan->status_tugas,
            'foto_bukti_url' => null,
            'created_at' => $penugasan->created_at?->toIso8601String(),
        ], 'Relawan berhasil ditugaskan', 201);
    }

    public function updateStatus(Request $request, $id)
    {
        $user = $request->user();
        $laporan = Laporan::find($id);

        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        if ($user->role === 'masyarakat') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        if ($user->role === 'relawan') {
            $isAssigned = $laporan->penugasan()->where('relawan_id', $user->id)->exists();
            if (!$isAssigned) {
                return $this->errorResponse('Akses ditolak', 403);
            }
        }

        $allowedStatuses = $user->role === 'relawan' ? ['ditangani', 'selesai'] : ['baru', 'diverifikasi', 'ditangani', 'selesai'];

        $validated = $request->validate([
            'status' => 'required|in:' . implode(',', $allowedStatuses),
            'keterangan' => 'nullable|string',
        ]);

        $statusLama = $laporan->status;
        $statusBaru = $validated['status'];

        if ($statusLama !== $statusBaru) {
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
                'keterangan' => $validated['keterangan'] ?? 'Status diubah menjadi ' . $statusBaru,
            ]);
        }

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Status laporan berhasil diperbarui');
    }

    public function updateUrgensi(Request $request, $id)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $laporan = Laporan::find($id);
        if (!$laporan) {
            return $this->errorResponse('Data tidak ditemukan', 404);
        }

        $validated = $request->validate([
            'urgensi' => 'required|in:rendah,sedang,tinggi,kritis',
            'keterangan' => 'nullable|string',
        ]);

        $urgensiLama = $laporan->urgensi;
        $urgensiBaru = $validated['urgensi'];

        if ($urgensiLama !== $urgensiBaru) {
            $laporan->urgensi = $urgensiBaru;
            $laporan->save();

            RiwayatStatus::create([
                'laporan_id' => $laporan->id,
                'user_id' => $user->id,
                'status_lama' => $laporan->status,
                'status_baru' => $laporan->status,
                'keterangan' => $validated['keterangan'] ?? "Urgensi diubah dari {$urgensiLama} menjadi {$urgensiBaru}",
            ]);
        }

        $laporan->load(['kategori', 'pelapor', 'penugasan.relawan', 'penugasan.petugas', 'riwayat.user']);

        return $this->successResponse($this->formatLaporanDetail($laporan), 'Urgensi laporan berhasil diperbarui');
    }
}
