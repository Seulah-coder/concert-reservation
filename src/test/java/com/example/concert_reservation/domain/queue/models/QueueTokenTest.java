package com.example.concert_reservation.domain.queue.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * QueueToken 단위 테스트
 */
@DisplayName("QueueToken 테스트")
class QueueTokenTest {
    
    @Test
    @DisplayName("generate()로 새로운 토큰을 생성할 수 있다")
    void generate_createsNewToken() {
        // when
        QueueToken token = QueueToken.generate();
        
        // then
        assertThat(token).isNotNull();
        assertThat(token.getValue()).isNotBlank();
    }
    
    @Test
    @DisplayName("generate()로 생성한 토큰은 매번 다른 값이다")
    void generate_createsDifferentTokens() {
        // when
        QueueToken token1 = QueueToken.generate();
        QueueToken token2 = QueueToken.generate();
        
        // then
        assertThat(token1.getValue()).isNotEqualTo(token2.getValue());
    }
    
    @Test
    @DisplayName("of()로 기존 토큰 값으로 QueueToken을 생성할 수 있다")
    void of_createsTokenFromValue() {
        // given
        String tokenValue = "test-token-123";
        
        // when
        QueueToken token = QueueToken.of(tokenValue);
        
        // then
        assertThat(token.getValue()).isEqualTo(tokenValue);
    }
    
    @Test
    @DisplayName("null 값으로 토큰 생성 시 예외가 발생한다")
    void of_withNullValue_throwsException() {
        // when & then
        assertThatThrownBy(() -> QueueToken.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("토큰 값은 비어있을 수 없습니다");
    }
    
    @Test
    @DisplayName("빈 문자열로 토큰 생성 시 예외가 발생한다")
    void of_withEmptyValue_throwsException() {
        // when & then
        assertThatThrownBy(() -> QueueToken.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("토큰 값은 비어있을 수 없습니다");
    }
    
    @Test
    @DisplayName("같은 값을 가진 토큰은 동일하다")
    void equals_sameValue_returnsTrue() {
        // given
        String tokenValue = "test-token";
        QueueToken token1 = QueueToken.of(tokenValue);
        QueueToken token2 = QueueToken.of(tokenValue);
        
        // when & then
        assertThat(token1).isEqualTo(token2);
        assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
    }
    
    @Test
    @DisplayName("다른 값을 가진 토큰은 동일하지 않다")
    void equals_differentValue_returnsFalse() {
        // given
        QueueToken token1 = QueueToken.of("token1");
        QueueToken token2 = QueueToken.of("token2");
        
        // when & then
        assertThat(token1).isNotEqualTo(token2);
    }
    
    @Test
    @DisplayName("toString()은 토큰 값을 반환한다")
    void toString_returnsTokenValue() {
        // given
        String tokenValue = "test-token";
        QueueToken token = QueueToken.of(tokenValue);
        
        // when
        String result = token.toString();
        
        // then
        assertThat(result).isEqualTo(tokenValue);
    }
}
