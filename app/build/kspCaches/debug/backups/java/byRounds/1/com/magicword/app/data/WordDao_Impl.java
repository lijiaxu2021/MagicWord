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

  private final EntityInsertionAdapter<Word> __insertionAdapterOfWord;

  private final EntityDeletionOrUpdateAdapter<Word> __deletionAdapterOfWord;

  private final EntityDeletionOrUpdateAdapter<Word> __updateAdapterOfWord;

  public WordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWord = new EntityInsertionAdapter<Word>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `words` (`id`,`word`,`phonetic`,`definitionCn`,`definitionEn`,`example`,`memoryMethod`,`libraryId`,`reviewCount`,`lastReviewTime`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
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
        return "UPDATE OR ABORT `words` SET `id` = ?,`word` = ?,`phonetic` = ?,`definitionCn` = ?,`definitionEn` = ?,`example` = ?,`memoryMethod` = ?,`libraryId` = ?,`reviewCount` = ?,`lastReviewTime` = ? WHERE `id` = ?";
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
        statement.bindLong(11, entity.getId());
      }
    };
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
            _item = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime);
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
  public Object getWordByText(final String word, final int libraryId,
      final Continuation<? super Word> $completion) {
    final String _sql = "SELECT * FROM words WHERE word = ? AND libraryId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, word);
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
            _result = new Word(_tmpId,_tmpWord,_tmpPhonetic,_tmpDefinitionCn,_tmpDefinitionEn,_tmpExample,_tmpMemoryMethod,_tmpLibraryId,_tmpReviewCount,_tmpLastReviewTime);
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
