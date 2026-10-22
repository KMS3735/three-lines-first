# 세 줄 먼저 – GitHub 진행 방법

기초모바일프로그래밍 수업의 텀 프로젝트 일정(Week01 강의자료 "Term project milestones")에 맞춰 이 저장소를 어떻게 관리할지 정리한 문서입니다.

## 1. 수업 텀 프로젝트 일정

| 시기 | 수업 일정 | 해야 할 일 |
| --- | --- | --- |
| ~ 10월 11일 | 프로젝트 계획서 제출 | 계획서 제출 |
| 계획서 제출 후 | 1:1 미팅 | 계획서 확인 |
| 13주차 | Progress meeting #1 | 진행 상황 점검 |
| 14주차 | Progress meeting #2 | 진행 상황 점검 |
| 15주차 ~ 12월 26일 | Final Report | 최종 결과 제출 |

수업 조건:

- 소스 코드는 Java로 작성 (Kotlin도 가능)
- Claude, ChatGPT, Gemini 같은 AI를 적극 활용하고, AI 에이전트를 먼저 쓰는 방식으로 계획
- 실제로 쓸 수 있는 앱

## 2. 저장소 구성

```
three-lines-first/
├── README.md                    # 프로젝트 소개 (GitHub 첫 화면)
├── app/                         # Android Studio 앱 모듈 (개발 시작 후 생성)
├── docs/
│   ├── project-plan.pdf         # 제출한 프로젝트 계획서
│   ├── android-studio-plan.md   # Android Studio 개발 계획서
│   ├── github-workflow.md       # 이 문서
│   ├── ai-usage.md              # AI 도구 활용 기록
│   ├── progress/                # 진행 미팅 자료
│   │   ├── meeting-1.md
│   │   └── meeting-2.md
│   ├── final-report.pdf         # 최종 보고서
│   └── images/                  # README와 문서에 쓰는 그림
└── .gitignore
```

- Android Studio 프로젝트는 이 저장소 폴더 안에 바로 만듭니다(**Save location**을 `three-lines-first/`로 지정). 그러면 `app/`, `gradle/`, `build.gradle.kts`가 저장소 바로 아래 생깁니다.
- 제출 문서(계획서, 진행 자료, 최종 보고서)는 모두 `docs/` 안에 둡니다. 교수님께 저장소 링크 하나만 드려도 전부 볼 수 있습니다.

## 3. GitHub 마일스톤

수업 일정을 GitHub **Issues → Milestones**에 그대로 등록합니다. 마일스톤마다 진행률이 자동으로 표시되어 진행 미팅 때 그대로 보여 줄 수 있습니다.

| 마일스톤 | 마감일 | 포함할 작업 (Android Studio 개발 계획서 9절 단계) |
| --- | --- | --- |
| 계획서 제출 | 10월 11일 | 계획서, README, Android Studio 개발 계획서 |
| Progress meeting #1 | 13주차 미팅 전날 | 0단계 준비, 1단계 촬영·글자 인식, 2단계 개인정보 가림, 3단계 세 줄 요약 화면 |
| Progress meeting #2 | 14주차 미팅 전날 | 4단계 날짜·금액 대조, 5단계 목록 저장, 6단계 행동 연결과 알림 |
| Final Report | 12월 26일 | 7단계 내 모드·재요약·가족 공유, 8단계 정확도·접근성 점검, 보고서, APK |

## 4. 이슈로 할 일 관리

개발 계획서 9절의 단계를 기능 단위 이슈로 나눠 등록하고, 끝나면 닫습니다.

**이슈 예시**

| 제목 | 라벨 | 마일스톤 |
| --- | --- | --- |
| 촬영 안내 화면과 CameraX 촬영 | `필수` | Progress meeting #1 |
| ML Kit 한국어 글자 인식 | `필수` | Progress meeting #1 |
| PrivacyMasker 정규식과 단위 테스트 | `필수` | Progress meeting #1 |
| Gemini 호출과 세 줄 카드 화면 | `필수` | Progress meeting #1 |
| Verifier 날짜·금액 대조와 경고 배지 | `필수` | Progress meeting #2 |
| 달력 등록·전화·공유 버튼 | `추가` | Progress meeting #2 |
| 쉬운 말·큰 글씨 모드 | `추가` | Final Report |
| 가상 독자 검수 | `향후` | (마일스톤 없음) |

- 라벨은 기획서의 범위 구분인 `필수`, `추가`, `향후` 세 개와 버그용 `bug`만 만듭니다.
- 미팅에서 받은 피드백도 이슈로 등록해 두면, 다음 미팅에서 무엇을 반영했는지 바로 보여 줄 수 있습니다.

## 5. 커밋 규칙

