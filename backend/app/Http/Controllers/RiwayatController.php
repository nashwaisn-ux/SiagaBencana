<?php

namespace App\Http\Controllers;

use App\Models\RiwayatStatus;
use Illuminate\Http\Request;

class RiwayatController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        $query = RiwayatStatus::with(['laporan', 'user']);

        if ($user->role === 'masyarakat') {
            $query->where(function ($q) use ($user) {
                $q->where('user_id', $user->id)
                  ->orWhereHas('laporan', function ($lq) use ($user) {
                      $lq->where('user_id', $user->id);
                  });
            });
        } elseif ($user->role === 'relawan') {
            $query->whereHas('laporan.penugasan', function ($q) use ($user) {
                $q->where('relawan_id', $user->id);
            });
        }

        if ($request->has('laporan_id')) {
            $query->where('laporan_id', $request->input('laporan_id'));
        }

        $query->orderBy('created_at', 'desc');
        $perPage = min(max((int) $request->input('per_page', 10), 1), 50);
        $paginator = $query->paginate($perPage);

        $transform = function ($r) {
            return [
                'id' => $r->id,
                'laporan' => $r->laporan ? [
                    'id' => $r->laporan->id,
                    'judul' => $r->laporan->judul,
                ] : null,
                'status_lama' => $r->status_lama,
                'status_baru' => $r->status_baru,
                'keterangan' => $r->keterangan,
                'user' => $this->formatUserPelapor($r->user),
                'created_at' => $r->created_at?->toIso8601String(),
            ];
        };

        return $this->paginatedResponse($paginator, 'OK', $transform);
    }
}
