package com.inpetu.connect.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSqlTest {
    @Test
    void importSqlDoesNotInsertUsers() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/import.sql"));
        Pattern userInsert = Pattern.compile(
            "(?is)\\bINSERT\\b[^;]*\\busuario\\b"
        );

        assertFalse(userInsert.matcher(sql).find(), "import.sql must not insert users");
    }

    @Test
    void importSqlSeedsTheGeneralWeeklyScheduleIdempotently() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/import.sql"));

        assertTrue(sql.contains("INSERT IGNORE INTO horario_funcionamento"));
        for (int day = 1; day <= 6; day++) {
            assertTrue(sql.contains("(" + day + ", '08:00:00', '22:00:00', false, false)"));
        }
        assertTrue(sql.contains("(7, '08:00:00', '22:00:00', false, true)"));
    }
}
