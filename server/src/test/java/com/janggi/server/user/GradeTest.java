package com.janggi.server.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GradeTest {

    @Test
    void ratingToGrade() {
        assertEquals("18급", Grade.name(0));
        assertEquals("18급", Grade.name(799));
        assertEquals("17급", Grade.name(800));
        assertEquals("9급", Grade.name(1200));
        assertEquals("9급", Grade.name(1249));
        assertEquals("8급", Grade.name(1250));
        assertEquals("1급", Grade.name(1600));
        assertEquals("초단", Grade.name(1650));
        assertEquals("2단", Grade.name(1700));
        assertEquals("9단", Grade.name(2050));
        assertEquals("9단", Grade.name(3000));
    }
}
