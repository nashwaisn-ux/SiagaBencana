<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\Request;

class RelawanController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Akses ditolak', 403);
        }

        $query = User::where('role', 'relawan');

        if ($request->has('search')) {
            $search = $request->input('search');
            $query->where('nama', 'like', "%{$search}%");
        }

        $relawans = $query->withCount(['penugasanSebagaiRelawan as tugas_aktif' => function ($q) {
            $q->whereIn('status_tugas', ['ditugaskan', 'diterima']);
        }])->get()->map(function ($r) {
            return [
                'id' => $r->id,
                'nama' => $r->nama,
                'no_hp' => $r->no_hp,
                'tugas_aktif' => (int) $r->tugas_aktif,
            ];
        });

        return $this->successResponse($relawans, 'OK');
    }
}
