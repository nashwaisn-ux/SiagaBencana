<?php

namespace App\Http\Controllers;

use App\Models\Laporan;
use Illuminate\Http\Request;

class PetaController extends Controller
{
    public function index(Request $request)
    {
        $query = Laporan::with(['kategori']);

        if ($request->has('status') && !empty($request->input('status'))) {
            $query->where('status', $request->input('status'));
        }
        if ($request->has('urgensi') && !empty($request->input('urgensi'))) {
            $query->where('urgensi', $request->input('urgensi'));
        }
        if ($request->has('kategori_id') && !empty($request->input('kategori_id'))) {
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
