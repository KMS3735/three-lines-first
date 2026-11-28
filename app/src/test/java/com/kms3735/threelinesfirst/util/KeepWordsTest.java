package com.kms3735.threelinesfirst.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeepWordsTest {

    @Test
    public void joinsLettersInsideWordsOnly() {
        assertEquals("안⁠내⁠문 이⁠에⁠요⁠.", KeepWords.join("안내문 이에요."));
    }

    @Test
    public void keepsSurrogatePairsTogether() {
        String emoji = "📅";
        assertEquals("가⁠" + emoji, KeepWords.join("가" + emoji));
    }

    @Test
    public void joiningTwiceChangesNothing() {
        String once = KeepWords.join("챙길 안내문이 2개 있어요");
        assertEquals(once, KeepWords.join(once));
        assertTrue(KeepWords.needsJoin("있어요"));
        assertFalse(KeepWords.needsJoin(once));
    }
}
