<?php

namespace Database\Seeders;

use App\Models\KategoriBencana;
use App\Models\Laporan;
use App\Models\Penugasan;
use App\Models\RiwayatStatus;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Carbon;

/**
 * 32 laporan demo di sekitar Kota Pekanbaru (koordinat perkiraan untuk demo).
 * Setiap laporan punya riwayat, dan penugasan bila status sudah lewat "baru".
 */
class LaporanSeeder extends Seeder
{
    public function run(): void
    {
        $pelapor = User::where('role', 'masyarakat')->orderBy('id')->pluck('id')->all();
        $petugas = User::where('role', 'petugas')->orderBy('id')->pluck('id')->all();
        $relawan = User::where('role', 'relawan')->orderBy('id')->pluck('id')->all();
        $bobot   = KategoriBencana::pluck('bobot_urgensi', 'id');

        $alur = ['baru', 'diverifikasi', 'ditangani', 'selesai'];
        $siklusStatus = [
            'baru', 'baru', 'baru',
            'diverifikasi', 'diverifikasi',
            'ditangani', 'ditangani', 'ditangani',
            'selesai', 'selesai',
        ];
        $keteranganStatus = [
            1 => 'Laporan diverifikasi petugas',
            2 => 'Tim relawan sedang menangani di lokasi',
            3 => 'Penanganan selesai',
        ];

        $templat = $this->templat();
        $lokasi  = $this->lokasi();

        foreach (range(0, 31) as $i) {
            [$kategoriId, $judul, $deskripsi, $terdampak] = $templat[$i % count($templat)];
            $terdampak += intdiv($i, 16) * 3;

            [$namaLokasi, $lat, $lng] = $lokasi[($i * 3 + intdiv($i, 16)) % count($lokasi)];
            $lat = round($lat + ((($i * 37) % 100) - 50) / 10000, 7);
            $lng = round($lng + ((($i * 53) % 100) - 50) / 10000, 7);

            $status   = $siklusStatus[$i % count($siklusStatus)];
            $idxAkhir = array_search($status, $alur);

            $pelaporId = $pelapor[$i % count($pelapor)];
            $petugasId = $petugas[$i % count($petugas)];
            $relawanId = $relawan[$i % count($relawan)];

            $waktu = Carbon::now()->subDays(14 - intdiv($i, 3))->subHours(($i * 5) % 20);

            // Waktu terakhir laporan berubah (untuk updated_at)
            $waktuAkhir = $waktu->copy()->addHours(3 * $idxAkhir);

            $laporan = Laporan::forceCreate([
                'user_id' => $pelaporId,
                'kategori_id' => $kategoriId,
                'judul' => "{$judul} ({$namaLokasi})",
                'deskripsi' => "{$deskripsi} Lokasi: Kecamatan {$namaLokasi}, Pekanbaru.",
                'foto' => null,
                'latitude' => $lat,
                'longitude' => $lng,
                'jumlah_terdampak' => $terdampak,
                'urgensi' => $this->hitungUrgensi($bobot[$kategoriId], $terdampak, $deskripsi),
                'status' => $status,
                'created_at' => $waktu,
                'updated_at' => $waktuAkhir,
            ]);

            RiwayatStatus::forceCreate([
                'laporan_id' => $laporan->id,
                'user_id' => $pelaporId,
                'status_lama' => null,
                'status_baru' => 'baru',
                'keterangan' => 'Laporan dibuat',
                'created_at' => $waktu,
            ]);

            for ($langkah = 1; $langkah <= $idxAkhir; $langkah++) {
                $waktuLangkah = $waktu->copy()->addHours(3 * $langkah);

                // Verifikasi oleh petugas; penanganan & penyelesaian oleh relawan
                RiwayatStatus::forceCreate([
                    'laporan_id' => $laporan->id,
                    'user_id' => $langkah === 1 ? $petugasId : $relawanId,
                    'status_lama' => $alur[$langkah - 1],
                    'status_baru' => $alur[$langkah],
                    'keterangan' => $keteranganStatus[$langkah],
                    'created_at' => $waktuLangkah,
                ]);
            }

            if ($idxAkhir >= 1) {
                Penugasan::forceCreate([
                    'laporan_id' => $laporan->id,
                    'relawan_id' => $relawanId,
                    'petugas_id' => $petugasId,
                    'catatan' => 'Mohon segera ditindaklanjuti dan laporkan perkembangan.',
                    'status_tugas' => [1 => 'ditugaskan', 2 => 'diterima', 3 => 'selesai'][$idxAkhir],
                    'foto_bukti' => null,
                    'created_at' => $waktu->copy()->addHours(3),
                    'updated_at' => $waktuAkhir,
                ]);
            }
        }
    }

