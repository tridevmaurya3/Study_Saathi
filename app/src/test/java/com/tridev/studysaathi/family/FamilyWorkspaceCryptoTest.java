package com.tridev.studysaathi.family;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FamilyWorkspaceCryptoTest {
    @Test public void inviteCodeNormalizationIsStableAcrossParents() {
        assertEquals("ABCD2345XY", FamilyWorkspaceCrypto.normalizeCode("ab-cd 2345 xy"));
    }

    @Test public void inviteCodeNormalizationRejectsFormattingCharacters() {
        assertEquals("PARENT2026", FamilyWorkspaceCrypto.normalizeCode(" Parent-2026 "));
    }
}
