# Android Group 11 — Weather Viewing App

Ứng dụng Android hỗ trợ người dùng **tra cứu, theo dõi và quản lý thông tin thời tiết** tại nhiều địa điểm khác nhau. Sử dụng **Kotlin** và kết nối trực tiếp đến **WeatherAPI** thông qua giao thức HTTPS để lấy dữ liệu thời tiết theo thành phố hoặc vị trí GPS.

---

## Thành viên

| STT |     MSSV     | Họ và tên               |
| :-: | :----------: | :---------------------- |
|  1  |  2251120373  | Trần Hoàng Phú          |
|  2  |  2251320005  | Phan Quốc Dũng          |
|  3  | 079205023687 | Lưu Gia Phúc            |
|  4  | 079205023687 | Nguyễn Huỳnh Nghĩa Nhân |
|  5  | 079305003873 | Hồ Thái Mỹ Hương        |

---

## Giới thiệu

**Weather Viewing App** là ứng dụng Android cho phép người dùng:

- Tìm kiếm địa điểm/thành phố.
- Xem thời tiết hiện tại.
- Xem dự báo thời tiết.
- Lấy thông tin thời tiết theo vị trí GPS.
- Theo dõi nhiệt độ, độ ẩm và tốc độ gió.
- Hiển thị tình trạng và biểu tượng thời tiết.
- Quản lý các địa điểm yêu thích.

---

# Kiến trúc hệ thống

## Mô hình

| Thành phần   | Công nghệ             |
| ------------ | --------------------- |
| Mô hình      | Client – External API |
| Client       | Android               |
| Ngôn ngữ     | Kotlin                |
| Protocol     | HTTPS                 |
| Port         | `443`                 |
| External API | WeatherAPI            |
| Data Format  | JSON                  |

## Tổng quan luồng hoạt động

```text
User
 │
 ▼
Android UI
 │
 ▼
WeatherRepository
 │
 │ HTTPS / JSON
 ▼
WeatherAPI
 │
 │ JSON Response
 ▼
Repository
 │
 ▼
UI Model
 │
 ▼
Android UI
```

---

---

# Cấu trúc API Message

Ứng dụng gửi HTTP request đến WeatherAPI và nhận response dưới dạng JSON.

## API chính

```text
GET /v1/search.json
GET /v1/current.json
GET /v1/forecast.json
```

## Ví dụ request

```http
GET https://api.weatherapi.com/v1/current.json?key=API_KEY&q=Hanoi
```

## Ví dụ response

```json
{
  "location": {
    "name": "Hanoi",
    "region": "",
    "country": "Vietnam",
    "lat": 21.03,
    "lon": 105.85
  },
  "current": {
    "temp_c": 30.5,
    "feelslike_c": 35.2,
    "humidity": 70,
    "wind_kph": 10.8,
    "condition": {
      "text": "Partly cloudy",
      "icon": "//cdn.weatherapi.com/..."
    }
  }
}
```

Dữ liệu JSON được ứng dụng xử lý và chuyển đổi thành các model:

```text
CityOption
CurrentWeatherUi
ForecastDayUi
WeatherBundle
```

---

# Yêu cầu môi trường

## Phần cứng

- Máy tính có thể chạy Android Studio.
- Android Emulator hoặc thiết bị Android thật.
- Kết nối Internet.

## Phần mềm

- **Android Studio:** Phiên bản tương thích với project.
- **Kotlin:** Ngôn ngữ lập trình chính.
- **Android SDK:** Phiên bản được cấu hình trong project.
- **Gradle:** Sử dụng Gradle Wrapper của project.
- **WeatherAPI:** API key hợp lệ.

## Công cụ & Dependency

- Kotlin
- Android SDK
- Android Studio
- Gradle
- WeatherAPI
- JSON
- Android HTTP Networking

---

# Cài đặt

## 1. Clone repository

```bash
git clone <REPOSITORY_URL>
cd Android_Group_11---Weather-viewing-app
```

## 2. Mở project

Mở thư mục project bằng **Android Studio**.

Sau đó chờ Android Studio thực hiện:

```text
Gradle Sync
```

và tải các dependency cần thiết.

## 3. Cấu hình Android SDK

Đảm bảo Android Studio đã cài đặt Android SDK phù hợp với phiên bản được khai báo trong project.

## 4. Cấu hình WeatherAPI

Ứng dụng yêu cầu **API key của WeatherAPI** để lấy dữ liệu thời tiết.

