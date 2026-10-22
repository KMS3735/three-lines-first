# 세 줄 먼저 (Three Lines First)

**받은 공공 안내문을 찍으면, 해야 할 일이 맨 위에 보이는 안드로이드 앱**

세금, 과태료, 건강검진, 복지 안내문을 촬영하면 AI가 **무슨 안내인지 · 해야 할 일 · 언제까지·얼마·어디서** 세 줄과 **"안 하면"** 경고 한 줄, 할 일 체크박스로 정리해 줍니다. 요약 속 날짜와 금액은 원문과 코드로 한 번 더 대조하고, 읽은 뒤에는 달력 등록 · 전화 · 가족 공유까지 버튼 하나로 이어집니다.

> 현재 개발 중인 프로젝트입니다. 아래 기능과 구조는 구현 목표를 기준으로 작성했습니다.
> 프로젝트 계획서: [docs/project-plan.pdf](docs/project-plan.pdf)
> Android Studio 개발 계획서: [docs/android-studio-plan.md](docs/android-studio-plan.md)
> 프로젝트 관리 계획: [docs/github-workflow.md](docs/github-workflow.md)

![핵심 화면 목업](docs/images/screen-mockup.png)

## 기획 배경

어르신은 건강검진, 기초연금, 재산세, 장기요양처럼 생활과 바로 이어지는 공공 안내문을 자주 받습니다. 하지만 안내문은 작은 글씨와 행정 용어로 쓰여 있어, 정작 "무엇을, 언제까지, 어떻게" 해야 하는지 찾기 어렵습니다.

| 문제 | 내용 |
| --- | --- |
| **읽기 어려운 안내문** | 작은 글씨, 긴 문장, 법 조항과 행정 용어 사이에 해야 할 일과 마감일이 묻혀 있음 |
| **물어볼 사람이 없음** | 혼자 사는 어르신은 안내문을 대신 읽어 줄 가족이 곁에 없는 경우가 많음 |
| **디지털 도구의 장벽** | 사진을 찍어 AI에 묻는 방법은 질문을 직접 입력하고 긴 답변을 읽어야 해서 오히려 더 어려움 |
| **놓치면 생기는 손해** | 마감일을 놓치면 가산금이 붙거나, 무료 검진·지원금 같은 받을 수 있는 혜택을 잃음 |

세 줄 먼저는 질문을 입력하지 않아도 사진 한 장으로 해야 할 일이 맨 위에 보이도록, 그리고 큰 글씨와 음성으로 읽을 수 있도록 어르신의 눈높이에 맞춰 설계했습니다.

**대상 사용자:** 공공 안내문을 읽기 어려운 어르신과, 멀리서 부모님의 안내문을 함께 챙기는 가족

## 이미 있는 도구와 무엇이 다른가

사용 흐름은 **찍는다 → 읽는다 → 한다** 세 동작으로 끝납니다.

| | 범용 도구 (사진 검색, AI 챗봇) | 세 줄 먼저 |
| --- | --- | --- |
| **사용 방법** | 촬영하고 질문을 직접 입력 | **큰 촬영 버튼 하나** |
| **결과 형식** | 매번 길이와 구성이 다른 긴 답변 | **항상 같은 세 줄 + 경고 + 할 일** |
| **정확성 확인** | 사용자가 스스로 판단 | **날짜·금액 자동 대조, 원문 함께 표시** |
| **개인정보** | 사진 전체가 외부로 전송 | **기기 안에서 가린 텍스트만 전송** |
| **읽은 다음** | 사용자가 기억하고 처리 | **달력 · 전화 · 가족 공유 · 알림** |
| **화면** | 일반 글씨 크기 | **큰 글씨 · 고대비 · 음성 읽어주기** |

## 세 줄 형식

요약은 자유 서술이 아니라 항상 같은 순서와 역할을 갖습니다. 사용자는 매번 같은 자리에서 같은 정보를 찾고, 코드는 각 줄을 따로 검증할 수 있습니다.

| 줄 | 역할 | 예시 · 아파트 관리비 고지서 |
| :---: | --- | --- |
| 1 | **무슨 안내인지** | 아파트 관리비 고지서예요. |
| 2 | **해야 할 일** | 이번 달 관리비를 내셔야 해요. |
| 3 | **언제까지 · 얼마 · 어디서** | 10월 31일까지 152,000원을 은행이나 인터넷뱅킹으로 내요. |
| 경고 | **안 하면** | 기한이 지나면 연체료가 붙을 수 있어요. |
| 할 일 | **체크박스** | □ 관리비 납부하기 |

## 주요 기능

촬영 한 번으로 **찍는다 → 읽는다 → 한다**가 이어집니다.

