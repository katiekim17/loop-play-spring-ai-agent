# PR: Week 1 미션 — Spring AI 배달 상담 에이전트 (1~4단계 전체)

## Summary

루퍼스 부트캠프 Week 1 미션 4단계를 모두 구현합니다.

- **1단계**: 기본 API + System Prompt + Structured Output 설계 및 구현
- **2단계**: Prompt Engineering 정량 비교 + 실패 관찰
- **3단계**: SSE 스트리밍 응답 구현
- **4단계**: Observability — PerformanceLoggingAdvisor 구현 및 측정

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `SupportResponse.java` | `estimatedResolutionMinutes`, `Actionability` enum, `COMPLAINT` category 추가 |
| `BaedalPrompt.java` | System Prompt 4섹션 설계 ([역할]/[규칙]/[금지]/[actionability 분류 기준]) |
| `SupportController.java` | TODO 구현: ChatClient 빌더 체인 + PerformanceLoggingAdvisor 등록 |
| `PromptLabController.java` | TODO 구현: 반복 호출로 consistency 측정 |
| `StreamingChatController.java` | TODO 구현: SSE Flux\<String\> 스트리밍 |
| `PerformanceLoggingAdvisor.java` | TODO 구현: elapsed time + token usage 로깅 |
| `build.gradle` | `useJUnitPlatform()` 추가 (JUnit 5 테스트 디스커버리) |
| `README.md` | 4단계 실험 결과 및 설계 결정 전체 기록 |

**테스트 파일 (신규 생성)**:
- `SupportResponseTest.java` (5개)
- `BaedalPromptTest.java` (5개)
- `SupportControllerTest.java` (2개)
- `PromptLabControllerTest.java` (2개)
- `StreamingChatControllerTest.java` (2개)
- `PerformanceLoggingAdvisorTest.java` (3개)

총 **19개 테스트**, 전부 통과.

---

## 단계별 구현 상세

### 1단계: 기본 API + System Prompt + Structured Output

**핵심 구현**

`ChatClient.Builder` 빌더 체인으로 System Prompt + Structured Output 연결:

```java
return builder
    .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
    .defaultAdvisors(performanceAdvisor)
    .build()
    .prompt()
    .user(req.message())
    .call()
    .entity(SupportResponse.class);
```

**설계 결정**

| 결정 | 이유 |
|------|------|
| `COMPLAINT` 카테고리 추가 | 라이더 사고·음식 파손을 REFUND나 ETC에 억지로 넣지 않기 위해 |
| `estimatedResolutionMinutes` 필드 추가 | "얼마나 걸려요?"를 구조화된 숫자로 분리해 프론트 렌더링 직결 |
| `actionability` 필드 추가 | 자유 텍스트 `nextAction`만으로는 "즉시 처리 가능" vs "담당팀 확인 필요"를 구조적으로 구별 불가 |
| [금지] 4가지 선택 | 타사 추천·개인정보·쿠폰 약속은 법적 리스크, 의료 판단은 배달 음식 알레르기·식중독 케이스에서 현실적으로 필요 |

---

### 2단계: Prompt Engineering 정량 비교

**실험**: 단순 프롬프트 vs 구조화 프롬프트, 각 5회 반복 (temperature=0.3)

| 시나리오 | 단순 consistency | 구조화 consistency |
|---------|-----------------|-------------------|
| 배달 어디쯤? | 1.00 | 1.00 |
| 주문 취소·환불 | 1.00 | 1.00 |
| 라이더 음식 엎음 | **1.00** | **0.60** |

> 구조화 프롬프트가 항상 더 일관적이지는 않다. 지시가 많아질수록 모델이 더 많은 컨텍스트를 고려해 분류 결정에서 흔들릴 수 있다.

**[금지] 섹션 제거 공격 시나리오**: 개인정보 요청, 경쟁사 비교, 쿠폰 협박 3가지에서 [금지] 없을 때 응답이 표면적으로는 거절하지만 전제를 묵시적으로 인정하거나 명시적 거절이 빠지는 것을 확인.

---

### 3단계: SSE 스트리밍

**구현**: `MediaType.TEXT_EVENT_STREAM_VALUE` + `Flux<String>`

```java
@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestBody ChatRequest req) {
    return builder.defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
            .build().prompt().user(req.message()).stream().content();
}
```

**체감 속도 비교** (동일 메시지, 로컬 Ollama qwen2.5):

