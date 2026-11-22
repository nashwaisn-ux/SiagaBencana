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
 * Data demo laporan bencana di Kota Pekanbaru (default 120 laporan dalam 60 hari terakhir).
 *
 * Yang dibuat, semuanya mengikuti alur yang sama dengan controller:
 *  - laporan  : kategori, urgensi (dihitung dgn aturan yang sama seperti LaporanController), status
 *  - riwayat  : "Laporan dibuat" -> diverifikasi -> (relawan menerima tugas) -> ditangani -> selesai,
 *               plus sesekali petugas mengubah urgensi
 *  - penugasan: 1 relawan per laporan, kadang 2 untuk laporan urgensi tinggi/kritis
 *
 * Kerealistisan data:
 *  - Laporan lama hampir semuanya selesai, laporan baru-baru ini banyak yang masih "baru".
 *    Ada sedikit laporan lama yang macet di tengah jalan (supaya dashboard tidak terlalu "bersih").
 *  - Pola musim: laporan lama condong ke karhutla, laporan terbaru condong ke banjir/pohon tumbang.
 *  - Jam kejadian, durasi penanganan, dan lokasi ikut menyesuaikan jenis bencana.
 *  - Tidak ada waktu yang melewati "sekarang".
 *
 * Catatan:
 *  - Nama kecamatan/kelurahan nyata, tetapi titik koordinat hanya perkiraan untuk demo.
 *  - foto & foto_bukti sengaja null supaya tidak ada gambar rusak (file tidak ada di storage).
 *  - RNG diberi seed tetap => struktur data selalu sama; tanggal selalu relatif terhadap hari ini.
 *  - Jalankan dengan: php artisan migrate:fresh --seed
 */
class LaporanSeeder extends Seeder
{
    private const JUMLAH_LAPORAN = 120;
    private const RENTANG_HARI   = 60;
    private const SEED_RNG       = 20261121;

    private Carbon $sekarang;

    /** @var array<int,string> id user => nama */
    private array $namaUser = [];

    /** @var array<int,int> id relawan => jumlah tugas aktif saat ini (untuk pemerataan beban) */
    private array $bebanRelawan = [];

    public function run(): void
    {
        mt_srand(self::SEED_RNG);
        $this->sekarang = Carbon::now();

        $this->namaUser = User::pluck('nama', 'id')->all();

        // Akun inti (id terkecil) sengaja lebih sering muncul supaya akun demo punya data yang kaya
        $pelapor = $this->bobotDariUrutan(User::where('role', 'masyarakat')->orderBy('id')->pluck('id')->all(), 4, 4);
        $petugas = $this->bobotDariUrutan(User::where('role', 'petugas')->orderBy('id')->pluck('id')->all(), 2, 3);
        $relawan = $this->bobotDariUrutan(User::where('role', 'relawan')->orderBy('id')->pluck('id')->all(), 3, 3);

        foreach (array_keys($relawan) as $id) {
            $this->bebanRelawan[$id] = 0;
        }

        $bobotKategori = KategoriBencana::pluck('bobot_urgensi', 'id')->all();
        $kecamatan     = $this->kecamatan();
        $templat       = $this->templat();

        foreach ($this->buatJadwal() as [$waktu, $kategoriId]) {
            $this->buatLaporan(
                $waktu, $kategoriId, $bobotKategori, $kecamatan, $templat[$kategoriId],
                $pelapor, $petugas, $relawan
            );
        }

        mt_srand(); // kembalikan RNG ke acak biasa
    }

    // ------------------------------------------------------------------
    // Penjadwalan: kapan tiap laporan dibuat & kategorinya
    // ------------------------------------------------------------------

    /** @return array<int,array{0:Carbon,1:int}> urut dari yang paling lama, jadi id naik = waktu naik */
    private function buatJadwal(): array
    {
        $jadwal = [];

        for ($i = 0; $i < self::JUMLAH_LAPORAN; $i++) {
            // pangkat > 1 membuat hari-hari terakhir lebih padat laporan
            $hari = (int) floor(self::RENTANG_HARI * ($this->pecahan() ** 1.3));

            // kategori bergantung musim (umur laporan)
            $kategoriId = $this->pilihBerbobot($this->bobotKategori($hari));
            $jam        = $this->pilihBerbobot($this->bobotJam($kategoriId));

            $waktu = $this->sekarang->copy()->subDays($hari)->setTime($jam, mt_rand(0, 59), mt_rand(0, 59));

            // jangan sampai laporan "dibuat" tepat sekarang atau di masa depan
            if ($waktu->gt($this->sekarang->copy()->subHours(1))) {
                $waktu->subDay();
            }

            $jadwal[] = [$waktu, $kategoriId];
        }

        usort($jadwal, fn ($a, $b) => $a[0]->getTimestamp() <=> $b[0]->getTimestamp());

        return $jadwal;
    }

