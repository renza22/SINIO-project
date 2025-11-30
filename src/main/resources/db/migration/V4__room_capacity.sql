-- Add kapasitas (jumlah orang) per kamar agar dapat diatur dari halaman admin.
-- Default ke 2 untuk kamar yang sudah ada agar tetap dapat digunakan.

ALTER TABLE rooms
ADD COLUMN IF NOT EXISTS max_occupancy INT NOT NULL DEFAULT 2;

-- Pastikan nilai terisi untuk baris lama jika kolom sudah terbuat tanpa default.
UPDATE rooms
SET max_occupancy = 2
WHERE max_occupancy IS NULL;
