package com.sinio.demo.dto;

public class EmployeeRoleOption {
    private String code;
    private String label;

    public EmployeeRoleOption(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