    /** Kategori: 1 Banjir, 2 Kebakaran, 3 Longsor, 4 Pencemaran, 5 Karhutla, 6 Lainnya */
    private function bobotKategori(int $umurHari): array
    {
        $lama = $umurHari > 30;

        return [
            1 => $lama ? 14 : 34,   // musim hujan: banjir makin sering belakangan ini
            2 => 20,
            3 => $lama ? 4 : 8,
            4 => 8,
            5 => $lama ? 24 : 6,    // karhutla mereda saat hujan mulai turun
            6 => $lama ? 16 : 24,   // pohon tumbang, drainase tersumbat, dst
        ];
    }

    /** Bobot jam (0-23) kapan laporan paling sering masuk, menurut jenis bencana */
    private function bobotJam(int $kategoriId): array
    {
        $jam = [];
        for ($h = 0; $h < 24; $h++) {
            $jam[$h] = ($h >= 6 && $h <= 20) ? 3 : 1;
        }

        $tambah = function (array $daftar, int $nilai) use (&$jam) {
            foreach ($daftar as $h) {
                $jam[$h] += $nilai;
            }
        };

        match ($kategoriId) {
            1 => $tambah([3, 4, 5, 6, 7, 16, 17, 18, 19], 4),   // hujan deras dini hari / sore
            2 => $tambah([12, 13, 14, 19, 20, 21, 22, 23], 3),  // memasak & listrik malam hari
            5 => $tambah([10, 11, 12, 13, 14, 15, 16, 17], 4),  // siang terik
            default => null,
        };

        return $jam;
    }

    // ------------------------------------------------------------------
    // Membuat satu laporan lengkap dengan riwayat & penugasannya
    // ------------------------------------------------------------------

