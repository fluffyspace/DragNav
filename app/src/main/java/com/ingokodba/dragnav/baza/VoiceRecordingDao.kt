package com.ingokodba.dragnav.baza

import androidx.room.*
import com.ingokodba.dragnav.modeli.VoiceRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceRecordingDao {
    @Query("SELECT * FROM voice_recordings WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAll(): List<VoiceRecording>

    @Query("SELECT * FROM voice_recordings WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAllFlow(): Flow<List<VoiceRecording>>

    @Query("SELECT * FROM voice_recordings WHERE id = :id")
    fun findById(id: Int): VoiceRecording?

    @Query("SELECT * FROM voice_recordings WHERE id = :id")
    fun findByIdFlow(id: Int): Flow<VoiceRecording?>

    @Insert
    fun insert(recording: VoiceRecording): Long

    @Update
    fun update(recording: VoiceRecording)

    @Delete
    fun delete(recording: VoiceRecording)

    @Query("UPDATE voice_recordings SET isDeleted = 1 WHERE id = :id")
    fun softDelete(id: Int)

    @Query("DELETE FROM voice_recordings WHERE isDeleted = 1")
    fun permanentlyDeleteMarked()

    @Query("SELECT * FROM voice_recordings WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): VoiceRecording?
}
