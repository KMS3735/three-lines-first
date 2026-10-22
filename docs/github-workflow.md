# 세 줄 먼저 – 프로젝트 관리 계획

기초모바일프로그래밍 텀 프로젝트 "세 줄 먼저"의 저장소 구성과 운영 방식을 정리한 문서입니다.

- 저장소: https://github.com/KMS3735/three-lines-first
- 개발 환경: Android Studio, Java
- 작성자: 김민상 (GitHub `KMS3735`)

## 1. 저장소 구성

```
three-lines-first/
├── README.md                    # 프로젝트 소개
├── app/                         # Android Studio 앱 모듈 (Java)
├── docs/
│   ├── project-plan.pdf         # 프로젝트 계획서
│   ├── android-studio-plan.md   # Android Studio 개발 계획서
│   ├── github-workflow.md       # 프로젝트 관리 계획 (이 문서)
│   └── images/                  # 문서용 그림
└── .gitignore
```

Android Studio 프로젝트를 저장소 최상위에 두어, 저장소를 클론해 Android Studio로 열면 바로 빌드할 수 있게 합니다. 계획 문서는 모두 `docs/`에 모읍니다.

## 2. 커밋 규칙

- 1인 프로젝트이므로 `main` 브랜치 하나로 운영합니다.
- 기능 하나가 동작하는 단위로 커밋해, 커밋 기록으로 개발 과정을 확인할 수 있게 합니다.
- 커밋 메시지는 한국어로 변경 내용을 한 줄로 씁니다. 예: `촬영 안내 화면과 가이드 틀 추가`

## 3. 저장소에 올리지 않는 파일

| 파일 | 이유 |
| --- | --- |
| **`local.properties`** | Gemini API 키 보관 |
| **`google-services.json`** | Firebase 프로젝트 설정 |
| **`*.jks` · `*.keystore`** | APK 서명 키 |
| **실제 안내문 원본 사진** | 이름, 주소 등 개인정보 포함<br>문서에는 개인정보를 가린 사진만 사용 |

위 파일은 `.gitignore`로 제외합니다.
