package com.tridev.studysaathi.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tridev.studysaathi.data.local.database.StudySaathiDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

public final class CloudBackupPayloadBuilder {

    public static final String BACKUP_FORMAT =
            "study_saathi_backup";

    public static final int BACKUP_FORMAT_VERSION = 1;

    public static final int DATABASE_SCHEMA_VERSION = 7;

    public static final String COMPRESSION_TYPE =
            "gzip";

    public static final String ENCODING_TYPE =
            "base64";

    /*
     * Firestore document limit is 1 MiB.
     * A 450,000-character Base64 string leaves enough
     * room for other document fields and metadata.
     */
    public static final int CHUNK_CHARACTER_LIMIT =
            450_000;

    /*
     * Prevent unexpectedly large cloud uploads.
     *
     * 40 chunks Ãƒâ€” 450,000 characters gives a maximum
     * encoded payload of about 18 million characters.
     */
    public static final int MAX_CHUNK_COUNT = 40;

    /*
     * Local restore currently supports backup files
     * up to 25 MB, so the same raw JSON limit is used.
     */
    private static final int MAX_RAW_JSON_BYTES =
            25 * 1024 * 1024;

    private static final String APP_PREFERENCE_PREFIX =
            "study_saathi_";

    private static final String BACKUP_STATE_PREFERENCES =
            "study_saathi_backup_state";

    private static final String CLOUD_STATE_PREFERENCES =
            "study_saathi_cloud_state";

    private static final String[] DATABASE_TABLES = {
            "student_profiles",
            "lesson_progress",
            "quiz_attempts",
            "doubt_history",
            "school_curriculum_profiles",
            "school_subjects",
            "school_books",
            "school_book_chapters"
    };

    private final Context applicationContext;

    private final StudySaathiDatabase database;

    public CloudBackupPayloadBuilder(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        database =
                StudySaathiDatabase.getInstance(
                        applicationContext
                );
    }

    /**
     * This method performs database and file operations.
     * Always call it from a background executor.
     */
    @NonNull
    public CloudBackupPayload build()
            throws JSONException, IOException {

        long createdAt =
                System.currentTimeMillis();

        ExportedDatabase exportedDatabase =
                exportDatabase();

        ExportedPreferences exportedPreferences =
                exportSharedPreferences();

        JSONObject backupJson =
                createCompleteBackupJson(
                        createdAt,
                        exportedDatabase,
                        exportedPreferences
                );

        byte[] rawJsonBytes =
                backupJson.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        if (rawJsonBytes.length
                > MAX_RAW_JSON_BYTES) {

            throw new IOException(
                    "Cloud backup JSON exceeds the "
                            + "supported 25 MB limit."
            );
        }

        byte[] compressedBytes =
                compressWithGzip(
                        rawJsonBytes
                );

        String encodedPayload =
                Base64.encodeToString(
                        compressedBytes,
                        Base64.NO_WRAP
                );

        List<String> chunks =
                splitIntoChunks(
                        encodedPayload
                );

        if (chunks.size() > MAX_CHUNK_COUNT) {
            throw new IOException(
                    "Cloud backup requires "
                            + chunks.size()
                            + " chunks. Maximum supported "
                            + "chunk count is "
                            + MAX_CHUNK_COUNT
                            + "."
            );
        }

        String checksum =
                createSha256Checksum(
                        compressedBytes
                );

        String backupId =
                createBackupId(
                        createdAt
                );

        BackupOverview backupOverview =
                new BackupOverview(
                        exportedDatabase.profileCount,
                        exportedDatabase.lessonProgressCount,
                        exportedDatabase.quizAttemptCount,
                        exportedDatabase.doubtCount,
                        exportedPreferences.preferenceItemCount
                );

        return new CloudBackupPayload(
                backupId,
                createdAt,
                checksum,
                rawJsonBytes.length,
                compressedBytes.length,
                encodedPayload.length(),
                chunks,
                backupOverview
        );
    }

