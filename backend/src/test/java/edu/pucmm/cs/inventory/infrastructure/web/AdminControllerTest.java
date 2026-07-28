package edu.pucmm.cs.inventory.infrastructure.web;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.pucmm.cs.inventory.application.KeycloakAdminService;
import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ChangeUserRoleRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CreateUserRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.RoleResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.UserResponseDTO;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getRoles_ShouldReturnAllSystemRoles() {
        // Act
        ResponseEntity<List<RoleResponseDTO>> response = adminController.getRoles();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(SystemRole.values().length, response.getBody().size());
    }

    @Test
    void getUsers_ShouldReturnAllUsers() {
        // Arrange
        UserResponseDTO user = new UserResponseDTO();
        user.setId("123");
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setRole("ADMIN");
        when(keycloakAdminService.listUsers()).thenReturn(List.of(user));

        // Act
        ResponseEntity<List<UserResponseDTO>> response = adminController.getUsers();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("testuser", response.getBody().get(0).getUsername());
        verify(keycloakAdminService, times(1)).listUsers();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        // Arrange
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("password");
        request.setRole(SystemRole.ADMIN);

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId("123");
        userResponse.setUsername("newuser");
        userResponse.setEmail("new@test.com");
        userResponse.setRole("ADMIN");
        when(keycloakAdminService.createUser(request)).thenReturn(userResponse);

        // Act
        ResponseEntity<UserResponseDTO> response = adminController.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newuser", response.getBody().getUsername());
        verify(keycloakAdminService, times(1)).createUser(request);
    }

    @Test
    void changeUserRole_ShouldReturnUpdatedUser() {
        // Arrange
        String userId = "123";
        ChangeUserRoleRequestDTO request = new ChangeUserRoleRequestDTO();
        request.setRole(SystemRole.VIEWER);
        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(userId);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@test.com");
        userResponse.setRole("VIEWER");
        when(keycloakAdminService.changeUserRole(userId, SystemRole.VIEWER)).thenReturn(userResponse);

        // Act
        ResponseEntity<UserResponseDTO> response = adminController.changeUserRole(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VIEWER", response.getBody().getRole());
        verify(keycloakAdminService, times(1)).changeUserRole(userId, SystemRole.VIEWER);
    }
}
