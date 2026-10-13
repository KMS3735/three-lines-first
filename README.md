# 세 줄 먼저 (Three Lines First)

**받은 공공 안내문을 찍으면, 해야 할 일이 맨 위에 보이는 안드로이드 앱**

세금, 과태료, 건강검진, 복지 안내문을 촬영하면 AI가 **무엇을 · 언제까지 · 어떻게 · 안 하면** 네 줄과 할 일 체크박스로 정리해 줍니다. 요약 속 날짜와 금액은 원문과 코드로 한 번 더 대조해서, AI가 틀린 마감일을 알려주는 일을 막습니다.

> 현재 개발 중인 프로젝트입니다. 아래 기능과 구조는 구현 목표를 기준으로 작성했습니다.
> 기획서 전문: [docs/project-plan.pdf](docs/project-plan.pdf)

![핵심 화면 목업](docs/images/screen-mockup.png)

## 왜 만들었나

공공 안내문은 삶에 직접 영향을 주지만, 법 조항과 행정 용어 사이에 "무엇을, 언제까지, 어떻게"가 묻혀 있습니다. 실제로 한 청년 지원사업은 공식 공고의 접수 마감(5월 29일)이 본문에 묻혀, 인터넷에 "상시 신청"이라는 잘못된 요약이 돌기도 했습니다.

이 앱은 '매일 쓰는 생활 속 AI' AI 활용 아이디어 공모전에 출품한 제도 아이디어 **"세 줄 먼저"**를, 누구나 바로 쓸 수 있는 개인용 앱으로 옮긴 것입니다.

**주요 사용자:** 어르신, 발달장애인, 외국인 주민처럼 공공 안내문을 이해하기 어려운 사람과 그 가족

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 안내문 촬영과 글자 인식 | 카메라나 갤러리 사진에서 ML Kit으로 한국어 글자를 인식 |
| 개인정보 자동 가림 | 이름, 주민등록번호, 전화번호, 계좌번호를 기기 안에서 가린 뒤에만 AI로 전송 |
| 세 줄 먼저 요약 | 무엇을 / 언제까지 / 어떻게 / 안 하면 네 줄을 큰 글씨 카드로 표시 |
| 할 일 체크박스 | "신분증 챙기기", "병원 예약" 같은 할 일을 체크 목록으로 제공 |
| 날짜·금액 자동 대조 | 요약의 날짜와 금액이 원문과 다르면 다시 요약하고, 그래도 다르면 경고 표시 |
| 원문 나란히 보기 | 요약 아래 원문 사진을 항상 표시하고 "효력은 원문을 따릅니다"를 고정 표기 |
| 마감일 알림 | 원문에서 찾은 마감일 전에 알림 |
| 읽어주기 | 요약을 음성으로 안내 |
| 내 모드 | 쉬운 말, 큰 글씨, 외국어 설정을 한 번 저장하면 모든 문서에 적용 |
| 이해가 안 돼요 | 누르면 더 쉬운 말로 다시 요약 |
| 가까운 기관 찾기 | 요약에 나온 기관(보건소, 주민센터 등)을 현재 위치 기준으로 지도에 표시 |
| 가족 함께 받기 | 본인이 동의하면 요약을 가족 계정에 공유 |

## 동작 방식

![AI 요약 처리 흐름](docs/images/summary-pipeline.png)

1. **글자 인식과 개인정보 가림은 기기 안에서 처리합니다.** 원문 사진은 외부로 나가지 않고, 가려진 텍스트만 AI에 보냅니다.
2. **AI는 정해진 JSON 형식으로만 답합니다.** Firebase AI Logic의 구조화 출력 기능으로 형식을 고정합니다.
3. **날짜와 금액은 규칙 코드로 대조합니다.** 원문과 요약에서 정규식으로 날짜·금액을 뽑아 비교하고, 다르면 한 번 다시 요약한 뒤 그래도 다르면 경고와 함께 보여 줍니다.
4. **최종 판단은 원문 기준입니다.** 요약 화면에는 항상 원문이 함께 표시됩니다.

