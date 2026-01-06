package org.example.eventregistry.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.example.eventregistry.entity.RegisteredEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RegisteredEventRepositoryImpl implements RegisteredEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<RegisteredEvent> findWithFilters(
            Pageable pageable,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String eventType,
            String serviceName) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<RegisteredEvent> query = cb.createQuery(RegisteredEvent.class);
        Root<RegisteredEvent> root = query.from(RegisteredEvent.class);

        List<Predicate> predicates = new ArrayList<>();

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        if (eventType != null && !eventType.isEmpty()) {
            predicates.add(cb.equal(root.get("eventType"), eventType));
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            predicates.add(cb.equal(root.get("serviceName"), serviceName));
        }

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        List<RegisteredEvent> result = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<RegisteredEvent> countRoot = countQuery.from(RegisteredEvent.class);
        countQuery.select(cb.count(countRoot));

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    @Override
    public List<String> findDistinctEventTypes() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<RegisteredEvent> root = query.from(RegisteredEvent.class);

        query.select(root.get("eventType")).distinct(true);
        query.orderBy(cb.asc(root.get("eventType")));

        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<String> findDistinctServiceNames() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<RegisteredEvent> root = query.from(RegisteredEvent.class);

        query.select(root.get("serviceName")).distinct(true);
        query.orderBy(cb.asc(root.get("serviceName")));

        return entityManager.createQuery(query).getResultList();
    }
}