    private function buatLaporan(
        Carbon $waktu,
        int $kategoriId,
        array $bobotKategori,
        array $kecamatan,
        array $templat,
        array $pelapor,
        array $petugas,
        array $relawan
    ): void {
        // --- isi laporan ---
        [$judul, $deskripsi, $min, $maks] = $this->pilih($templat['laporan']);

        $namaKec = $this->pilihBerbobot($this->bobotKecamatan($kategoriId, $kecamatan));
        $kec     = $kecamatan[$namaKec];
        $kel     = $this->pilih($kec['kelurahan']);

        $terdampak = mt_rand($min, $maks);
        $penutup   = $this->pilih([
            '', '', '',
            ' Warga sudah mengabari ketua RT setempat.',
            ' Mohon bantuan secepatnya.',
            ' Foto kondisi terbaru akan dikirim menyusul.',
            ' Kondisi masih bisa berubah, mohon dipantau.',
        ]);

        $judulFinal = mb_substr("{$judul}, Kel. {$kel}", 0, 120);
        $isiFinal   = "{$deskripsi}{$penutup} Lokasi: Kel. {$kel}, Kec. {$namaKec}, Pekanbaru.";

        $lat = round($kec['lat'] + (($this->pecahan() * 2) - 1) * 0.008, 7);
        $lng = round($kec['lng'] + (($this->pecahan() * 2) - 1) * 0.008, 7);

        $urgensi = $this->hitungUrgensi($bobotKategori[$kategoriId] ?? 1, $terdampak, $isiFinal);

        // --- pemeran ---
        $pelaporId = $this->pilihBerbobot($pelapor);
        $petugasId = $this->pilihBerbobot($petugas);
        $relawanId = $this->pilihRelawan($relawan);

        // --- rangkaian waktu kejadian (menit sejak dibuat) ---
        $umurHari = ($this->sekarang->getTimestamp() - $waktu->getTimestamp()) / 86400;

        $jedaVerifikasi = match ($urgensi) {
            'kritis' => mt_rand(5, 30),
            'tinggi' => mt_rand(10, 90),
            'sedang' => mt_rand(30, 360),
            default  => mt_rand(60, 720),
        };
        $cepat = in_array($urgensi, ['kritis', 'tinggi'], true);

        $tV = $waktu->copy()->addMinutes($jedaVerifikasi);                         // diverifikasi + ditugaskan
        $tA = $tV->copy()->addMinutes($cepat ? mt_rand(3, 30) : mt_rand(10, 180)); // relawan menerima
        $tD = $tA->copy()->addMinutes(mt_rand(10, 120));                           // mulai ditangani
        [$jamMin, $jamMaks] = $templat['durasi_jam'];
        $tS = $tD->copy()->addMinutes(mt_rand($jamMin * 60, $jamMaks * 60));       // selesai

        // --- sejauh mana laporan sudah berjalan: 0 baru, 1 diverifikasi, 2 diterima, 3 ditangani, 4 selesai ---
        $tahap = $this->pilihBerbobot($this->bobotTahap($umurHari));
        $waktuTahap = [1 => $tV, 2 => $tA, 3 => $tD, 4 => $tS];
        while ($tahap > 0 && $waktuTahap[$tahap]->gt($this->sekarang)) {
            $tahap--; // belum sempat terjadi karena belum waktunya
        }

        $statusAkhir = ['baru', 'diverifikasi', 'diverifikasi', 'ditangani', 'selesai'][$tahap];

        // --- urgensi kadang diubah petugas setelah verifikasi ---
        $riwayat = [];
        $urgensiAkhir = $urgensi;
        $tU = null;
        if ($tahap >= 1 && mt_rand(1, 100) <= 9) {
            $tU = $tV->copy()->addMinutes(mt_rand(2, 8));
            if ($tU->lte($this->sekarang)) {
                $urutan = ['rendah', 'sedang', 'tinggi', 'kritis'];
                $idx    = array_search($urgensi, $urutan);
                $naik   = mt_rand(1, 100) <= 60;
                $baru   = $urutan[max(0, min(3, $idx + ($naik ? 1 : -1)))];

                if ($baru !== $urgensi) {
                    $urgensiAkhir = $baru;
                    $riwayat[] = [$tU, $petugasId, 'diverifikasi', 'diverifikasi',
                        mt_rand(1, 100) <= 50
                            ? "Urgensi diubah dari {$urgensi} menjadi {$baru}"
                            : ($naik
                                ? 'Urgensi dinaikkan karena kondisi di lapangan lebih parah dari laporan'
                                : 'Urgensi diturunkan setelah verifikasi, kondisi sudah relatif terkendali')];
                } else {
                    $tU = null;
                }
            } else {
                $tU = null;
            }
        }

        // --- riwayat status utama ---
        $riwayat[] = [$waktu, $pelaporId, null, 'baru', 'Laporan dibuat'];

        if ($tahap >= 1) {
            $riwayat[] = [$tV, $petugasId, 'baru', 'diverifikasi',
                mt_rand(1, 100) <= 65
                    ? 'Laporan diverifikasi otomatis saat ditugaskan'
                    : $this->pilih([
                        'Laporan diverifikasi, kondisi sesuai laporan warga',
                        'Laporan diverifikasi setelah konfirmasi ke perangkat kelurahan',
                        'Laporan diverifikasi, tim relawan segera dikerahkan',
                    ])];
        }

        // --- penugasan (utama + kadang tambahan) ---
        $penugasan = [];
        if ($tahap >= 1) {
            $tP = $tV->copy()->addMinutes(mt_rand(0, 2));
            $tP = $tP->gt($this->sekarang) ? $this->sekarang->copy() : $tP;
            $catatanPetugas = $this->catatanPetugas($urgensiAkhir);

            $penugasan[] = $this->susunPenugasan(
                $relawanId, $petugasId, $tP, $tA, $tS, $tahap, $catatanPetugas, $riwayat, [$tV, $tA, $tD, $tS]
            );

            $peluangDua = ['kritis' => 70, 'tinggi' => 25][$urgensiAkhir] ?? 5;
            if (count($relawan) > 1 && mt_rand(1, 100) <= $peluangDua) {
                $tP2 = $tP->copy()->addMinutes(mt_rand(10, 40));
                if ($tP2->lte($this->sekarang)) {
                    $relawanId2 = $this->pilihRelawan($relawan, [$relawanId]);
                    $tA2 = $tP2->copy()->addMinutes(mt_rand(5, 90));
                    $penugasan[] = $this->susunPenugasan(
                        $relawanId2, $petugasId, $tP2, $tA2, $tS, $tahap,
                        'Bantuan tambahan untuk tim di lokasi. Koordinasi dengan relawan yang sudah lebih dulu ditugaskan.',
                        $riwayat, [$tV, $tA2, $tD, $tS]
                    );
                }
            }
        }

        if ($tahap >= 3) {
            $olehPetugas = mt_rand(1, 100) <= 20;
            $riwayat[] = [$tD, $olehPetugas ? $petugasId : $relawanId, 'diverifikasi', 'ditangani',
                $this->pilih($templat['ditangani'])];
        }
        if ($tahap >= 4) {
            $riwayat[] = [$tS, $relawanId, 'ditangani', 'selesai', $this->pilih($templat['selesai'])];
        }

        // --- simpan ---
        $terakhir = collect($riwayat)->max(fn ($r) => $r[0]->getTimestamp());
        $updatedAt = Carbon::createFromTimestamp($terakhir);

        $laporan = Laporan::forceCreate([
            'user_id'          => $pelaporId,
            'kategori_id'      => $kategoriId,
            'judul'            => $judulFinal,
            'deskripsi'        => $isiFinal,
            'foto'             => null,
            'latitude'         => $lat,
            'longitude'        => $lng,
            'jumlah_terdampak' => $terdampak,
            'urgensi'          => $urgensiAkhir,
            'status'           => $statusAkhir,
            'created_at'       => $waktu,
            'updated_at'       => $updatedAt,
        ]);

        usort($riwayat, fn ($a, $b) => $a[0]->getTimestamp() <=> $b[0]->getTimestamp());
        foreach ($riwayat as [$t, $userId, $lama, $baru, $ket]) {
            RiwayatStatus::forceCreate([
                'laporan_id'  => $laporan->id,
                'user_id'     => $userId,
                'status_lama' => $lama,
                'status_baru' => $baru,
                'keterangan'  => $ket,
                'created_at'  => $t,
            ]);
        }

        foreach ($penugasan as $p) {
            Penugasan::forceCreate(['laporan_id' => $laporan->id] + $p);
        }
    }

