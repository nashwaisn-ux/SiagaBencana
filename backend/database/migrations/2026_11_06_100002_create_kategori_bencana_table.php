<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('kategori_bencana', function (Blueprint $table) {
            $table->id();
            $table->string('nama', 100)->unique();
            // dipakai fitur skor urgensi otomatis (makin tinggi = makin darurat)
            $table->unsignedTinyInteger('bobot_urgensi')->default(0);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('kategori_bencana');
    }
};