    @NonNull
    private JSONObject createCompleteBackupJson(
            long createdAt,
            @NonNull ExportedDatabase exportedDatabase,
            @NonNull ExportedPreferences exportedPreferences
    ) throws JSONException {

        JSONObject rootObject =
                new JSONObject();

        rootObject.put(
                "backup_format",
                BACKUP_FORMAT
        );

        rootObject.put(
                "backup_format_version",
                BACKUP_FORMAT_VERSION
        );

        rootObject.put(
                "database_schema_version",
                DATABASE_SCHEMA_VERSION
        );

        rootObject.put(
                "created_at",
                createdAt
        );

        rootObject.put(
                "created_at_iso",
                Instant.ofEpochMilli(createdAt)
                        .toString()
        );

        rootObject.put(
                "package_name",
                applicationContext.getPackageName()
        );

        rootObject.put(
                "backup_target",
                "cloud_firestore"
        );

        rootObject.put(
                "preference_scope",
                "study_saathi_owned_only"
        );

        rootObject.put(
                "database",
                exportedDatabase.databaseJson
        );

        rootObject.put(
                "shared_preferences",
                exportedPreferences.preferencesJson
        );

        return rootObject;
    }

    @NonNull
    private ExportedDatabase exportDatabase()
            throws JSONException {

        SupportSQLiteDatabase readableDatabase =
                database.getOpenHelper()
                        .getWritableDatabase();

        JSONObject databaseObject =
                new JSONObject();

        JSONArray tableArray =
                new JSONArray();

        int profileCount = 0;
        int lessonProgressCount = 0;
        int quizAttemptCount = 0;
        int doubtCount = 0;

        /*
         * All Room tables are read inside one SQLite
         * transaction so they represent one database state.
         */
        readableDatabase.beginTransaction();

        try {
            for (String tableName : DATABASE_TABLES) {
                JSONObject tableObject =
                        exportDatabaseTable(
                                readableDatabase,
                                tableName
                        );

                tableArray.put(
                        tableObject
                );

                int rowCount =
                        tableObject.getInt(
                                "row_count"
                        );

                switch (tableName) {
                    case "student_profiles":
                        profileCount = rowCount;
                        break;

                    case "lesson_progress":
                        lessonProgressCount = rowCount;
                        break;

                    case "quiz_attempts":
                        quizAttemptCount = rowCount;
                        break;

                    case "doubt_history":
                        doubtCount = rowCount;
                        break;

                    default:
                        break;
                }
            }

            readableDatabase.setTransactionSuccessful();

        } finally {
            readableDatabase.endTransaction();
        }

        databaseObject.put(
                "database_name",
                "study_saathi_database"
        );

        databaseObject.put(
                "schema_version",
                DATABASE_SCHEMA_VERSION
        );

        databaseObject.put(
                "tables",
                tableArray
        );

        return new ExportedDatabase(
                databaseObject,
                profileCount,
                lessonProgressCount,
                quizAttemptCount,
                doubtCount
        );
    }

    @NonNull
    private JSONObject exportDatabaseTable(
            @NonNull SupportSQLiteDatabase readableDatabase,
            @NonNull String tableName
    ) throws JSONException {

        JSONObject tableObject =
                new JSONObject();

        JSONArray rowsArray =
                new JSONArray();

        String query =
                "SELECT * FROM `"
                        + tableName
                        + "`";

        try (Cursor cursor =
                     readableDatabase.query(query)) {

            String[] columnNames =
                    cursor.getColumnNames();

            while (cursor.moveToNext()) {
                JSONObject rowObject =
                        new JSONObject();

                for (int columnIndex = 0;
                     columnIndex < columnNames.length;
                     columnIndex++) {

                    rowObject.put(
                            columnNames[columnIndex],
                            getCursorValue(
                                    cursor,
                                    columnIndex
                            )
                    );
                }

                rowsArray.put(
                        rowObject
                );
            }
        }

        tableObject.put(
                "table_name",
                tableName
        );

        tableObject.put(
                "row_count",
                rowsArray.length()
        );

        tableObject.put(
                "rows",
                rowsArray
        );

        return tableObject;
    }