    /**
     * Menyusun satu baris penugasan sekaligus menambahkan riwayat "status penugasan" ke $riwayat
     * (persis seperti yang dicatat PenugasanController::updateStatus).
     *
     * @param array{0:Carbon,1:Carbon,2:Carbon,3:Carbon} $t  [verifikasi, diterima, ditangani, selesai] milik laporan
     */
    private function susunPenugasan(
        int $relawanId,
        int $petugasId,
        Carbon $tDitugaskan,
        Carbon $tDiterima,
        Carbon $tSelesai,
        int $tahap,
        string $catatan,
        array &$riwayat,
        array $t
    ): array {
        $nama   = $this->namaUser[$relawanId] ?? 'Relawan';
        $status = 'ditugaskan';
        $update = $tDitugaskan;

        $statusLaporanPada = function (Carbon $waktu) use ($t): string {
            if ($waktu->gte($t[3])) return 'selesai';
            if ($waktu->gte($t[2])) return 'ditangani';
            return 'diverifikasi';
        };

        // Tahap 2 ke atas: relawan sudah menerima tugas
        if ($tahap >= 2 && $tDiterima->lte($this->sekarang)) {
            $status = 'diterima';
            $update = $tDiterima;
            $riwayat[] = [$tDiterima, $relawanId, $statusLaporanPada($tDiterima), $statusLaporanPada($tDiterima),
                "Status penugasan relawan ({$nama}) diubah dari ditugaskan menjadi diterima"];
        }

        // Laporan selesai: penugasan ikut selesai sesaat sebelumnya
        if ($tahap >= 4) {
            $tSelesaiTugas = $tSelesai->copy()->subMinutes(mt_rand(1, 8));
            if ($tSelesaiTugas->lt($tDitugaskan)) {
                $tSelesaiTugas = $tSelesai->copy();
            }
            $status = 'selesai';
            $update = $tSelesaiTugas;
            $riwayat[] = [$tSelesaiTugas, $relawanId, 'ditangani', 'ditangani',
                "Status penugasan relawan ({$nama}) diubah dari diterima menjadi selesai"];

            // sebagian relawan menimpa catatan dengan laporan singkat saat upload bukti
            if (mt_rand(1, 100) <= 50) {
                $catatan = $this->pilih([
                    'Sudah ditangani bersama warga dan perangkat RT setempat. Kondisi aman.',
                    'Penanganan selesai, warga sudah diberi arahan agar tetap waspada.',
                    'Selesai. Dokumentasi lapangan sudah diambil, mohon ditinjau.',
                    'Lokasi sudah kembali normal. Tidak ada tambahan korban.',
                ]);
            }
        }

        if (in_array($status, ['ditugaskan', 'diterima'], true)) {
            $this->bebanRelawan[$relawanId] = ($this->bebanRelawan[$relawanId] ?? 0) + 1;
        }

        return [
            'relawan_id'   => $relawanId,
            'petugas_id'   => $petugasId,
            'catatan'      => $catatan,
            'status_tugas' => $status,
            'foto_bukti'   => null,
            'created_at'   => $tDitugaskan,
            'updated_at'   => $update,
        ];
    }

    // ------------------------------------------------------------------
    // Aturan bisnis & pemilihan acak
    // ------------------------------------------------------------------

    /** Sama persis dengan Controller::hitungUrgensi (supaya data seed konsisten dengan data hasil input). */
    private function hitungUrgensi(int $bobotKategori, int $terdampak, string $deskripsi): string
    {
        $skor = $bobotKategori;

        if ($terdampak >= 50) {
            $skor += 3;
        } elseif ($terdampak >= 20) {
            $skor += 2;
        } elseif ($terdampak >= 5) {
            $skor += 1;
        }

        $desc = strtolower($deskripsi);
        foreach (['korban', 'terjebak', 'meluas', 'evakuasi', 'meninggal'] as $kata) {
            if (str_contains($desc, $kata)) {
                $skor += 2;
                break;
            }
        }

        if ($skor >= 7) return 'kritis';
        if ($skor >= 5) return 'tinggi';
        if ($skor >= 3) return 'sedang';
        return 'rendah';
    }

