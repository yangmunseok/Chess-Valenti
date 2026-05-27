package org.spring.createa.chessvalenti.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.spring.createa.chessvalenti.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<User> findUsersWithFilters(String username, String email,
      List<String> onlineUsernames, LocalDateTime startDate, Pageable pageable) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> query = cb.createQuery(User.class);
    Root<User> root = query.from(User.class);

    query.where(buildPredicates(cb, root, username, email, onlineUsernames, startDate)
        .toArray(new Predicate[0]));

    // Apply sorting
    if (pageable.getSort().isSorted()) {
      List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
      pageable.getSort().forEach(order -> {
        if (order.isAscending()) {
          orders.add(cb.asc(root.get(order.getProperty())));
        } else {
          orders.add(cb.desc(root.get(order.getProperty())));
        }
      });
      query.orderBy(orders);
    }

    TypedQuery<User> typedQuery = entityManager.createQuery(query);
    typedQuery.setFirstResult((int) pageable.getOffset());
    typedQuery.setMaxResults(pageable.getPageSize());

    List<User> users = typedQuery.getResultList();

    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<User> countRoot = cq.from(User.class);
    cq.select(cb.count(countRoot))
        .where(buildPredicates(cb, countRoot, username, email, onlineUsernames, startDate)
            .toArray(new Predicate[0]));
    Long count = entityManager.createQuery(cq).getSingleResult();

    return new PageImpl<>(users, pageable, count);
  }

  private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<User> root, String username,
      String email, List<String> onlineUsernames, LocalDateTime startDate) {
    List<Predicate> predicates = new ArrayList<>();

    if (username != null && !username.isBlank()) {
      predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
    }

    if (email != null && !email.isBlank()) {
      predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
    }

    if (onlineUsernames != null) {
      predicates.add(root.get("username").in(onlineUsernames));
    }

    if (startDate != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
    }

    return predicates;
  }
}