**Đăng ký tài khoản và lấy API key tại:**
[WeatherAPI — Get API Key](https://www.weatherapi.com/)

Sau khi đăng ký và đăng nhập, API key sẽ được cung cấp trong tài khoản WeatherAPI.

---

# Hướng dẫn chạy

## Kiến trúc kết nối:

```text
┌──────────────┐
│ Android App  │
└──────┬───────┘
       │
       │ HTTPS
       ▼
┌──────────────┐
│  WeatherAPI  │
└──────────────┘
```

## Android Client

1. Mở project bằng Android Studio.
2. Chờ Gradle Sync hoàn tất.
3. Khởi động Android Emulator hoặc kết nối thiết bị Android thật.
4. Chọn module `app`.
5. Nhấn **Run**.
6. Đảm bảo thiết bị có kết nối Internet.
7. Sử dụng các chức năng của ứng dụng.

---

# Kiểm thử

## Functional Test

Kiểm tra các chức năng chính:

- Tìm kiếm thành phố hợp lệ.
- Tìm kiếm thành phố không tồn tại.
- Xem thông tin thời tiết hiện tại.
- Xem dự báo thời tiết.
- Lấy thời tiết theo vị trí GPS.
- Hiển thị nhiệt độ.
- Hiển thị độ ẩm.
- Hiển thị tốc độ gió.
- Hiển thị tình trạng thời tiết.
- Hiển thị biểu tượng thời tiết.
- Thêm địa điểm yêu thích.
- Xóa địa điểm yêu thích.

---

## Test dữ liệu không hợp lệ

Kiểm tra các trường hợp:

- Người dùng nhập tên thành phố không tồn tại.
- Query rỗng.
- Tọa độ GPS không hợp lệ.
- API key không hợp lệ.
- API trả về dữ liệu lỗi.
- Response không chứa đầy đủ trường dữ liệu cần thiết.

---

## Test mất kết nối

Kiểm tra ứng dụng khi:

- Thiết bị không có Internet.
- Kết nối Internet bị gián đoạn.
- WeatherAPI không phản hồi.
- Request bị timeout.
- WeatherAPI trả về HTTP error.

Ứng dụng phải xử lý exception phù hợp và **không bị crash**.

---

## Stress Test

Thực hiện nhiều request tìm kiếm và truy vấn thời tiết liên tiếp để kiểm tra khả năng hoạt động ổn định.

Các trường hợp:

- Nhiều lần tìm kiếm thành phố liên tiếp.
- Liên tục chuyển đổi giữa các địa điểm.
- Liên tục cập nhật dữ liệu thời tiết.
- Nhiều request API trong thời gian ngắn.

---

## Performance Test

Đánh giá:

- Thời gian gửi request.
- Thời gian nhận response.
- Thời gian xử lý JSON.
- Thời gian tải icon thời tiết.
- Thời gian hiển thị dữ liệu lên giao diện.
- Mức độ ổn định khi sử dụng trong thời gian dài.

**Bằng chứng kiểm thử:**

```text
Extra/
```

---

# Demo

| Tài liệu   | Vị trí                |
| ---------- | --------------------- |
| Video Demo | Public / Unlisted URL |
| Slide      | `PPTX/`               |
| Báo cáo    | `DOCX/`               |

---

# Giới hạn

- Ứng dụng phụ thuộc trực tiếp vào WeatherAPI.
- Cần kết nối Internet để lấy dữ liệu thời tiết.
- Nếu WeatherAPI không hoạt động hoặc API key hết hạn, dữ liệu thời tiết không thể được cập nhật.
- API key cần được bảo vệ và không được commit trực tiếp vào repository.
- Ứng dụng không có Backend Server riêng.
- Android Client gọi trực tiếp đến WeatherAPI.
- Chưa có hệ thống tài khoản và xác thực người dùng riêng.
- Chưa hỗ trợ đồng bộ dữ liệu giữa nhiều thiết bị.
- Dữ liệu thời tiết phụ thuộc vào dữ liệu do WeatherAPI cung cấp.
- Chức năng GPS phụ thuộc vào quyền truy cập vị trí và khả năng định vị của thiết bị.

---

# 📁 Cấu trúc Repository

```text
Android_Group_11---Weather-viewing-app/
│
|── Code/                   # Source code
│      │
│      ├── app/             # Android application
│      │
│      ├── gradle/          # Gradle configuration
│
├── PPTX/                   # Presentation
│
├── DOCX/                   # Project report
│
├── Extra/                  # Testing evidence
│
└── README.md
```

---

# Link

# (\*)Tiến độ: https://1drv.ms/w/c/8c38ac15479b7ff7/IQDGkBdYzLZCRqaGSOdfoS4mAQqz1L_7mScFd3Kbgf85KZM?e=6qEU9p

# lên ý tưởng và yêu cầu: https://1drv.ms/w/c/8c38ac15479b7ff7/IQC7rfyI_gBJSYRZK2WQ6MyEASJBL4FmGDFIENOOopwTfiQ?e=8zEwtC

# diagram(vô bằng mail trường): https://drive.google.com/file/d/1itXOMq5N0mqwMc-Hz5bm89eg6M4r4fmm/view?usp=sharing

# Doc (báo cáo): https://1drv.ms/w/c/8c38ac15479b7ff7/IQBr-7tREnRxSJJ2C7P1NibLAdT0qOly9CVy8p_k-6WWIH0?e=9Hdpbl
