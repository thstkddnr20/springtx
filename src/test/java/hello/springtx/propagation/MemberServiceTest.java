package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     * memberService @Transactional:OFF
     * memberRepository @Transactional:ON
     * logRepository @Transactional:ON
     */
    @Test
    void outerTxOff_success() { // 둘다 커밋
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then: 모든 데이터가 정상 저장된다.
        Assertions.assertThat(memberRepository.find(username)).isNotEmpty();
        Assertions.assertThat(logRepository.find(username)).isNotEmpty();

    }

    /**
     * memberService @Transactional:OFF
     * memberRepository @Transactional:ON
     * logRepository @Transactional:ON Exception
     */
    @Test
    void outerTxOff_fail() { // member는 커밋, log는 롤백
        //given
        String username = "로그예외_outerTxOff_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);

        //then:
        Assertions.assertThat(memberRepository.find(username)).isNotEmpty();
        Assertions.assertThat(logRepository.find(username)).isEmpty();

    }

    /**
     * memberService @Transactional:ON
     * memberRepository @Transactional:OFF
     * logRepository @Transactional:OFF
     */
    @Test
    void singleTx() { // 둘다 커밋
        //given
        String username = "singleTx";

        //when
        memberService.joinV1(username);

        //then
        Assertions.assertThat(memberRepository.find(username)).isNotEmpty();
        Assertions.assertThat(logRepository.find(username)).isNotEmpty();

    }

    /**
     * memberService @Transactional:ON
     * memberRepository @Transactional:ON
     * logRepository @Transactional:ON
     */
    @Test
    void outerTxOn_success() { // 둘다 커밋
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then
        Assertions.assertThat(memberRepository.find(username)).isNotEmpty();
        Assertions.assertThat(logRepository.find(username)).isNotEmpty();

    }

    /**
     * memberService    @Transactional:ON
     * memberRepository @Transactional:ON
     * logRepository    @Transactional:ON Exception
     */
    @Test
    void outerTxOn_fail() { // member는 롤백, log는 롤백
        //왜냐? 트랜잭션 전파 옵션중 required로 인하여 서비스 트랜잭션 -> 리포지토리 트랜잭션으로 이동할 때 새로운 트랜잭션이 생성되는 것이 아닌 서비스의 트랜잭션을 그대로 이용하기 때문
        //given
        String username = "로그예외_outerTxOn_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username)).isInstanceOf(RuntimeException.class);
        //rollback-Only로 설정됨

        //then:
        Assertions.assertThat(memberRepository.find(username)).isEmpty();
        Assertions.assertThat(logRepository.find(username)).isEmpty();

    }

    /**
     * memberService    @Transactional:ON
     * memberRepository @Transactional:ON
     * logRepository    @Transactional:ON Exception
     */
    @Test
    void recoverException_fail() { // 내부 트랜잭션에서 이미 rollback-Only로 설정했기때문에 joinV2로 예외를 잡더라도 정상흐름으로 되는것이 아닌 롤백이 되어야한다 -> UnExpectedRollbackException 발생
        //given
        String username = "로그예외_recoverException_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV2(username)).isInstanceOf(UnexpectedRollbackException.class);

        //then:
        Assertions.assertThat(memberRepository.find(username)).isEmpty();
        Assertions.assertThat(logRepository.find(username)).isEmpty();

    }

    /**
     * memberService    @Transactional:ON
     * memberRepository @Transactional:ON
     * logRepository    @Transactional:ON (REQUIRES_NEW) Exception
     */
    @Test
    void recoverException_success() {
        //given
        String username = "로그예외_recoverException_success";

        //when
        memberService.joinV2(username);

        //then: member 저장, log 롤백
        Assertions.assertThat(memberRepository.find(username)).isPresent();
        Assertions.assertThat(logRepository.find(username)).isEmpty();

    }
}