    private Object getCursorValue(
            @NonNull Cursor cursor,
            int columnIndex
    ) {
        switch (cursor.getType(columnIndex)) {
            case Cursor.FIELD_TYPE_NULL:
                return JSONObject.NULL;

            case Cursor.FIELD_TYPE_INTEGER:
                return cursor.getLong(
                        columnIndex
                );

            case Cursor.FIELD_TYPE_FLOAT:
                return cursor.getDouble(
                        columnIndex
                );

            case Cursor.FIELD_TYPE_BLOB:
                return Base64.encodeToString(
                        cursor.getBlob(
                                columnIndex
                        ),
                        Base64.NO_WRAP
                );

            case Cursor.FIELD_TYPE_STRING:
            default:
                return cursor.getString(
                        columnIndex
                );
        }
    }

    @NonNull
    private ExportedPreferences exportSharedPreferences()
            throws JSONException {

        JSONArray preferenceFilesArray =
                new JSONArray();

        int preferenceItemCount = 0;

        for (File preferenceFile
                : getStudySaathiPreferenceFiles()) {

            String preferenceName =
                    removeXmlExtension(
                            preferenceFile.getName()
                    );

            SharedPreferences sharedPreferences =
                    applicationContext
                            .getSharedPreferences(
                                    preferenceName,
                                    Context.MODE_PRIVATE
                            );

            Map<String, ?> preferenceValues =
                    sharedPreferences.getAll();

            List<String> sortedKeys =
                    new ArrayList<>(
                            preferenceValues.keySet()
                    );

            Collections.sort(
                    sortedKeys
            );

            JSONObject entriesObject =
                    new JSONObject();

            for (String key : sortedKeys) {
                entriesObject.put(
                        key,
                        createPreferenceValueObject(
                                preferenceValues.get(key)
                        )
                );
            }

            JSONObject preferenceFileObject =
                    new JSONObject();

            preferenceFileObject.put(
                    "preference_name",
                    preferenceName
            );

            preferenceFileObject.put(
                    "entry_count",
                    entriesObject.length()
            );

            preferenceFileObject.put(
                    "entries",
                    entriesObject
            );

            preferenceFilesArray.put(
                    preferenceFileObject
            );

            preferenceItemCount +=
                    entriesObject.length();
        }

        return new ExportedPreferences(
                preferenceFilesArray,
                preferenceItemCount
        );
    }