### AI 출력 형식

```json
{
  "what": "올해 무료 건강검진 대상이에요.",
  "when": "12월 31일까지 받으세요.",
  "how": "신분증을 들고 지정 병원에 가세요.",
  "ifNot": "올해 무료 검진 기회가 사라져요.",
  "deadline": "2026-12-31",
  "amounts": [],
  "agency": "지정 병원",
  "todos": ["병원 예약", "신분증 챙기기"]
}
```

- 원문에 없는 내용은 쓰지 않고, 찾을 수 없는 항목은 `null`로 둡니다.
- `deadline`은 대조용으로 `YYYY-MM-DD` 형식을 따릅니다.
- "안 하면" 줄에는 가산금, 자격 상실 같은 불이익을 반드시 씁니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어·환경 | Java, Android Studio |
| 화면 | XML 레이아웃, RecyclerView, CardView |
| 글자 인식 | ML Kit Text Recognition (한국어) |
| AI | Firebase AI Logic, Gemini Developer API |
| 저장 | SQLite, SharedPreferences |
| 지도·위치 | Google Maps SDK for Android, Geocoder |
| 알림·음성 | AlarmManager, Notification, TextToSpeech |
| 계정·공유 | Firebase Authentication, Realtime Database |

## 프로젝트 구조 (예정)

```
app/src/main/java/.../threelinesfirst/
├── ui/
│   ├── MainActivity.java        # 저장한 안내문 목록
│   ├── CaptureActivity.java     # 촬영, 갤러리 선택
│   ├── ResultActivity.java      # 네 줄 요약, 할 일, 원문
│   ├── MapActivity.java         # 가까운 기관 지도
│   └── SettingsActivity.java    # 내 모드, 알림 설정
├── ocr/OcrHelper.java           # ML Kit 글자 인식
├── privacy/PrivacyMasker.java   # 개인정보 가림
├── ai/SummaryClient.java        # Firebase AI Logic 호출, JSON 변환
├── verify/Verifier.java         # 날짜·금액 대조
├── data/NoticeDbHelper.java     # SQLite 저장
└── util/
    ├── DeadlineAlarm.java       # 마감일 알림
    └── TtsHelper.java           # 읽어주기
```

### 데이터 저장

| 테이블 | 주요 열 | 내용 |
| --- | --- | --- |
| `notice` | id, title, masked_text, image_path, deadline, amount, verified, created_at | 안내문 한 건과 대조 결과 |
| `summary` | notice_id, what, until, how, if_not, warning | 네 줄 요약과 경고 문구 |
| `todo` | id, notice_id, content, done | 할 일 체크박스 |

가족 공유를 켜면 Firebase에는 네 줄 요약과 마감일만 올라가고, 원문과 사진은 기기에만 저장됩니다.

## 시작하기

1. 저장소를 클론하고 Android Studio로 엽니다.
   ```bash
   git clone https://github.com/KMS3735/three-lines-first.git
   ```
2. [Firebase 콘솔](https://console.firebase.google.com/)에서 프로젝트를 만들고 Android 앱을 등록한 뒤, `google-services.json`을 `app/` 폴더에 넣습니다.
3. Firebase 콘솔의 **AI Logic** 메뉴에서 **Gemini Developer API**를 활성화합니다.
4. Google Maps API 키를 `local.properties`에 추가합니다. 이 파일은 Git에 올라가지 않습니다.
   ```properties
   MAPS_API_KEY=발급받은_키
   ```
5. 카메라 기능을 쓰려면 에뮬레이터 대신 실제 안드로이드 기기에서 실행하는 것을 권장합니다.

## 개인정보 보호

- 개인정보는 AI로 보내기 전에 기기 안에서 먼저 가립니다.
- 원문 사진은 기기에만 저장하며 외부로 전송하지 않습니다.
- 가족 공유는 본인 동의가 있을 때만 동작하고 언제든 끌 수 있습니다.
- 요약은 이해를 돕기 위한 참고용이며, 효력은 항상 원문을 따릅니다.