    /**
     * Peluang tiap tahap menurut umur laporan (hari):
     * 0 baru, 1 diverifikasi (belum diterima), 2 diterima, 3 ditangani, 4 selesai.
     */
    private function bobotTahap(float $umurHari): array
    {
        return match (true) {
            $umurHari >= 21 => [0 => 1,  1 => 3,  2 => 3,  3 => 6,  4 => 87],
            $umurHari >= 7  => [0 => 3,  1 => 6,  2 => 8,  3 => 18, 4 => 65],
            $umurHari >= 2  => [0 => 18, 1 => 16, 2 => 15, 3 => 23, 4 => 28],
            default         => [0 => 50, 1 => 20, 2 => 10, 3 => 12, 4 => 8],
        };
    }

    /** Pilih relawan: yang bebannya sedikit lebih mungkin dipilih. */
    private function pilihRelawan(array $relawan, array $kecuali = []): int
    {
        $bobot = [];
        foreach ($relawan as $id => $b) {
            if (in_array($id, $kecuali, true)) {
                continue;
            }
            $bobot[$id] = $b / (1 + $this->bebanRelawan[$id]);
        }

        return $this->pilihBerbobot($bobot);
    }

    private function catatanPetugas(string $urgensi): string
    {
        if ($urgensi === 'kritis') {
            return $this->pilih([
                'PRIORITAS TINGGI. Segera ke lokasi dan kabari saya jika butuh tambahan personel.',
                'Kondisi darurat, mohon langsung berangkat dan laporkan situasi setiap 30 menit.',
            ]);
        }

        return $this->pilih([
            'Mohon segera ke lokasi dan laporkan kondisi terkini.',
            'Koordinasi dengan perangkat kelurahan setempat sebelum penanganan.',
            'Bawa perlengkapan dasar dan dokumentasikan kondisi lokasi.',
            'Tolong cek langsung ke lokasi dan update statusnya di aplikasi.',
            'Hubungi pelapor lebih dulu untuk memastikan titik lokasi.',
            'Mohon ditindaklanjuti hari ini dan laporkan perkembangan.',
        ]);
    }

    /** Bobot kecamatan menurut jenis bencana (daerah rawan air, lahan gambut, padat, industri). */
    private function bobotKecamatan(int $kategoriId, array $kecamatan): array
    {
        $bobot = [];
        foreach ($kecamatan as $nama => $k) {
            $b = 2;
            if (in_array($kategoriId, [1, 3], true) && in_array('rawan_air', $k['tag'], true)) $b += 4;
            if ($kategoriId === 2 && in_array('padat', $k['tag'], true))                       $b += 4;
            if ($kategoriId === 4 && in_array('industri', $k['tag'], true))                    $b += 3;
            if ($kategoriId === 5 && in_array('lahan', $k['tag'], true))                       $b += 5;
            $bobot[$nama] = $b;
        }

        return $bobot;
    }

    /** Daftar id -> bobot; tiap $n id pertama (akun inti) diberi bobot $tinggi, sisanya 1. */
    private function bobotDariUrutan(array $ids, int $n, int $tinggi): array
    {
        $hasil = [];
        foreach (array_values($ids) as $i => $id) {
            $hasil[$id] = $i < $n ? $tinggi : 1;
        }

        return $hasil;
    }

    private function pecahan(): float
    {
        return mt_rand() / mt_getrandmax();
    }

    private function pilih(array $daftar)
    {
        return $daftar[mt_rand(0, count($daftar) - 1)];
    }

    /** @param array<int|string,int|float> $bobot kunci => bobot, mengembalikan sebuah kunci */
    private function pilihBerbobot(array $bobot)
    {
        $r = $this->pecahan() * array_sum($bobot);
        foreach ($bobot as $kunci => $b) {
            $r -= $b;
            if ($r <= 0) {
                return $kunci;
            }
        }

        return array_key_last($bobot);
    }

    // ------------------------------------------------------------------
    // Data: lokasi & templat teks
    // ------------------------------------------------------------------