    /**
     * Versi sederhana aturan skor urgensi (lihat api-contract.md bagian 5).
     * Nanti boleh diganti memakai service milik Anggota 3 agar satu sumber aturan.
     */
    private function hitungUrgensi(int $bobotKategori, int $terdampak, string $deskripsi): string
    {
        $skor = $bobotKategori;
        $skor += match (true) {
            $terdampak >= 50 => 3,
            $terdampak >= 20 => 2,
            $terdampak >= 5  => 1,
            default          => 0,
        };

        foreach (['korban', 'terjebak', 'meluas', 'evakuasi', 'meninggal'] as $kata) {
            if (str_contains(mb_strtolower($deskripsi), $kata)) {
                $skor += 2;
                break;
            }
        }

        return match (true) {
            $skor >= 7 => 'kritis',
            $skor >= 5 => 'tinggi',
            $skor >= 3 => 'sedang',
            default    => 'rendah',
        };
    }

    /** [kategori_id, judul, deskripsi, jumlah_terdampak] */
    private function templat(): array
    {
        return [
            [1, 'Banjir rendam pemukiman warga', 'Air sungai meluap dan masuk ke rumah warga sejak subuh dengan ketinggian sekitar 1 meter.', 45],
            [1, 'Genangan air menutup jalan utama', 'Hujan deras semalam membuat jalan tergenang setinggi lutut sehingga kendaraan sulit lewat.', 8],
            [1, 'Banjir bandang, warga terjebak di rumah', 'Arus deras dan sejumlah warga terjebak di lantai dua, butuh evakuasi dengan perahu.', 60],
            [2, 'Kebakaran rumah semi permanen', 'Api berasal dari korsleting listrik dan menjalar ke dua rumah di sebelahnya.', 12],
            [2, 'Kebakaran kios pasar tradisional', 'Api membesar cepat di deretan kios dan petugas pemadam belum tiba.', 30],
            [2, 'Kebakaran meluas ke permukiman padat', 'Api meluas ke belasan rumah, ada korban luka bakar dan warga butuh evakuasi.', 70],
            [3, 'Tanah longsor menutup akses jalan', 'Longsoran tanah dari tebing menutup separuh badan jalan.', 5],
            [3, 'Longsor di tepi sungai, rumah terancam', 'Tebing sungai longsor dan dua rumah nyaris roboh, warga diminta mengungsi.', 15],
            [4, 'Sungai tercemar limbah berwarna hitam', 'Air sungai berbau menyengat dan ikan mati mengambang di sepanjang aliran.', 20],
            [4, 'Tumpahan minyak di parit permukiman', 'Cairan minyak memenuhi parit dan mulai masuk ke sumur warga.', 10],
            [5, 'Kebakaran lahan gambut dekat pemukiman', 'Asap pekat menyelimuti kampung dan api terus meluas di lahan gambut.', 120],
            [5, 'Titik api kecil di lahan kosong', 'Titik api terlihat di semak kering dan masih bisa dijangkau warga.', 2],
            [6, 'Pohon tumbang menimpa kabel listrik', 'Pohon besar tumbang akibat angin kencang dan memutus aliran listrik satu RT.', 25],
            [6, 'Sampah menumpuk menyumbat drainase', 'Tumpukan sampah menyumbat saluran sehingga air mulai menggenang.', 6],
            [1, 'Banjir kiriman dari hulu', 'Debit air naik cepat dan warga bantaran sungai diminta waspada.', 35],
            [2, 'Kebakaran gudang, ada pekerja terjebak', 'Gudang terbakar hebat dan satu pekerja dikabarkan masih terjebak di dalam.', 3],
        ];
    }

    /** [nama kecamatan, latitude, longitude] (perkiraan untuk data demo) */
    private function lokasi(): array
    {
        return [
            ['Sukajadi',        0.5100, 101.4350],
            ['Senapelan',       0.5230, 101.4300],
            ['Rumbai',          0.5700, 101.4200],
            ['Tampan',          0.4600, 101.3900],
            ['Payung Sekaki',   0.4700, 101.4000],
            ['Marpoyan Damai',  0.4800, 101.4700],
            ['Bukit Raya',      0.5300, 101.4800],
            ['Tenayan Raya',    0.5000, 101.5200],
        ];
    }
}
