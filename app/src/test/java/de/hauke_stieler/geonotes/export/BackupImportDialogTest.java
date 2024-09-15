package de.hauke_stieler.geonotes.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BackupImportDialogTest {
    @Test
    public void testVersionCheck() {
        assertTrue(BackupImportDialog.isVersionCompatible(1007003, 1007002));
        assertTrue(BackupImportDialog.isVersionCompatible(1007003, 1007003));
        assertTrue(BackupImportDialog.isVersionCompatible(1007003, 1007004));
        assertTrue(BackupImportDialog.isVersionCompatible(1007003, 1008004));

        assertFalse(BackupImportDialog.isVersionCompatible(1007003, 2007003));
        assertFalse(BackupImportDialog.isVersionCompatible(1006005, 1007003));
        assertFalse(BackupImportDialog.isVersionCompatible(0007003, 1007003));
    }
}