    /** nama kecamatan => pusat perkiraan + kelurahan + tag untuk pembobotan */
    private function kecamatan(): array
    {
        return [
            'Sukajadi'        => ['lat' => 0.5100, 'lng' => 101.4350, 'tag' => ['padat'],
                'kelurahan' => ['Kampung Tengah', 'Kampung Melayu', 'Kedung Sari', 'Harjosari', 'Jadirejo']],
            'Senapelan'       => ['lat' => 0.5230, 'lng' => 101.4300, 'tag' => ['padat', 'rawan_air'],
                'kelurahan' => ['Kampung Baru', 'Kampung Dalam', 'Padang Bulan', 'Padang Terubuk', 'Sago']],
            'Pekanbaru Kota'  => ['lat' => 0.5105, 'lng' => 101.4470, 'tag' => ['padat'],
                'kelurahan' => ['Simpang Empat', 'Sumahilang', 'Tanah Datar', 'Kota Tinggi', 'Sukaramai']],
            'Sail'            => ['lat' => 0.5040, 'lng' => 101.4600, 'tag' => ['padat', 'rawan_air'],
                'kelurahan' => ['Cinta Raja', 'Sukamaju', 'Suka Mulya']],
            'Lima Puluh'      => ['lat' => 0.5250, 'lng' => 101.4530, 'tag' => ['padat', 'rawan_air'],
                'kelurahan' => ['Pesisir', 'Rintis', 'Sekip', 'Tanjung Rhu']],
            'Bukit Raya'      => ['lat' => 0.5300, 'lng' => 101.4800, 'tag' => ['padat'],
                'kelurahan' => ['Simpang Tiga', 'Air Dingin', 'Tangkerang Selatan', 'Tangkerang Utara']],
            'Marpoyan Damai'  => ['lat' => 0.4800, 'lng' => 101.4700, 'tag' => ['padat', 'rawan_air'],
                'kelurahan' => ['Maharatu', 'Sidomulyo Timur', 'Wonorejo', 'Tangkerang Barat', 'Perhentian Marpoyan']],
            'Tenayan Raya'    => ['lat' => 0.5000, 'lng' => 101.5200, 'tag' => ['rawan_air', 'lahan', 'industri'],
                'kelurahan' => ['Rejosari', 'Melebung', 'Kulim', 'Tangkerang Timur', 'Bambu Kuning']],
            'Payung Sekaki'   => ['lat' => 0.4700, 'lng' => 101.4000, 'tag' => ['rawan_air', 'lahan'],
                'kelurahan' => ['Labuh Baru Barat', 'Labuh Baru Timur', 'Air Hitam', 'Bandar Raya']],
            'Tampan'          => ['lat' => 0.4600, 'lng' => 101.3900, 'tag' => ['rawan_air', 'lahan', 'padat'],
                'kelurahan' => ['Delima', 'Simpang Baru', 'Sialang Munggu', 'Tuah Karya', 'Air Putih', 'Sidomulyo Barat']],
            'Rumbai'          => ['lat' => 0.5700, 'lng' => 101.4200, 'tag' => ['lahan', 'industri'],
                'kelurahan' => ['Umban Sari', 'Rumbai Bukit', 'Maharani', 'Agrowisata']],
            'Rumbai Pesisir'  => ['lat' => 0.5850, 'lng' => 101.3900, 'tag' => ['rawan_air', 'lahan', 'industri'],
                'kelurahan' => ['Limbungan', 'Lembah Damai', 'Meranti Pandak', 'Palas', 'Sri Meranti', 'Tebing Tinggi Okura']],
        ];
    }

