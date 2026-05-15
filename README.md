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
  "summary": "주문번호 2024-1234의 배달 위치를 알려드리겠습니다.",
  "category": "DELIVERY",
  "urgency": "NORMAL",
  "nextAction": "배달 진행 상황을 확인합니다.",
  "neededInfo": [],
  "estimatedResolutionMinutes": 5
}
```

**시나리오 2: 주문 취소·환불 문의**
```
POST /api/v1/support
{"message": "방금 시킨 주문 취소하고 싶어요. 환불은 얼마나 걸려요?"}
```
```json
{
  "summary": "즉시 주문을 취소할 수 있습니다.",
  "category": "ORDER",
  "urgency": "NORMAL",
  "nextAction": "주문 취소 요청 처리",
  "neededInfo": ["주문번호"],
  "estimatedResolutionMinutes": 15
}
```

**시나리오 3: 라이더 사고 보상**
```
POST /api/v1/support
{"message": "라이더가 음식을 엎었다는데 보상 받을 수 있나요?"}
```
```json
{
  "summary": "보상 여부는 배송 상황과 정책에 따라 다릅니다.",
  "category": "DELIVERY",
  "urgency": "NORMAL",
  "nextAction": "라이더와 연락하여 상황을 확인합니다.",
  "neededInfo": ["주문번호", "배달 주소"],
  "estimatedResolutionMinutes": 30
}
```

> **관찰**: 시나리오 3은 `COMPLAINT`가 아닌 `DELIVERY`로 분류되었다. 시스템 프롬프트에 각 Category 값의 의미를 명시하지 않았기 때문에 LLM이 새로 추가된 `COMPLAINT` enum을 활용하지 못했다. 시스템 프롬프트의 `[응답 포맷]` 섹션에 카테고리별 분류 기준을 추가하면 개선된다.

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
- 배달 위치 확인: 5분
- 주문 취소: 15분
- 라이더 사고 확인: 30분
