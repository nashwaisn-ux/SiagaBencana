<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Laporan extends Model
{
    protected $table = 'laporan';

    protected $fillable = [
        'user_id', 'kategori_id', 'judul', 'deskripsi', 'foto',
        'latitude', 'longitude', 'jumlah_terdampak', 'urgensi', 'status',
    ];

    // Kontrak API: koordinat harus angka, bukan string
    protected $casts = [
        'latitude' => 'float',
        'longitude' => 'float',
        'jumlah_terdampak' => 'integer',
    ];

    public function pelapor(): BelongsTo
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    public function kategori(): BelongsTo
    {
        return $this->belongsTo(KategoriBencana::class, 'kategori_id');
    }

    public function penugasan(): HasMany
    {
        return $this->hasMany(Penugasan::class, 'laporan_id');
    }

    public function riwayat(): HasMany
    {
        return $this->hasMany(RiwayatStatus::class, 'laporan_id');
    }
}