    @NonNull
    private JSONObject createPreferenceValueObject(
            Object value
    ) throws JSONException {

        JSONObject valueObject =
                new JSONObject();

        if (value == null) {
            valueObject.put(
                    "type",
                    "null"
            );

            valueObject.put(
                    "value",
                    JSONObject.NULL
            );

            return valueObject;
        }

        if (value instanceof String) {
            valueObject.put(
                    "type",
                    "string"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Integer) {
            valueObject.put(
                    "type",
                    "integer"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Long) {
            valueObject.put(
                    "type",
                    "long"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Float) {
            valueObject.put(
                    "type",
                    "float"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Boolean) {
            valueObject.put(
                    "type",
                    "boolean"
            );

            valueObject.put(
                    "value",
                    value
            );

            return valueObject;
        }

        if (value instanceof Set<?>) {
            valueObject.put(
                    "type",
                    "string_set"
            );

            List<String> sortedValues =
                    new ArrayList<>();

            for (Object setValue
                    : (Set<?>) value) {

                sortedValues.add(
                        String.valueOf(
                                setValue
                        )
                );
            }

            Collections.sort(
                    sortedValues
            );

            JSONArray setArray =
                    new JSONArray();

            for (String setValue
                    : sortedValues) {

                setArray.put(
                        setValue
                );
            }

            valueObject.put(
                    "value",
                    setArray
            );

            return valueObject;
        }

        valueObject.put(
                "type",
                "string"
        );

        valueObject.put(
                "value",
                String.valueOf(value)
        );

        return valueObject;
    }

    @NonNull
    private List<File> getStudySaathiPreferenceFiles() {
        File sharedPreferencesDirectory =
                new File(
                        applicationContext
                                .getApplicationInfo()
                                .dataDir,
                        "shared_prefs"
                );

        File[] preferenceFiles =
                sharedPreferencesDirectory.listFiles(
                        (directory, fileName) -> {
                            if (fileName == null
                                    || !fileName.endsWith(
                                    ".xml"
                            )) {
                                return false;
                            }

                            String preferenceName =
                                    removeXmlExtension(
                                            fileName
                                    );

                            return isCloudBackupPreference(
                                    preferenceName
                            );
                        }
                );

        if (preferenceFiles == null
                || preferenceFiles.length == 0) {

            return new ArrayList<>();
        }

        Arrays.sort(
                preferenceFiles,
                Comparator.comparing(
                        File::getName
                )
        );

        return new ArrayList<>(
                Arrays.asList(
                        preferenceFiles
                )
        );
    }

    private boolean isCloudBackupPreference(
            @NonNull String preferenceName
    ) {
        if (!preferenceName.startsWith(
                APP_PREFERENCE_PREFIX
        )) {
            return false;
        }

        if (BACKUP_STATE_PREFERENCES.equals(
                preferenceName
        )) {
            return false;
        }

        return !CLOUD_STATE_PREFERENCES.equals(
                preferenceName
        );
    }

    @NonNull
    private String removeXmlExtension(
            @NonNull String fileName
    ) {
        if (fileName.endsWith(".xml")
                && fileName.length() > 4) {

            return fileName.substring(
                    0,
                    fileName.length() - 4
            );
        }

        return fileName;
    }

    @NonNull
    private byte[] compressWithGzip(
            @NonNull byte[] sourceBytes
    ) throws IOException {

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream();

             GZIPOutputStream gzipOutputStream =
                     new GZIPOutputStream(
                             outputStream
                     )) {

            gzipOutputStream.write(
                    sourceBytes
            );

            gzipOutputStream.finish();

            return outputStream.toByteArray();
        }
    }

    @NonNull
    private List<String> splitIntoChunks(
            @NonNull String encodedPayload
    ) {
        if (encodedPayload.isEmpty()) {
            return Collections.singletonList("");
        }

        List<String> chunks =
                new ArrayList<>();

        int payloadLength =
                encodedPayload.length();

        int startIndex = 0;

        while (startIndex < payloadLength) {
            int endIndex =
                    Math.min(
                            startIndex
                                    + CHUNK_CHARACTER_LIMIT,
                            payloadLength
                    );

            chunks.add(
                    encodedPayload.substring(
                            startIndex,
                            endIndex
                    )
            );

            startIndex = endIndex;
        }

        return chunks;
    }

    @NonNull
    private String createSha256Checksum(
            @NonNull byte[] compressedBytes
    ) throws IOException {

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] checksumBytes =
                    messageDigest.digest(
                            compressedBytes
                    );

            StringBuilder checksumBuilder =
                    new StringBuilder(
                            checksumBytes.length * 2
                    );

            for (byte checksumByte
                    : checksumBytes) {

                checksumBuilder.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                checksumByte & 0xff
                        )
                );
            }

            return checksumBuilder.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IOException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    @NonNull
    private String createBackupId(
            long createdAt
    ) {
        String randomPart =
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        );

        return "backup_"
                + createdAt
                + "_"
                + randomPart;
    }

    public static final class CloudBackupPayload {

        private final String backupId;

        private final long createdAt;

        private final String checksumSha256;

        private final int rawJsonBytes;

