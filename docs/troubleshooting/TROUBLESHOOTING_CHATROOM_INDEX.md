# TROUBLESHOOTING: 채팅방 목록 인덱스 미적용

## 증상

`chat_rooms` 테이블에 `idx_chat_rooms_last_message_at (last_message_at DESC)` 인덱스를 추가했음에도 `EXPLAIN` 결과가 개선되지 않음.

```
chat_rooms → type: ALL, key: NULL, rows: 50,668, Extra: Using filesort
```

Before와 After의 실행 계획이 동일하고, 실행 시간도 61.6ms → 57.3ms로 유의미한 개선 없음.

## 원인

`ChatRoomRepository.findRoomsByMember` 쿼리의 `ORDER BY`에 `CASE WHEN` 표현식이 사용되고 있기 때문이다.

```sql
ORDER BY CASE WHEN cr.last_message_at IS NULL THEN 1 ELSE 0 END ASC,
         cr.last_message_at DESC, cr.created_at DESC
```

B-Tree 인덱스는 **컬럼 값을 직접 정렬할 때만** 사용 가능하다. `CASE WHEN ... IS NULL THEN 1 ELSE 0 END`는 컬럼 값이 아닌 **표현식 계산 결과**를 기준으로 정렬하기 때문에 옵티마이저가 인덱스를 사용하지 못하고 `Using filesort`로 처리한다.

MySQL은 `ORDER BY` 절에 함수나 표현식이 포함되면 해당 컬럼의 인덱스를 정렬에 활용할 수 없다.

## 발생 배경

MySQL 8.0은 `ORDER BY ... NULLS LAST` 구문을 지원하지 않는다. `last_message_at`이 `NULL`인 채팅방(메시지가 없는 방)을 목록 맨 뒤로 보내기 위해 `CASE WHEN` 표현식으로 우회한 것이 원인이다.

```java
// ChatRoomRepository.java
@Query("""
        SELECT cr FROM ChatRoom cr
        LEFT JOIN FETCH cr.product
        JOIN ChatMember cm ON cm.chatRoom = cr
        WHERE cm.member.id = :memberId
        ORDER BY CASE WHEN cr.lastMessageAt IS NULL THEN 1 ELSE 0 END ASC,
                 cr.lastMessageAt DESC, cr.createdAt DESC
        """)
Page<ChatRoom> findRoomsByMember(...);
```

## 해결 방안

### 방법 1: `last_message_at` NULL 제거 (권장)

채팅방 생성 시 `last_message_at`을 `NULL` 대신 `created_at`으로 초기화한다. `NULL`이 없어지면 `CASE WHEN` 표현식이 필요 없어지고, `ORDER BY last_message_at DESC`만으로 정렬할 수 있어 인덱스가 적용된다.

```java
// ChatRoom.java 엔티티
@Column(name = "last_message_at")
private LocalDateTime lastMessageAt;

public static ChatRoom createTradeRoom(Product product) {
    ChatRoom room = new ChatRoom();
    // ...
    room.lastMessageAt = LocalDateTime.now(); // NULL 대신 생성 시각으로 초기화
    return room;
}
```

```java
// ChatRoomRepository.java 쿼리 단순화
ORDER BY cr.lastMessageAt DESC, cr.createdAt DESC
```

이렇게 변경하면 `idx_chat_rooms_last_message_at`이 정렬에 실제로 적용된다.

### 방법 2: 현 상태 유지 (현재 적용 중)

`CASE WHEN` 표현식을 유지하되, `idx_chat_rooms_last_message_at` 인덱스의 한계를 문서화한다. 채팅방 수가 수백만 건 이상이 되지 않는 한 허용 가능한 수준으로 판단할 수 있다.

## 현재 상태

방법 2를 유지 중. `idx_chat_rooms_last_message_at` 인덱스는 코드에 선언되어 있으나 현재 쿼리에서 정렬에 사용되지 않는다. 쓰기 시 B-Tree 재구성 비용만 발생하는 상태다.

방법 1 적용 시 엔티티와 쿼리 수정이 필요하며, 기존에 `last_message_at = NULL`로 저장된 채팅방 데이터에 대한 마이그레이션도 고려해야 한다.
