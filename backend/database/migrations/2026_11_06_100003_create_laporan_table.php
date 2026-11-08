<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('laporan', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
            $table->foreignId('kategori_id')->constrained('kategori_bencana');
            $table->string('judul', 120);
            $table->text('deskripsi');
            $table->string('foto')->nullable();
            $table->decimal('latitude', 10, 7);
            $table->decimal('longitude', 10, 7);
            $table->unsignedInteger('jumlah_terdampak')->default(0);
            $table->enum('urgensi', ['rendah', 'sedang', 'tinggi', 'kritis'])->default('sedang');
            $table->enum('status', ['baru', 'diverifikasi', 'ditangani', 'selesai'])->default('baru');
            $table->timestamps();

            $table->index('status');
            $table->index('urgensi');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('laporan');
    }
};
