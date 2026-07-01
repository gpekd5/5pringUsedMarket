package com.example.fivespringusedmarket.chat.entity;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatRoom 상태 전이 단위 테스트")
class ChatRoomTest {

    private ChatRoom csRoom;

    @BeforeEach
    void setUp() {
        csRoom = ChatRoom.createCsRoom("테스트 문의");
    }

    @Test
    @DisplayName("WAITING → IN_PROGRESS 전이 성공")
    void changeCsStatus_waitingToInProgress_success() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);

        assertThat(csRoom.getCsStatus()).isEqualTo(CsStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("IN_PROGRESS → COMPLETED 전이 성공")
    void changeCsStatus_inProgressToCompleted_success() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);
        csRoom.changeCsStatus(CsStatus.COMPLETED);

        assertThat(csRoom.getCsStatus()).isEqualTo(CsStatus.COMPLETED);
    }

    @Test
    @DisplayName("COMPLETED 이후 전이 시도 시 예외 발생")
    void changeCsStatus_afterCompleted_throwsException() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);
        csRoom.changeCsStatus(CsStatus.COMPLETED);

        assertThatThrownBy(() -> csRoom.changeCsStatus(CsStatus.IN_PROGRESS))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("WAITING → COMPLETED 직접 전이 시 예외 발생")
    void changeCsStatus_waitingToCompleted_throwsException() {
        assertThatThrownBy(() -> csRoom.changeCsStatus(CsStatus.COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("IN_PROGRESS → WAITING 롤백 시 예외 발생")
    void changeCsStatus_inProgressToWaiting_throwsException() {
        csRoom.changeCsStatus(CsStatus.IN_PROGRESS);

        assertThatThrownBy(() -> csRoom.changeCsStatus(CsStatus.WAITING))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("CS 방 초기 상태는 WAITING")
    void createCsRoom_initialStatus_isWaiting() {
        assertThat(csRoom.getCsStatus()).isEqualTo(CsStatus.WAITING);
        assertThat(csRoom.getType()).isEqualTo(ChatRoomType.CS);
    }
}