| 단계 | 기능 | 하는 일 |
| --- | --- | --- |
| **시작** | 개인정보 동의 | 외부 AI로 보내는 정보를 그림으로 안내하고 동의를 받음 |
| **촬영** | 촬영 안내<br>글자 인식<br>개인정보 가림 | 가이드 틀로 찍고, 기기 안에서 글자를 읽은 뒤<br>주민번호·계좌·휴대전화·주소·이름을 가림 |
| **요약** | 세 줄 카드<br>할 일 체크박스<br>원문 함께 보기<br>이해가 안 돼요 | 큰 글씨 카드 아래 원문 사진을 항상 표시<br>"이해가 안 돼요"를 누르면 더 쉬운 말로 다시 요약 |
| **행동** | 읽어주기<br>달력 등록<br>전화 걸기<br>가족에게 보내기 | 버튼 하나로 음성 안내, 마감일 등록,<br>기관 전화, 메신저 공유 |
| **알림** | 마감일 알림 | 마감일 3일 전과 당일에 알림 |
| **설정** | 내 모드<br>원문 사진 보관 | 쉬운 말·큰 글씨를 한 번 저장하면 모든 문서에 적용<br>요약 후 원문 사진을 남길지 선택 |

### 접근성 기준

| 항목 | 기준 |
| --- | --- |
| **글씨 크기** | 본문 20sp 이상, 큰 글씨 모드 28sp 이상 |
| **버튼** | 터치 영역 48dp 이상, 아이콘과 글자를 함께 표시 |
| **색 대비** | 배경과 글자 대비를 높게, 경고 줄은 색과 아이콘을 함께 사용 |
| **화면낭독기** | 모든 이미지·아이콘 버튼에 `contentDescription`을 달아 TalkBack 지원 |

## 동작 방식

![AI 요약 처리 흐름](docs/images/summary-pipeline.png)

1. **글자 인식과 개인정보 가림은 기기 안에서 처리합니다.** 원문 사진은 외부로 나가지 않고, 가려진 텍스트만 AI에 보냅니다.
2. **AI 응답의 형식을 검사합니다.** 정해진 JSON 형식이 아니거나 필수 줄이 비어 있으면 화면에 띄우지 않고 한 번 다시 요청합니다.
3. **날짜와 금액은 규칙 코드로 대조합니다.** 원문과 요약에서 정규식으로 날짜·금액을 뽑아 같은 형식으로 바꾼 뒤(`2026. 10. 31.`, `10월 31일` → `2026-10-31` / `152,000원`, `15만 2천원` → `152000`), 요약의 값이 모두 원문에 있는지 검사합니다. 없으면 한 번 다시 요약하고, 그래도 다르면 경고 배지를 붙입니다.
4. **확신이 없을 때는 사용자에게 묻습니다.** 날짜가 여러 개면 "까지", "마감", "기한" 주변 날짜를 우선 고르고, 그래도 불확실하면 후보를 보여 주고 고르게 합니다.
5. **최종 판단은 원문 기준입니다.** 요약 화면에는 항상 원문이 함께 표시되고, 전화 버튼으로 기관에 바로 확인할 수 있습니다.

### AI 출력 형식

```json
{
  "line1_what": "아파트 관리비 고지서예요.",
  "line2_todo": "이번 달 관리비를 내셔야 해요.",
  "line3_when_where": "10월 31일까지 152,000원을 은행이나 인터넷뱅킹으로 내요.",
  "warning": "기한이 지나면 연체료가 붙을 수 있어요.",
  "todos": ["관리비 납부하기"],
  "deadline": "2026-10-31",
  "amounts": [152000]
}
```

- 마감일을 `deadline`(`YYYY-MM-DD`)으로 따로 받아 자동 대조, 달력 등록, 마감일 알림에 그대로 씁니다.
- 원문에 없는 내용은 쓰지 않고, 찾을 수 없는 항목은 `null`로 둡니다.

## 기술 스택

<p>
  <img src="https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Android%20Studio-3DDC84?style=flat-square&logo=androidstudio&logoColor=white" alt="Android Studio">
  <img src="https://img.shields.io/badge/ML%20Kit-4285F4?style=flat-square&logo=google&logoColor=white" alt="ML Kit">
  <img src="https://img.shields.io/badge/Gemini%20API-8E75B2?style=flat-square&logo=googlegemini&logoColor=white" alt="Gemini API">
  <img src="https://img.shields.io/badge/Room-4CAF50?style=flat-square&logo=sqlite&logoColor=white" alt="Room">
  <img src="https://img.shields.io/badge/Retrofit-48B983?style=flat-square&logo=square&logoColor=white" alt="Retrofit">
  <img src="https://img.shields.io/badge/Firebase-DD2C00?style=flat-square&logo=firebase&logoColor=white" alt="Firebase">