혼자 하는 프로젝트이므로 브랜치를 나누지 않고 `main`에 바로 커밋합니다.

- **기능 하나가 동작할 때마다 커밋**합니다. 한 번에 몰아서 올리지 않습니다. 커밋 기록이 곧 개발 과정의 증거가 됩니다.
- 커밋 메시지는 한국어로, 무엇을 했는지 한 줄로 씁니다. 이슈와 연결하려면 끝에 이슈 번호를 붙입니다.
  ```
  촬영 안내 화면과 가이드 틀 추가 (#1)
  개인정보 가림 정규식에 계좌번호 형식 추가 (#3)
  ```
- 메시지에 `closes #3`을 쓰면 푸시할 때 해당 이슈가 자동으로 닫힙니다.
- Android Studio에서는 **Git → Commit**(Ctrl+K)으로 커밋하고, **Git → Push**(Ctrl+Shift+K)로 GitHub에 올립니다.

## 6. 올리면 안 되는 파일

| 파일 | 이유 | 처리 |
| --- | --- | --- |
| `local.properties` | Gemini API 키가 들어 있음 | `.gitignore`에 포함됨 |
| `app/google-services.json` | Firebase 프로젝트 설정 | `.gitignore`에 포함됨 |
| `*.jks`, `*.keystore` | APK 서명 키 | `.gitignore`에 포함됨 |
| `build/`, `.gradle/`, `.idea/` | 빌드 결과와 개인 IDE 설정 | `.gitignore`에 포함됨 |
| 실제 안내문 사진 | 이름, 주소 같은 개인정보 | 저장소에 올리지 않음. 샘플은 개인정보를 가린 뒤 `docs/images/`에만 올림 |

API 키를 실수로 커밋했다면 파일만 지워서는 기록에 남아 있으므로, **즉시 Google AI Studio에서 키를 폐기하고 새로 발급**합니다.

## 7. 진행 미팅 준비

미팅 전날 `docs/progress/meeting-1.md`(또는 `meeting-2.md`)를 작성하고 **Release**를 하나 만듭니다.

**진행 자료 형식**

```markdown
# Progress meeting #1 (13주차)

## 완료한 기능
- 촬영 안내 화면, ML Kit 글자 인식 (#1, #2)

## 동작 화면
![촬영 화면](../images/progress1-capture.png)

## 계획과 달라진 점
- 

## 다음 미팅까지 할 일
- 

## 질문할 것
- 
```

**Release 만들기**

1. GitHub 저장소 → **Releases → Create a new release**
2. 태그: 미팅 #1은 `v0.1`, 미팅 #2는 `v0.2`, 최종 제출은 `v1.0`
3. 설명에 진행 자료 링크를 붙이고, Android Studio에서 만든 APK를 첨부
4. 미팅 때는 Release 페이지를 보여 주거나, APK를 휴대폰에 설치해 시연

Release를 만들어 두면 "미팅 당시 앱 상태"가 태그로 고정되어, 미팅 사이에 무엇이 바뀌었는지 GitHub의 비교 화면(`v0.1...v0.2`)으로 바로 볼 수 있습니다.

## 8. AI 도구 활용 기록

수업에서 AI 도구를 적극 쓰라고 했으므로, 어떻게 썼는지 `docs/ai-usage.md`에 기록해 최종 보고서에 활용합니다.

| 날짜 | 도구 | 한 일 | 내가 확인하거나 고친 것 |
| --- | --- | --- | --- |
| 10월 | Claude | README와 Android Studio 개발 계획서 초안 작성 | 기획서와 내용 대조, 기능 범위 수정 |
| | | | |

- 앱 안에서 쓰는 AI(Gemini 요약)와 개발에 쓰는 AI(코드 작성 도우미)를 구분해서 적습니다.
- AI가 만든 코드는 직접 실행해 확인하고, 고친 부분을 함께 적어 두면 보고서에서 "AI를 어떻게 검증했는지" 보여 줄 수 있습니다.

## 9. 최종 제출 점검 (12월 26일까지)

- [ ] `main`에 최종 소스 코드가 올라가 있고, 새로 클론해서 Android Studio로 열면 빌드됨
- [ ] README의 기능 표와 실제 구현이 일치 (구현 못 한 기능은 "향후 계획"으로 옮김)
- [ ] README에 실제 앱 화면 캡처와 시연 영상 링크 추가
- [ ] `docs/final-report.pdf` 업로드
- [ ] `docs/ai-usage.md` 정리
- [ ] Release `v1.0` 생성, 서명된 APK 첨부
- [ ] API 키, 서명 키, 개인정보가 담긴 사진이 저장소에 없는지 확인
- [ ] 제출 양식에 저장소 링크 `https://github.com/KMS3735/three-lines-first` 기재
