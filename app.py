import argparse
from pathlib import Path
import sqlite3
import tkinter as tk
from tkinter import messagebox, ttk

from payments import CASH, TRANSFER, UNPAID, PaymentError, Store


def money(amount):
    return f"{amount:,}".replace(",", ".") + " ₫"


class PaymentWindow:
    def __init__(self, root, store):
        self.store = store
        root.title("Nhà thuốc · UC02 Thanh toán hóa đơn")
        root.geometry("900x670")
        root.minsize(700, 600)
        body = ttk.Frame(root, padding=22)
        body.pack(fill="both", expand=True)
        ttk.Label(body, text="Thanh toán hóa đơn", font=("Segoe UI", 22, "bold")).pack(anchor="w")
        self.invoices = self.table(body, ("Mã hóa đơn", "Tổng tiền", "Trạng thái"), 5)
        self.invoices.bind("<<TreeviewSelect>>", lambda event: self.show_invoice())

        ttk.Label(body, text="Chi tiết hóa đơn", font=("Segoe UI", 12, "bold")).pack(anchor="w", pady=(14, 0))
        self.lines = self.table(body, ("Mã lô", "Thuốc", "Số lượng", "Đơn giá", "Thành tiền"), 5)
        self.summary = ttk.Label(body, wraplength=650)
        self.summary.pack(anchor="w", pady=8)

        form = ttk.Frame(body)
        form.pack(fill="x")
        form.columnconfigure(1, weight=1)
        self.method = ttk.Combobox(form, values=(CASH, TRANSFER), width=18)
        self.method.set(CASH)
        self.method.grid(row=0, column=1, sticky="w")
        self.method.bind("<<ComboboxSelected>>", lambda event: self.change_method())
        ttk.Label(form, text="Phương thức").grid(row=0, column=0, sticky="w", padx=(0, 16))
        self.input_label = ttk.Label(form)
        self.input_label.grid(row=1, column=0, sticky="w")
        self.value = tk.StringVar()
        self.entry = ttk.Entry(form, textvariable=self.value, width=25)
        self.entry.grid(row=1, column=1, sticky="w", pady=8)
        self.pay_button = ttk.Button(form, text="Xác nhận thanh toán", command=self.confirm_payment)
        self.pay_button.grid(row=2, column=1, sticky="w")
        ttk.Button(form, text="Làm mới", command=self.refresh).grid(row=2, column=2)
        self.change_method()
        self.refresh()

    @staticmethod
    def table(parent, columns, height):
        frame = ttk.Frame(parent)
        frame.pack(fill="x", pady=6)
        tree = ttk.Treeview(frame, columns=columns, show="headings", height=height, selectmode="browse")
        scroll = ttk.Scrollbar(frame, orient="vertical", command=tree.yview)
        tree.configure(yscrollcommand=scroll.set)
        for column in columns:
            tree.heading(column, text=column)
            tree.column(column, width=130, minwidth=65)
        tree.pack(side="left", fill="x", expand=True)
        scroll.pack(side="right", fill="y")
        return tree

    def refresh(self):
        selected = self.invoices.selection()
        self.invoices.delete(*self.invoices.get_children())
        for invoice in self.store.invoices():
            self.invoices.insert("", "end", iid=invoice["MaHD"], values=(
                invoice["MaHD"], money(invoice["TongTien"]), invoice["TrangThai"],
            ))
        children = self.invoices.get_children()
        if children:
            self.invoices.selection_set(selected[0] if selected and selected[0] in children else children[0])
        self.show_invoice()

    def change_method(self):
        self.value.set("")
        label = "Tiền khách đưa (₫)" if self.method.get() == CASH else "Mã giao dịch"
        self.input_label.configure(text=label)

    def show_invoice(self):
        self.lines.delete(*self.lines.get_children())
        selected = self.invoices.selection()
        if not selected:
            self.summary.configure(text="Chưa có hóa đơn.")
            self.set_payable(False)
            return
        invoice_id = selected[0]
        lines, payment = self.store.invoice_details(invoice_id)
        for line in lines:
            self.lines.insert("", "end", values=(
                line["MaLo"], line["TenThuoc"], line["SoLuong"], money(line["DonGia"]),
                money(line["DonGia"] * line["SoLuong"]),
            ))
        _, total, status = self.invoices.item(invoice_id, "values")
        self.value.set("")
        if payment:
            extra = (f' · Mã giao dịch {payment["MaGiaoDich"]}' if payment["MaGiaoDich"] else
                     f' · Tiền thừa {money(payment["SoTienNhan"] - payment["SoTien"])}')
            self.summary.configure(text=f'{invoice_id} · {total} · {status} · {payment["PhuongThuc"]}'
                                        f'{extra}\n{payment["ThoiGian"]}')
        else:
            self.summary.configure(text=f"{invoice_id} · {total} · {status}")
        self.set_payable(status == UNPAID and payment is None)

    def set_payable(self, enabled):
        self.method.configure(state="readonly" if enabled else "disabled")
        self.entry.configure(state="normal" if enabled else "disabled")
        self.pay_button.configure(state="normal" if enabled else "disabled")

    def confirm_payment(self):
        selected = self.invoices.selection()
        if not selected:
            return
        invoice_id = selected[0]
        _, total, _ = self.invoices.item(invoice_id, "values")
        method = self.method.get()
        value = self.value.get().strip()
        if method == CASH:
            if not value.isascii() or not value.isdecimal() or len(value) > 19:
                messagebox.showwarning("Số tiền không hợp lệ", "Nhập số tiền bằng chữ số, ví dụ 60000.")
                return
            arguments = {"cash_received": int(value)}
            prompt = f"Ghi nhận tiền mặt {money(int(value))} cho {invoice_id} ({total})?"
        else:
            if not value:
                messagebox.showwarning("Thiếu mã giao dịch", "Nhập mã giao dịch chuyển khoản.")
                return
            arguments = {"reference": value}
            prompt = f"Bạn đã kiểm tra và nhận đủ {total} cho {invoice_id}, mã giao dịch {value}?"
        if not messagebox.askyesno("Xác nhận thanh toán", prompt):
            return
        try:
            change = self.store.pay(invoice_id, method, **arguments)
        except PaymentError as error:
            messagebox.showwarning("Thanh toán chưa được ghi nhận", str(error))
            return
        except sqlite3.Error:
            messagebox.showerror("Lỗi lưu dữ liệu", "Hãy làm mới hóa đơn trước khi thử lại.")
            return
        self.refresh()
        result = f"Đã thanh toán {invoice_id}."
        if method == CASH:
            result += f" Tiền thừa {money(change)}."
        messagebox.showinfo("Thanh toán thành công", result)


def main():
    parser = argparse.ArgumentParser(description="UC02 - Invoice payment")
    parser.add_argument("--demo", action="store_true", help="Open sample invoices")
    args = parser.parse_args()
    filename = "thanh_toan_demo.sqlite3" if args.demo else "thanh_toan.sqlite3"
    store = Store(Path(__file__).with_name(filename))
    if args.demo:
        store.seed_demo()
    root = tk.Tk()
    PaymentWindow(root, store)
    root.mainloop()


if __name__ == "__main__":
    main()