        private final int compressedBytes;

        private final int encodedCharacters;

        private final List<String> chunks;

        private final BackupOverview overview;

        private CloudBackupPayload(
                @NonNull String backupId,
                long createdAt,
                @NonNull String checksumSha256,
                int rawJsonBytes,
                int compressedBytes,
                int encodedCharacters,
                @NonNull List<String> chunks,
                @NonNull BackupOverview overview
        ) {
            this.backupId = backupId;
            this.createdAt = createdAt;
            this.checksumSha256 =
                    checksumSha256;
            this.rawJsonBytes =
                    rawJsonBytes;
            this.compressedBytes =
                    compressedBytes;
            this.encodedCharacters =
                    encodedCharacters;
            this.chunks =
                    Collections.unmodifiableList(
                            new ArrayList<>(chunks)
                    );
            this.overview = overview;
        }

        @NonNull
        public String getBackupId() {
            return backupId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        @NonNull
        public String getChecksumSha256() {
            return checksumSha256;
        }

        public int getRawJsonBytes() {
            return rawJsonBytes;
        }

        public int getCompressedBytes() {
            return compressedBytes;
        }

        public int getEncodedCharacters() {
            return encodedCharacters;
        }

        public int getChunkCount() {
            return chunks.size();
        }

        @NonNull
        public List<String> getChunks() {
            return chunks;
        }

        @NonNull
        public BackupOverview getOverview() {
            return overview;
        }

        @NonNull
        public String getBackupFormat() {
            return BACKUP_FORMAT;
        }

        public int getBackupFormatVersion() {
            return BACKUP_FORMAT_VERSION;
        }

        public int getDatabaseSchemaVersion() {
            return DATABASE_SCHEMA_VERSION;
        }

        @NonNull
        public String getCompressionType() {
            return COMPRESSION_TYPE;
        }

        @NonNull
        public String getEncodingType() {
            return ENCODING_TYPE;
        }
    }

    public static final class BackupOverview {

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private final int preferenceItemCount;

        private BackupOverview(
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount,
                int preferenceItemCount
        ) {
            this.profileCount =
                    profileCount;

            this.lessonProgressCount =
                    lessonProgressCount;

            this.quizAttemptCount =
                    quizAttemptCount;

            this.doubtCount =
                    doubtCount;

            this.preferenceItemCount =
                    preferenceItemCount;
        }

        public int getProfileCount() {
            return profileCount;
        }

        public int getLessonProgressCount() {
            return lessonProgressCount;
        }

        public int getQuizAttemptCount() {
            return quizAttemptCount;
        }

        public int getDoubtCount() {
            return doubtCount;
        }

        public int getPreferenceItemCount() {
            return preferenceItemCount;
        }

        public int getTotalDatabaseRows() {
            return profileCount
                    + lessonProgressCount
                    + quizAttemptCount
                    + doubtCount;
        }
    }

    private static final class ExportedDatabase {

        private final JSONObject databaseJson;

        private final int profileCount;

        private final int lessonProgressCount;

        private final int quizAttemptCount;

        private final int doubtCount;

        private ExportedDatabase(
                @NonNull JSONObject databaseJson,
                int profileCount,
                int lessonProgressCount,
                int quizAttemptCount,
                int doubtCount
        ) {
            this.databaseJson =
                    databaseJson;

            this.profileCount =
                    profileCount;

            this.lessonProgressCount =
                    lessonProgressCount;

            this.quizAttemptCount =
                    quizAttemptCount;

            this.doubtCount =
                    doubtCount;
        }
    }

    private static final class ExportedPreferences {

        private final JSONArray preferencesJson;

        private final int preferenceItemCount;

        private ExportedPreferences(
                @NonNull JSONArray preferencesJson,
                int preferenceItemCount
        ) {
            this.preferencesJson =
                    preferencesJson;

            this.preferenceItemCount =
                    preferenceItemCount;
        }
    }
}

