<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    protected $fillable = ['nama', 'email', 'password', 'role', 'no_hp', 'alamat', 'foto_url', 'spesialisasi'];

    protected $hidden = ['password', 'remember_token'];

    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
        ];
    }

    public function laporan(): HasMany
    {
        return $this->hasMany(Laporan::class, 'user_id');
    }

    public function penugasanSebagaiRelawan(): HasMany
    {
        return $this->hasMany(Penugasan::class, 'relawan_id');
    }
}
