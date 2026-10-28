package com.example.greatest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreatestThreeServerTest {
    @Test
    void findsTheGreatestOfThreeNumbers() {
        assertEquals(9, GreatestThreeServer.greatest(3, 9, 5));
    }

    @Test
    void handlesNegativeNumbers() {
        assertEquals(-2, GreatestThreeServer.greatest(-8, -2, -4));
    }
}
