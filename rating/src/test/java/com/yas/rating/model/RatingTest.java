package com.yas.rating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void equals_shouldReturnTrueForSameInstance() {
        Rating rating = Rating.builder().id(1L).build();

        assertTrue(rating.equals(rating));
    }

    @Test
    void equals_shouldReturnFalseForDifferentType() {
        Rating rating = Rating.builder().id(1L).build();

        assertFalse(rating.equals("not-rating"));
    }

    @Test
    void equals_shouldReturnTrueWhenSameNonNullId() {
        Rating rating1 = Rating.builder().id(1L).build();
        Rating rating2 = Rating.builder().id(1L).build();

        assertEquals(rating1, rating2);
    }

    @Test
    void equals_shouldReturnFalseWhenIdIsNull() {
        Rating rating1 = Rating.builder().id(null).build();
        Rating rating2 = Rating.builder().id(1L).build();

        assertNotEquals(rating1, rating2);
    }

    @Test
    void equals_shouldReturnFalseWhenDifferentIds() {
        Rating rating1 = Rating.builder().id(1L).build();
        Rating rating2 = Rating.builder().id(2L).build();

        assertNotEquals(rating1, rating2);
    }

    @Test
    void hashCode_shouldBeClassBased() {
        Rating rating1 = Rating.builder().id(1L).build();
        Rating rating2 = Rating.builder().id(2L).build();

        assertEquals(rating1.hashCode(), rating2.hashCode());
    }
}
