# Catatan Progres — Application Platform

Terakhir diperbarui: 14 Oktober 2026

## Cara menjalankan di lokal

```powershell
$env:JAVA_HOME = "C:\Users\10101804\Downloads\jdk-21.0.12_windows-x64_bin\jdk-21.0.12"
.\mvnw.cmd spring-boot:run
```

- Buka http://localhost:8080, login `admin` / `admin123`
- `JAVA_HOME` belum di-set permanen di laptop. Tanpa baris pertama, Maven memakai JRE dan gagal compile.
  Sebaiknya set permanen di Environment Variables Windows, dan pindahkan JDK keluar dari folder Downloads.
- Menjalankan test: `.\mvnw.cmd test` (46 test, semua lulus)

### Melihat isi database
- Buka http://localhost:8080/h2-console (harus login aplikasi dulu)
- JDBC URL: `jdbc:h2:file:./data/devdb`, user `sa`, password kosong
- Reset database dari awal: matikan aplikasi, hapus folder `data/`, lalu jalankan lagi. Seed data dibuat ulang otomatis.

---

## Yang sudah selesai

### 1. GitHub
- Remote `origin` → https://github.com/hunter12/application-platform.git
- Semua commit sudah dibuat (termasuk Approval Workflows, Approval Rules, dan Delegation), tapi **belum di-push**.
  Perlu login GitHub sekali: `git push -u origin main`

### 2. Database H2 (mode file)
- Dependency: `spring-boot-starter-data-jpa`, `h2`, `spring-boot-h2console` (Boot 4 memisahkan H2 console jadi modul sendiri)
- Konfigurasi ada di `application-dev.properties` (hanya profile `dev`)
- Data tersimpan di folder `data/` (sudah di `.gitignore`)
- H2 console hanya terbuka di profile `dev` dan tetap wajib login → `DevH2ConsoleSecurityConfig`
- Test memakai H2 in-memory (`src/test/resources/config/`), jadi tidak mengotori database dev

### 3. Kolom standar semua tabel (`BaseEntity`)
| Kolom | Keterangan |
|---|---|
| `id` | UUID v7 (berurutan waktu, index tetap cepat) |
| `created_date`, `created_by` | Otomatis diisi, dari user yang login atau `system` |
| `modified_date`, `modified_by` | Otomatis, kosong sampai data benar-benar diubah |
| `deleted_date`, `deleted_by` | Soft delete: data tidak benar-benar dihapus, hanya disembunyikan |
| `version` | Mencegah dua orang menimpa edit satu sama lain |
| `is_active` | Aktif atau nonaktif |

### 4. Tabel yang sudah ada
| Tabel | Relasi (FK) |
|---|---|
| `organizations` | `parent_id` → organizations |
| `users` | `organization_id` → organizations (wajib), `supervisor_id` → users |
| `roles` | — |
| `permissions` | — |
| `user_roles` | user, role, organization (scope, opsional). Satu user bisa punya banyak role. |
| `role_permissions` | role, permission |
| `modules` | — (master modul: Finance, Invoice, Stock, Product, Report) |
| `approval_workflows` | `module_id` → modules |
| `approval_workflow_steps` | `workflow_id` → approval_workflows, `role_id` → roles (kosong untuk Atasan langsung) |
| `approval_rules` | `module_id` → modules, `workflow_id` → approval_workflows |
| `delegations` | `delegator_id` → users, `delegate_id` → users |
| `notifications` | `recipient_id` → users (wajib), `sender_id` → users (kosong = dari sistem) |

### 5. Login dari database
- Password disimpan sebagai BCrypt hash
- Role menjadi `ROLE_<code>`, permission menjadi kodenya (contoh `INVOICE_APPROVE`)
- User nonaktif tidak bisa login. User yang sudah dihapus dianggap tidak ada.
- Seed modul (`ModuleSeeder`) jalan di semua profile, hanya kalau tabel `modules` kosong. Belum ada halaman admin untuk modul.
- Seed data dev (`DevDataSeeder`), hanya jalan kalau tabel `users` kosong: 10 organisasi, 6 role, 12 permission, user `admin` (Super Admin)

### 6. Halaman yang sudah terhubung ke database
| Halaman | Fitur |
|---|---|
| Organizations | daftar, cari/filter, tambah, edit, aktif/nonaktif |
| Organization Hierarchy | pohon dari `parent_id` |
| Users | daftar + kolom Roles, tambah, edit, hapus (soft delete) |
| Roles | daftar + jumlah user, tambah, edit, aktif/nonaktif, **View Access** (atur permission per role) |
| Permissions | daftar, filter, tambah, edit, aktif/nonaktif |
| Approval Workflows | daftar, filter modul/status, tambah, edit (termasuk step), View step, aktif/nonaktif |
| Approval Rules | daftar, filter modul/status, tambah, edit, aktif/nonaktif |
| Delegation | daftar, filter status, tambah, edit (hanya Scheduled), View, Cancel |
| Notifications | inbox milik sendiri, cari/filter tipe & status, View (otomatis Read), Mark as Read, Mark all as read, Delete, Add (kirim pengumuman) |

Perubahan form dibanding UI lama:
- Add Notification: field Status dibuang (selalu Unread), tipe hanya System / Information
- Menu samping: badge jumlah notifikasi unread di "Notifications"
- Add User: tambah field **Password**, Role jadi **multi-select**, kolom email dan phone ditambahkan ke tabel
- Add Role: tambah **Role Code** (tidak bisa diubah setelah dibuat)
- Permission: kolom `name` ditambahkan. Code tidak bisa diubah setelah dibuat.

