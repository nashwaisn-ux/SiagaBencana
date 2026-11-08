<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Penugasan extends Model
{
    protected $table = 'penugasan';

    protected $fillable = [
        'laporan_id', 'relawan_id', 'petugas_id', 'catatan', 'status_tugas', 'foto_bukti',
    ];

    public function laporan(): BelongsTo
    {
        return $this->belongsTo(Laporan::class, 'laporan_id');
    }

    public function relawan(): BelongsTo
    {
        return $this->belongsTo(User::class, 'relawan_id');
    }

    public function petugas(): BelongsTo
    {
        return $this->belongsTo(User::class, 'petugas_id');
    }
}
