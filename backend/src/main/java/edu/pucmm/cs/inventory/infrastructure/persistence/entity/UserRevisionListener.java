package edu.pucmm.cs.inventory.infrastructure.persistence.entity;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Listener de revisiones de Envers.
 * Captura el usuario autenticado desde el contexto de seguridad de Spring
 * y lo inyecta en la entidad de revisión.
 */
public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        UserRevisionEntity userRevisionEntity = (UserRevisionEntity) revisionEntity;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                // Keycloak típicamente usa preferred_username
                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getSubject();
                }
                userRevisionEntity.setUsername(username);
            } else {
                userRevisionEntity.setUsername(authentication.getName());
            }
        } else {
            userRevisionEntity.setUsername("system");
        }
    }
}
