<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class RelawanController extends Controller
{
    public function index(Request $request)
    {
        $query = User::where('role', 'relawan');

        if ($request->has('search') && !empty($request->input('search'))) {
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

    public function store(Request $request)
    {
        $user = $request->user();
        if ($user->role !== 'petugas') {
            return $this->errorResponse('Hanya petugas yang dapat mendaftarkan relawan', 403);
        }

        $v = $request->validate([
            'nama' => 'required|string|max:100',
            'email' => 'required|email|max:255|unique:users,email',
            'no_hp' => 'required|string|max:20',
            'password' => 'required|string|min:6',
            'posko' => 'nullable|string|max:255',
            'spesialisasi' => 'nullable|string|max:100',
        ]);

        $relawan = User::create([
            'nama' => $v['nama'],
            'email' => $v['email'],
            'password' => Hash::make($v['password']),
            'role' => 'relawan',
            'no_hp' => $v['no_hp'],
            'alamat' => $v['posko'] ?? null,
            'spesialisasi' => $v['spesialisasi'] ?? null,
        ]);

        return $this->successResponse([
            'id' => $relawan->id,
            'nama' => $relawan->nama,
            'no_hp' => $relawan->no_hp,
            'tugas_aktif' => 0,
        ], 'Relawan berhasil didaftarkan', 201);
    }
}
