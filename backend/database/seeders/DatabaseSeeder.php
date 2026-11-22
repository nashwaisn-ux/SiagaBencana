<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // Urutan penting: user & kategori dulu, baru laporan
        $this->call([
            UserSeeder::class,
            AdminPetugasSeeder::class,
            KategoriBencanaSeeder::class,
            LaporanSeeder::class,
        ]);
    }
}
