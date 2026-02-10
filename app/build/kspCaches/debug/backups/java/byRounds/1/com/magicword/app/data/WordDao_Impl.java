package com.magicword.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WordDao_Impl implements WordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Library> __insertionAdapterOfLibrary;

  private final EntityInsertionAdapter<Word> __insertionAdapterOfWord;

  private final EntityInsertionAdapter<TestHistory> __insertionAdapterOfTestHistory;

  private final EntityInsertionAdapter<TestSession> __insertionAdapterOfTestSession;

  private final EntityDeletionOrUpdateAdapter<Word> __deletionAdapterOfWord;

  private final EntityDeletionOrUpdateAdapter<Word> __updateAdapterOfWord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteWordById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLibraryLastIndex;

  private final SharedSQLiteStatement __preparedStmtOfUpdateWordStats;

  private final SharedSQLiteStatement __preparedStmtOfClearTestSession;

  public WordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLibrary = new EntityInsertionAdapter<Library>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `libraries` (`id`,`name`,`description`,`createdAt`,`lastIndex`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Library entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getLastIndex());
      }
    };
    this.__insertionAdapterOfWord = new EntityInsertionAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `words` (`id`,`word`,`phonetic`,`definitionCn`,`definitionEn`,`example`,`memoryMethod`,`libraryId`,`reviewCount`,`lastReviewTime`,`createdAt`,`correctCount`,`incorrectCount`,`sortOrder`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Word entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getWord());
        if (entity.getPhonetic() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhonetic());
        }
        statement.bindString(4, entity.getDefinitionCn());
        if (entity.getDefinitionEn() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDefinitionEn());
        }
        if (entity.getExample() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getExample());
        }
        if (entity.getMemoryMethod() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMemoryMethod());
        }
        statement.bindLong(8, entity.getLibraryId());
        statement.bindLong(9, entity.getReviewCount());
        statement.bindLong(10, entity.getLastReviewTime());
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getCorrectCount());
        statement.bindLong(13, entity.getIncorrectCount());
        statement.bindLong(14, entity.getSortOrder());
      }
    };
    this.__insertionAdapterOfTestHistory = new EntityInsertionAdapter<TestHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `test_history` (`id`,`timestamp`,`totalQuestions`,`correctCount`,`testType`,`durationSeconds`,`questionsJson`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TestHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindLong(3, entity.getTotalQuestions());
        statement.bindLong(4, entity.getCorrectCount());
        statement.bindString(5, entity.getTestType());
        statement.bindLong(6, entity.getDurationSeconds());
        statement.bindString(7, entity.getQuestionsJson());
      }
    };
    this.__insertionAdapterOfTestSession = new EntityInsertionAdapter<TestSession>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `test_session` (`id`,`currentIndex`,`score`,`isFinished`,`shuffledIndicesJson`,`testType`,`libraryId`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TestSession entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCurrentIndex());
        statement.bindLong(3, entity.getScore());
        final int _tmp = entity.isFinished() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindString(5, entity.getShuffledIndicesJson());
        statement.bindString(6, entity.getTestType());
        statement.bindLong(7, entity.getLibraryId());
      }
    };
    this.__deletionAdapterOfWord = new EntityDeletionOrUpdateAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `words` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Word entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfWord = new EntityDeletionOrUpdateAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `words` SET `id` = ?,`word` = ?,`phonetic` = ?,`definitionCn` = ?,`definitionEn` = ?,`example` = ?,`memoryMethod` = ?,`libraryId` = ?,`reviewCount` = ?,`lastReviewTime` = ?,`createdAt` = ?,`correctCount` = ?,`incorrectCount` = ?,`sortOrder` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Word entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getWord());
        if (entity.getPhonetic() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhonetic());
        }
        statement.bindString(4, entity.getDefinitionCn());
        if (entity.getDefinitionEn() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDefinitionEn());
        }
        if (entity.getExample() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getExample());
        }
        if (entity.getMemoryMethod() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMemoryMethod());
        }
        statement.bindLong(8, entity.getLibraryId());
        statement.bindLong(9, entity.getReviewCount());
        statement.bindLong(10, entity.getLastReviewTime());
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getCorrectCount());
        statement.bindLong(13, entity.getIncorrectCount());
        statement.bindLong(14, entity.getSortOrder());
        statement.bindLong(15, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteWordById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM words WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLibraryLastIndex = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE libraries SET lastIndex = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateWordStats = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE words SET reviewCount = reviewCount + 1, correctCount = correctCount + ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearTestSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM test_session WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLibrary(final Library library, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfLibrary.insertAndReturnId(library);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertWord(final Word word, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWord.insertAndReturnId(word);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTestHistory(final TestHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTestHistory.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveTestSession(final TestSession session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTestSession.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteWord(final Word word, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWord.handle(word);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateWord(final Word word, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWord.handle(word);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteWordById(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteWordById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteWordById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLibraryLastIndex(final int libraryId, final int index,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLibraryLastIndex.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, index);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, libraryId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLibraryLastIndex.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateWordStats(final int id, final int correct,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateWordStats.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, correct);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateWordStats.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearTestSession(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearTestSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearTestSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Word>> getWordsByLibrary(final int libraryId) {
    final String _sql = "SELECT * FROM words WHERE libraryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, libraryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"words"}, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfDefinitionCn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionCn");
          final int _cursorIndexOfDefinitionEn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionEn");
          final int _cursorIndexOfExample = CursorUtil.getColumnIndexOrThrow(_cursor, "example");
          final int _cursorIndexOfMemoryMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "memoryMethod");
          final int _cursorIndexOfLibraryId = CursorUtil.getColumnIndexOrThrow(_cursor, "libraryId");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewCount");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfIncorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "incorrectCount");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpDefinitionCn;
            _tmpDefinitionCn = _cursor.getString(_cursorIndexOfDefinitionCn);
            final String _tmpDefinitionEn;
            if (_cursor.isNull(_cursorIndexOfDefinitionEn)) {
              _tmpDefinitionEn = null;
            } else {
              _tmpDefinitionEn = _cursor.getString(_cursorIndexOfDefinitionEn);
            }
            final String _tmpExample;
            if (_cursor.isNull(_cursorIndexOfExample)) {
              _tmpExample = null;
            } else {
              _tmpExample = _cursor.getString(_cursorIndexOfExample);
            }
            final String _tmpMemoryMethod;
            if (_cursor.isNull(_cursorIndexOfMemoryMethod)) {
              _tmpMemoryMethod = null;
            } else {
              _tmpMemoryMethod = _cursor.getString(_cursorIndexOfMemoryMethod);
            }
            final int _tmpLibraryId;
            _tmpLibraryId = _cursor.getInt(_cursorIndexOfLibraryId);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpIncorrectCount;
            _tmpIncorrectCount = _cursor.getInt(_cursorIndexOfIncorrectCount);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime,_tmpCreatedAt,_tmpCorrectCount,_tmpIncorrectCount,_tmpSortOrder);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Library>> getAllLibraries() {
    final String _sql = "SELECT * FROM libraries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"libraries"}, new Callable<List<Library>>() {
      @Override
      @NonNull
      public List<Library> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "lastIndex");
          final List<Library> _result = new ArrayList<Library>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Library _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpLastIndex;
            _tmpLastIndex = _cursor.getInt(_cursorIndexOfLastIndex);
            _item = new Library(_tmpId,_tmpName,_tmpDescription,_tmpCreatedAt,_tmpLastIndex);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLibraryById(final int id, final Continuation<? super Library> $completion) {
    final String _sql = "SELECT * FROM libraries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Library>() {
      @Override
      @Nullable
      public Library call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "lastIndex");
          final Library _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpLastIndex;
            _tmpLastIndex = _cursor.getInt(_cursorIndexOfLastIndex);
            _result = new Library(_tmpId,_tmpName,_tmpDescription,_tmpCreatedAt,_tmpLastIndex);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWordByText(final String text, final int libraryId,
      final Continuation<? super Word> $completion) {
    final String _sql = "SELECT * FROM words WHERE word = ? AND libraryId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, text);
    _argIndex = 2;
    _statement.bindLong(_argIndex, libraryId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Word>() {
      @Override
      @Nullable
      public Word call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfDefinitionCn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionCn");
          final int _cursorIndexOfDefinitionEn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionEn");
          final int _cursorIndexOfExample = CursorUtil.getColumnIndexOrThrow(_cursor, "example");
          final int _cursorIndexOfMemoryMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "memoryMethod");
          final int _cursorIndexOfLibraryId = CursorUtil.getColumnIndexOrThrow(_cursor, "libraryId");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewCount");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfIncorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "incorrectCount");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final Word _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpDefinitionCn;
            _tmpDefinitionCn = _cursor.getString(_cursorIndexOfDefinitionCn);
            final String _tmpDefinitionEn;
            if (_cursor.isNull(_cursorIndexOfDefinitionEn)) {
              _tmpDefinitionEn = null;
            } else {
              _tmpDefinitionEn = _cursor.getString(_cursorIndexOfDefinitionEn);
            }
            final String _tmpExample;
            if (_cursor.isNull(_cursorIndexOfExample)) {
              _tmpExample = null;
            } else {
              _tmpExample = _cursor.getString(_cursorIndexOfExample);
            }
            final String _tmpMemoryMethod;
            if (_cursor.isNull(_cursorIndexOfMemoryMethod)) {
              _tmpMemoryMethod = null;
            } else {
              _tmpMemoryMethod = _cursor.getString(_cursorIndexOfMemoryMethod);
            }
            final int _tmpLibraryId;
            _tmpLibraryId = _cursor.getInt(_cursorIndexOfLibraryId);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpIncorrectCount;
            _tmpIncorrectCount = _cursor.getInt(_cursorIndexOfIncorrectCount);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _result = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime,_tmpCreatedAt,_tmpCorrectCount,_tmpIncorrectCount,_tmpSortOrder);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWordsByLibraryList(final int libraryId,
      final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words WHERE libraryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, libraryId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfDefinitionCn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionCn");
          final int _cursorIndexOfDefinitionEn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionEn");
          final int _cursorIndexOfExample = CursorUtil.getColumnIndexOrThrow(_cursor, "example");
          final int _cursorIndexOfMemoryMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "memoryMethod");
          final int _cursorIndexOfLibraryId = CursorUtil.getColumnIndexOrThrow(_cursor, "libraryId");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewCount");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfIncorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "incorrectCount");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpDefinitionCn;
            _tmpDefinitionCn = _cursor.getString(_cursorIndexOfDefinitionCn);
            final String _tmpDefinitionEn;
            if (_cursor.isNull(_cursorIndexOfDefinitionEn)) {
              _tmpDefinitionEn = null;
            } else {
              _tmpDefinitionEn = _cursor.getString(_cursorIndexOfDefinitionEn);
            }
            final String _tmpExample;
            if (_cursor.isNull(_cursorIndexOfExample)) {
              _tmpExample = null;
            } else {
              _tmpExample = _cursor.getString(_cursorIndexOfExample);
            }
            final String _tmpMemoryMethod;
            if (_cursor.isNull(_cursorIndexOfMemoryMethod)) {
              _tmpMemoryMethod = null;
            } else {
              _tmpMemoryMethod = _cursor.getString(_cursorIndexOfMemoryMethod);
            }
            final int _tmpLibraryId;
            _tmpLibraryId = _cursor.getInt(_cursorIndexOfLibraryId);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpIncorrectCount;
            _tmpIncorrectCount = _cursor.getInt(_cursorIndexOfIncorrectCount);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime,_tmpCreatedAt,_tmpCorrectCount,_tmpIncorrectCount,_tmpSortOrder);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllWordsList(final Continuation<? super List<Word>> $completion) {
    final String _sql = "SELECT * FROM words";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Word>>() {
      @Override
      @NonNull
      public List<Word> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfPhonetic = CursorUtil.getColumnIndexOrThrow(_cursor, "phonetic");
          final int _cursorIndexOfDefinitionCn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionCn");
          final int _cursorIndexOfDefinitionEn = CursorUtil.getColumnIndexOrThrow(_cursor, "definitionEn");
          final int _cursorIndexOfExample = CursorUtil.getColumnIndexOrThrow(_cursor, "example");
          final int _cursorIndexOfMemoryMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "memoryMethod");
          final int _cursorIndexOfLibraryId = CursorUtil.getColumnIndexOrThrow(_cursor, "libraryId");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reviewCount");
          final int _cursorIndexOfLastReviewTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReviewTime");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfIncorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "incorrectCount");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<Word> _result = new ArrayList<Word>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Word _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpPhonetic;
            if (_cursor.isNull(_cursorIndexOfPhonetic)) {
              _tmpPhonetic = null;
            } else {
              _tmpPhonetic = _cursor.getString(_cursorIndexOfPhonetic);
            }
            final String _tmpDefinitionCn;
            _tmpDefinitionCn = _cursor.getString(_cursorIndexOfDefinitionCn);
            final String _tmpDefinitionEn;
            if (_cursor.isNull(_cursorIndexOfDefinitionEn)) {
              _tmpDefinitionEn = null;
            } else {
              _tmpDefinitionEn = _cursor.getString(_cursorIndexOfDefinitionEn);
            }
            final String _tmpExample;
            if (_cursor.isNull(_cursorIndexOfExample)) {
              _tmpExample = null;
            } else {
              _tmpExample = _cursor.getString(_cursorIndexOfExample);
            }
            final String _tmpMemoryMethod;
            if (_cursor.isNull(_cursorIndexOfMemoryMethod)) {
              _tmpMemoryMethod = null;
            } else {
              _tmpMemoryMethod = _cursor.getString(_cursorIndexOfMemoryMethod);
            }
            final int _tmpLibraryId;
            _tmpLibraryId = _cursor.getInt(_cursorIndexOfLibraryId);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final long _tmpLastReviewTime;
            _tmpLastReviewTime = _cursor.getLong(_cursorIndexOfLastReviewTime);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpIncorrectCount;
            _tmpIncorrectCount = _cursor.getInt(_cursorIndexOfIncorrectCount);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime,_tmpCreatedAt,_tmpCorrectCount,_tmpIncorrectCount,_tmpSortOrder);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TestHistory>> getAllTestHistory() {
    final String _sql = "SELECT * FROM test_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"test_history"}, new Callable<List<TestHistory>>() {
      @Override
      @NonNull
      public List<TestHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalQuestions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestions");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfTestType = CursorUtil.getColumnIndexOrThrow(_cursor, "testType");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfQuestionsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "questionsJson");
          final List<TestHistory> _result = new ArrayList<TestHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TestHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpTotalQuestions;
            _tmpTotalQuestions = _cursor.getInt(_cursorIndexOfTotalQuestions);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final String _tmpTestType;
            _tmpTestType = _cursor.getString(_cursorIndexOfTestType);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpQuestionsJson;
            _tmpQuestionsJson = _cursor.getString(_cursorIndexOfQuestionsJson);
            _item = new TestHistory(_tmpId,_tmpTimestamp,_tmpTotalQuestions,_tmpCorrectCount,_tmpTestType,_tmpDurationSeconds,_tmpQuestionsJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTestSession(final int id, final Continuation<? super TestSession> $completion) {
    final String _sql = "SELECT * FROM test_session WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TestSession>() {
      @Override
      @Nullable
      public TestSession call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCurrentIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "currentIndex");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfIsFinished = CursorUtil.getColumnIndexOrThrow(_cursor, "isFinished");
          final int _cursorIndexOfShuffledIndicesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "shuffledIndicesJson");
          final int _cursorIndexOfTestType = CursorUtil.getColumnIndexOrThrow(_cursor, "testType");
          final int _cursorIndexOfLibraryId = CursorUtil.getColumnIndexOrThrow(_cursor, "libraryId");
          final TestSession _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCurrentIndex;
            _tmpCurrentIndex = _cursor.getInt(_cursorIndexOfCurrentIndex);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final boolean _tmpIsFinished;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFinished);
            _tmpIsFinished = _tmp != 0;
            final String _tmpShuffledIndicesJson;
            _tmpShuffledIndicesJson = _cursor.getString(_cursorIndexOfShuffledIndicesJson);
            final String _tmpTestType;
            _tmpTestType = _cursor.getString(_cursorIndexOfTestType);
            final int _tmpLibraryId;
            _tmpLibraryId = _cursor.getInt(_cursorIndexOfLibraryId);
            _result = new TestSession(_tmpId,_tmpCurrentIndex,_tmpScore,_tmpIsFinished,_tmpShuffledIndicesJson,_tmpTestType,_tmpLibraryId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
