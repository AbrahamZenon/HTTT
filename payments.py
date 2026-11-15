from contextlib import closing, contextmanager
from datetime import date
import sqlite3


UNPAID = "Chưa thanh toán"
PAID = "Đã thanh toán"
CASH = "Tiền mặt"
TRANSFER = "Chuyển khoản"

SCHEMA = """
CREATE TABLE IF NOT EXISTS HOADON (
    MaHD TEXT PRIMARY KEY,
    TongTien INTEGER NOT NULL CHECK (TongTien > 0),
    TrangThai TEXT NOT NULL CHECK (TrangThai IN ('Chưa thanh toán', 'Đã thanh toán'))
);
CREATE TABLE IF NOT EXISTS LOTHUOC (
    MaLo TEXT PRIMARY KEY,
    TenThuoc TEXT NOT NULL,
    HanSuDung TEXT NOT NULL,
    SoLuongTon INTEGER NOT NULL CHECK (SoLuongTon >= 0)
);
CREATE TABLE IF NOT EXISTS CHITIETHOADON (
    MaHD TEXT NOT NULL REFERENCES HOADON,
    MaLo TEXT NOT NULL REFERENCES LOTHUOC,
    SoLuong INTEGER NOT NULL CHECK (SoLuong > 0),
    DonGia INTEGER NOT NULL CHECK (DonGia > 0),
    PRIMARY KEY (MaHD, MaLo)
);
CREATE TABLE IF NOT EXISTS THANHTOAN (
    MaTT INTEGER PRIMARY KEY,
    MaHD TEXT NOT NULL UNIQUE REFERENCES HOADON,
    SoTien INTEGER NOT NULL CHECK (SoTien > 0),
    PhuongThuc TEXT NOT NULL CHECK (PhuongThuc IN ('Tiền mặt', 'Chuyển khoản')),
    SoTienNhan INTEGER NOT NULL CHECK (SoTienNhan >= SoTien),
    MaGiaoDich TEXT UNIQUE,
    ThoiGian TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
"""


class PaymentError(ValueError):
    pass


class Store:
    def __init__(self, path):
        self.path = path
        with self.connection() as db:
            db.executescript(SCHEMA)

    @contextmanager
    def connection(self):
        with closing(sqlite3.connect(self.path)) as db, db:
            db.row_factory = sqlite3.Row
            db.execute("PRAGMA foreign_keys = ON")
            yield db

    def seed_demo(self):
        with self.connection() as db:
            db.execute("BEGIN IMMEDIATE")
            if db.execute("SELECT 1 FROM HOADON LIMIT 1").fetchone():
                return
            db.executemany("INSERT INTO HOADON VALUES (?, ?, ?)", [
                ("HD001", 57000, UNPAID), ("HD002", 90000, UNPAID),
            ])
            db.executemany("INSERT INTO LOTHUOC VALUES (?, ?, ?, ?)", [
                ("LO001", "Paracetamol 500 mg", "2099-12-31", 100),
                ("LO002", "Vitamin C 500 mg", "2099-12-31", 50),
            ])
            db.executemany("INSERT INTO CHITIETHOADON VALUES (?, ?, ?, ?)", [
                ("HD001", "LO001", 1, 12000), ("HD001", "LO002", 1, 45000),
                ("HD002", "LO002", 2, 45000),
            ])

    def invoices(self):
        with self.connection() as db:
            return db.execute("SELECT * FROM HOADON ORDER BY MaHD").fetchall()

    def details(self, db, invoice_id):
        return db.execute("""
            SELECT c.*, l.TenThuoc, l.HanSuDung, l.SoLuongTon
            FROM CHITIETHOADON c JOIN LOTHUOC l USING (MaLo)
            WHERE c.MaHD = ? ORDER BY c.MaLo
        """, (invoice_id,)).fetchall()

    def invoice_details(self, invoice_id):
        with self.connection() as db:
            lines = self.details(db, invoice_id)
            payment = db.execute("SELECT * FROM THANHTOAN WHERE MaHD = ?", (invoice_id,)).fetchone()
            return lines, payment

    def pay(self, invoice_id, method, cash_received=None, reference=None):
        with self.connection() as db:
            db.execute("BEGIN IMMEDIATE")
            invoice = db.execute("SELECT * FROM HOADON WHERE MaHD = ?", (invoice_id,)).fetchone()
            if invoice is None:
                raise PaymentError("Không tìm thấy hóa đơn.")
            if invoice["TrangThai"] != UNPAID:
                raise PaymentError("Hóa đơn đã được thanh toán.")

            total = invoice["TongTien"]
            if method == CASH:
                if type(cash_received) is not int or not total <= cash_received <= 2**63 - 1:
                    raise PaymentError("Nhập số tiền mặt hợp lệ và đủ để thanh toán.")
                received, reference = cash_received, None
            elif method == TRANSFER:
                reference = (reference or "").strip().upper()
                if not reference:
                    raise PaymentError("Nhập mã giao dịch sau khi xác nhận đã nhận đủ tiền.")
                if db.execute("SELECT 1 FROM THANHTOAN WHERE MaGiaoDich = ?", (reference,)).fetchone():
                    raise PaymentError("Mã giao dịch đã được sử dụng.")
                received = total
            else:
                raise PaymentError("Phương thức thanh toán không hợp lệ.")

            lines = self.details(db, invoice_id)
            if not lines or sum(line["SoLuong"] * line["DonGia"] for line in lines) != total:
                raise PaymentError("Chi tiết hóa đơn không khớp với tổng tiền.")
            today = date.today()
            for line in lines:
                if date.fromisoformat(line["HanSuDung"]) < today:
                    raise PaymentError(f'Lô {line["MaLo"]} đã hết hạn sử dụng.')
                if line["SoLuongTon"] < line["SoLuong"]:
                    raise PaymentError(f'Lô {line["MaLo"]} không đủ số lượng tồn.')
                db.execute("UPDATE LOTHUOC SET SoLuongTon = SoLuongTon - ? WHERE MaLo = ?",
                           (line["SoLuong"], line["MaLo"]))

            db.execute("""
                INSERT INTO THANHTOAN (MaHD, SoTien, PhuongThuc, SoTienNhan, MaGiaoDich)
                VALUES (?, ?, ?, ?, ?)
            """, (invoice_id, total, method, received, reference))
            db.execute("UPDATE HOADON SET TrangThai = ? WHERE MaHD = ?", (PAID, invoice_id))
            return received - total
