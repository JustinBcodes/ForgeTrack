package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.Membership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    @EntityGraph(attributePaths = "user")
    List<Membership> findByOrganizationIdOrderByUserDisplayName(UUID organizationId);
}
