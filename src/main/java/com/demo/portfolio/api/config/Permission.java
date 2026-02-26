package com.demo.portfolio.api.config;

import java.util.EnumSet;
import java.util.Set;

public enum Permission {

    ADMIN(1), WRITER(2), READER(4);

    public static final String ROLE_WRITER = "hasRole('WRITER')";
    public static final String ROLE_READER = "hasRole('READER')";
    public static final String ROLE_ADMIN = "hasRole('ADMIN')";


    private final int bit;

    Permission(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public static Set<Permission> fromMask(int mask) {
        EnumSet<Permission> set = EnumSet.noneOf(Permission.class);
        for (Permission p : values()) {
            if ((mask & p.bit) != 0) {
                set.add(p);
            }
        }
        return set;
    }
}
