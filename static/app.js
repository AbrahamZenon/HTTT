/**
 * app.js - Logic Giao diện Thanh toán Hóa đơn Nhà thuốc Long Châu (TV2)
 * Xử lý tương tác, gọi REST API và cập nhật trạng thái thời gian thực.
 */

// Trạng thái ứng dụng hiện tại
let currentInvoices = [];
let currentFilterStatus = "CHO_THANH_TOAN";
let selectedInvoice = null;
let currentPayMethod = "TIEN_MAT"; // 'TIEN_MAT', 'CHUYEN_KHOAN', 'THE'

// Khởi chạy khi tải trang
document.addEventListener("DOMContentLoaded", () => {
  initEventListeners();
  loadStats();
  loadInvoices();
});

function initEventListeners() {
  // Lọc theo trạng thái
  document.querySelectorAll(".filter-tab").forEach(tab => {
    tab.addEventListener("click", (e) => {
      document.querySelectorAll(".filter-tab").forEach(t => t.classList.remove("active"));
      e.target.classList.add("active");
      currentFilterStatus = e.target.getAttribute("data-status");
      loadInvoices();
    });
  });

  // Tìm kiếm hóa đơn
  const searchInput = document.getElementById("invoiceSearchInput");
  searchInput.addEventListener("input", (e) => {
    filterInvoiceCards(e.target.value.trim().toLowerCase());
  });

  // Chuyển đổi tab phương thức thanh toán
  document.querySelectorAll(".pay-tab").forEach(tab => {
    tab.addEventListener("click", (e) => {
      const targetBtn = e.target.closest(".pay-tab");
      document.querySelectorAll(".pay-tab").forEach(t => t.classList.remove("active"));
      targetBtn.classList.add("active");
      currentPayMethod = targetBtn.getAttribute("data-method");

      // Ẩn/Hiện nội dung tương ứng
      document.getElementById("paneTienMat").style.display = (currentPayMethod === "TIEN_MAT") ? "block" : "none";
      document.getElementById("paneChuyenKhoan").style.display = (currentPayMethod === "CHUYEN_KHOAN") ? "block" : "none";
      document.getElementById("paneThe").style.display = (currentPayMethod === "THE") ? "block" : "none";
    });
  });

  // Nhập tiền mặt tính tiền thừa tự động
  const inputTienKhach = document.getElementById("inputTienKhachDua");
  inputTienKhach.addEventListener("input", calculateChange);

  // Nút mệnh giá nhanh
  document.querySelectorAll(".btn-chip").forEach(chip => {
    chip.addEventListener("click", (e) => {
      if (!selectedInvoice) return;
      const val = e.target.getAttribute("data-val");
      if (val === "exact") {
        inputTienKhach.value = selectedInvoice.TongTien;
      } else {
        inputTienKhach.value = parseFloat(val);
      }
      calculateChange();
    });
  });

  // Xác nhận thanh toán
  document.getElementById("btnXacNhanThanhToan").addEventListener("click", handlePaymentSubmit);

  // Hủy hóa đơn
  document.getElementById("btnHuyHoaDon").addEventListener("click", openCancelModal);
  document.getElementById("btnCloseHuyModal").addEventListener("click", closeCancelModal);
  document.getElementById("btnCancelHuy").addEventListener("click", closeCancelModal);
  document.getElementById("btnConfirmHuy").addEventListener("click", handleCancelSubmit);

  // In hóa đơn / Biên lai
  document.getElementById("btnInHoaDon").addEventListener("click", openReceiptModal);
  document.getElementById("btnCloseReceiptModal").addEventListener("click", closeReceiptModal);
  document.getElementById("btnDongReceipt").addEventListener("click", closeReceiptModal);

  // Reset database demo
  document.getElementById("btnResetDb").addEventListener("click", handleResetDb);
}

// ==========================================
// API CALLS
// ==========================================

async function loadStats() {
  try {
    const res = await fetch("/api/stats");
    const json = await res.json();
    if (json.success) {
      const hds = json.data.trang_thai_hoa_don || {};
      const pts = json.data.phuong_thuc_thanh_toan || {};

      const pending = hds["CHO_THANH_TOAN"] ? hds["CHO_THANH_TOAN"].SoLuong : 0;
      const paid = hds["DA_THANH_TOAN"] ? hds["DA_THANH_TOAN"].SoLuong : 0;
      const cancelled = hds["DA_HUY"] ? hds["DA_HUY"].SoLuong : 0;

      let totalRevenue = 0;
      for (const k in pts) {
        totalRevenue += pts[k].DoanhThu;
      }

      document.getElementById("statPendingCount").textContent = pending;
      document.getElementById("statPaidCount").textContent = paid;
      document.getElementById("statCancelledCount").textContent = cancelled;
      document.getElementById("statTotalRevenue").textContent = formatMoney(totalRevenue);
    }
  } catch (err) {
    console.error("Lỗi tải thống kê:", err);
  }
}