Aturan yang dijaga sistem:
- Kode/username unik, termasuk terhadap data yang sudah dihapus
- Organisasi tidak bisa dipindah ke bawah sub-unitnya sendiri. User tidak bisa melapor ke bawahannya sendiri.
- Admin tidak bisa menghapus/menonaktifkan akunnya sendiri. Role Super Admin tidak bisa dinonaktifkan.
- Role/permission yang dicabut hanya dinonaktifkan, tidak dihapus (jejak audit tetap ada)

### 7. Bersih-bersih
- File mock untuk Users, Roles, Permissions, Organizations, Approval, Delegation, dan Notifications dihapus
- Komentar di bagian atas setiap class dan file properties dihapus
- File log `run-err*.log` dan `run-out*.log` dihapus, dan `*.log` ditambahkan ke `.gitignore`

---

## Keputusan desain yang sudah disepakati

1. **Satu user boleh banyak role**
2. **Delegasi** tidak memindahkan role. Delegasi dicek saat approval dan dicatat sebagai "B atas nama A". Tidak boleh berantai, dan penerima harus dari organisasi yang sama atau di atasnya.
3. **Approval hanya boleh naik ke atas, tidak ke samping.** Cabang Bogor tidak boleh di-approve oleh Bandung, dan IT tidak boleh di-approve oleh Finance. Naik ke Regional atau Head Office boleh.
4. **Rule nominal**: `min_amount` dan `max_amount` wajib diisi, keduanya inklusif dengan selisih 0,01
   (contoh: 0–10.000.000 / 10.000.000,01–100.000.000 / 100.000.000,01–9.999.999.999.999,99)
5. **Cukup `is_active`**, tanpa jadwal aktivasi (`effective_from/to`)
6. **Module diambil dari tabel `modules`**, dipilih lewat dropdown, tidak boleh teks bebas
7. **Tipe approver**: Atasan langsung pemohon / Role di organisasi pemohon / Role di organisasi induk.
   "Organisasi induk" artinya naik terus sampai ketemu organisasi yang punya user dengan role itu.
8. **Pengajuan tidak bisa diedit** setelah diajukan. Kalau salah, approver Reject, lalu pemohon perbaiki dan ajukan ulang.
9. **Workflow dikunci selama masih ada pengajuan pending.** Pengajuan langsung memakai step workflow, tanpa salinan.
   Pengecekan ini ditambahkan saat tabel pengajuan dibuat.
10. **Approval Rule tanpa priority**: rentang nominal rule aktif dalam satu modul tidak boleh tumpang tindih,
    jadi satu nominal pasti hanya cocok ke satu rule.
11. **Celah nominal dibolehkan.** Kalau saat pengajuan tidak ada rule yang cocok, pengajuan ditolak
    ("Belum ada approval rule untuk nominal ini").
12. **Semua nominal dalam Rupiah**, decimal(15,2)
13. Workflow rule harus dari **modul yang sama**. Workflow yang dipakai rule aktif tidak bisa dinonaktifkan,
    dan modul workflow tidak bisa diganti kalau sudah dipakai rule.
14. **Delegation**: status dihitung dari tanggal (Scheduled / Active / Completed), Cancelled dari `cancelled_date`.
    - Edit hanya selama Scheduled. Active hanya bisa Cancel. Completed/Cancelled tidak bisa diubah.
    - Tanggal mulai tidak boleh sebelum hari ini
    - Berlaku untuk semua modul
    - Satu pemberi tidak boleh punya dua delegasi yang periodenya tumpang tindih
    - Tidak berantai: penerima tidak boleh sedang mendelegasikan, dan pemberi tidak boleh sedang menjadi penerima,
      di periode yang sama
    - Admin input untuk siapa saja. User input untuk diri sendiri menunggu pembatasan akses per permission.
15. **Notifications**
    - Setiap user (termasuk admin) hanya melihat dan mengubah notifikasi miliknya sendiri
    - Status Read dari `read_date`, Mark as Read satu arah (tidak ada Mark as Unread)
    - Tidak bisa diedit setelah dikirim. Delete = soft delete.
    - Kirim manual hanya tipe System / Information, ke satu user aktif. Approval / Delegation dibuat otomatis oleh sistem.
    - Notifikasi otomatis untuk delegasi (dibuat/di-cancel) ditunda sampai Notification Policy

---

## Task berikutnya (sesuai saran yang sudah disetujui)

Hapus semua data mock yang tersisa dan hubungkan ke tabel:

- [x] **Approval Workflows**: tabel `modules`, `approval_workflows`, `approval_workflow_steps`
- [x] **Approval Rules**: tabel `approval_rules`
- [x] **Delegation**: tabel `delegations`
- [x] **Notifications**: tabel `notifications`
- [ ] **Notification Policy**: tabel `notification_policies` (untuk sekarang hanya disimpan sebagai pengaturan)
- [ ] **Audit Log**: tabel `audit_logs`, **diisi otomatis** (create/update/delete + login berhasil/gagal), hanya bisa dibaca
  - Filter tanggal diganti date picker
- [ ] **Dashboard**: angka asli dari database, dan Recent Activity diambil dari audit log
- [ ] Update catatan ini setelah selesai

## Task setelahnya (belum dibahas detail)

- [ ] Batasi akses halaman berdasarkan permission. Saat ini semua user yang login bisa membuka semua halaman admin.
- [ ] Tabel pengajuan approval yang sebenarnya (request, riwayat approve/reject), supaya Pending Approvals berfungsi
  - Tolak edit/nonaktifkan workflow selama masih ada pengajuan pending (keputusan no. 9)
  - Tolak pengajuan kalau tidak ada rule yang cocok (keputusan no. 11)
- [ ] Database untuk production (misalnya PostgreSQL). Profile `prod` belum punya konfigurasi database.
- [ ] Commit dan push ke GitHub
