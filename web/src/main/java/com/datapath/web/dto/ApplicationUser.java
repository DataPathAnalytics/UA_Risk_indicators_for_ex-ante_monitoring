package com.datapath.web.dto;


import lombok.Data;

@Data
public class ApplicationUser {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String region;
    private String role;
}