async function loadInvoices() {
  const container = document.getElementById("invoiceCardsList");
  container.innerHTML = `<div class="loading-state"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải...</div>`;

  try {
    const url = currentFilterStatus ? `/api/invoices?status=${currentFilterStatus}` : "/api/invoices";
    const res = await fetch(url);
    const json = await res.json();

    if (json.success) {
      currentInvoices = json.data;
      renderInvoiceCards(currentInvoices);

      // Nếu đang chọn một hóa đơn và nó vẫn còn trong danh sách thì giữ chọn
      if (selectedInvoice) {
        const stillExists = currentInvoices.find(i => i.MaHD === selectedInvoice.MaHD);
        if (stillExists) {
          selectInvoice(stillExists.MaHD);
        } else if (currentInvoices.length > 0) {
          selectInvoice(currentInvoices[0].MaHD);
        } else {
          deselectInvoice();
        }
      } else if (currentInvoices.length > 0) {
        selectInvoice(currentInvoices[0].MaHD);
      } else {
        deselectInvoice();
      }
    }
  } catch (err) {
    container.innerHTML = `<div class="empty-state">Lỗi kết nối máy chủ!</div>`;
  }
}

function renderInvoiceCards(invoices) {
  const container = document.getElementById("invoiceCardsList");
  if (!invoices || invoices.length === 0) {
    container.innerHTML = `<div class="empty-state" style="padding: 20px; font-size: 0.9rem;">
      <i class="fa-regular fa-folder-open fa-2x" style="margin-bottom: 8px;"></i>
      <p>Không có hóa đơn nào</p>
    </div>`;
    return;
  }

  container.innerHTML = invoices.map(inv => {
    const isActive = selectedInvoice && selectedInvoice.MaHD === inv.MaHD ? "active" : "";
    const statusText = inv.TrangThai === "CHO_THANH_TOAN" ? "Chờ TT" : (inv.TrangThai === "DA_THANH_TOAN" ? "Đã TT" : "Đã hủy");

    return `
      <div class="invoice-card ${isActive}" onclick="selectInvoice('${inv.MaHD}')" data-mahd="${inv.MaHD}">
        <div class="card-top">
          <span class="code">${inv.MaHD}</span>
          <span class="badge badge-${inv.TrangThai}">${statusText}</span>
        </div>
        <div class="card-mid">
          <span><i class="fa-regular fa-clock"></i> ${formatTime(inv.NgayLap)}</span>
          <span>${inv.TenNV || inv.MaNV}</span>
        </div>
        <div class="card-bottom">
          <span class="card-amount">${formatMoney(inv.TongTien)}</span>
          <span class="card-note" title="${inv.GhiChu || ''}">${inv.GhiChu || ''}</span>
        </div>
      </div>
    `;
  }).join("");
}

function filterInvoiceCards(query) {
  if (!query) {
    renderInvoiceCards(currentInvoices);
    return;
  }
  const filtered = currentInvoices.filter(i => 
    i.MaHD.toLowerCase().includes(query) || 
    (i.GhiChu && i.GhiChu.toLowerCase().includes(query)) ||
    (i.TenNV && i.TenNV.toLowerCase().includes(query))
  );
  renderInvoiceCards(filtered);
}

async function selectInvoice(maHd) {
  try {
    const res = await fetch(`/api/invoices/${maHd}`);
    const json = await res.json();
    if (json.success) {
      selectedInvoice = json.data;
      renderInvoiceDetail(selectedInvoice);

      // Cập nhật class active ở list bên trái
      document.querySelectorAll(".invoice-card").forEach(c => {
        if (c.getAttribute("data-mahd") === maHd) {
          c.classList.add("active");
        } else {
          c.classList.remove("active");
        }
      });
    }
  } catch (err) {
    showToast("Không thể tải chi tiết hóa đơn!", "error");
  }
}

function deselectInvoice() {
  selectedInvoice = null;
  document.getElementById("emptyStateView").style.display = "flex";
  document.getElementById("detailContentView").style.display = "none";
}

