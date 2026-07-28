package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRoleRepository extends JpaRepository<UserRoleEntity, Long> {
    Optional<UserRoleEntity> findByCode(UserRole code);
    List<UserRoleEntity> findByCodeIn(Collection<UserRole> codes);
}
