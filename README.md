# Split Bill

Aplikasi untuk mencatat pengeluaran bersama dalam satu grup, misalnya saat liburan bareng teman, lalu menghitung **siapa harus membayar ke siapa** supaya semuanya lunas dengan jumlah transfer sesedikit mungkin.

## Apa saja yang bisa dilakukan

- Membuat pengguna dan grup, lalu menambahkan anggota ke grup
- Mencatat pengeluaran dan membaginya:
  - **rata** ke semua peserta,
  - berdasarkan **persentase**, atau
  - dengan **nominal tertentu** per orang
- Melihat saldo setiap anggota: siapa yang masih berutang dan siapa yang masih harus menerima
- Melihat laporan pengeluaran per bulan dan per kategori
- Mendapatkan saran transfer untuk melunasi semua utang
- Mencatat pelunasan yang sudah dilakukan

## Cara menjalankan

Yang dibutuhkan hanya **Docker Desktop**.

1. Buka terminal di folder project ini, lalu jalankan:
   ```bash
   docker compose up --build -d
   ```
   Saat pertama kali, prosesnya bisa memakan beberapa menit.

2. Buka halaman ini di browser:

   **http://localhost:8080/swagger-ui.html**

   Dari halaman tersebut semua fitur bisa dicoba langsung. Pilih bagian aplikasi di pojok kanan atas (*Select a definition*).

3. Untuk mematikan aplikasi:
   ```bash
   docker compose down
   ```
   Tambahkan `-v` di akhir perintah kalau ingin menghapus semua data.

## Contoh cara mencoba

Cerita singkatnya: Budi, Ani, Citra, dan Dodi liburan ke Bali.

1. **Buat pengguna** (bagian *user-service*, `POST /user`), satu per satu:
   ```json
   { "name": "Budi", "email": "budi@example.com" }
   ```
2. **Buat grup** (`POST /group`). Pembuat grup otomatis menjadi anggota:
   ```json
   { "name": "Liburan Bali", "createdByUserId": 1 }
   ```
3. **Tambahkan anggota lain** (`POST /group/user`):
   ```json
   { "groupId": 1, "userId": 2 }
   ```
4. **Catat pengeluaran** (bagian *expense-service*, `POST /expense`). Contoh: Budi membayar hotel untuk berempat, dibagi rata:
   ```json
   {
     "groupId": 1,
     "paidBy": 1,
     "description": "Hotel 2 malam",
     "category": "ACCOMMODATION",
     "amount": 3000000,
     "expenseDate": "2026-11-20",
     "splitType": "EQUAL",
     "participants": [ { "userId": 1 }, { "userId": 2 }, { "userId": 3 }, { "userId": 4 } ]
   }
   ```
5. **Lihat saldo** (`GET /expense/balance?groupId=1`). Angka positif artinya masih harus menerima uang, angka negatif artinya masih berutang.
6. **Lihat saran transfer** (bagian *settlement-service*, `GET /settlement/suggestion?groupId=1`).
7. **Catat pelunasan** (`POST /settlement`) setelah transfer dilakukan, lalu cek lagi saran transfernya:
   ```json
   { "groupId": 1, "fromUserId": 2, "toUserId": 1, "amount": 750000 }
   ```

## Isi project

| Folder | Fungsi |
|---|---|
| `user-service` | Mengelola pengguna dan grup |
| `expense-service` | Mencatat pengeluaran, menghitung saldo, dan membuat laporan |
| `settlement-service` | Memberi saran transfer dan mencatat pelunasan |
| `split-bill-gateway` | Pintu masuk tunggal untuk semua fitur, sekaligus halaman Swagger |