function renderInvoiceDetail(inv) {
  document.getElementById("emptyStateView").style.display = "none";
  document.getElementById("detailContentView").style.display = "flex";

  // Thông tin tiêu đề
  document.getElementById("viewMaHD").textContent = inv.MaHD;
  const badge = document.getElementById("viewTrangThaiBadge");
  badge.className = `badge badge-${inv.TrangThai}`;
  badge.textContent = inv.TrangThai === "CHO_THANH_TOAN" ? "Chờ Thanh Toán" : (inv.TrangThai === "DA_THANH_TOAN" ? "Đã Thanh Toán (Hoàn Tất)" : "Đã Hủy");

  document.getElementById("viewNgayLap").textContent = inv.NgayLap;
  document.getElementById("viewNhanVien").textContent = `${inv.TenNV} (${inv.MaNV})`;

  // Bảng chi tiết thuốc
  const tbody = document.getElementById("itemsTableBody");
  tbody.innerHTML = inv.ChiTiet.map((item, idx) => `
    <tr>
      <td>${idx + 1}</td>
      <td>
        <strong>${item.TenThuoc}</strong>
        <div style="font-size: 0.78rem; color: #6b778c;">ĐVT: ${item.DonViTinh}</div>
      </td>
      <td><span class="badge-batch">${item.MaLo}</span></td>
      <td><span style="font-size: 0.85rem;">${item.HanSuDung}</span></td>
      <td class="text-right">${formatMoney(item.DonGia)}</td>
      <td class="text-center"><strong>${item.SoLuong}</strong></td>
      <td class="text-right" style="font-weight: 700; color: var(--accent-red);">${formatMoney(item.ThanhTien)}</td>
    </tr>
  `).join("");

  // Tổng tiền
  document.getElementById("viewTongTienHang").textContent = formatMoney(inv.TongTien);
  document.getElementById("viewTongThanhToan").textContent = formatMoney(inv.TongTien);

  // Hiển thị khung tùy theo trạng thái
  const payActionBox = document.getElementById("paymentActionBox");
  const paidResultBox = document.getElementById("paidResultBox");
  const cancelledResultBox = document.getElementById("cancelledResultBox");

  if (inv.TrangThai === "CHO_THANH_TOAN") {
    payActionBox.style.display = "block";
    paidResultBox.style.display = "none";
    cancelledResultBox.style.display = "none";

    // Reset form thanh toán
    document.getElementById("inputTienKhachDua").value = inv.TongTien; // Mặc định điền số tiền vừa đủ
    calculateChange();

    // Điền dữ liệu cho Tab Chuyển Khoản QR
    document.getElementById("qrSoTien").textContent = formatMoney(inv.TongTien);
    document.getElementById("qrNoiDung").textContent = `LONGCHAU ${inv.MaHD}`;

    // Điền dữ liệu cho Tab Thẻ POS
    document.getElementById("posSoTien").textContent = formatMoney(inv.TongTien);

  } else if (inv.TrangThai === "DA_THANH_TOAN") {
    payActionBox.style.display = "none";
    paidResultBox.style.display = "block";
    cancelledResultBox.style.display = "none";

    if (inv.ThanhToan) {
      document.getElementById("viewMaTT").textContent = `${inv.ThanhToan.MaTT} (${inv.ThanhToan.MaGiaoDich || ''})`;
      document.getElementById("viewPhuongThucTT").textContent = inv.ThanhToan.PhuongThuc;
      document.getElementById("viewThoiGianTT").textContent = inv.ThanhToan.NgayTao;
    }

  } else if (inv.TrangThai === "DA_HUY") {
    payActionBox.style.display = "none";
    paidResultBox.style.display = "none";
    cancelledResultBox.style.display = "block";
    document.getElementById("viewCancelledReason").textContent = inv.GhiChu || "Không có lý do chi tiết.";
  }
}

function calculateChange() {
  if (!selectedInvoice) return;
  const tongTien = selectedInvoice.TongTien;
  const inputVal = parseFloat(document.getElementById("inputTienKhachDua").value) || 0;
  const changeVal = inputVal - tongTien;
  const displayBox = document.getElementById("viewTienThua");

  if (changeVal >= 0) {
    displayBox.textContent = formatMoney(changeVal);
    displayBox.className = "change-val";
  } else {
    displayBox.textContent = `Thiếu ${formatMoney(Math.abs(changeVal))}`;
    displayBox.className = "change-val negative";
  }
}

// ==========================================
// SUBMIT PAYMENT (UC02 - Thanh Toán Hóa Đơn)
// ==========================================

