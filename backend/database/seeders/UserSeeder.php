<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

/**
 * 9 akun demo inti (sama seperti sebelumnya, urutan & email tidak berubah)
 * + akun tambahan agar data terlihat ramai: total 3 petugas, 8 relawan, 25 masyarakat.
 *
 * Semua akun: password = "password".
 * Aman dijalankan ulang (pakai email sebagai kunci), tapi laporan tidak ikut dihapus.
 */
class UserSeeder extends Seeder
{
    public function run(): void
    {
        mt_srand(1101); // supaya nomor HP selalu sama tiap seed

        $password = Hash::make('password');

        $akun = [
            // [nama, email, role, no_hp]  -- akun inti, jangan diubah urutannya
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

        // Akun tambahan: email dibuat dari nama, nomor HP acak berawalan operator seluler
        $tambahan = [
            'petugas' => [
                'Hasan Basri',
            ],
            'relawan' => [
                'Yoga Prasetyo', 'Lia Marlina', 'Fahmi Ramadhan', 'Citra Dewanti', 'Syahrul Anwar',
            ],
            'masyarakat' => [
                'Hendra Gunawan', 'Nurul Aini', 'Yusuf Hakim', 'Maya Sari Lubis', 'Ahmad Fauzi',
                'Fitriani Siregar', 'Agus Salim', 'Lestari Handayani', 'Rizky Ramadhan',
                'Putri Anggraini', 'Zulkifli Harahap', 'Mega Pertiwi', 'Fajar Nugroho',
                'Sri Wahyuni', 'Taufik Hidayat', 'Elva Yanti', 'Bambang Irawan',
                'Nia Kurniasih', 'Irwan Syahputra', 'Dedi Kurniawan', 'Wulan Septiani',
            ],
        ];

        $operator = ['0812', '0813', '0821', '0822', '0852', '0853', '0811', '0895', '0857'];

        foreach ($tambahan as $role => $daftarNama) {
            foreach ($daftarNama as $nama) {
                $noHp = $operator[array_rand($operator)] . str_pad((string) mt_rand(0, 99999999), 8, '0', STR_PAD_LEFT);
                $akun[] = [$nama, Str::slug($nama, '.') . '@demo.test', $role, $noHp];
            }
        }

        foreach ($akun as [$nama, $email, $role, $noHp]) {
            User::firstOrCreate(
                ['email' => $email],
                [
                    'nama' => $nama,
                    'password' => $password,
                    'role' => $role,
                    'no_hp' => $noHp,
                ]
            );
        }

        mt_srand(); // kembalikan RNG ke acak biasa
    }
}