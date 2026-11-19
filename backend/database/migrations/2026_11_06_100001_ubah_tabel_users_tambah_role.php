<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Samakan nama kolom dengan ERD & kontrak API: name -> nama
        Schema::table('users', function (Blueprint $table) {
            $table->renameColumn('name', 'nama');
        });

        Schema::table('users', function (Blueprint $table) {
            $table->enum('role', ['masyarakat', 'relawan', 'petugas'])
                  ->default('masyarakat')->after('password');
            $table->string('no_hp', 20)->nullable()->after('role');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['role', 'no_hp']);
        });

        Schema::table('users', function (Blueprint $table) {
            $table->renameColumn('nama', 'name');
        });
    }
};