    /**
     * Per kategori:
     *  laporan    => [judul, deskripsi, min_terdampak, maks_terdampak]
     *  ditangani  => variasi keterangan saat mulai ditangani
     *  selesai    => variasi keterangan saat selesai
     *  durasi_jam => [min, maks] lama penanganan dari mulai sampai selesai
     */
    private function templat(): array
    {
        return [
            // 1 Banjir
            1 => [
                'durasi_jam' => [6, 30],
                'ditangani' => [
                    'Tim relawan tiba di lokasi, warga dibantu memindahkan barang ke tempat tinggi',
                    'Penanganan dimulai, relawan membantu warga dan membersihkan saluran air',
                ],
                'selesai' => [
                    'Genangan sudah surut, warga kembali ke rumah masing-masing',
                    'Penanganan selesai, saluran air sudah dibersihkan dan kondisi aman',
                ],
                'laporan' => [
                    ['Banjir rendam permukiman warga', 'Hujan deras sejak dini hari membuat air meluap dan masuk ke rumah warga dengan ketinggian sekitar 40-60 cm. Warga memindahkan perabot ke tempat yang lebih tinggi.', 8, 40],
                    ['Genangan air tutup badan jalan', 'Setelah hujan semalaman, jalan tergenang setinggi lutut sehingga motor mogok dan mobil sulit lewat.', 3, 15],
                    ['Banjir setinggi dada, warga butuh evakuasi', 'Air naik sangat cepat dan sudah setinggi dada orang dewasa. Banyak warga lansia dan anak-anak yang butuh evakuasi dengan perahu.', 45, 110],
                    ['Drainase meluap, air masuk ke ruko', 'Saluran drainase tidak mampu menampung debit hujan sehingga air masuk ke beberapa ruko dan merusak barang dagangan.', 4, 14],
                    ['Banjir kiriman, lansia terjebak di rumah', 'Air dari hulu naik tiba-tiba. Ada beberapa lansia yang terjebak di rumah dan tidak bisa keluar sendiri.', 20, 60],
                    ['Air sungai naik, bantaran terancam', 'Debit sungai naik terus sejak sore dan hampir menyentuh pemukiman bantaran. Warga mulai bersiap mengungsi.', 10, 35],
                    ['Sekolah dasar terendam banjir', 'Air masuk ke ruang kelas dan halaman sekolah sehingga kegiatan belajar diliburkan sementara.', 60, 180],
                    ['Jalan perumahan tergenang, motor mogok', 'Genangan setinggi betis menyulitkan warga berangkat kerja. Air belum surut setelah beberapa jam.', 2, 10],
                ],
            ],
            // 2 Kebakaran
            2 => [
                'durasi_jam' => [2, 10],
                'ditangani' => [
                    'Relawan tiba bersama petugas pemadam dan membantu menjaga area sekitar',
                    'Penanganan dimulai, relawan membantu warga mengamankan barang dan mengatur lalu lintas',
                ],
                'selesai' => [
                    'Api sudah padam, area diamankan dan warga terdampak didata',
                    'Penanganan selesai, lokasi sudah didinginkan dan dipasang garis pengaman',
                ],
                'laporan' => [
                    ['Kebakaran rumah akibat korsleting listrik', 'Api muncul dari kabel listrik yang korsleting lalu menjalar ke rumah di sebelahnya. Warga berusaha memadamkan dengan alat seadanya.', 4, 15],
                    ['Kebakaran kios pasar tradisional', 'Api membesar cepat di deretan kios dan mobil pemadam belum tiba di lokasi.', 15, 40],
                    ['Kebakaran ruko, api meluas ke bangunan sebelah', 'Api berasal dari lantai dua sebuah ruko dan meluas ke bangunan di sebelahnya. Asap hitam terlihat dari jauh.', 20, 60],
                    ['Kebakaran gudang, pekerja terjebak', 'Gudang terbakar hebat dan satu pekerja dikabarkan masih terjebak di dalam.', 3, 12],
                    ['Kebakaran dapur rumah makan', 'Kompor gas bocor lalu menyambar minyak penggorengan. Api berhasil dilokalisir tetapi dapur rusak berat.', 2, 8],
                    ['Kebakaran rumah kayu, ada korban luka bakar', 'Rumah kayu terbakar saat penghuni tidur. Ada korban luka bakar yang sudah dibawa ke puskesmas terdekat.', 5, 14],
                    ['Mobil terbakar di tepi jalan', 'Mobil tiba-tiba mengeluarkan api dari bagian mesin. Pengemudi selamat tetapi api menjalar ke semak di sekitarnya.', 1, 5],
                    ['Kebakaran permukiman padat, warga mengungsi', 'Puluhan rumah semi permanen terbakar sekaligus karena berdempetan. Warga membutuhkan evakuasi dan tempat penampungan sementara.', 50, 140],
                ],
            ],
            // 3 Tanah longsor
            3 => [
                'durasi_jam' => [8, 48],
                'ditangani' => [
                    'Relawan tiba dan mulai membersihkan material longsor bersama warga',
                    'Penanganan dimulai, area dipasangi tanda bahaya dan material dipindahkan',
                ],
                'selesai' => [
                    'Material longsor sudah dibersihkan dan akses jalan kembali dibuka',
                    'Penanganan selesai, lokasi dipasangi pengaman sementara',
                ],
                'laporan' => [
                    ['Tanah longsor tutup separuh badan jalan', 'Longsoran tanah dari tebing menutup separuh badan jalan sehingga kendaraan harus bergantian lewat.', 3, 10],
                    ['Longsor di tepi sungai, rumah terancam roboh', 'Tebing tepi sungai longsor dan menggerus halaman dua rumah. Penghuni diminta mengungsi sementara.', 8, 20],
                    ['Longsor menimpa rumah, korban tertimbun', 'Longsoran menimpa bagian belakang rumah warga. Ada korban tertimbun dan butuh pertolongan segera.', 3, 9],
                    ['Jalan lingkungan amblas', 'Badan jalan amblas sedalam sekitar satu meter setelah hujan deras dan retakan terus bertambah.', 10, 40],
                    ['Turap penahan tebing runtuh', 'Turap penahan tebing runtuh pada sisi parit besar sehingga tanah di sekitarnya mulai bergeser.', 5, 15],
                ],
            ],
            // 4 Pencemaran
            4 => [
                'durasi_jam' => [24, 96],
                'ditangani' => [
                    'Relawan bersama perangkat kelurahan mengambil sampel air dan mendokumentasikan sumber pencemaran',
                    'Penanganan dimulai, relawan membantu membersihkan permukaan air dan memasang pembatas sementara',
                ],
                'selesai' => [
                    'Sumber pencemaran sudah ditindaklanjuti dengan pihak terkait, kondisi air membaik',
                    'Penanganan selesai, lokasi dipantau dan warga diberi imbauan',
                ],
                'laporan' => [
                    ['Sungai tercemar limbah berwarna hitam', 'Air sungai berubah hitam, berbau menyengat, dan ikan mati mengambang di sepanjang aliran.', 15, 60],
                    ['Tumpahan minyak di parit permukiman', 'Cairan minyak memenuhi parit dan mulai merembes ke sumur warga sekitar.', 6, 25],
                    ['Bau menyengat dari limbah cair pabrik', 'Bau menyengat muncul setiap malam dan beberapa warga mengeluh pusing serta mual.', 40, 120],
                    ['Asap hitam pekat dari pembakaran limbah', 'Pembakaran limbah di lahan kosong menghasilkan asap hitam yang masuk ke rumah-rumah warga.', 10, 50],
                    ['Air sumur warga berbau dan berminyak', 'Beberapa sumur warga mendadak berbau dan ada lapisan minyak di permukaan air.', 8, 30],
                    ['Sampah plastik menumpuk di sungai', 'Tumpukan sampah plastik menutup aliran sungai kecil dan mulai mengeluarkan bau busuk.', 3, 12],
                ],
            ],
            // 5 Karhutla
            5 => [
                'durasi_jam' => [12, 72],
                'ditangani' => [
                    'Relawan bergabung dengan tim pemadam lahan dan warga di lokasi',
                    'Penanganan dimulai, relawan membantu membuat sekat bakar dan membasahi lahan',
                ],
                'selesai' => [
                    'Titik api sudah dipadamkan, lokasi akan terus dipantau',
                    'Penanganan selesai, lahan dibasahi ulang dan tidak ditemukan titik api baru',
                ],
                'laporan' => [
                    ['Lahan gambut terbakar, asap pekat', 'Api terus meluas di lahan gambut dan asap pekat menyelimuti kampung sehingga jarak pandang menurun.', 30, 150],
                    ['Titik api kecil di semak kering', 'Titik api terlihat di semak kering pinggir jalan dan masih bisa dijangkau warga.', 1, 6],
                    ['Kebakaran lahan kosong dekat permukiman', 'Lahan kosong terbakar dan api mulai mendekati pagar belakang rumah warga.', 10, 40],
                    ['Kebun sawit dan semak terbakar', 'Api membakar semak di sela kebun sawit milik warga dan merambat pelan mengikuti angin.', 5, 20],
                    ['Asap tebal ganggu pernapasan warga', 'Asap dari lahan terbakar di dekat kampung membuat anak-anak dan lansia sesak napas.', 60, 200],
                    ['Api merambat di lahan gambut dekat sekolah', 'Api merambat di lahan gambut sekitar 200 meter dari sekolah. Pihak sekolah menyiapkan evakuasi siswa jika api mendekat.', 40, 120],
                ],
            ],
            // 6 Lainnya
            6 => [
                'durasi_jam' => [2, 24],
                'ditangani' => [
                    'Relawan tiba di lokasi dan mulai membersihkan bersama warga',
                    'Penanganan dimulai, area diberi tanda dan diatur agar tidak membahayakan pengguna jalan',
                ],
                'selesai' => [
                    'Penanganan selesai, lokasi sudah dibersihkan dan aman dilewati',
                    'Selesai ditangani bersama warga dan pihak terkait',
                ],
                'laporan' => [
                    ['Pohon tumbang timpa kabel listrik', 'Pohon besar tumbang akibat angin kencang dan memutus aliran listrik satu RT.', 5, 30],
                    ['Pohon tumbang halangi jalan', 'Pohon tumbang menutup seluruh badan jalan dan kendaraan harus memutar.', 2, 10],
                    ['Sampah menumpuk sumbat drainase', 'Tumpukan sampah menyumbat saluran sehingga air hujan mulai menggenang di depan rumah warga.', 4, 20],
                    ['Atap rumah warga roboh diterjang angin', 'Angin kencang menerbangkan atap beberapa rumah. Tidak ada yang terluka tetapi rumah tidak bisa ditempati malam ini.', 6, 25],
                    ['Jalan berlubang parah usai hujan', 'Lubang besar di badan jalan sudah menyebabkan beberapa pengendara motor terjatuh.', 5, 20],
                    ['Tiang listrik miring hampir roboh', 'Tiang listrik di pinggir jalan miring setelah tanah di bawahnya tergerus air hujan.', 4, 15],
                    ['Ular masuk ke rumah warga', 'Ular berukuran besar masuk ke rumah dan bersembunyi di bawah lemari. Penghuni takut masuk ke dalam.', 1, 4],
                ],
            ],
        ];
    }
}