</p>

| 분야 | 기술 | 쓰는 곳 |
| --- | --- | --- |
| **화면** | XML 레이아웃 · RecyclerView · CardView | 세 줄 카드, 안내문 목록 |
| **촬영 · 글자 인식** | CameraX · ML Kit (한국어) | 가이드 틀 촬영, 기기 안 글자 인식 |
| **AI · 통신** | Gemini API · Retrofit | 세 줄 요약, 쉬운 말 재요약 |
| **검증** | 정규식 | 날짜·금액 대조, 개인정보 가림 |
| **저장** | Room · SharedPreferences · 내부 저장소 | 안내문·할 일, 내 모드 설정, 원문 사진 |
| **백그라운드** | Thread · Handler | DB 작업, 화면 갱신 |
| **알림 · 행동** | AlarmManager · TextToSpeech · 암시적 인텐트 | 마감일 알림, 읽어주기, 달력·전화·공유 |
| **가족 공유** | Firebase Authentication · Realtime Database | 자녀 계정에 요약 공유 |

## 프로젝트 구조 (예정)

```
app/src/main/java/.../threelinesfirst/
├── ui/
│   ├── MainActivity.java        # 저장한 안내문 목록
│   ├── ConsentActivity.java     # 첫 실행 개인정보 동의
│   ├── CaptureActivity.java     # 촬영 안내, 촬영
│   ├── ResultActivity.java      # 세 줄 요약, 할 일, 원문, 행동 버튼
│   └── SettingsActivity.java    # 내 모드, 사진 보관, 알림 설정
├── ocr/OcrHelper.java           # ML Kit 글자 인식
├── privacy/PrivacyMasker.java   # 개인정보 가림
├── ai/SummaryClient.java        # Gemini API 호출(Retrofit), JSON 변환
├── verify/Verifier.java         # 날짜·금액 대조, 마감일·전화번호 추출
├── data/                        # Room: AppDatabase, Notice·Todo 엔티티, NoticeDao
└── util/
    ├── ActionIntents.java       # 달력 등록, 전화, 공유 인텐트
    ├── DeadlineAlarm.java       # 마감일 알림
    └── TtsHelper.java           # 읽어주기
```

### 데이터 저장

| 테이블 | 담는 내용 | 주요 열 |
| --- | --- | --- |
| **`notice`** | 안내문 정보 | `title` · `maskedText` · `imagePath` · `createdAt` |
| | 세 줄 요약 | `line1What` · `line2Todo` · `line3WhenWhere` · `warning` |
| | 대조 결과 | `deadline` · `amount` · `phone` · `verified` |
| **`todo`** | 할 일 체크박스 | `noticeId` · `content` · `done` |

Room으로 저장하고, DB 작업은 백그라운드 스레드에서 처리합니다. 원문 사진 보관을 끄면 `imagePath` 없이 요약 텍스트만 남습니다.

## 시작하기

모든 개발과 빌드는 Android Studio에서 진행합니다. 프로젝트 설정, 의존성, 권한, 개발 순서는 [Android Studio 개발 계획서](docs/android-studio-plan.md)에 정리했습니다.

1. 저장소를 클론하고 Android Studio로 엽니다.
   ```bash
   git clone https://github.com/KMS3735/three-lines-first.git
   ```
2. [Google AI Studio](https://aistudio.google.com/)에서 Gemini API 키를 발급받아 `local.properties`에 추가합니다. 이 파일은 Git에 올라가지 않습니다.
   ```properties
   GEMINI_API_KEY=발급받은_키
   ```
3. 가족 자동 공유를 쓰려면 [Firebase 콘솔](https://console.firebase.google.com/)에서 Android 앱을 등록하고 `google-services.json`을 `app/` 폴더에 넣습니다.
4. 카메라 기능을 쓰려면 에뮬레이터 대신 실제 안드로이드 기기에서 실행하는 것을 권장합니다.

## 개인정보 보호

- 첫 실행 때 어떤 정보가 외부 AI로 전송되는지 안내하고 동의를 받습니다.
- 개인정보는 AI로 보내기 전에 기기 안에서 먼저 가립니다.
- 원문 사진은 외부로 전송하지 않고, 기기에 보관할지도 사용자가 고릅니다.
- 기기 안 글자 인식으로 끝내 읽히지 않는 안내문은, 사용자가 다시 동의한 경우에만 사진을 AI로 보냅니다.
- 요약은 이해를 돕기 위한 참고용이며, 효력은 항상 원문을 따릅니다.
