<?php

namespace App\Http\Controllers;

use App\Models\KategoriBencana;
use Illuminate\Http\Request;

class KategoriController extends Controller
{
    public function index()
    {
        $kategori = KategoriBencana::all()->map(function ($k) {
            return [
                'id' => $k->id,
                'nama' => $k->nama,
            ];
        });

        return $this->successResponse($kategori, 'OK');
    }
}
