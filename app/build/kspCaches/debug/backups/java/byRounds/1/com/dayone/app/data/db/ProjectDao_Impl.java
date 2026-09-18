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
public final class ProjectDao_Impl implements ProjectDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Project> __insertionAdapterOfProject;

  private final EntityDeletionOrUpdateAdapter<Project> __deletionAdapterOfProject;

  private final EntityDeletionOrUpdateAdapter<Project> __updateAdapterOfProject;

  public ProjectDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProject = new EntityInsertionAdapter<Project>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `projects` (`id`,`name`,`folderName`,`startDateMillis`,`birthDateMillis`,`reminderMinuteOfDay`,`reminderEnabled`,`colorArgb`,`createdAtMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Project entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFolderName());
        statement.bindLong(4, entity.getStartDateMillis());
        if (entity.getBirthDateMillis() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getBirthDateMillis());
        }
        statement.bindLong(6, entity.getReminderMinuteOfDay());
        final int _tmp = entity.getReminderEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getColorArgb());
        statement.bindLong(9, entity.getCreatedAtMillis());
      }
    };
    this.__deletionAdapterOfProject = new EntityDeletionOrUpdateAdapter<Project>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `projects` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Project entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfProject = new EntityDeletionOrUpdateAdapter<Project>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `projects` SET `id` = ?,`name` = ?,`folderName` = ?,`startDateMillis` = ?,`birthDateMillis` = ?,`reminderMinuteOfDay` = ?,`reminderEnabled` = ?,`colorArgb` = ?,`createdAtMillis` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Project entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFolderName());
        statement.bindLong(4, entity.getStartDateMillis());
        if (entity.getBirthDateMillis() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getBirthDateMillis());
        }
        statement.bindLong(6, entity.getReminderMinuteOfDay());
        final int _tmp = entity.getReminderEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getColorArgb());
        statement.bindLong(9, entity.getCreatedAtMillis());
        statement.bindLong(10, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final Project project, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfProject.insertAndReturnId(project);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Project project, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfProject.handle(project);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Project project, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfProject.handle(project);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Project>> observeAll() {
    final String _sql = "SELECT * FROM projects ORDER BY createdAtMillis ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"projects"}, new Callable<List<Project>>() {
      @Override
      @NonNull
      public List<Project> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFolderName = CursorUtil.getColumnIndexOrThrow(_cursor, "folderName");
          final int _cursorIndexOfStartDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateMillis");
          final int _cursorIndexOfBirthDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDateMillis");
          final int _cursorIndexOfReminderMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinuteOfDay");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfColorArgb = CursorUtil.getColumnIndexOrThrow(_cursor, "colorArgb");
          final int _cursorIndexOfCreatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMillis");
          final List<Project> _result = new ArrayList<Project>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Project _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFolderName;
            _tmpFolderName = _cursor.getString(_cursorIndexOfFolderName);
            final long _tmpStartDateMillis;
            _tmpStartDateMillis = _cursor.getLong(_cursorIndexOfStartDateMillis);
            final Long _tmpBirthDateMillis;
            if (_cursor.isNull(_cursorIndexOfBirthDateMillis)) {
              _tmpBirthDateMillis = null;
            } else {
              _tmpBirthDateMillis = _cursor.getLong(_cursorIndexOfBirthDateMillis);
            }
            final int _tmpReminderMinuteOfDay;
            _tmpReminderMinuteOfDay = _cursor.getInt(_cursorIndexOfReminderMinuteOfDay);
            final boolean _tmpReminderEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp != 0;
            final int _tmpColorArgb;
            _tmpColorArgb = _cursor.getInt(_cursorIndexOfColorArgb);
            final long _tmpCreatedAtMillis;
            _tmpCreatedAtMillis = _cursor.getLong(_cursorIndexOfCreatedAtMillis);
            _item = new Project(_tmpId,_tmpName,_tmpFolderName,_tmpStartDateMillis,_tmpBirthDateMillis,_tmpReminderMinuteOfDay,_tmpReminderEnabled,_tmpColorArgb,_tmpCreatedAtMillis);
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
  public Object getById(final long id, final Continuation<? super Project> $completion) {
    final String _sql = "SELECT * FROM projects WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Project>() {
      @Override
      @Nullable
      public Project call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFolderName = CursorUtil.getColumnIndexOrThrow(_cursor, "folderName");
          final int _cursorIndexOfStartDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateMillis");
          final int _cursorIndexOfBirthDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDateMillis");
          final int _cursorIndexOfReminderMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinuteOfDay");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfColorArgb = CursorUtil.getColumnIndexOrThrow(_cursor, "colorArgb");
          final int _cursorIndexOfCreatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMillis");
          final Project _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFolderName;
            _tmpFolderName = _cursor.getString(_cursorIndexOfFolderName);
            final long _tmpStartDateMillis;
            _tmpStartDateMillis = _cursor.getLong(_cursorIndexOfStartDateMillis);
            final Long _tmpBirthDateMillis;
            if (_cursor.isNull(_cursorIndexOfBirthDateMillis)) {
              _tmpBirthDateMillis = null;
            } else {
              _tmpBirthDateMillis = _cursor.getLong(_cursorIndexOfBirthDateMillis);
            }
            final int _tmpReminderMinuteOfDay;
            _tmpReminderMinuteOfDay = _cursor.getInt(_cursorIndexOfReminderMinuteOfDay);
            final boolean _tmpReminderEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp != 0;
            final int _tmpColorArgb;
            _tmpColorArgb = _cursor.getInt(_cursorIndexOfColorArgb);
            final long _tmpCreatedAtMillis;
            _tmpCreatedAtMillis = _cursor.getLong(_cursorIndexOfCreatedAtMillis);
            _result = new Project(_tmpId,_tmpName,_tmpFolderName,_tmpStartDateMillis,_tmpBirthDateMillis,_tmpReminderMinuteOfDay,_tmpReminderEnabled,_tmpColorArgb,_tmpCreatedAtMillis);
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
  public Object getAllReminderEnabled(final Continuation<? super List<Project>> $completion) {
    final String _sql = "SELECT * FROM projects WHERE reminderEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Project>>() {
      @Override
      @NonNull
      public List<Project> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFolderName = CursorUtil.getColumnIndexOrThrow(_cursor, "folderName");
          final int _cursorIndexOfStartDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateMillis");
          final int _cursorIndexOfBirthDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDateMillis");
          final int _cursorIndexOfReminderMinuteOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinuteOfDay");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfColorArgb = CursorUtil.getColumnIndexOrThrow(_cursor, "colorArgb");
          final int _cursorIndexOfCreatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMillis");
          final List<Project> _result = new ArrayList<Project>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Project _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFolderName;
            _tmpFolderName = _cursor.getString(_cursorIndexOfFolderName);
            final long _tmpStartDateMillis;
            _tmpStartDateMillis = _cursor.getLong(_cursorIndexOfStartDateMillis);
            final Long _tmpBirthDateMillis;
            if (_cursor.isNull(_cursorIndexOfBirthDateMillis)) {
              _tmpBirthDateMillis = null;
            } else {
              _tmpBirthDateMillis = _cursor.getLong(_cursorIndexOfBirthDateMillis);
            }
            final int _tmpReminderMinuteOfDay;
            _tmpReminderMinuteOfDay = _cursor.getInt(_cursorIndexOfReminderMinuteOfDay);
            final boolean _tmpReminderEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp != 0;
            final int _tmpColorArgb;
            _tmpColorArgb = _cursor.getInt(_cursorIndexOfColorArgb);
            final long _tmpCreatedAtMillis;
            _tmpCreatedAtMillis = _cursor.getLong(_cursorIndexOfCreatedAtMillis);
            _item = new Project(_tmpId,_tmpName,_tmpFolderName,_tmpStartDateMillis,_tmpBirthDateMillis,_tmpReminderMinuteOfDay,_tmpReminderEnabled,_tmpColorArgb,_tmpCreatedAtMillis);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
