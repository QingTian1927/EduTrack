package com.example.edutrack.accounts.dto;

import java.util.UUID;

public class UserWithRoleDTO {
        private UUID id;
        private String email;
        private String fullName;
        private String role;
        private boolean isLocked;
        private boolean isActive;

        public UserWithRoleDTO(UUID id, String email, String fullName, String role, boolean isLocked, boolean isActive) {
                this.id = id;
                this.email = email;
                this.fullName = fullName;
                this.role = role;
                this.isLocked = isLocked;
                this.isActive = isActive;
        }
        public UUID getId() {
                return id;
        }
        public void setId(UUID id) {
                this.id = id;
        }
        public String getEmail() {
                return email;
        }
        public void setEmail(String email) {
                this.email = email;
        }
        public String getFullName() {
                return fullName;
        }
        public void setFullName(String fullName) {
                this.fullName = fullName;
        }
        public String getRole() {
                return role;
        }
        public void setRole(String role) {
                this.role = role;
        }
        public boolean isLocked() {
                return isLocked;
        }
        public void setLocked(boolean isLocked) {
                this.isLocked = isLocked;
        }
        public boolean isActive() {
                return isActive;
        }
        public void setActive(boolean isActive) {
                this.isActive = isActive;
        }
}
