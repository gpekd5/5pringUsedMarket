package com.example.fivespringusedmarket.coupon.repository;

import com.example.fivespringusedmarket.coupon.entity.UserCoupon;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.example.fivespringusedmarket.coupon.entity.QUserCoupon.userCoupon;

@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByMemberIdAndCouponId(Long memberId, Long couponId) {
        Integer result = queryFactory
                .selectOne()
                .from(userCoupon)
                .where(
                        userCoupon.member.id.eq(memberId),
                        userCoupon.coupon.id.eq(couponId)
                )
                .fetchFirst();
        return result != null;
    }

    @Override
    public Optional<UserCoupon> findByIdAndMemberId(Long id, Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(userCoupon)
                        .where(
                                userCoupon.id.eq(id),
                                userCoupon.member.id.eq(memberId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public Page<UserCoupon> findByMemberId(Long memberId, Pageable pageable) {
        return findByMemberIdWithUsedFilter(memberId, null, pageable);
    }

    @Override
    public Page<UserCoupon> findByMemberIdAndUsedAtIsNull(Long memberId, Pageable pageable) {
        return findByMemberIdWithUsedFilter(memberId, false, pageable);
    }

    @Override
    public Page<UserCoupon> findByMemberIdAndUsedAtIsNotNull(Long memberId, Pageable pageable) {
        return findByMemberIdWithUsedFilter(memberId, true, pageable);
    }

    // used=null: 전체, used=true: 사용 완료, used=false: 미사용
    private Page<UserCoupon> findByMemberIdWithUsedFilter(Long memberId, Boolean used, Pageable pageable) {
        List<UserCoupon> content = queryFactory
                .selectFrom(userCoupon)
                .join(userCoupon.coupon).fetchJoin()
                .where(
                        userCoupon.member.id.eq(memberId),
                        usedCondition(used)
                )
                .orderBy(userCoupon.issuedAt.desc(), userCoupon.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(userCoupon.count())
                .from(userCoupon)
                .where(
                        userCoupon.member.id.eq(memberId),
                        usedCondition(used)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanExpression usedCondition(Boolean used) {
        if (used == null) return null;
        return used ? userCoupon.usedAt.isNotNull() : userCoupon.usedAt.isNull();
    }
}
