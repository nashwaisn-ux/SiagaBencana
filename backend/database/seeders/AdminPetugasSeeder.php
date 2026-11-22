<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class AdminPetugasSeeder extends Seeder
{
    public function run(): void
    {
        // Akun Petugas / Administrator Utama untuk Demo & Manajemen Bencana
        User::updateOrCreate(
            ['email' => 'admin.petugas@siagabencana.id'],
            [
                'nama' => 'Administrator BPBD',
                'password' => Hash::make('password123'),
                'role' => 'petugas',
                'no_hp' => '081299998888',
                'alamat' => 'Kantor Pusat BPBD / Posko Utama Siaga Bencana',
            ]
        );
    }
}
