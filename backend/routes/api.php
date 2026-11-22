<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\KategoriController;
use App\Http\Controllers\RelawanController;
use App\Http\Controllers\LaporanController;
use App\Http\Controllers\PenugasanController;
use App\Http\Controllers\PesanController;
use App\Http\Controllers\RiwayatController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\PetaController;

// Public routes
Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:10,1');
Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:10,1');
Route::post('/reset-password', [AuthController::class, 'resetPassword'])->middleware('throttle:5,1');
Route::get('/kategori', [KategoriController::class, 'index']);

// Protected routes (Sanctum)
Route::middleware('auth:sanctum')->group(function () {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/me', [AuthController::class, 'me']);
    Route::post('/profile/update', [AuthController::class, 'updateProfile']);
    Route::post('/profile/photo', [AuthController::class, 'uploadPhoto']);
    Route::post('/profile/password', [AuthController::class, 'changePassword'])->middleware('throttle:10,1');

    // Laporan routes
    Route::get('/laporan', [LaporanController::class, 'index']);
    Route::post('/laporan', [LaporanController::class, 'store']);
    Route::post('/laporan/bulk-delete', [LaporanController::class, 'bulkDelete']);
    Route::get('/laporan/{id}', [LaporanController::class, 'show']);
    Route::put('/laporan/{id}', [LaporanController::class, 'update']);
    Route::post('/laporan/{id}', [LaporanController::class, 'update']); // Handling PUT multipart form-data via POST + _method=PUT
    Route::delete('/laporan/{id}', [LaporanController::class, 'destroy']);

    Route::post('/laporan/{id}/tugaskan', [LaporanController::class, 'tugaskan']);
    Route::patch('/laporan/{id}/status', [LaporanController::class, 'updateStatus']);
    Route::patch('/laporan/{id}/urgensi', [LaporanController::class, 'updateUrgensi']);

    // Pesan (Chat per Laporan)
    Route::get('/laporan/{id}/pesan', [PesanController::class, 'index']);
    Route::post('/laporan/{id}/pesan', [PesanController::class, 'store']);

    // Relawan
    Route::get('/relawan', [RelawanController::class, 'index']);
    Route::post('/relawan', [RelawanController::class, 'store']);

    // Penugasan
    Route::get('/penugasan', [PenugasanController::class, 'index']);
    Route::patch('/penugasan/{id}/status', [PenugasanController::class, 'updateStatus']);
    Route::post('/penugasan/{id}/bukti', [PenugasanController::class, 'uploadBukti']);

    // Riwayat, Dashboard, Peta
    Route::get('/riwayat', [RiwayatController::class, 'index']);
    Route::get('/dashboard', [DashboardController::class, 'index']);
    Route::get('/peta', [PetaController::class, 'index']);
});
