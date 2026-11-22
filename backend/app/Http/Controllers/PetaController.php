<?php

namespace App\Http\Controllers;

use App\Models\Laporan;
use Illuminate\Http\Request;

class PetaController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        $query = Laporan::with(['kategori']);

        if ($user->role === 'masyarakat') {
            $query->where('user_id', $user->id);
        } elseif ($user->role === 'relawan') {
            $query->whereHas('penugasan', function ($q) use ($user) {
                $q->where('relawan_id', $user->id);
            });
        }

        if ($request->has('status')) {
            $query->where('status', $request->input('status'));
        }
        if ($request->has('urgensi')) {
            $query->where('urgensi', $request->input('urgensi'));
        }
        if ($request->has('kategori_id')) {
            $query->where('kategori_id', $request->input('kategori_id'));
        }

        $laporans = $query->get()->map(function ($l) {
            return [
                'id' => $l->id,
                'judul' => $l->judul,
                'latitude' => (float) $l->latitude,
                'longitude' => (float) $l->longitude,
                'urgensi' => $l->urgensi,
                'status' => $l->status,
                'kategori' => $l->kategori ? $l->kategori->nama : null,
            ];
        });

        return $this->successResponse($laporans, 'OK');
    }
}
