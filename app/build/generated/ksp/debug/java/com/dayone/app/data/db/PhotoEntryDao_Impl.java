package com.dayone.app.data.db;

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
import java.lang.Float;
import java.lang.Integer;
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
public final class PhotoEntryDao_Impl implements PhotoEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PhotoEntry> __insertionAdapterOfPhotoEntry;

  private final EntityDeletionOrUpdateAdapter<PhotoEntry> __deletionAdapterOfPhotoEntry;

  public PhotoEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPhotoEntry = new EntityInsertionAdapter<PhotoEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `photo_entries` (`id`,`projectId`,`dateEpochDay`,`filePath`,`faceLeft`,`faceTop`,`faceRight`,`faceBottom`,`capturedAtMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getProjectId());
        statement.bindLong(3, entity.getDateEpochDay());
        statement.bindString(4, entity.getFilePath());
        if (entity.getFaceLeft() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getFaceLeft());
        }
        if (entity.getFaceTop() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getFaceTop());
        }
        if (entity.getFaceRight() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getFaceRight());
        }
        if (entity.getFaceBottom() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getFaceBottom());
        }
        statement.bindLong(9, entity.getCapturedAtMillis());
      }
    };
    this.__deletionAdapterOfPhotoEntry = new EntityDeletionOrUpdateAdapter<PhotoEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `photo_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final PhotoEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPhotoEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final PhotoEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPhotoEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PhotoEntry>> observeForProject(final long projectId) {
    final String _sql = "SELECT * FROM photo_entries WHERE projectId = ? ORDER BY dateEpochDay ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, projectId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photo_entries"}, new Callable<List<PhotoEntry>>() {
      @Override
      @NonNull
      public List<PhotoEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "projectId");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFaceLeft = CursorUtil.getColumnIndexOrThrow(_cursor, "faceLeft");
          final int _cursorIndexOfFaceTop = CursorUtil.getColumnIndexOrThrow(_cursor, "faceTop");
          final int _cursorIndexOfFaceRight = CursorUtil.getColumnIndexOrThrow(_cursor, "faceRight");
          final int _cursorIndexOfFaceBottom = CursorUtil.getColumnIndexOrThrow(_cursor, "faceBottom");
          final int _cursorIndexOfCapturedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAtMillis");
          final List<PhotoEntry> _result = new ArrayList<PhotoEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpProjectId;
            _tmpProjectId = _cursor.getLong(_cursorIndexOfProjectId);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final Float _tmpFaceLeft;
            if (_cursor.isNull(_cursorIndexOfFaceLeft)) {
              _tmpFaceLeft = null;
            } else {
              _tmpFaceLeft = _cursor.getFloat(_cursorIndexOfFaceLeft);
            }
            final Float _tmpFaceTop;
            if (_cursor.isNull(_cursorIndexOfFaceTop)) {
              _tmpFaceTop = null;
            } else {
              _tmpFaceTop = _cursor.getFloat(_cursorIndexOfFaceTop);
            }
            final Float _tmpFaceRight;
            if (_cursor.isNull(_cursorIndexOfFaceRight)) {
              _tmpFaceRight = null;
            } else {
              _tmpFaceRight = _cursor.getFloat(_cursorIndexOfFaceRight);
            }
            final Float _tmpFaceBottom;
            if (_cursor.isNull(_cursorIndexOfFaceBottom)) {
              _tmpFaceBottom = null;
            } else {
              _tmpFaceBottom = _cursor.getFloat(_cursorIndexOfFaceBottom);
            }
            final long _tmpCapturedAtMillis;
            _tmpCapturedAtMillis = _cursor.getLong(_cursorIndexOfCapturedAtMillis);
            _item = new PhotoEntry(_tmpId,_tmpProjectId,_tmpDateEpochDay,_tmpFilePath,_tmpFaceLeft,_tmpFaceTop,_tmpFaceRight,_tmpFaceBottom,_tmpCapturedAtMillis);
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
  public Object getLatest(final long projectId,
      final Continuation<? super PhotoEntry> $completion) {
    final String _sql = "SELECT * FROM photo_entries WHERE projectId = ? ORDER BY dateEpochDay DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, projectId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PhotoEntry>() {
      @Override
      @Nullable
      public PhotoEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "projectId");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFaceLeft = CursorUtil.getColumnIndexOrThrow(_cursor, "faceLeft");
          final int _cursorIndexOfFaceTop = CursorUtil.getColumnIndexOrThrow(_cursor, "faceTop");
          final int _cursorIndexOfFaceRight = CursorUtil.getColumnIndexOrThrow(_cursor, "faceRight");
          final int _cursorIndexOfFaceBottom = CursorUtil.getColumnIndexOrThrow(_cursor, "faceBottom");
          final int _cursorIndexOfCapturedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAtMillis");
          final PhotoEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpProjectId;
            _tmpProjectId = _cursor.getLong(_cursorIndexOfProjectId);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final Float _tmpFaceLeft;
            if (_cursor.isNull(_cursorIndexOfFaceLeft)) {
              _tmpFaceLeft = null;
            } else {
              _tmpFaceLeft = _cursor.getFloat(_cursorIndexOfFaceLeft);
            }
            final Float _tmpFaceTop;
            if (_cursor.isNull(_cursorIndexOfFaceTop)) {
              _tmpFaceTop = null;
            } else {
              _tmpFaceTop = _cursor.getFloat(_cursorIndexOfFaceTop);
            }
            final Float _tmpFaceRight;
            if (_cursor.isNull(_cursorIndexOfFaceRight)) {
              _tmpFaceRight = null;
            } else {
              _tmpFaceRight = _cursor.getFloat(_cursorIndexOfFaceRight);
            }
            final Float _tmpFaceBottom;
            if (_cursor.isNull(_cursorIndexOfFaceBottom)) {
              _tmpFaceBottom = null;
            } else {
              _tmpFaceBottom = _cursor.getFloat(_cursorIndexOfFaceBottom);
            }
            final long _tmpCapturedAtMillis;
            _tmpCapturedAtMillis = _cursor.getLong(_cursorIndexOfCapturedAtMillis);
            _result = new PhotoEntry(_tmpId,_tmpProjectId,_tmpDateEpochDay,_tmpFilePath,_tmpFaceLeft,_tmpFaceTop,_tmpFaceRight,_tmpFaceBottom,_tmpCapturedAtMillis);
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
  public Object getForDay(final long projectId, final long epochDay,
      final Continuation<? super PhotoEntry> $completion) {
    final String _sql = "SELECT * FROM photo_entries WHERE projectId = ? AND dateEpochDay = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, projectId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, epochDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PhotoEntry>() {
      @Override
      @Nullable
      public PhotoEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "projectId");
          final int _cursorIndexOfDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "dateEpochDay");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfFaceLeft = CursorUtil.getColumnIndexOrThrow(_cursor, "faceLeft");
          final int _cursorIndexOfFaceTop = CursorUtil.getColumnIndexOrThrow(_cursor, "faceTop");
          final int _cursorIndexOfFaceRight = CursorUtil.getColumnIndexOrThrow(_cursor, "faceRight");
          final int _cursorIndexOfFaceBottom = CursorUtil.getColumnIndexOrThrow(_cursor, "faceBottom");
          final int _cursorIndexOfCapturedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAtMillis");
          final PhotoEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpProjectId;
            _tmpProjectId = _cursor.getLong(_cursorIndexOfProjectId);
            final long _tmpDateEpochDay;
            _tmpDateEpochDay = _cursor.getLong(_cursorIndexOfDateEpochDay);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final Float _tmpFaceLeft;
            if (_cursor.isNull(_cursorIndexOfFaceLeft)) {
              _tmpFaceLeft = null;
            } else {
              _tmpFaceLeft = _cursor.getFloat(_cursorIndexOfFaceLeft);
            }
            final Float _tmpFaceTop;
            if (_cursor.isNull(_cursorIndexOfFaceTop)) {
              _tmpFaceTop = null;
            } else {
              _tmpFaceTop = _cursor.getFloat(_cursorIndexOfFaceTop);
            }
            final Float _tmpFaceRight;
            if (_cursor.isNull(_cursorIndexOfFaceRight)) {
              _tmpFaceRight = null;
            } else {
              _tmpFaceRight = _cursor.getFloat(_cursorIndexOfFaceRight);
            }
            final Float _tmpFaceBottom;
            if (_cursor.isNull(_cursorIndexOfFaceBottom)) {
              _tmpFaceBottom = null;
            } else {
              _tmpFaceBottom = _cursor.getFloat(_cursorIndexOfFaceBottom);
            }
            final long _tmpCapturedAtMillis;
            _tmpCapturedAtMillis = _cursor.getLong(_cursorIndexOfCapturedAtMillis);
            _result = new PhotoEntry(_tmpId,_tmpProjectId,_tmpDateEpochDay,_tmpFilePath,_tmpFaceLeft,_tmpFaceTop,_tmpFaceRight,_tmpFaceBottom,_tmpCapturedAtMillis);
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
  public Object countForProject(final long projectId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM photo_entries WHERE projectId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, projectId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
