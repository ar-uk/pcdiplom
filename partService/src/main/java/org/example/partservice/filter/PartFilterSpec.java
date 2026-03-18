package org.example.partservice.filter;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic reusable filter builder using JPA Specifications.
 * Instead of writing individual query methods for every field combination,
 * you pass in filter params and this builds the WHERE clause dynamically.
 *
 * Usage example:
 *   Specification<Cpu> spec = PartFilterSpec.<Cpu>builder()
 *       .like("name", "ryzen")
 *       .gte("price_usd", new BigDecimal("100"))
 *       .lte("price_usd", new BigDecimal("500"))
 *       .eq("core_count", 8)
 *       .build();
 *   cpuRepository.findAll(spec, pageable);
 */
public class PartFilterSpec<T> {

    private final List<SpecEntry> entries = new ArrayList<>();

    public static <T> PartFilterSpec<T> builder() {
        return new PartFilterSpec<>();
    }

    /** Case-insensitive partial match — for name, brand, color etc. */
    public PartFilterSpec<T> like(String field, String value) {
        if (value != null && !value.isBlank()) {
            entries.add(new SpecEntry(SpecType.LIKE, field, value));
        }
        return this;
    }

    /** Exact match — for integers, enums, booleans */
    public PartFilterSpec<T> eq(String field, Object value) {
        if (value != null) {
            entries.add(new SpecEntry(SpecType.EQ, field, value));
        }
        return this;
    }

    /** Greater than or equal — for price, clock speed, wattage etc. */
    public PartFilterSpec<T> gte(String field, BigDecimal value) {
        if (value != null) {
            entries.add(new SpecEntry(SpecType.GTE, field, value));
        }
        return this;
    }

    /** Less than or equal */
    public PartFilterSpec<T> lte(String field, BigDecimal value) {
        if (value != null) {
            entries.add(new SpecEntry(SpecType.LTE, field, value));
        }
        return this;
    }

    /** Greater than or equal for integers */
    public PartFilterSpec<T> gteInt(String field, Integer value) {
        if (value != null) {
            entries.add(new SpecEntry(SpecType.GTE, field, new BigDecimal(value)));
        }
        return this;
    }

    /** Less than or equal for integers */
    public PartFilterSpec<T> lteInt(String field, Integer value) {
        if (value != null) {
            entries.add(new SpecEntry(SpecType.LTE, field, new BigDecimal(value)));
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Specification<T> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (SpecEntry e : entries) {
                switch (e.type) {
                    case LIKE -> predicates.add(
                        cb.like(cb.lower(root.get(e.field)), "%" + e.value.toString().toLowerCase() + "%")
                    );
                    case EQ -> predicates.add(
                        cb.equal(root.get(e.field), e.value)
                    );
                    case GTE -> predicates.add(
                        cb.greaterThanOrEqualTo(root.get(e.field), (Comparable) e.value)
                    );
                    case LTE -> predicates.add(
                        cb.lessThanOrEqualTo(root.get(e.field), (Comparable) e.value)
                    );
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private enum SpecType { LIKE, EQ, GTE, LTE }

    private record SpecEntry(SpecType type, String field, Object value) {}
}