async function handlePaymentSubmit() {
  if (!selectedInvoice) return;

  const tongTien = selectedInvoice.TongTien;
  let soTienKhachTra = tongTien;

  if (currentPayMethod === "TIEN_MAT") {
    soTienKhachTra = parseFloat(document.getElementById("inputTienKhachDua").value) || 0;
    if (soTienKhachTra < tongTien) {
      showToast("Số tiền khách đưa không đủ để thanh toán!", "error");
      return;
    }
  }

  const payload = {
    phuong_thuc: currentPayMethod,
    so_tien_khach_tra: soTienKhachTra,
    ma_nv: "NV01",
    ghi_chu: `Thanh toán qua ${currentPayMethod}`
  };

  try {
    const res = await fetch(`/api/invoices/${selectedInvoice.MaHD}/pay`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const json = await res.json();

    if (json.success) {
      showToast("Thanh toán thành công & đã trừ kho theo lô!", "success");
      await loadStats();
      await loadInvoices();
      await selectInvoice(selectedInvoice.MaHD);

      // Tự động mở hóa đơn in sau 400ms để thu ngân đưa cho khách
      setTimeout(() => {
        openReceiptModal();
      }, 400);
    } else {
      showToast(json.message || "Lỗi thanh toán!", "error");
    }
  } catch (err) {
    showToast("Lỗi kết nối khi gửi yêu cầu thanh toán!", "error");
  }
}

// ==========================================
// HỦY HÓA ĐƠN (QUẢN LÝ TRẠNG THÁI)
// ==========================================

function openCancelModal() {
  if (!selectedInvoice) return;
  document.getElementById("modalHuyMaHD").textContent = selectedInvoice.MaHD;
  document.getElementById("modalHuyHoaDon").classList.add("show");
}

function closeCancelModal() {
  document.getElementById("modalHuyHoaDon").classList.remove("show");
}

async function handleCancelSubmit() {
  if (!selectedInvoice) return;
  const lyDo = document.getElementById("selectLyDoHuy").value;

  try {
    const res = await fetch(`/api/invoices/${selectedInvoice.MaHD}/cancel`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ lyDo })
    });
    const json = await res.json();

    if (json.success) {
      closeCancelModal();
      showToast(json.message, "success");
      await loadStats();
      await loadInvoices();
      await selectInvoice(selectedInvoice.MaHD);
    } else {
      showToast(json.message, "error");
    }
  } catch (err) {
    showToast("Lỗi khi hủy hóa đơn!", "error");
  }
}

// ==========================================
// IN BIÊN LAI / HÓA ĐƠN BÁN LẺ
// ==========================================

function openReceiptModal() {
  if (!selectedInvoice) return;

  const inv = selectedInvoice;
  document.getElementById("recMaHD").textContent = inv.MaHD;
  document.getElementById("recBarcode").textContent = inv.MaHD;
  document.getElementById("recNgayLap").textContent = inv.NgayLap;
  document.getElementById("recThuNgan").textContent = inv.TenNV || inv.MaNV;
  document.getElementById("recTongTien").textContent = formatMoney(inv.TongTien);

  if (inv.ThanhToan) {
    document.getElementById("recMaTT").textContent = inv.ThanhToan.MaTT;
    document.getElementById("recPhuongThuc").textContent = inv.ThanhToan.PhuongThuc;
  } else {
    document.getElementById("recMaTT").textContent = "--";
    document.getElementById("recPhuongThuc").textContent = "Tiền mặt";
  }

  // Render chi tiết món
  const recItems = document.getElementById("recItemsBody");
  recItems.innerHTML = inv.ChiTiet.map(item => `
    <tr>
      <td>${item.TenThuoc}<br><small style="color:#666">Lô: ${item.MaLo} | HSD: ${item.HanSuDung}</small></td>
      <td align="center">${item.SoLuong}</td>
      <td align="right">${formatMoney(item.DonGia)}</td>
      <td align="right">${formatMoney(item.ThanhTien)}</td>
    </tr>
  `).join("");

  document.getElementById("modalInHoaDon").classList.add("show");
}

function closeReceiptModal() {
  document.getElementById("modalInHoaDon").classList.remove("show");
}

// ==========================================
// RESET DEMO DATABASE
// ==========================================

async function handleResetDb() {
  if (!confirm("Khôi phục lại dữ liệu mẫu của Nhà thuốc Long Châu? Dữ liệu kiểm thử cũ sẽ được đặt lại.")) {
    return;
  }
  try {
    const res = await fetch("/api/reset-db", { method: "POST" });
    const json = await res.json();
    if (json.success) {
      showToast(json.message, "success");
      await loadStats();
      await loadInvoices();
    }
  } catch (err) {
    showToast("Không thể reset CSDL!", "error");
  }
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

function formatMoney(amount) {
  if (typeof amount !== "number") amount = parseFloat(amount) || 0;
  return amount.toLocaleString("vi-VN") + " đ";
}

function formatTime(isoString) {
  if (!isoString) return "";
  const parts = isoString.split(" ");
  return parts.length > 1 ? parts[1] : isoString;
}

function showToast(message, type = "success") {
  const toast = document.getElementById("toastNotification");
  toast.textContent = message;
  toast.className = `toast show ${type}`;
  setTimeout(() => {
    toast.className = "toast";
  }, 3500);
}

