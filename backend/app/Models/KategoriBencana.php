<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class KategoriBencana extends Model
{
    protected $table = 'kategori_bencana';
    public $timestamps = false;

    protected $fillable = ['nama', 'bobot_urgensi'];

    public function laporan(): HasMany
    {
        return $this->hasMany(Laporan::class, 'kategori_id');
    }
}