| | 동기 | 스트리밍 |
|--|------|---------|
| 첫 글자 | 20.3초 후 | **3.6초 후** |
| 전체 완료 | 20.3초 | 8.3초 |

**주요 발견**:
- `.stream()` + `.entity()` 동시 사용 불가 — JSON이 완성되기 전에 파싱이 시작되므로 Structured Output은 반드시 `.call()`을 사용해야 함
- 스트리밍이 토큰 소비를 늘리지 않음 — LLM 연산은 동일, 전송 방식만 다름
- Structured Output용 System Prompt를 스트리밍에 그대로 쓰면 `IMMEDIATE:` 같은 enum 설명 텍스트가 응답에 노출됨

---

### 4단계: Observability

**CallAdvisor 구현**: `chain.nextCall()` 전후로 경과 시간 + 토큰 사용량 로깅

```
[PerformanceLoggingAdvisor] elapsed=25262ms inputTokens=748 outputTokens=108 totalTokens=856
```

**System Prompt 2배 토큰 실험**:

| | inputTokens | outputTokens |
|--|-------------|-------------|
| 원본 프롬프트 | 748 | 108 |
| 2배 프롬프트 | 1233 | 94~96 |
| 변화 | **+485 (+65%)** | 거의 동일 |

> 프롬프트가 2배가 됐는데 65% 증가에 그친 이유: Spring AI가 Structured Output을 위해 JSON 스키마를 자동 주입하므로 원본 입력의 일부는 이미 Spring AI 주입분이다. outputTokens은 System Prompt 길이와 무관하게 동일한 JSON 구조를 출력하므로 거의 변하지 않는다.

**AI 코드 리뷰 — 나이브한 챗봇의 3가지 프로덕션 문제**:

| # | 문제 | 위험 | 수정 |
|---|------|------|------|
| 1 | 매 요청마다 `ChatClient` 재생성 | HTTP 연결 풀 고갈, 소켓 CLOSE_WAIT 축적 | `@Component` Bean으로 싱글톤 관리 |
| 2 | `@RequestParam`으로 고객 메시지 수신 | 주문번호·주소가 서버 로그/브라우저 히스토리에 평문 기록 | `@RequestBody`로 변경 |
| 3 | 타임아웃 없음 | LLM 지연 시 스레드 풀 200개 고갈 → 서버 전체 다운 | `spring.ai.ollama.chat.options.timeout` + `@ExceptionHandler` |

---

## Test Plan

- [ ] `./gradlew test` 실행 → 19개 전부 PASS 확인
- [ ] `./gradlew bootRun` 후 시나리오 3개 수동 호출 확인
  - `POST /api/v1/support` — JSON 응답 + advisor 로그 출력 확인
  - `POST /api/v1/chat` — 동기 텍스트 응답 확인
  - `POST /api/v1/chat/stream` — SSE 토큰 스트리밍 확인
- [ ] 로그에서 `[PerformanceLoggingAdvisor] elapsed=...ms inputTokens=...` 확인

---

## 학습 기록 요약

**내가 배운 것**
- SSE와 REST는 체감 속도가 다르지만 토큰 소비량과 응답 정확성은 동일 — 전송 방식만 다를 뿐 LLM 연산은 같다
- `builder.build()`를 매 요청마다 호출해도 괜찮은 이유 — `OllamaApi`(HTTP 연결 풀)는 `Builder` 싱글톤 안에 있고, `.build()`는 설정 객체만 만드는 가벼운 작업이다
- Spring AI가 `.entity(SupportResponse.class)` 호출 시 JSON 스키마를 User 메시지에 자동 주입한다는 것을 DEBUG 로그로 직접 확인

**의문점**
- System Prompt 2배 실험을 2배 한 가지만 측정했는데 3배로 늘리면 토큰도 선형으로 느는지 확인 안 했다
- JSON 스키마가 System이 아닌 User 메시지에 붙는 이유가 Ollama 같은 로컬 모델이 System 지시를 잘 무시해서라고 했는데, GPT-4 같은 클라우드 모델에서도 같은 방식인지 궁금하다

**다음 주차 아이디어**
- Tool Calling으로 조건부 취소·환불 구현 (배달 출발 전 → 자동 취소, 출발 후 → 상담원 연결)
- 대화 히스토리 저장 및 멀티턴 상담 구현, 히스토리 압축 기준 실험
- LLM이 틀린 Tool을 호출해도 피해가 없도록 Tool 자체에 조건 검사 설계

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)