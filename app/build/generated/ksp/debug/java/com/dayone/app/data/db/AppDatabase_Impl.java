package com.dayone.app.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ProjectDao _projectDao;

  private volatile PhotoEntryDao _photoEntryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `folderName` TEXT NOT NULL, `startDateMillis` INTEGER NOT NULL, `birthDateMillis` INTEGER, `reminderMinuteOfDay` INTEGER NOT NULL, `reminderEnabled` INTEGER NOT NULL, `colorArgb` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `photo_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `faceLeft` REAL, `faceTop` REAL, `faceRight` REAL, `faceBottom` REAL, `capturedAtMillis` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_photo_entries_projectId_dateEpochDay` ON `photo_entries` (`projectId`, `dateEpochDay`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '19e83802dbd73e57f05203dc5b7a3969')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `projects`");
        db.execSQL("DROP TABLE IF EXISTS `photo_entries`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsProjects = new HashMap<String, TableInfo.Column>(9);
        _columnsProjects.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("folderName", new TableInfo.Column("folderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("startDateMillis", new TableInfo.Column("startDateMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("birthDateMillis", new TableInfo.Column("birthDateMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("reminderMinuteOfDay", new TableInfo.Column("reminderMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("reminderEnabled", new TableInfo.Column("reminderEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("colorArgb", new TableInfo.Column("colorArgb", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProjects.put("createdAtMillis", new TableInfo.Column("createdAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProjects = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProjects = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProjects = new TableInfo("projects", _columnsProjects, _foreignKeysProjects, _indicesProjects);
        final TableInfo _existingProjects = TableInfo.read(db, "projects");
        if (!_infoProjects.equals(_existingProjects)) {
          return new RoomOpenHelper.ValidationResult(false, "projects(com.dayone.app.data.db.Project).\n"
                  + " Expected:\n" + _infoProjects + "\n"
                  + " Found:\n" + _existingProjects);
        }
        final HashMap<String, TableInfo.Column> _columnsPhotoEntries = new HashMap<String, TableInfo.Column>(9);
        _columnsPhotoEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("projectId", new TableInfo.Column("projectId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("dateEpochDay", new TableInfo.Column("dateEpochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("filePath", new TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("faceLeft", new TableInfo.Column("faceLeft", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("faceTop", new TableInfo.Column("faceTop", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("faceRight", new TableInfo.Column("faceRight", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("faceBottom", new TableInfo.Column("faceBottom", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhotoEntries.put("capturedAtMillis", new TableInfo.Column("capturedAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPhotoEntries = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPhotoEntries.add(new TableInfo.ForeignKey("projects", "CASCADE", "NO ACTION", Arrays.asList("projectId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPhotoEntries = new HashSet<TableInfo.Index>(1);
        _indicesPhotoEntries.add(new TableInfo.Index("index_photo_entries_projectId_dateEpochDay", true, Arrays.asList("projectId", "dateEpochDay"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoPhotoEntries = new TableInfo("photo_entries", _columnsPhotoEntries, _foreignKeysPhotoEntries, _indicesPhotoEntries);
        final TableInfo _existingPhotoEntries = TableInfo.read(db, "photo_entries");
        if (!_infoPhotoEntries.equals(_existingPhotoEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "photo_entries(com.dayone.app.data.db.PhotoEntry).\n"
                  + " Expected:\n" + _infoPhotoEntries + "\n"
                  + " Found:\n" + _existingPhotoEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "19e83802dbd73e57f05203dc5b7a3969", "e5653eabc24217bdd7a796bc262a3396");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "projects","photo_entries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `projects`");
      _db.execSQL("DELETE FROM `photo_entries`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ProjectDao.class, ProjectDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PhotoEntryDao.class, PhotoEntryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ProjectDao projectDao() {
    if (_projectDao != null) {
      return _projectDao;
    } else {
      synchronized(this) {
        if(_projectDao == null) {
          _projectDao = new ProjectDao_Impl(this);
        }
        return _projectDao;
      }
    }
  }

  @Override
  public PhotoEntryDao photoEntryDao() {
    if (_photoEntryDao != null) {
      return _photoEntryDao;
    } else {
      synchronized(this) {
        if(_photoEntryDao == null) {
          _photoEntryDao = new PhotoEntryDao_Impl(this);
        }
        return _photoEntryDao;
      }
    }
  }
}
