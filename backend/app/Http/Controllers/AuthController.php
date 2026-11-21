<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;

class AuthController extends Controller
{
    public function register(Request $request)
    {
        $validated = $request->validate([
            'nama' => 'required|string|max:100',
            'email' => 'required|string|email|max:255|unique:users',
            'password' => 'required|string|min:8|confirmed',
            'no_hp' => 'nullable|string|max:20',
        ]);

        $user = User::create([
            'nama' => $validated['nama'],
            'email' => $validated['email'],
            'password' => Hash::make($validated['password']),
            'role' => 'masyarakat',
            'no_hp' => $validated['no_hp'] ?? null,
        ]);

        $token = $user->createToken('api-token')->plainTextToken;

        return $this->successResponse([
            'token' => $token,
            'user' => $this->formatUser($user),
        ], 'Registrasi berhasil', 201);
    }

    public function login(Request $request)
    {
        $credentials = $request->validate([
            'email' => 'required|email',
            'password' => 'required|string',
        ]);

        if (!Auth::attempt($credentials)) {
            return $this->errorResponse('Email atau password salah', 401);
        }

        $user = Auth::user();
        $token = $user->createToken('api-token')->plainTextToken;

        return $this->successResponse([
            'token' => $token,
            'user' => $this->formatUser($user),
        ], 'Login berhasil');
    }

    public function logout(Request $request)
    {
        $request->user()->currentAccessToken()->delete();

        return $this->successResponse(null, 'Logout berhasil');
    }

    public function me(Request $request)
    {
        return $this->successResponse($this->formatUser($request->user()), 'OK');
    }

    public function updateProfile(Request $request)
    {
        $user = $request->user();

        $validated = $request->validate([
            'nama' => 'required|string|max:255',
            'email' => 'nullable|email|unique:users,email,' . $user->id,
            'no_hp' => 'nullable|string|max:20',
            'alamat' => 'nullable|string',
        ]);

        $user->nama = $validated['nama'];
        if (!empty($validated['email'])) {
            $user->email = $validated['email'];
        }
        if (isset($validated['no_hp'])) {
            $user->no_hp = $validated['no_hp'];
        }
        if (isset($validated['alamat'])) {
            $user->alamat = $validated['alamat'];
        }
        $user->save();

        return $this->successResponse($this->formatUser($user), 'Profil berhasil diperbarui');
    }

    public function uploadPhoto(Request $request)
    {
        $request->validate([
            'foto' => 'required|image|mimes:jpeg,png,jpg,webp|max:3072', // Batas < 3 MB (3072 KB)
        ]);

        $user = $request->user();

        if ($request->hasFile('foto')) {
            if ($user->foto_url && !str_starts_with($user->foto_url, 'http')) {
                Storage::disk('public')->delete($user->foto_url);
            }

            $path = $request->file('foto')->store('avatars', 'public');
            $user->foto_url = $path;
            $user->save();
        }

        return $this->successResponse($this->formatUser($user), 'Foto profil berhasil diunggah');
    }

    public function resetPassword(Request $request)
    {
        $validated = $request->validate([
            'email' => 'required|email|exists:users,email',
            'password' => 'required|string|min:6',
        ]);

        $user = User::where('email', $validated['email'])->first();

        if (!$user) {
            return $this->errorResponse('Email tidak ditemukan', 404);
        }

        $user->password = Hash::make($validated['password']);
        $user->save();

        return $this->successResponse(null, 'Kata sandi berhasil diperbarui dan tersimpan di database');
    }
}
