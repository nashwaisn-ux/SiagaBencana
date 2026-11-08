<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class UserSeeder extends Seeder
{
    public function run(): void
    {
        $password = Hash::make('password'); // semua akun demo: password = "password"

        $akun = [
            // [nama, email, role, no_hp]
            ['Pak Joko',        'petugas@demo.test',     'petugas',    '081200000010'],
            ['Bu Ratna',        'petugas2@demo.test',    'petugas',    '081200000011'],
            ['Andi Pratama',    'relawan@demo.test',     'relawan',    '081200000001'],
            ['Rina Wulandari',  'relawan2@demo.test',    'relawan',    '081200000002'],
            ['Dimas Saputra',   'relawan3@demo.test',    'relawan',    '081200000003'],
            ['Siti Aminah',     'masyarakat@demo.test',  'masyarakat', '081300000001'],
            ['Budi Santoso',    'masyarakat2@demo.test', 'masyarakat', '081300000002'],
            ['Dewi Lestari',    'masyarakat3@demo.test', 'masyarakat', '081300000003'],
            ['Rudi Hartono',    'masyarakat4@demo.test', 'masyarakat', '081300000004'],
        ];

        foreach ($akun as [$nama, $email, $role, $noHp]) {
            User::create([
                'nama' => $nama,
                'email' => $email,
                'password' => $password,
                'role' => $role,
                'no_hp' => $noHp,
            ]);
        }
    }
}
