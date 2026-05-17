# loop-play-spring-ai-agent

Spring AI 기반 배달 상담 에이전트 학습용 스타터 코드입니다.

## 개요

루퍼스 부트캠프 "Spring AI 배달 상담 에이전트" 6주 과정의 Week 1 미션 스타터 코드입니다.
`ChatClient`, System Prompt, Structured Output, Streaming, Observability 개념을 실습합니다.

## 빠른 시작

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

---

## 1단계: 기본 API + System Prompt + Structured Output

### 시나리오별 응답

**시나리오 1: 배달 위치 문의**
```
POST /api/v1/support
{"message": "주문번호 2024-1234 배달 어디쯤에 있어요?"}
```
```json
{
  "summary": "주문번호 2024-1234의 배송 위치를 알려주시면 현재 배송 상황을 안내해 드리겠습니다.",
  "category": "DELIVERY",
  "urgency": "NORMAL",
  "nextAction": "배달 상태 확인 후 답변 제공",
  "neededInfo": ["현재 배달 위치"],
  "estimatedResolutionMinutes": 15,
  "actionability": "NEEDS_INFO"
}
```

**시나리오 2: 주문 취소·환불 문의**
```
POST /api/v1/support
{"message": "방금 시킨 주문 취소하고 싶어요. 환불은 얼마나 걸려요?"}
```
```json
{
  "summary": "주문 취소를 원하시는 것으로 이해했습니다. 주문번호를 알려주시면 즉시 처리하겠습니다.",
  "category": "ORDER",
  "urgency": "NORMAL",
  "nextAction": "주문 취소 요청 접수 후 처리",
  "neededInfo": ["주문번호"],
  "estimatedResolutionMinutes": 15,
  "actionability": "NEEDS_INFO"
}
```

**시나리오 3: 라이더 사고 보상**
```
POST /api/v1/support
{"message": "라이더가 음식을 엎었다는데 보상 받을 수 있나요?"}
```
```json
{
  "summary": "라이더가 음식을 엎었다는 사항으로 보상 여부를 확인해야 합니다.",
  "category": "COMPLAINT",
  "urgency": "NORMAL",
  "nextAction": "추가 정보를 제공해 주시면 확인 후 처리하겠습니다.",
  "neededInfo": ["주문번호", "사고 발생 시간"],
  "estimatedResolutionMinutes": 30,
  "actionability": "NEEDS_INFO"
}
```

> **관찰**: `actionability` 필드와 프롬프트에 분류 기준을 명시한 후 시나리오 3이 `DELIVERY`에서 `COMPLAINT`로 올바르게 분류되었다. LLM이 enum 값을 제대로 활용하려면 프롬프트에 각 값의 의미를 명시해야 한다.

---

### 설계 결정 문서

#### [금지] 규칙 3+1가지를 선택한 이유

| 규칙 | 근거 | 빼면 생기는 사고 |
|------|------|----------------|
| 타사 플랫폼 추천 금지 | 경쟁사 유도 방지, 브랜드 신뢰 보호 | "쿠팡이츠가 더 빠르지 않아요?"에 AI가 동의 |
| 개인정보 노출 금지 | 개인정보보호법 의무 | 고객 요청 시 라이더 연락처 노출 |
| 쿠폰/보상 약속 금지 | 무권한 약속 방지, 법적 리스크 제거 | "5000원 쿠폰 드릴게요"가 법적 구속력 발생 |
| 의료적 판단 금지 (추가) | 알레르기·식중독 판단은 의료 행위 | "증상이 경미하니 괜찮을 것 같습니다" → 의료 과실 리스크 |

3가지 원래 규칙은 모두 "빼면 실제 운영에서 법적·비즈니스 사고가 나는 규칙"이다. 의료 규칙은 배달 상담에서 음식 알레르기 반응이나 식중독 의심 사례에서 현실적으로 필요하다.

#### Category enum — 왜 이 6개인가

| Category | 해당 문의 유형 |
|----------|-------------|
| ORDER | 주문 접수, 메뉴 오류, 주문 변경 |
| DELIVERY | 배달 현황, 지연, 위치 확인 |
| REFUND | 환불 요청, 취소 후 처리 |
| PAYMENT | 결제 오류, 이중 청구 |
| COMPLAINT | 음식 파손, 오배달, 라이더 사고, 품질 불만 |
| ETC | 위 카테고리에 속하지 않는 문의 |

원래 5개(COMPLAINT 없음)에서 시나리오 3 "라이더가 음식을 엎었다"를 분류할 때 REFUND나 ETC에 억지로 넣어야 했다. COMPLAINT를 추가해 피해 경험 불만을 독립 카테고리로 분리했다. 다만 LLM이 COMPLAINT를 제대로 활용하려면 시스템 프롬프트에 카테고리 정의를 명시해야 한다.

#### 추가 필드 `estimatedResolutionMinutes` 선택 근거

고객이 가장 자주 묻는 "얼마나 걸려요?"를 구조화된 숫자로 뽑아낸다. `urgency`는 우선순위만 나타내고 시간 정보는 없다. 숫자 필드로 만들면 프론트엔드에서 "약 N분 소요 예정" 형태로 바로 렌더링 가능하고, LLM이 urgency + category를 종합해 추론하도록 유도한다.

실제 응답에서 나온 값:
- 배달 위치 확인: 15분
- 주문 취소: 15분
- 라이더 사고 확인: 30분

#### 추가 필드 `actionability` 선택 근거

`nextAction`은 자유 텍스트라 "즉시 처리 가능"과 "담당팀 확인 필요"를 구조적으로 구별할 방법이 없었다. `actionability` enum으로 처리 가능 여부를 명확히 분리하면 프론트엔드에서 "지금 바로 처리 가능" / "정보 입력 필요" / "담당팀 검토 중" 같은 상태 UI를 직접 렌더링할 수 있다.

| 값 | 의미 | 예시 |
|----|------|------|
| `IMMEDIATE` | 고객이 앱에서 즉시 처리 가능 | 접수 직후 주문 취소 |
| `NEEDS_INFO` | 추가 정보만 있으면 처리 가능 | 주문번호 미제공 |
| `NEEDS_REVIEW` | 정책 검토 또는 담당팀 확인 필요 | 보상 금액 산정 |
| `ESCALATED` | 상위팀 이관 필요 | 법적·의료적 사안 |
