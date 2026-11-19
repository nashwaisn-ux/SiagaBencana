<?php

namespace Database\Seeders;

use App\Models\KategoriBencana;
use Illuminate\Database\Seeder;

class KategoriBencanaSeeder extends Seeder
{
    public function run(): void
    {
        // Urutan = id 1..6, HARUS sama dengan contoh di kontrak API
        $kategori = [
            ['Banjir',                      2],
            ['Kebakaran',                   3],
            ['Tanah Longsor',               3],
            ['Pencemaran Lingkungan',       1],
            ['Kebakaran Hutan dan Lahan',   3],
            ['Lainnya',                     0],
        ];

        foreach ($kategori as [$nama, $bobot]) {
            KategoriBencana::create(['nama' => $nama, 'bobot_urgensi' => $bobot]);
        }
    }
}
